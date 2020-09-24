import java.io.BufferedOutputStream;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class PartialHTTP1Server implements Runnable{

	static final File WEB_ROOT = new File(".");
	static final String DEFAULT_FILE = "index.html";
	//static final String FILE_NOT_FOUND = "404.html";
	//static final String METHOD_NOT_SUPPORTED = "not_supported.html";
	// port to listen connection
	//static final int PORT = 8000;

	// verbose mode
	static final boolean verbose = true;

	// Client Connection via Socket Class
	private Socket connect;

	static private ThreadPoolExecutor pool;

	public PartialHTTP1Server(Socket c) {
		connect = c;
	}

	public static void main(String[] args) {
		try {
			ServerSocket serverConnect = new ServerSocket(Integer.parseInt(args[0]));
			System.out.println("Server started.\nListening for connections on port : " + Integer.parseInt(args[0])+ " ...\n");
			pool = (ThreadPoolExecutor)Executors.newFixedThreadPool(5);
			// we listen until user halts server execution
			while (true) {
				PartialHTTP1Server myServer = new PartialHTTP1Server(serverConnect.accept());

				if (verbose) {
					System.out.println("Connecton opened. (" + new Date() + ")");
				}

				// create dedicated thread to manage the client connection
				Thread thread = new Thread(myServer);

				pool.execute(thread);


			}

		} catch (IOException e) {
			System.err.println("Server Connection error : " + e.getMessage());
		}
	}

	@Override
	public void run() {
		// we manage our particular client connection
		BufferedReader in = null; PrintWriter out = null; BufferedOutputStream dataOut = null;
		String fileRequested = null;

		/*try
		{
			Thread.sleep(5000);
		}
		catch(InterruptedException ex)
		{
			Thread.currentThread().interrupt();
		}*/

		try {

			// we read characters from the client via input stream on the socket
			in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
			// we get character output stream to client (for headers)
			out = new PrintWriter(connect.getOutputStream());
			// get binary output stream to client (for requested data)
			dataOut = new BufferedOutputStream(connect.getOutputStream());

			// get first line of the request from the client
			String input = in.readLine();
			// we parse the request with a string tokenizer
			StringTokenizer parse = new StringTokenizer(input);

			//Request most have 3 parts
			if(parse.countTokens()!=3){
				File file = new File(WEB_ROOT, DEFAULT_FILE);
				int fileLength = (int) file.length();
				String contentMimeType = "text/html";
				//read content to return to client
				byte[] fileData = readFileData(file, fileLength);

				// we send HTTP Headers with data to client
				out.println("HTTP/1.0 400 Bad Request\r\n");
				out.println("\r\n");
				out.println("Server: Java HTTP Server\r\n");
				out.println("\r\n");
				Date localtime = new Date();
				DateFormat converter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
				converter.setTimeZone(TimeZone.getTimeZone("GMT"));
				out.println("Date: " + converter.format(localtime) + " GMT\r\n");
				out.println("\r\n");
				out.println("Server: Apache/1.3.27 (Unix)\r\n");
				out.println("\r\n");
				out.println("MIME-version: 1.0\r\n");
				out.println("\r\n");
				long lastModified = file.lastModified();
				Date modified  = new Date(lastModified);
				DateFormat converter2 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
				converter2.setTimeZone(TimeZone.getTimeZone("GMT"));
				out.println("Last-Modified: " + converter2.format(modified) + " GMT\r\n");
				out.println("\r\n");
				out.println("Content-type: " + contentMimeType + "\r\n");
				out.println("\r\n");
				out.println("Content-length: " + fileLength + "\r\n");
				out.println("\r\n");
				out.println(); // blank line between headers and content, very important !
				out.flush(); // flush character output stream buffer
				// file
				dataOut.write(fileData, 0, fileLength);
				dataOut.flush();
				return;
			}

			String method = parse.nextToken(); // we get the HTTP method of the client
			// we get file requested
			fileRequested = parse.nextToken();


			// we support only GET, HEAD, and POST methods, we check
			if (!method.equals("GET")  &&  !method.equals("HEAD") &&  !method.equals("POST")) {
				//not implemented requests
				if(method.equals("PUT") || method.equals("DELETE")) {
					if (verbose) {
						System.out.println("501 Not Implemented : " + method + " method.");
					}

					// we return the not supported file to the client
					File file = new File(WEB_ROOT, DEFAULT_FILE);
					int fileLength = (int) file.length();
					String contentMimeType = "text/html";
					//read content to return to client
					byte[] fileData = readFileData(file, fileLength);

					// we send HTTP Headers with data to client
					out.println("HTTP/1.0 501 Not Implemented\r\n");
					out.println("\r\n");
					out.println("Server: Java HTTP Server from SSaurel : 1.0\r\n");
					out.println("\r\n");
					Date localtime = new Date();
					DateFormat converter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
					converter.setTimeZone(TimeZone.getTimeZone("GMT"));
					out.println("Date: " + converter.format(localtime) + " GMT\r\n");
					out.println("\r\n");
					out.println("Server: Apache/1.3.27 (Unix)\r\n");
					out.println("\r\n");
					out.println("MIME-version: 1.0\r\n");
					out.println("\r\n");
					long lastModified = file.lastModified();
					Date modified = new Date(lastModified);
					DateFormat converter2 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
					converter2.setTimeZone(TimeZone.getTimeZone("GMT"));
					out.println("Last-Modified: " + converter2.format(modified) + " GMT\r\n");
					out.println("\r\n");
					out.println("Content-type: " + contentMimeType + "\r\n");
					out.println("\r\n");
					out.println("Content-length: " + fileLength + "\r\n");
					out.println("\r\n");
					out.println(); // blank line between headers and content, very important !
					out.flush(); // flush character output stream buffer
					// file
					dataOut.write(fileData, 0, fileLength);
					dataOut.flush();
				}else{//non-existent http requests
					File file = new File(WEB_ROOT, DEFAULT_FILE);
					int fileLength = (int) file.length();
					String contentMimeType = "text/html";
					//read content to return to client
					byte[] fileData = readFileData(file, fileLength);

					// we send HTTP Headers with data to client
					out.println("HTTP/1.0 400 Bad Request\r\n");
					out.println("\r\n");
					out.println("Server: Java HTTP Server\r\n");
					out.println("\r\n");
					Date localtime = new Date();
					DateFormat converter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
					converter.setTimeZone(TimeZone.getTimeZone("GMT"));
					out.println("Date: " + converter.format(localtime) + " GMT\r\n");
					out.println("\r\n");
					out.println("Server: Apache/1.3.27 (Unix)\r\n");
					out.println("\r\n");
					out.println("MIME-version: 1.0\r\n");
					out.println("\r\n");
					long lastModified = file.lastModified();
					Date modified  = new Date(lastModified);
					DateFormat converter2 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
					converter2.setTimeZone(TimeZone.getTimeZone("GMT"));
					out.println("Last-Modified: " + converter2.format(modified) + " GMT\r\n");
					out.println("\r\n");
					out.println("Content-type: " + contentMimeType + "\r\n");
					out.println("\r\n");
					out.println("Content-length: " + fileLength + "\r\n");
					out.println("\r\n");
					out.println(); // blank line between headers and content, very important !
					out.flush(); // flush character output stream buffer
					// file
					dataOut.write(fileData, 0, fileLength);
					dataOut.flush();
				}

			} else {
				// GET or HEAD method
				if (fileRequested.endsWith("/")) {
					fileRequested += DEFAULT_FILE;
				}

				File file = new File(WEB_ROOT, fileRequested);
				int fileLength = (int) file.length();
				String content = getContentType(fileRequested);

				if (method.equals("GET")) { // GET method so we return content
					byte[] fileData = readFileData(file, fileLength);
					// send HTTP Headers
					out.println("HTTP/1.0 200 OK");
					Date localtime = new Date();
					DateFormat converter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
					converter.setTimeZone(TimeZone.getTimeZone("GMT"));
					out.println("Date: " + converter.format(localtime) + " GMT");
					out.println("Server: Apache/1.3.27 (Unix)");
					out.println("MIME-version: 1.0");
					long lastModified = file.lastModified();
					Date modified  = new Date(lastModified);
					DateFormat converter2 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
					converter2.setTimeZone(TimeZone.getTimeZone("GMT"));
					out.println("Last-Modified: " + converter2.format(modified) + " GMT");
					out.println("Content-type: " + content);
					out.println("Content-length: " + fileLength);
					out.println(); // blank line between headers and content, very important !
					out.flush(); // flush character output stream buffer

					dataOut.write(fileData, 0, fileLength);
					dataOut.flush();
				}

				if (verbose) {
					System.out.println("File " + fileRequested + " of type " + content + " returned");
				}

			}

		} catch (FileNotFoundException fnfe) {
			try {
				fileNotFound(out, dataOut, fileRequested);
			} catch (IOException ioe) {
				System.err.println("Error with file not found exception : " + ioe.getMessage());
			}

		} catch (IOException ioe) {
			System.err.println("Server error : " + ioe);
		} finally {
			try {
				in.close();
				out.close();
				dataOut.close();
				connect.close(); // we close socket connection
			} catch (Exception e) {
				System.err.println("Error closing stream : " + e.getMessage());
			}

			if (verbose) {
				System.out.println("Connection closed.\n");
			}
		}


	}

	private byte[] readFileData(File file, int fileLength) throws IOException {
		FileInputStream fileIn = null;
		byte[] fileData = new byte[fileLength];

		try {
			fileIn = new FileInputStream(file);
			fileIn.read(fileData);
		} finally {
			if (fileIn != null)
				fileIn.close();
		}

		return fileData;
	}

	// return supported MIME Types
	private String getContentType(String fileRequested) {
		if (fileRequested.endsWith(".htm")  ||  fileRequested.endsWith(".html"))
			return "text/html";
		else
			return "text/plain";
	}

	private void fileNotFound(PrintWriter out, OutputStream dataOut, String fileRequested) throws IOException {
		File file = new File(WEB_ROOT, DEFAULT_FILE);
		int fileLength = (int) file.length();
		String content = "text/html";
		byte[] fileData = readFileData(file, fileLength);

		out.println("HTTP/1.0 400 Bad Request");
		Date localtime = new Date();
		DateFormat converter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
		converter.setTimeZone(TimeZone.getTimeZone("GMT"));
		out.println("Date: " + converter.format(localtime) + " GMT");
		out.println("Server: Apache/1.3.27 (Unix)");
		out.println("MIME-version: 1.0");
		long lastModified = file.lastModified();
		Date modified  = new Date(lastModified);
		DateFormat converter2 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
		converter2.setTimeZone(TimeZone.getTimeZone("GMT"));
		out.println("Last-Modified: " + converter2.format(modified) + " GMT");
		out.println("Content-type: " + content);
		out.println("Content-length: " + fileLength);
		out.println(); // blank line between headers and content, very important !
		out.flush(); // flush character output stream buffer

		dataOut.write(fileData, 0, fileLength);
		dataOut.flush();

		if (verbose) {
			System.out.println("File " + fileRequested + " not found");
		}
	}

}
