import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;

// TODO No longer makes sense to have two separate classes,
// combine HTTPFetcher and HTMLFetcher

/**
 * An abstract class designed to make fetching the results of different HTTP
 * operations easier.
 * 
 * @see HTTPFetcher
 * @see HTMLFetcher
 * @see HeaderFetcher
 */
public abstract class HTTPFetcher {
	/** Port used by socket. For web servers, should be port 80. */
	private static final int PORT = 80;

	/** The URL to fetch from a web server. */
	private final URL url;

	/**
	 * Initializes this fetcher. Must call {@link #fetch()} to actually start
	 * the process.
	 * 
	 * @param url
	 *            - the link to fetch from the webserver
	 * @throws MalformedURLException
	 *             if unable to parse URL
	 */
	public HTTPFetcher(String url) throws MalformedURLException {
		this.url = new URL(url);
	}

	/**
	 * Returns the port being used to fetch URLs.
	 * 
	 * @return port number
	 */
	public int getPort() {
		return PORT;
	}

	/**
	 * Returns the URL being used by this fetcher.
	 * 
	 * @return URL
	 */
	public URL getURL() {
		return url;
	}

	/**
	 * Crafts the HTTP request from the URL. Must be overridden.
	 * 
	 * @return HTTP request
	 */
	protected abstract String craftRequest();

	/**
	 * Handles each line fetched from the web server. May be overridden.
	 * 
	 * @param line
	 *            - text retrieved from web server
	 * @return
	 */
	protected String processLine(String line) {
		return line;
	}

	/**
	 * Will connect to the web server and fetch the URL using the HTTP request
	 * from {@link #craftRequest()}, and then call {@link #processLine(String)}
	 * on each of the returned lines.
	 */
	public String fetch() {
		System.out.println("Server: " + url.getHost() + ":" + PORT);

		StringBuffer buffer = new StringBuffer();

		try (Socket socket = new Socket(url.getHost(), PORT);
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(socket.getInputStream()));
				PrintWriter writer = new PrintWriter(socket.getOutputStream());) {
			String request = craftRequest();
			System.out.println("HTTP: " + request);

			writer.println(request);
			writer.flush();

			// TODO Including headers in the HTML
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (line.trim().isEmpty()) {
					break;
				}
			}
			
//			String line = reader.readLine();

//			while (line != null) {
			while ((line = reader.readLine()) != null) {
				line = processLine(line);
				buffer.append(line);
//				line = reader.readLine();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return buffer.toString();
	}
}