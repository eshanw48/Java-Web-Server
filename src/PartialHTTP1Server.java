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

/**
 * @author Shivam Patel
 * @author Dhanush Gandham
 * @author Eshan Wadhwa
 */

/**
 * We created a simple Java Web Server that supports HTTP 1.0 requests. We accept multiple socket connections,
 * and pass those connections into threads, which are managed by the thread pool executor. We read requests from the client,
 * and send back the appropriate HTTP response.
 */
public class PartialHTTP1Server implements Runnable{

	static final File WEB_ROOT = new File(".");

	static final boolean verbose = true;

	private Socket connect;

	static private ThreadPoolExecutor pool;

	private BufferedReader IN = null;

	/**
	 *
	 * @param c Takes in a Socket as an argument, which helps with communication between the server and the client.
	 * @throws IOException Throws an exception if there is an error with BufferedReader.
	 */
	public PartialHTTP1Server(Socket c) throws IOException {
		connect = c;
		try {
			IN = new BufferedReader(new InputStreamReader(connect.getInputStream()));
		} catch (IOException e) {
			System.out.println(e);
		}

	}

	/**
	 *
	 * @param args Takes in the port number for which the server is hosted on.
	 */
	public static void main(String[] args) {

		// We create a server socket to accept incoming connections from the client.
		try {
			ServerSocket serverConnect = new ServerSocket(Integer.parseInt(args[0]));
			System.out.println("Server started.\nListening for connections on port : " + Integer.parseInt(args[0])+ " ...\n");
			pool = new ThreadPoolExecutor(5, 50,
					0, TimeUnit.SECONDS,
					new SynchronousQueue<Runnable>());
			// We listen until user ends server execution.
			while (true) {
				PartialHTTP1Server myServer = new PartialHTTP1Server(serverConnect.accept());

				if (verbose) {
					//System.out.println("Connecton opened. (" + new Date() + ")");
				}

				//System.out.println("Pool Size:" + pool.getPoolSize());

				// We manage the threads here.
				if(pool.getPoolSize()>=50){
					//System.out.println("Service Unavailable");
					PrintWriter out = new PrintWriter(myServer.connect.getOutputStream());
					out.println("HTTP/1.0 503 Service Unavailable\r");
					out.println("\r");
					out.flush();
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

	/**
	 *
	 * @return String which has the client's input.
	 * @throws IOException
	 */
	private String receive() throws IOException {
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = IN.readLine()) != null && !line.isEmpty()) {
			sb.append(line).append("\r\n");
		}
		//System.out.println(sb.toString());
		return sb.toString();
	}

	/**
	 * The threads execute this code, so that the server and client have interaction.
	 */
	@Override
	public void run() {

		long opened = System.currentTimeMillis();

		PrintWriter out = null; BufferedOutputStream dataOut = null;
		String fileRequested = null;


		try {

			// We read characters from the client via input stream on the socket.
			// We get character output stream to the client.
			out = new PrintWriter(connect.getOutputStream());
			dataOut = new BufferedOutputStream(connect.getOutputStream());

			Timer timer = new Timer();
			TimerTask task = new Helper(out);
			timer.schedule(task,5000);

			// get first line of the request from the client
			String header = receive();
			String request;
			String request2;
			String lines[] = header.split("\r?\n");

			if(lines.length == 0){
				out.println("HTTP/1.0 400 Bad Request\r");
				out.println("\r");
				out.flush();
				return;
			}
			request = lines[0];


			long received = System.currentTimeMillis();
			float sec = (received-opened)/ 1000F;
			if(sec>5){

				return;
			}

			// We parse the request with a string tokenizer.
			//System.out.println(header);
			StringTokenizer parse = new StringTokenizer(request);

			// Request most have 3 parts.
			if(parse.countTokens()!=3){

				out.println("HTTP/1.0 400 Bad Request\r");
				out.println("\r");
				out.flush();

				return;
			}

			String method = parse.nextToken(); // We get the HTTP method of the client.
			// We get file requested.
			fileRequested = parse.nextToken();
			// We get the HTTP version.
			String version = parse.nextToken();
			double num = 0.0;

			for(int i = 0; i < version.length() ; i++)
			{
				try
				{

					if(version.charAt(i) == '/')
					{
						num = Double.parseDouble(version.substring(i+1));
					}
				}
				catch(Exception e)
				{
					out.println("HTTP/1.0 400 Bad Request\r");
					out.println("\r");
					out.flush();

					return;

				}
			}
			//If http version > 1.0, then the version is not supported.
			if(num > 1.0)
			{
				//System.out.println(num);
				out.println("HTTP/1.0 505 HTTP Version Not Supported\r");
				out.println("\r");
				out.flush();

				return;
			}


			// We support only GET, HEAD, and POST methods.
			if (!method.equals("GET")  &&  !method.equals("HEAD") &&  !method.equals("POST")) {
				// Not implemented requests.
				if(method.equals("PUT") || method.equals("DELETE") || method.equals("LINK") || method.equals("UNLINK")) {
					if (verbose) {
						//System.out.println("501 Not Implemented : " + method + " method.");
					}


					out.println("HTTP/1.0 501 Not Implemented\r");
					out.println("\r");
					out.flush();

				}else{// Non-existent http requests.


					out.println("HTTP/1.0 400 Bad Request\r");
					out.println("\r");
					out.flush();

				}

			} else {

				File file = new File(WEB_ROOT, fileRequested);
				int fileLength = (int) file.length();
				String content = getContentType(fileRequested);


				if(!file.exists())
				{
					out.println("HTTP/1.0 404 Not Found\r");
					out.println("\r");
					out.flush();
					return;
				}


				//If method is GET or POST.
				if (method.equals("GET")) {
					if(!file.canRead())
					{
						out.println("HTTP/1.0 403 Forbidden\r");
						out.println("\r");
						out.flush();

						return;

					}


					byte[] fileData = readFileData(file, fileLength);
					long lastModified = file.lastModified();
					Date modified  = new Date(lastModified);
					DateFormat converter2 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
					converter2.setTimeZone(TimeZone.getTimeZone("GMT"));
					DateFormat formatter=new SimpleDateFormat("E,dd MMM yyyy HH:mm:ss");


					if(lines.length > 1) {
						String mod="";
						for (int i = 0; i < lines[1].length(); i++) {
							if (lines[1].charAt(i) == ':' ) {
								mod = lines[1].substring(i+1);
								break;
							}
						}
						mod=mod.trim();
						//System.out.println(mod + " is a different mod");
						Date date1 = null;
						try {
							date1 = formatter.parse(mod);
							formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
							if(modified.compareTo(date1)<0){
								out.println("HTTP/1.0 304 Not Modified\r");
								Calendar calendar = Calendar.getInstance();
								calendar.add(Calendar.YEAR, 1);
								Date tomorrow = calendar.getTime();
								DateFormat converter3 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
								converter3.setTimeZone(TimeZone.getTimeZone("GMT"));
								out.println("Expires: " + converter3.format(tomorrow) + " GMT\r");
								out.println("\r");
								out.flush();

								return;
							}
						} catch(ParseException ioe){
							System.out.println("Is not a valid date");
						}

					}



					out.println("HTTP/1.0 200 OK\r");
					Date localtime = new Date();
					DateFormat converter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
					converter.setTimeZone(TimeZone.getTimeZone("GMT"));
					out.println("Date: " + converter.format(localtime) + " GMT\r");
					out.println("Server: Apache/1.3.27 (Unix)\r");
					out.println("MIME-version: 1.0\r");
					out.println("Last-Modified: " + converter2.format(modified) + " GMT\r");
					out.println("Content-Type: " + content + "\r");
					out.println("Content-Length: " + fileLength + "\r");
					out.println("Content-Encoding: identity\r");
					out.println("Allow: GET, POST, HEAD\r");
					Calendar calendar = Calendar.getInstance();
					calendar.add(Calendar.YEAR, 1);
					Date tomorrow = calendar.getTime();
					DateFormat converter3 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
					converter.setTimeZone(TimeZone.getTimeZone("GMT"));
					out.println("Expires: " + converter3.format(tomorrow) + " GMT\r");
					out.println("\r");
					out.flush();

					dataOut.write(fileData, 0, fileLength);
					dataOut.flush();
				}

				if(method.equals("POST")){

					if(!file.canRead())
					{
						out.println("HTTP/1.0 403 Forbidden\r");
						out.println("\r");
						out.flush();

						return;

					}

					System.out.println(fileRequested);
					if(!fileRequested.endsWith(".cgi")){
						out.println("HTTP/1.0 405 Method Not Allowed\r");
						out.println("\r");
						out.flush();
						return;
					}

					boolean content_type = false;
					boolean content_length = false;

					for(int i = 1; i < lines.length; i++){
						StringTokenizer parse_temp = new StringTokenizer(lines[i]);
						if(parse_temp.countTokens() != 2){
							out.println("HTTP/1.0 400 Bad Request\r");
							out.println("\r");
							out.flush();
							return;
						}
							String token1 = parse_temp.nextToken();
							String token2 = parse_temp.nextToken();
							if(token1.equals("Content-Length:")){
								try{
									Integer.parseInt(token2);
									content_length = true;
								}catch(Exception e){

								}
							}else if(token1.equals("Content-Type:")){
								content_type = true;
							}
					}

					if(!content_type){
						out.println("HTTP/1.0 500 Internal Server Error\r");
						out.println("\r");
						out.flush();
						return;
					}if(!content_length){
						out.println("HTTP/1.0 411 Length Required\r");
						out.println("\r");
						out.flush();
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
						//System.out.println(mod + " is a different mod");
						Date date1 = null;
						try {
							date1 = formatter.parse(mod);
							formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
							if(modified.compareTo(date1)<0){
								out.println("HTTP/1.0 304 Not Modified\r");
								Calendar calendar = Calendar.getInstance();
								calendar.add(Calendar.YEAR, 1);
								Date tomorrow = calendar.getTime();
								DateFormat converter3 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
								converter3.setTimeZone(TimeZone.getTimeZone("GMT"));
								out.println("Expires: " + converter3.format(tomorrow) + " GMT\r");
								out.println("\r");
								out.flush();

								return;
							}
						} catch(ParseException ioe){
							//System.out.println("Is not a valid date");
						}

					}



					out.println("HTTP/1.0 200 OK\r");
					Date localtime = new Date();
					DateFormat converter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
					converter.setTimeZone(TimeZone.getTimeZone("GMT"));
					out.println("Date: " + converter.format(localtime) + " GMT\r");
					out.println("Server: Apache/1.3.27 (Unix)\r");
					out.println("MIME-version: 1.0\r");
					out.println("Last-Modified: " + converter2.format(modified) + " GMT\r");
					out.println("Content-Type: " + content + "\r");
					out.println("Content-Length: " + fileLength + "\r");
					out.println("Content-Encoding: identity\r");
					out.println("Allow: GET, POST, HEAD\r");
					Calendar calendar = Calendar.getInstance();
					calendar.add(Calendar.YEAR, 1);
					Date tomorrow = calendar.getTime();
					DateFormat converter3 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
					converter.setTimeZone(TimeZone.getTimeZone("GMT"));
					out.println("Expires: " + converter3.format(tomorrow) + " GMT\r");
					out.println("\r");
					out.flush();

					dataOut.write(fileData, 0, fileLength);
					dataOut.flush();

				}

				// If method is HEAD.
				if(method.equals("HEAD"))
				{
					if(!file.canRead())
					{
						out.println("HTTP/1.0 403 Forbidden\r");
						out.println("\r");
						out.flush();
						return;

					}

					out.println("HTTP/1.0 200 OK\r");

					Date localtime = new Date();
					DateFormat converter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
					converter.setTimeZone(TimeZone.getTimeZone("GMT"));
					out.println("Date: " + converter.format(localtime) + " GMT\r");

					out.println("Server: Apache/1.3.27 (Unix)\r");

					out.println("MIME-version: 1.0\r");

					long lastModified = file.lastModified();
					Date modified  = new Date(lastModified);
					DateFormat converter2 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
					converter2.setTimeZone(TimeZone.getTimeZone("GMT"));
					out.println("Last-Modified: " + converter2.format(modified) + " GMT\r");
					out.println("Content-Type: " + content + "\r");
					out.println("Content-Length: " + fileLength + "\r");
					out.println("Content-Encoding: identity\r");
					out.println("Allow: GET, POST, HEAD\r");

					Calendar calendar = Calendar.getInstance();
					calendar.add(Calendar.YEAR, 1);
					Date tomorrow = calendar.getTime();
					DateFormat converter3 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
					converter.setTimeZone(TimeZone.getTimeZone("GMT"));
					out.println("Expires: " + converter3.format(tomorrow) + " GMT\r");
					out.println("\r");
					out.flush();

				}

				if (verbose) {
					//System.out.println("File " + fileRequested + " of type " + content + " returned");
				}

			}

		} catch (IOException ioe) {
			System.err.println("Server error : " + ioe);
			out.println("HTTP/1.0 500 Internal Server Error\r");
			out.println("\r");
			out.flush();
			return;
			// We close all the connections here.
		} finally {
			try {
				try {
					Thread.sleep(250);
				} catch(Exception e){

				}
				IN.close();
				out.close();
				dataOut.close();
				connect.close();
			} catch (Exception e) {
				System.err.println("Error closing stream : " + e.getMessage());
			}

			if (verbose) {
				//System.out.println("Connection closed.\n");
			}
		}


	}

	/**
	 *
	 * @param file File which we want to read.
	 * @param fileLength Length of the file we are reading.
	 * @return We return the Payload.
	 * @throws IOException
	 */
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

	/**
	 *
	 * @param fileRequested File extension.
	 * @return Return the MIME types.
	 */
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
		else if(fileRequested.endsWith("ls") || fileRequested.endsWith(".cgi"))
		{
			return "application/octet-stream";
		}
		else
			return "text/plain";
	}

	/**
	 * Helper Class to help us with Request Timeout.
	 */
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
