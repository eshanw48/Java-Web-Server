import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

public class PartialHTTP1Server{

	static final int PORT = 8000;

	public static void main(String argv[]) throws Exception {
		String clientSentence;
		String capitalizedSentence;
		try {
		ServerSocket welcomeSocket = new ServerSocket(PORT);
		System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");

		while (true) {

			Socket connectionSocket = welcomeSocket.accept();
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
			DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
			clientSentence = inFromClient.readLine();
			capitalizedSentence = clientSentence.toUpperCase() + '\n';
			outToClient.writeBytes(capitalizedSentence);
		}
		} catch (Exception e) {
			System.err.println("Server Connection error : " + e.getMessage());
		}
	}
}