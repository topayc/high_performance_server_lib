package ean.network.test;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class TestClient {

	/**
	 * @param args
	 * @throws IOException
	 * @throws UnknownHostException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws UnknownHostException,
			InterruptedException {
		ArrayList<Socket> socketList = new ArrayList<Socket>();
		for (int i = 0; i < 1000; i++) {
			Socket socket;
			try {
				socket = new Socket("127.0.0.1", 9090);
				socketList.add(socket);
				Thread.sleep(2);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		for (int i = 0; i < 1000; i++) {
			Socket socket = socketList.get(i);
			Thread.sleep(2);
			try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		socketList.clear();

	}

}
