import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Creates Threads to perform work from the WorkQueue
 * 
 * @author CaylaR
 * 
 */
public class MultithreadedInvertedIndexBuilder extends IndexBuilder {

	private static final Logger logger = LogManager.getLogger();
	private final InvertedIndex indexmap;

	private final WorkQueue minions;
	private int pending;

	/**
	 * Constructor for the Thread Class. Instantiates the WorkQueue to be used
	 * and how many threads to use is a parameter.
	 * 
	 * @param index
	 * @param threads
	 */
	public MultithreadedInvertedIndexBuilder(InvertedIndex index, int threads) {
		minions = new WorkQueue(threads);
		pending = 0;
		indexmap = index;
	}

	/**
	 * Helper method, that helps a thread wait until all of the current work is
	 * done. This is useful for resetting the counters or shutting down the work
	 * queue.
	 */
	public synchronized void finish() {
		try {
			while (pending > 0) {
				logger.debug("Waiting until finished");
				this.wait();
			}
		} catch (InterruptedException e) {
			logger.debug("Finish interrupted", e);
		}
	}

	/**
	 * Will shutdown the work queue after all the current pending work is
	 * finished. Necessary to prevent our code from running forever in the
	 * background.
	 */
	public synchronized void shutdown() {
		logger.debug("Shutting down");
		finish();
		minions.shutdown();
	}

	/**
	 * Sends the minions to start excecuting work from the WorkQueue
	 * 
	 * @param directory
	 */
	public void addMainInvertedIndex(Path directory) {

		try {
			for (Path path : Files.newDirectoryStream(directory)) {
				if (Files.isDirectory(path)) {
					addMainInvertedIndex(path);
				} else if (path.toString().toLowerCase().endsWith(".txt")) {
					minions.execute(new Minion(path));
				}
			}
		} catch (IOException e) {
			System.err
					.println("There was a problem with your specified directory. "
							+ e);
		}
	}

	/**
	 * Traverses a directory and creates a single Minion for each .txt file to
	 * create their own mini InvertedIndex to add to the main InvertedIndex.
	 * 
	 * @author CaylaR
	 * 
	 */
	private class Minion implements Runnable {

		private final Path path;

		public Minion(Path directory) {
			logger.debug("Minion created for {}", directory);
			this.path = directory;
			incrementPending();
		}

		@Override
		public void run() {
			// where a mini InvertedIndex is created for each single thread
			if (path.toString().toLowerCase().endsWith(".txt")) {
				InvertedIndex mini = new InvertedIndex();
				IndexBuilder.parseFile(path, mini);
				indexmap.addAll(mini);
			}
			decrementPending();
			logger.debug("Minion finished {}", path);
		}
	}

	/**
	 * Indicates that we now have additional "pending" work to wait for.
	 */
	private synchronized void incrementPending() {
		pending++;
		logger.debug("Pending is now {}", pending);
	}

	/**
	 * Indicates that we now have one less "pending" work, and will notify any
	 * waiting threads if we no longer have any more pending work left.
	 */
	private synchronized void decrementPending() {
		pending--;
		logger.debug("Pending is now {}", pending);

		if (pending <= 0) {
			this.notifyAll();
		}
	}
}