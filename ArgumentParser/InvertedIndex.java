import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

/**
 * This class stores and writes information to a text file.
 * 
 * @author CaylaR
 * 
 */

public class InvertedIndex {

	private final TreeMap<String, TreeMap<String, ArrayList<Integer>>> indexmap;
	private MultiReaderLock lock;

	public InvertedIndex() {
		indexmap = new TreeMap<>();
		lock = new MultiReaderLock();
	}

	/**
	 * Takes the main InvertedIndex and the mini thread InvertedIndex and adds
	 * all of the minis to the main InvertedIndex. Uses the, MultiReaderLock
	 * class, lock to keep all information protected.
	 * 
	 * @param mini
	 */
	public void addAll(InvertedIndex mini) {
		lock.lockWrite();
		for (String key : mini.indexmap.keySet()) {
			if (this.indexmap.containsKey(key) == false) {
				this.indexmap.put(key, mini.indexmap.get(key));
			} else {
				for (String path : mini.indexmap.get(key).keySet()) {
					if (this.indexmap.get(key).containsKey(path) == false) {
						this.indexmap.get(key).put(path,
								mini.indexmap.get(key).get(path));
					} else {
						this.indexmap.get(key).get(path)
								.addAll(mini.indexmap.get(key).get(path));
					}
				}
			}
		}
		lock.unlockWrite();
	}

	/**
	 * Creates a TreeMap with all parameters see @param. Uses the,
	 * MultiReaderLock class, lock to keep all information protected.
	 * 
	 * @param word
	 * @param path
	 * @param location
	 */
	public void add(String word, Path path, Integer location) {

		add(word, path.toAbsolutePath().normalize().toString(), location);
		// TODO Call add(word, path.toString(), location)
		
//		lock.lockWrite();
//		if (!indexmap.containsKey(word)) {
//			indexmap.put(word, new TreeMap<String, ArrayList<Integer>>());
//		}
//
//		if (!indexmap.get(word).containsKey(
//				path.toAbsolutePath().normalize().toString())) {
//			indexmap.get(word).put(
//					path.toAbsolutePath().normalize().toString(),
//					new ArrayList<Integer>());
//		}
//
//		indexmap.get(word).get(path.toAbsolutePath().normalize().toString())
//				.add(location);
//		lock.unlockWrite();
	}

	// TODO Add this
	public void add(String word, String path, Integer location) {
		lock.lockWrite();
		if (!indexmap.containsKey(word)) {
			indexmap.put(word, new TreeMap<String, ArrayList<Integer>>());
		}

		if (!indexmap.get(word).containsKey(path)) {
			indexmap.get(word).put(path,
					new ArrayList<Integer>());
		}

		indexmap.get(word).get(path)
				.add(location);
		lock.unlockWrite();
	}
	
	/**
	 * Adds all of the WordParser words list to a new TreeMap. Uses the,
	 * MultiReaderLock class, lock to keep all information protected.
	 * 
	 * @param listofwords
	 * @param filename
	 */
	public void storeInfo(List<String> listofwords, Path filename) {

		for (int i = 0; i <= listofwords.size() - 1; i++) {
			add(listofwords.get(i), filename, i + 1);
		}
	}

	/**
	 * Writes all of the information from the TreeMap and writes it to an output
	 * file. Uses the, MultiReaderLock class, lock to keep all information
	 * protected.
	 * 
	 * @param output
	 */
	public void outputInvertedIndex(Path output) {

		lock.lockRead();
		try (BufferedWriter writer = Files.newBufferedWriter(output,
				Charset.forName("UTF-8"))) {
			for (String word : indexmap.keySet()) {
				writer.write(word.toString());
				writer.newLine();

				for (String path : indexmap.get(word).keySet()) {
					writer.write("\"" + path.toString() + "\"");
					for (Integer location : indexmap.get(word).get(path)) {
						writer.write(", " + location.toString());
					}
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
	 * 
	 * Walks through all the keys of a TreeMap and searches for queries. Uses
	 * the, MultiReaderLock class, lock to keep all information protected.
	 * 
	 * @param queries
	 *            - list of queries from a file
	 * @return list of searchresults
	 */
	public List<SearchResult> partialSearch(List<String> queries) {

		lock.lockRead();

		HashMap<String, SearchResult> map = new HashMap<>();

		int frequency;
		int position;

		for (String query : queries) {
			for (String word : indexmap.tailMap(query).keySet()) {
				if (word.startsWith(query)) {
					for (String path : indexmap.get(word).keySet()) {
						if (map.containsKey(path)) {

							SearchResult result = map.get(path);
							frequency = indexmap.get(word).get(path).size();
							position = indexmap.get(word).get(path).get(0);
							result.update(frequency, position);
						} else {

							frequency = indexmap.get(word).get(path).size();
							position = indexmap.get(word).get(path).get(0);
							SearchResult result = new SearchResult(frequency,
									position, path);
							map.put(path, result);
						}

					}
				} else {
					break;
				}
			}
		}
		lock.unlockRead();
		List<SearchResult> searchResults = new ArrayList<>();
		searchResults.addAll(map.values());
		Collections.sort(searchResults);
		return searchResults;
	}
}