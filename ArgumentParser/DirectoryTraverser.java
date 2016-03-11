import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Directory Traverser traverses a directory
 * 
 * @CaylaR
 */
public class DirectoryTraverser {

	/**
	 * 
	 * Recursively traverses a path and creates a list of files.
	 * 
	 * @see #traverse(Path, List)
	 * 
	 * @param path
	 * @return list of files
	 */
	public static List<String> traverse(Path path) {
		List<String> files = new ArrayList<>();
		traverse(path, files);
		return files;
	}

	/**
	 * Recursively traverses a list and creates a list of string paths ending in
	 * .txt.
	 * 
	 * @param path
	 * @param list
	 */
	public static void traverse(Path path, List<String> list) {
		/*
		 * The try-with-resources block makes sure the directory stream gets
		 * closed when its done, to make sure there aren't any issues later when
		 * accessing this directory.
		 */

		if (Files.isDirectory(path)) {

			try (DirectoryStream<Path> listing = Files.newDirectoryStream(path)) {
				// Efficiently iterate through the files and sub-directories.
				for (Path file : listing) {
					// Print the name with the proper format.
					if (!Files.isDirectory(file)) {
						if (file.toString().toLowerCase().endsWith(".txt")) {
							list.add(file.toString());
						}
					} else {
						// recursively iterates through the rest of the
						// directory.
						traverse(file, list);
					}
				}
			} catch (IOException e) {
				System.out.println("Bad Directory:  " + path);
			}
		}
	}
}