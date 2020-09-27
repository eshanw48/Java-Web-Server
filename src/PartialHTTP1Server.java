import java.io.BufferedOutputStream;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.net.SocketTimeoutException;

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

	private BufferedReader IN = null;
	public PartialHTTP1Server(Socket c) throws IOException {
		connect = c;
		try {
			IN = new BufferedReader(new InputStreamReader(connect.getInputStream()));
		} catch (IOException e) {
			System.out.println(e);
		}finally {
			System.out.println("It works!");
		}

	}

	public static void main(String[] args) {



		try {
			ServerSocket serverConnect = new ServerSocket(Integer.parseInt(args[0]));
			System.out.println("Server started.\nListening for connections on port : " + Integer.parseInt(args[0])+ " ...\n");
			pool = new ThreadPoolExecutor(5, 50,
					60L, TimeUnit.SECONDS,
					new SynchronousQueue<Runnable>());
			// we listen until user halts server execution
			while (true) {
				PartialHTTP1Server myServer = new PartialHTTP1Server(serverConnect.accept());

				if (verbose) {
					System.out.println("Connecton opened. (" + new Date() + ")");
				}

				System.out.println("Pool Size:" + pool.getPoolSize());

				if(pool.getPoolSize()>=50){
					System.out.println("Service Unavailable");
					PrintWriter out = new PrintWriter(myServer.connect.getOutputStream());
					out.println("HTTP/1.0 503 Service Unavailable\r");
					out.println("\r");
					out.flush(); // flush character output stream buffer
					out.close();
					myServer.connect.close();

				}else{
					// create dedicated thread to manage the client connection
					Thread thread = new Thread(myServer);
					pool.execute(thread);
				}

			}

		} catch (IOException e) {
			System.err.println("Server Connection error : " + e.getMessage());
		}
	}

	private String receive() throws IOException {
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = IN.readLine()) != null && !line.isEmpty()) {
			sb.append(line).append("\r\n");
		}
		return sb.toString();
	}

	@Override
	public void run() {
		long opened = System.currentTimeMillis();
		// we manage our particular client connection
		/*BufferedReader in = null;*/ PrintWriter out = null; BufferedOutputStream dataOut = null;
		String fileRequested = null;


		try {

			// we read characters from the client via input stream on the socket
			//in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
			// we get character output stream to client (for headers)
			out = new PrintWriter(connect.getOutputStream());
			// get binary output stream to client (for requested data)
			dataOut = new BufferedOutputStream(connect.getOutputStream());


			int callback = 0;
			Timer timer = new Timer();
			TimerTask task = new Helper(out);
			timer.schedule(task,5000);

			// get first line of the request from the client

			//String input = in.readLine();
			String header = receive();
			String request;
			String request2;
			String lines[] = header.split("\r?\n");
			if(lines.length==2){
				request=lines[0];
				request2=lines[1];
			}else if(lines.length==1){
				request=lines[0];
			}else{
				// we send HTTP Headers with data to client
				out.println("HTTP/1.0 400 Bad Request\r");
				out.println("\r");
				out.flush(); // flush character output stream buffer
				// file
				return;
			}

			long received = System.currentTimeMillis();
			float sec = (received-opened)/ 1000F;
			if(sec>5){

				return;
			}

			// we parse the request with a string tokenizer
			System.out.println(header);
			StringTokenizer parse = new StringTokenizer(request);

			//Request most have 3 parts
			if(parse.countTokens()!=3){

				// we send HTTP Headers with data to client
				out.println("HTTP/1.0 400 Bad Request\r\n");
				out.println("\r\n");
				out.println(); // blank line between headers and content, very important !
				out.flush(); // flush character output stream buffer
				// file
				return;
			}

			String method = parse.nextToken(); // we get the HTTP method of the client
			// we get file requested
			fileRequested = parse.nextToken();

			String version = parse.nextToken();
			double num = 0.0;

			for(int i = 0; i < version.length() ; i++)
			{
				if(version.charAt(i) == '/' && !Character.isLetter(version.charAt(i+1)))
				{
					num = Double.parseDouble(version.substring(i+1));
				}
			}


			if(num > 1.0)
			{
				System.out.println(num);
				out.println("HTTP/1.0 505 HTTP Version Not Supported\r\n");
				out.println("\r\n");
				out.println(); // blank line between headers and content, very important !
				out.flush(); // flush character output stream buffer

				return;
			}
			else if(!version.equals("HTTP/1.0"))
			{
				out.println("HTTP/1.0 400 Bad Request\r\n");
				out.println("\r\n");
				out.println(); // blank line between headers and content, very important !
				out.flush(); // flush character output stream buffer

				return;
			}




			// we support only GET, HEAD, and POST methods, we check
			if (!method.equals("GET")  &&  !method.equals("HEAD") &&  !method.equals("POST")) {
				//not implemented requests
				if(method.equals("PUT") || method.equals("DELETE") || method.equals("LINK") || method.equals("UNLINK")) {
					if (verbose) {
						System.out.println("501 Not Implemented : " + method + " method.");
					}

					// we return the not supported file to the client


					// we send HTTP Headers with data to client
					out.println("HTTP/1.0 501 Not Implemented\r\n");
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
					out.println(); // blank line between headers and content, very important !
					out.flush(); // flush character output stream buffer
					// file
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


/*
				if (fileRequested.endsWith("/")) {
					fileRequested += DEFAULT_FILE;
				}
*/


				File file = new File(WEB_ROOT, fileRequested);
				int fileLength = (int) file.length();
				String content = getContentType(fileRequested);


				if(!file.exists())
				{
					out.println("HTTP/1.0 404 Not Found\r\n");
					out.println("\r\n");
					out.println(); // blank line between headers and content, very important !
					out.flush(); // flush character output stream buffer
					return;
				}



				if (method.equals("GET") || method.equals("POST")) { // GET method so we return content
					if(!file.canRead())
					{
						out.println("HTTP/1.0 403 Forbidden\r\n");
						out.println("\r\n");
						out.println(); // blank line between headers and content, very important !
						out.flush(); // flush character output stream buffer

						return;

					}


					byte[] fileData = readFileData(file, fileLength);
					long lastModified = file.lastModified();
					Date modified  = new Date(lastModified);
					DateFormat converter2 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
					converter2.setTimeZone(TimeZone.getTimeZone("GMT"));
					DateFormat formatter=new SimpleDateFormat("E,dd MMM yyyy HH:mm:ss");


					if(lines.length==2) {
						String mod="";
						for (int i = 0; i < lines[1].length(); i++) {
							if (lines[1].charAt(i) == ':' ) {
								mod = lines[1].substring(i+1);
								break;
							}
						}
						mod=mod.trim();
						System.out.println(mod + " is a different mod");
						Date date1 = null;
						try {
							date1 = formatter.parse(mod);
							formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
							if(modified.compareTo(date1)<0){
								out.println("HTTP/1.0 304 Not Modified\r");
								Calendar calendar = Calendar.getInstance();
								Date today = calendar.getTime();
								calendar.add(Calendar.DAY_OF_YEAR, 1);
								Date tomorrow = calendar.getTime();
								Date localtime3 = new Date();
								DateFormat converter3 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
								converter3.setTimeZone(TimeZone.getTimeZone("GMT"));
								out.println("Expires: " + converter3.format(tomorrow) + " GMT\r");
								out.println("\r");
								out.flush(); // flush character output stream buffer

								return;
							}
						} catch(ParseException ioe){
							System.out.println("Is not a valid date");
						}
						//Tue, 14 Jul 2015 18:00:00 GMT

					}


					// send HTTP Headers
					out.println("HTTP/1.0 200 OK\r");
					//	out.println("\r");
					Date localtime = new Date();
					DateFormat converter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
					converter.setTimeZone(TimeZone.getTimeZone("GMT"));
					out.println("Date: " + converter.format(localtime) + " GMT\r");
					//	out.println("\r");
					out.println("Server: Apache/1.3.27 (Unix)\r");
					//	out.println("\r");
					out.println("MIME-version: 1.0\r");
					//    out.println("\r");

					out.println("Last-Modified: " + converter2.format(modified) + " GMT\r");
					//	out.println("\r");
					out.println("Content-Type: " + content + "\r");
					//	out.println("\r");
					out.println("Content-Length: " + fileLength + "\r");
					//	out.println("\r");
					out.println("Content-Encoding: identity\r");
					//	out.println("\r");
					out.println("Allow: GET, POST, HEAD\r");
					//	out.println("\r");
					Calendar calendar = Calendar.getInstance();
					Date today = calendar.getTime();
					calendar.add(Calendar.DAY_OF_YEAR, 1);
					Date tomorrow = calendar.getTime();
					Date localtime3 = new Date();
					DateFormat converter3 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
					converter.setTimeZone(TimeZone.getTimeZone("GMT"));
					out.println("Expires: " + converter3.format(tomorrow) + " GMT\r");
					//	out.println("\r");

					//out.println(); // blank line between headers and content, very important !

					out.println("\r");
					out.flush(); // flush character output stream buffer

					dataOut.write(fileData, 0, fileLength);
					dataOut.flush();
				}


				if(method.equals("HEAD"))
				{
					if(!file.canRead())
					{
						out.println("HTTP/1.0 403 Forbidden\r\n");
						out.println("\r\n");
						out.println(); // blank line between headers and content, very important !
						out.flush(); // flush character output stream buffer

						return;

					}


					//	byte[] fileData = readFileData(file, fileLength);


					// send HTTP Headers
					out.println("HTTP/1.0 200 OK\r");
					//	out.println("\r");
					Date localtime = new Date();
					DateFormat converter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
					converter.setTimeZone(TimeZone.getTimeZone("GMT"));
					out.println("Date: " + converter.format(localtime) + " GMT\r");
					//	out.println("\r");
					out.println("Server: Apache/1.3.27 (Unix)\r");
					//	out.println("\r");
					out.println("MIME-version: 1.0\r");
					//    out.println("\r");
					long lastModified = file.lastModified();
					Date modified  = new Date(lastModified);
					DateFormat converter2 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
					converter2.setTimeZone(TimeZone.getTimeZone("GMT"));
					out.println("Last-Modified: " + converter2.format(modified) + " GMT\r");
					//	out.println("\r");
					out.println("Content-Type: " + content + "\r");
					//	out.println("\r");
					out.println("Content-Length: " + fileLength + "\r");
					//	out.println("\r");
					out.println("Content-Encoding: identity\r");
					//	out.println("\r");
					out.println("Allow: GET, POST, HEAD\r");
					//	out.println("\r");
					Calendar calendar = Calendar.getInstance();
					Date today = calendar.getTime();
					calendar.add(Calendar.DAY_OF_YEAR, 1);
					Date tomorrow = calendar.getTime();
					Date localtime3 = new Date();
					DateFormat converter3 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
					converter.setTimeZone(TimeZone.getTimeZone("GMT"));
					out.println("Expires: " + converter3.format(tomorrow) + " GMT\r");
					//	out.println("\r");

					//out.println(); // blank line between headers and content, very important !

					out.println("\r");
					out.flush(); // flush character output stream buffer

					//	dataOut.write(fileData, 0, fileLength);
					//	dataOut.flush();

				}

				if (verbose) {
					System.out.println("File " + fileRequested + " of type " + content + " returned");
				}

			}



		} catch (IOException ioe) {
			System.err.println("Server error : " + ioe);
			out.println("HTTP/1.0 500 Internal Server Error\r");
			out.println("\r");// blank line between headers and content, very important !
			out.flush(); // flush character output stream buffer
			return;
		} finally {
			try {
				IN.close();
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
		if (fileRequested.endsWith(".html"))
			return "text/html";

		else if(fileRequested.endsWith(".png"))
		{
			return "image/png";
		}
		else if(fileRequested.endsWith(".gif"))
		{
			return "image/gif";
		}
		else if(fileRequested.endsWith(".jpeg"))
		{
			return "image/jpeg";
		}
		else if(fileRequested.endsWith(".pdf"))
		{
			return "application/pdf";
		}
		else if(fileRequested.endsWith(".zip"))
		{
			return "application/zip";
		}
		else if(fileRequested.endsWith(".x-gzip"))
		{
			return "application/x-gzip";
		}
		else if(fileRequested.endsWith("ls"))
		{
			return "application/octet-stream";
		}
		else
			return "text/plain";
	}

	private void fileNotFound(PrintWriter out, OutputStream dataOut, String fileRequested) throws IOException {

		out.println("HTTP/1.0 404 Not Found\r\n");
		out.println("\r\n");
		out.println(); // blank line between headers and content, very important !
		out.flush(); // flush character output stream buffer



		if (verbose) {
			System.out.println("File " + fileRequested + " not found");
		}
	}
	class Helper extends TimerTask
	{
		PrintWriter out;

		Helper ( PrintWriter out)
		{
			this.out = out;
		}
		public void run()
		{
			out.println("HTTP/1.0 408 Request Timeout\r");
			out.println("\r");
			out.flush(); // flush character output stream buffer
			// file
			return;
		}
	}
}
