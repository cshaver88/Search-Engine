import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * This class adds items to an InvertedIndex
 * 
 * @author CaylaR
 * 
 */
public class IndexBuilder {

	/**
	 * Build takes in a directory and the InvertedIndex to traverse the list of
	 * files
	 * 
	 * @see parseFile
	 * @param dir
	 * @param indexmap
	 */
	public static void build(Path dir, InvertedIndex indexmap) {

		List<String> filelist = DirectoryTraverser.traverse(dir);

		for (String file : filelist) {
			parseFile(Paths.get(file), indexmap);
		}
	}

	/**
	 * ParseFile takes in the string version of a filename and the InvertedIndex
	 * and reads all of the information from that file
	 * 
	 * @see build
	 * @param file
	 * @param indexmap
	 */
	public static void parseFile(Path file, InvertedIndex indexmap) {
		try (BufferedReader reader = Files.newBufferedReader(
				Paths.get(file.toString()), Charset.forName("UTF-8"));) {
			String line = null;
			Integer position = 0;
			while ((line = reader.readLine()) != null) {
				String list = WordParser.cleanText(line);
				String[] newtext = list.split("\\s");
				for (String item : newtext) {
					if (!item.isEmpty()) {
						position++;
						indexmap.add(item, file, position);
					}
				}
			}
		} catch (IOException e) {
			System.err
					.println("An error has occurred with your file!! " + file);
		}

	}
}