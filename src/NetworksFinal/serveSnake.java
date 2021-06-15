package NetworksFinal;

import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONString;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class serveSnake implements Runnable {

	Socket client;
	private static Scanner s;
	final static String CRLF = "\r\n";

	// Constructor
	private Snake s1;
	private Snake s2;
	private Food f;

	public serveSnake(Socket socket, Snake s1, Snake s2, Food f) throws Exception {
		this.client = socket;
		this.s1 = s1;
		this.s2 = s2;
		this.f = f;
	}

	@Override
	public void run() {
		try {
			processRequest();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void processRequest() throws Exception {
		try {

			InputStream in = client.getInputStream();
			OutputStream out = client.getOutputStream();
			s = new Scanner(in, "UTF-8");
			try {
				handshake(client, s, out);
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("A client connected.");

			while (true) {
				String decodedMessage = constructRequestMessage(in);
				// If it is not the initial message process the request
				if (decodedMessage.length() != 2) {
					try {
						// Takes the score and snake values out of the JSON string
						int score = Integer.parseInt(decodedMessage.substring(
								decodedMessage.indexOf('"' + "score" + '"' + ":") + 8, decodedMessage.indexOf(",")));
						String snake = decodedMessage.substring(decodedMessage.indexOf('"' + "snake" + '"' + ":") + 8,
								decodedMessage.length() - 1);
						s1.setSnake(snake);
						// If snake ate food, increment score by 10 and generate new food
						if (snakeAteFood(s1, f)) {
							s1.setScore(score + 10);
							f.genRandomFood();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				// Every 100 ms send out new frame to client with new snake and score values
				CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS).execute(() -> {
					try {
						String combineString = "{" + '"' + "ended" + '"' + ":" + checkGameEnd(s1, s2) + "," + '"'
								+ "food" + '"' + ":" + "{" + '"' + "x" + '"' + ":" + f.getX() + "," + '"' + "y" + '"'
								+ ":" + f.getY() + "}" + "," + '"' + "snake1" + '"' + ":" + s1.getFullSnake() + ","
								+ '"' + "snake2" + '"' + ":" + s2.getFullSnake() + "}";
						int contentLength = combineString.getBytes().length;
						byte[] encodedString = encodeMessage(contentLength, combineString.getBytes());
						out.write(encodedString);
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			s.close();
		}
	}

	/**
	 * Processes the incoming request and if it is HTTP GET sends the response to
	 * convert the connection to WebSocket Protocol.
	 * 
	 * @param client Socket connection passed down from MultiThreadWebSocket
	 * @param s      Scanner object
	 * @param out    Output stream to send data through to connection back to the
	 *               client
	 * @throws IOException
	 */
	private static void handshake(Socket client, Scanner s, OutputStream out) throws IOException {
		try {
			String data = s.useDelimiter("\\r\\n\\r\\n").next();
			Matcher get = Pattern.compile("^GET").matcher(data);
			if (get.find()) {
				Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(data);
				match.find();
				byte[] response = ("HTTP/1.1 101 Switching Protocols\r\n" + "Connection: Upgrade\r\n"
						+ "Upgrade: websocket\r\n" + "Sec-WebSocket-Accept: "
						+ Base64.getEncoder()
								.encodeToString(MessageDigest.getInstance("SHA-1").digest(
										(match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes("UTF-8")))
						+ "\r\n\r\n").getBytes("UTF-8");
				out.write(response, 0, response.length);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Constructs a string from the bytes of the incoming requests.
	 * 
	 * @param in InputStream object to read incoming request
	 * @return String request decoded from the initial request.
	 * @throws IOException
	 */
	private static String constructRequestMessage(InputStream in) throws IOException {
		in.read();
		int length = in.read() - 128;
		if (length == 126) {
			byte[] lengthInBytes = in.readNBytes(2);
			length = ByteBuffer.wrap(lengthInBytes).getShort();
		} else if (length == 127) {
			byte[] lengthInBytes = in.readNBytes(8);
			length = ByteBuffer.wrap(lengthInBytes).getInt();
		}
		byte[] key = in.readNBytes(4);
		byte[] message = in.readNBytes(length);
		return new String(decodeMessage(length, key, message), "UTF-8");
	}

	/**
	 * Decodes the bytes of the frame into ASCII bytes.
	 * 
	 * @param length  Length of the payload of the frame
	 * @param key     masking key of the frame
	 * @param encoded payload of the frame
	 * @return byte array of ASCII code of the JSON string.
	 */
	public static byte[] decodeMessage(int length, byte[] key, byte[] encoded) {
		byte[] decoded = new byte[length];
		for (int i = 0; i < encoded.length; i++) {
			decoded[i] = (byte) (encoded[i] ^ key[i & 0x3]);
		}

		return decoded;
	}

	/**
	 * Encodes bytes of a string into a WebSocket frame
	 * 
	 * @param length Length of the payload of the message
	 * @param string ASCII bytes of the text string
	 * @return byte array that is an encoded WebSocket frame.
	 */
	public static byte[] encodeMessage(int length, byte[] string) {
		int contentLength = 0;
		byte lengthByte = 0;
		byte[] lengthByteArr = null;
		if (length < 126) {
			contentLength = 1;
			lengthByte = (byte) length;
		} else if (length < 65535) {
			contentLength = 3;
			lengthByte = (byte) 126;
			lengthByteArr = ByteBuffer.allocate(2).putShort((short) length).array();
		} else {
			contentLength = 9;
			lengthByte = (byte) 127;
			lengthByteArr = ByteBuffer.allocate(8).putInt(length).array();
		}
		List<Byte> list = new ArrayList<Byte>();

		list.add((byte) 129);

		if (contentLength == 1) {
			list.add((byte) length);
		} else {
			list.add(lengthByte);
			addBytesToList(lengthByteArr, list);
		}
		addBytesToList(string, list);

		int i = 0;
		byte[] byteArr = new byte[list.size()];
		for (Byte b : list) {
			byteArr[i++] = b.byteValue();
		}
		return byteArr;

	}

	public static void addBytesToList(byte[] arrToAdd, List<Byte> list) {
		for (int i = 0; i < arrToAdd.length; i++) {
			list.add(arrToAdd[i]);
		}
	}

	public static boolean snakeAteFood(Snake snake, Food food) {
		return snake.getHeadX() == food.getX() && snake.getHeadY() == food.getY();
	}

	public static boolean checkGameEnd(Snake snake1, Snake snake2) {
		ArrayList<JSONObject> stringArray1 = new ArrayList<JSONObject>();
		JSONArray jsonArray1 = new JSONArray(snake1.getSnake());

		ArrayList<JSONObject> stringArray2 = new ArrayList<JSONObject>();
		JSONArray jsonArray2 = new JSONArray(snake2.getSnake());

		for (int i = 0; i < jsonArray1.length(); i++) {
			stringArray1.add(jsonArray1.getJSONObject(i));
		}

		for (int i = 0; i < jsonArray2.length(); i++) {
			stringArray2.add(jsonArray2.getJSONObject(i));
		}

		for (int i = 0; i < stringArray2.size(); i++) {

			if (stringArray1.get(0).getInt("x") == stringArray2.get(i).getInt("x")
					&& stringArray1.get(0).getInt("y") == stringArray2.get(i).getInt("y")) {
				return true;
			}
		}
		for (int i = 0; i < stringArray1.size(); i++) {
			if (stringArray2.get(0).getInt("x") == stringArray1.get(i).getInt("x")
					&& stringArray2.get(0).getInt("y") == stringArray1.get(i).getInt("y")) {
				return true;
			}

		}
		if (snake1.getScore() == 200 || snake2.getScore() == 200) {
			return true;
		}

		return false;

	}

}
