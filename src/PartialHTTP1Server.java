import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

public class PartialHTTP1Server implements Runnable{

	private Socket connect;

	static final int PORT = 8000;

	public PartialHTTP1Server(Socket c) {
		connect = c;
	}

	public static void main(String argv[]) throws Exception {

		try {
	//	ServerSocket welcomeSocket = new ServerSocket(PORT);
	//	System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");

			ServerSocket serverConnect = new ServerSocket(PORT);

			System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");

		while (true) {




			PartialHTTP1Server myServer = new PartialHTTP1Server(serverConnect.accept());

			Thread thread = new Thread(myServer);
			thread.start();

			//Socket connectionSocket = welcomeSocket.accept();

		}
		} catch (Exception e) {
			System.err.println("Server Connection error : " + e.getMessage());
		}
	}

	@Override
	public void run() {
		String clientSentence;
		String capitalizedSentence;
		try{

			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connect.getInputStream()));
			DataOutputStream outToClient = new DataOutputStream(connect.getOutputStream());
			clientSentence = inFromClient.readLine();
			capitalizedSentence = clientSentence.toUpperCase() + '\n';
			outToClient.writeBytes(capitalizedSentence);

		} catch (Exception e) {
			e.printStackTrace();
		}



	}
}