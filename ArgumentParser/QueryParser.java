import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * QueryHelper is a class created to parse through a file of queries
 * 
 * @author CaylaR
 * 
 */
public class QueryParser {

	private final LinkedHashMap<String, List<SearchResult>> result;

	public QueryParser() {
		result = new LinkedHashMap<>();
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

				// line = WordParser.cleanText(line);
				// String[] listofqueries = line.split("\\s");
				// List<String> querylist = new ArrayList<>();
				// for (String item : listofqueries) {
				// querylist.add(item);
				// }

				List<String> querylist = WordParser.parseText(line);

				result.put(line, index.partialSearch(querylist));

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
	}
}