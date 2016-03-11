import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Crawls the Web to search for relevant links
 * 
 * @author CaylaR
 * 
 */
public class WebCrawler {

	private final Logger logger = LogManager
			.getLogger(MultithreadedInvertedIndexBuilder.class);
	private final WorkQueue minions;
	private int pending;
	private final InvertedIndex indexmap;
	private final MultiReaderLock lock;
	private final HashSet<String> links = new HashSet<>(); // TODO Initialize in the constructor

	/**
	 * Constructor for the WebCrawler
	 * 
	 * @param index
	 * @param threads
	 */
	public WebCrawler(InvertedIndex index, int threads) {
		indexmap = index;
		lock = new MultiReaderLock();
		pending = 0;
		if (threads != 0) {
			minions = new WorkQueue(threads);
		} else {
			minions = new WorkQueue();
		}
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
	 * @return
	 */
	private void parse(String url, String basestring) {
		List<String> wordList = new ArrayList<>();
		List<String> urlList = new ArrayList<>();
		try {
			URL base = new URL(basestring);
			URL absoluteURL = new URL(base, url);
			logger.debug("Absolute: {}", absoluteURL.toString());
			
			HTMLFetcher fetcher = new HTMLFetcher(absoluteURL.toString());
			String link = fetcher.fetch();
			
			logger.debug("Fetched html for {}", url);
			urlList = HTMLLinkParser.listLinks(link);
			logger.debug("Got links for {}", url);
			
			lock.lockWrite();
			for (String newLink : urlList) {
				URL temporary = new URL(base, newLink);
				if (!this.links.contains(temporary.getPath())
						&& (temporary.getPath() != null)) {
					if (this.links.size() < 50) {
						this.links.add(temporary.getPath());
						this.minions.execute(new Minion(temporary.getPath(),
								basestring));
					}
				}
			}
			lock.unlockWrite();
			link = HTMLCleaner.cleanHTML(link);
			wordList = HTMLCleaner.parseWords(link);
			addWordsToIndex(wordList, absoluteURL.toString());

		} catch (MalformedURLException e) {
			System.out.println("URL is not valid");
		}
	}

	// TODO Make private?
	/**
	 * adds words to the indexmap
	 * 
	 * @param wordList
	 * @param url
	 */
	public void addWordsToIndex(List<String> wordList, String url) {
		InvertedIndex local = new InvertedIndex();
		for (int i = 0; i < wordList.size(); i++) {
			// TODO Just add url, Paths.get(url) might not be what you expect
//			local.add(wordList.get(i), Paths.get(url), i + 1);
			local.add(wordList.get(i), url, i + 1);
			logger.debug("Added {} to local", wordList.get(i));
		}
		indexmap.addAll(local);
	}

	/**
	 * sends minions to start crawling the links
	 * 
	 * @param link
	 */
	public void crawl(String link) {

		lock.lockWrite();
		this.links.add(link);
		lock.unlockWrite();
		this.minions.execute(new Minion(link, link));

	}

	/**
	 * Traverses a url link and creates a single Minion for each found url
	 * 
	 * @author CaylaR
	 * 
	 */
	private class Minion implements Runnable {

		String link, base;

		public Minion(String link, String base) {
			logger.debug("Minion created for {}", link);
			this.link = link;
			this.base = base;
			incrementPending();
		}

		@Override
		public void run() {
			parse(link, base);
			decrementPending();
			logger.debug("Minion finished {}", link);
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