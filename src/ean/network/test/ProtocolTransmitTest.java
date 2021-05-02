package ean.network.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import ean.network.session.PacketSession;


public class ProtocolTransmitTest {
	public static void main(String[] arg) throws UnknownHostException, IOException {
		Socket socket = new Socket("127.0.0.1",9090);
		OutputStream os = socket.getOutputStream();
		InputStream is = socket.getInputStream();
		ByteBuffer buffer = ByteBuffer.allocate(1000);
		
		String message = "안녕하세요반갑습니다";
		byte[] s = message.getBytes();
		int packetLength = 4 + 4 + 4 + 2 + s.length;
		System.out.println(packetLength);
		
		buffer.putInt(4 + 4 + 4 + 2 + s.length);  // 패킷 길이 44
		buffer.putInt(10);  // 패킷 번호 10
		buffer.putInt(10000002);  // 패킷 프로토콜 25 
		buffer.put(s);
		buffer.put(new byte[]{(byte)255,(byte)255});
		
		byte[] packet = buffer.array();
		os.write(packet, 0, packet.length);
		byte[] recvBuffer = new byte[2048];
		int readBytes = is.read(recvBuffer);
		
		
		
		
	}
}
