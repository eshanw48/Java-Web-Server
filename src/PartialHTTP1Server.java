import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

public class PartialHTTP1Server implements Runnable {

	private Socket connect;

	static final int PORT = 8000;

	public PartialHTTP1Server(Socket c) {
		connect = c;
	}


	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		try {

		//Hi
		ServerSocket serverConnect = new ServerSocket(PORT);

		System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");

		while (true) {

			PartialHTTP1Server myServer = new PartialHTTP1Server(serverConnect.accept());

			Thread thread = new Thread(myServer);
			thread.start();


		}

		} catch (IOException e) {
			System.err.println("Server Connection error : " + e.getMessage());
		}

	}

	@Override
	public void run() {

		BufferedReader in = null;
		PrintWriter out = null;
		BufferedOutputStream dataOut = null;

		try {
			in = new BufferedReader(new InputStreamReader(connect.getInputStream()));

			out = new PrintWriter(connect.getOutputStream());

			dataOut = new BufferedOutputStream(connect.getOutputStream());

			String input = in.readLine();

			StringTokenizer parse = new StringTokenizer(input);
			String http = parse.nextToken().toUpperCase();

			if (http.equals("GET")  &&  http.equals("HEAD")) {

			}



			} catch (IOException e) {
			e.printStackTrace();
		}


	}
}
