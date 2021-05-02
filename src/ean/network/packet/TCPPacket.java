package ean.network.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import ean.network.server.config.Config;
import ean.network.server.config.Configuration;

/*
 * TCP 에서 수신된  데이타에 대해서 패킷 구조별 정보를 보관하는 단순한 데이타 클래스 
 */
public class TCPPacket {
	private Object object;
	private ByteBuffer packetBuffer;
	private int packetLength;
	private int protocol;
	
	private static ByteOrder byteOrder;;
	{
		byteOrder = Configuration.getInstance().getString(Config.BYTE_ORDER).toLowerCase().trim().equals("big")? 
				ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
	}
	
	public TCPPacket() {	
		object = null;
		packetLength = 0;
		protocol = 0;
		packetBuffer = ByteBuffer.allocateDirect(4096);
		packetBuffer.order(TCPPacket.byteOrder);
	}


	public void initialize(){
		object = null;
		packetLength = 0;
		protocol = 0;
		packetBuffer.clear();
	}
	
	public void bufferFlip(){this.packetBuffer.flip();}
	public Object getObject() {return object;}
	public void setObject(Object object) {this.object = object;}
	public ByteBuffer getPacketBuffer() {return packetBuffer;}

	public void setPacketBuffer(ByteBuffer packetBuffer) {
		this.packetBuffer.clear();
		this.packetBuffer.put(packetBuffer);
		this.packetBuffer.flip();
	}

	public int getPacketLength() {return packetLength;}
	public void setPacketLength(int packetLength) {this.packetLength = packetLength;}
	public int getProtocol() {return protocol;}
	public void setProtocol(int protocol) {this.protocol = protocol;}

}
