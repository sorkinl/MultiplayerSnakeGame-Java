package NetworksFinal;

import java.net.ServerSocket;
import java.net.Socket;

public class MultiThreadWebSocket {

	public static void main(String argv[]) throws Exception {
		// Establish the listen socket.
		ServerSocket socket = new ServerSocket(81);
		// Listen for a TCP connection request.
		while(true) {
			Snake s1 = new Snake(
					"[{\"x\":200,\"y\":100},{\"x\":190,\"y\":100},{\"x\":180,\"y\":100},{\"x\":170,\"y\":100},{\"x\":160,\"y\":100}]");
			Snake s2 = new Snake(
					"[{\"x\":200,\"y\":300},{\"x\":190,\"y\":300},{\"x\":180,\"y\":300},{\"x\":170,\"y\":300},{\"x\":160,\"y\":300}]");
			Food f = new Food();
		Socket connection = socket.accept();
		// Construct an object to process the incoming request
		serveSnake request1 = new serveSnake(connection, s1, s2, f);
		Thread thread = new Thread(request1);
		

		// Wait for the second connection
		Socket connection2 = socket.accept();
		// Construct an object to process the incoming request
		serveSnake request2 = new serveSnake(connection2, s2, s1, f);
		Thread thread1 = new Thread(request2);
		
		
		thread.start();
		thread1.start();
		}
		

	}

}
