import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This class checks the command line arguments and if the proper command-line
 * arguments are provided then execution of other classes is commenced. If they
 * are not provided, this class outputs a user-friendly message.
 * 
 * 
 * @author CaylaR
 * 
 */
public class Driver {

	/**
	 * Checks if there are any directory flags and a directory value following
	 * it. The directory is then recursively parsed to locate all .txt files
	 * within that directory and add them to a list. If there is no directory
	 * passed in then a no directory error is given. It also checks for the -t
	 * flag to tell the InvertedIndex how many threads to use for
	 * multi-threading. If no value is specified the default value of 5 is used.
	 * If no -t is given the InvertedIndex continues to work using a single
	 * thread.This list of files is then gone through and each file is passed to
	 * the WordParser. The WordParser takes the path and cleans and separates
	 * all of the words within the file and adds them to a list of words. This
	 * list of words and the file path associated with it are then passed into
	 * the InvertedIndex. The InvertedIndex then takes that information and adds
	 * all the words and file paths associated with them and add them into a
	 * HashMap. For each word in each file path there is an array list that is
	 * populated with the location in the file for each word. Also checks if
	 * there is a file to output the new HashMap into. If no output file is
	 * specified index.txt is created for the output. The InvertedIndex then
	 * takes the HashMap and writes it to an output file. Also checks for the
	 * query file and if there is a query file given goes to perform a search
	 * for the piece of word specified. Then checks if there is a result file
	 * requested then outputs the results from the search into that text file.
	 * 
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		ArgumentParser myargs = new ArgumentParser(args);

		InvertedIndex inv = new InvertedIndex();

		QueryParser parser = new QueryParser();

		Path input, output = null, queryPath, queryoutput = null;

		int queue = 5;
		
		// TODO Move this here
		if (myargs.hasFlag("-u")) {
			if (myargs.getValue("-u") != null) {
				WebCrawler crawler = new WebCrawler(inv, queue);
				crawler.crawl(myargs.getValue("-u"));
				crawler.shutdown();
			}
		}

		if (myargs.hasFlag("-t")) {
			if (myargs.getValue("-t") != null) {
				try {
					queue = Integer.parseInt(myargs.getValue("-t"));
					if (queue != 0) {
						MultithreadedInvertedIndexBuilder thread = new MultithreadedInvertedIndexBuilder(
								inv, queue);
						if (myargs.hasValue("-d")) {
							input = Paths.get(myargs.getValue("-d"));
							thread.addMainInvertedIndex(input);
							thread.shutdown();
							MultithreadedQueryParser queryThread = new MultithreadedQueryParser(
									inv, queue);
							queryThread.runThreads(input.toString(), inv);
							queryThread.shutdown();
						}
					} else {
						System.err
								.println("You have entered a 0 as a number of threads this is Unacceptable!");
					}
				} catch (NumberFormatException e) {
					System.err.println("Unacceptable " + e);
				}
			} else {
				MultithreadedInvertedIndexBuilder thread = new MultithreadedInvertedIndexBuilder(
						inv, queue);
				if (myargs.hasValue("-d")) {
					input = Paths.get(myargs.getValue("-d"));
					thread.addMainInvertedIndex(input);
					thread.shutdown();
					MultithreadedQueryParser queryThread = new MultithreadedQueryParser(
							inv, queue);
					queryThread.runThreads(input.toString(), inv);
					queryThread.shutdown();
				}
			}
		}

		else if (myargs.hasFlag("-d")) {
			if (myargs.hasValue("-d")) {
				input = Paths.get(myargs.getValue("-d"));

				IndexBuilder.build(input, inv);
			} else {
				System.out.println("There was no Directory entered.");
//				if (myargs.hasFlag("-u")) {
//					if (myargs.getValue("-u") != null) {
//						WebCrawler crawler = new WebCrawler(inv, queue);
//						crawler.crawl(myargs.getValue("-u"));
//						crawler.shutdown();
//					}
//				}
			}
		}

		else {
			System.out.println("There was no -d Flag entered.");
		}
		if (myargs.hasFlag("-i")) {
			if (myargs.getValue("-i") != null) {
				output = Paths.get(myargs.getValue("-i"));
			} else {
				output = Paths.get("index.txt");
			}
			inv.outputInvertedIndex(output);
		} else {
			System.out.println("No Output.");
		}

		if (myargs.hasFlag("-q")) {
			if (myargs.getValue("-q") != null) {
				queryPath = Paths.get(myargs.getValue("-q"));

				parser.queryParser(queryPath, inv);
			} else {
				System.out.println("There was no query file entered.");
			}
		} else {
			System.out.println("There was no -q Flag entered.");
		}

		if (myargs.hasFlag("-r")) {
			if (myargs.getValue("-r") != null) {
				queryoutput = Paths.get(myargs.getValue("-r"));
			} else {
				queryoutput = Paths.get("results.txt");
			}
			parser.write(queryoutput.toString());
		} else {
			System.out.println("There was no result file requested.");
		}
	}
}