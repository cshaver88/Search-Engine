import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Creates Threads to perform work from the WorkQueue
 * 
 * @author CaylaR
 * 
 */
public class MultithreadedQueryParser {

	private static final Logger logger = LogManager.getLogger();
	private final MultiReaderLock lock;

	private final LinkedHashMap<String, List<SearchResult>> result;
	private final WorkQueue minions;
	private int pending;

	/**
	 * Constructor for the Thread Class. Instantiates the WorkQueue to be used
	 * and how many threads to use is a parameter.
	 * 
	 * @param index
	 * @param threads
	 */
	public MultithreadedQueryParser(InvertedIndex index, int threads) {
		minions = new WorkQueue(threads);
		pending = 0;
		result = new LinkedHashMap<>();
		lock = new MultiReaderLock();

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
	 * Sends the minions to start executing work from the WorkQueue
	 * 
	 * @param directory
	 */
	public void runThreads(String file, InvertedIndex index) {
		minions.execute(new Minion(file, index));
	}

	/**
	 * Parses the file into cleaned words.
	 * 
	 * @param filename
	 *            - path to take in
	 */
	public void queryParser(Path filename, InvertedIndex index) {

		try (BufferedReader reader = Files.newBufferedReader(filename,
				Charset.forName("UTF-8"));) {
			String line = null;
			while ((line = reader.readLine()) != null) {
				lock.lockWrite();
				result.put(line, null);
				lock.unlockWrite();
				minions.execute(new Minion(line, index));
			}
		} catch (IOException e) {
			System.err.println("An error has occurred with your file!!"
					+ filename);
		}
	}

	/**
	 * Outputs search results to file.
	 * 
	 * @param output
	 *            - path to output file
	 */
	public void write(String output) {

		lock.lockRead();
		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(output),
				Charset.forName("UTF-8"))) {
			for (String word : result.keySet()) {

				writer.write(word);
				writer.newLine();
				for (SearchResult searchresult : result.get(word)) {
					writer.write(searchresult.toString());
					writer.newLine();
				}
				writer.newLine();
			}
		} catch (IOException e) {
			System.err.println("No File Written");
		}
		lock.unlockRead();
	}

	/**
	 * Take new thread and send it to do a partial search and output to a file
	 * 
	 * @author CaylaR
	 * 
	 */
	private class Minion implements Runnable {

		private final String line;
		private final InvertedIndex indexmap;

		public Minion(String line, InvertedIndex index) {
			logger.debug("Minion created for {}", line);
			this.line = line;
			indexmap = index;
			incrementPending();
		}

		@Override
		public void run() {
			List<String> querylist = WordParser.parseText(line);
			List<SearchResult> searchResult;

			searchResult = indexmap.partialSearch(querylist);

			lock.lockWrite();
			result.put(line, searchResult);
			lock.unlockWrite();
			decrementPending();
			logger.debug("Minion finished {}", line);
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