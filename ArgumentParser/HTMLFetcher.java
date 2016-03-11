import java.net.MalformedURLException;

/**
 * A class designed to make fetching the results of different HTTP operations
 * easier. This particular class handles the GET operation.
 * 
 * @see HTTPFetcher
 * @see HTMLFetcher
 * @see HeaderFetcher
 */
public class HTMLFetcher extends HTTPFetcher {
	/** Used to determine if headers have been read. */
	private boolean head;

	/**
	 * Initializes this fetcher. Must call {@link #fetch()} to actually start
	 * the process.
	 * 
	 * @param url
	 *            - the link to fetch from the webserver
	 * @throws MalformedURLException
	 *             if unable to parse URL
	 */
	public HTMLFetcher(String url) throws MalformedURLException {
		super(url);
		head = true;
	}

	/**
	 * Crafts the HTTP GET request from the URL.
	 * 
	 * @return HTTP request
	 */
	@Override
	protected String craftRequest() {
		String host = this.getURL().getHost();
		String resource = this.getURL().getFile().isEmpty() ? "/" : this
				.getURL().getFile();

		StringBuffer output = new StringBuffer();
		output.append("GET " + resource + " HTTP/1.1\n");
		output.append("Host: " + host + "\n");
		output.append("Connection: close\n");
		output.append("\r\n");

		return output.toString();
	}

	/**
	 * Will skip any headers returned by the web server, and then output each
	 * line of HTML to the console.
	 */
	@Override
	protected String processLine(String line) {
		if (head) {
			// Check if we hit the blank line separating headers and HTML
			if (line.trim().isEmpty()) {
				head = false;
			}
		} else {
			System.out.println(line);
		}
		return line;
	}

	public static void main(String[] args) throws MalformedURLException {
		new HTMLFetcher("http://www.cs.usfca.edu/~sjengle/archived.html")
				.fetch();
	}
}