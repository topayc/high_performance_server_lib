package ean.network.event.callback;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

public class IoReadEventArgs {
	public SelectionKey key;
	public ByteBuffer packet;
	public int bytesRead;
	public Object object;
	
	public IoReadEventArgs(Object object, SelectionKey key, int bytesRead) {
		this.object = object;
		this.key = key;
		this.bytesRead = bytesRead;
	}

	public SelectionKey getKey() {return key;}
	public void setKey(SelectionKey key) {this.key = key;}
	public ByteBuffer getPacket() {return packet;}
	public void setPacket(ByteBuffer packet) {this.packet = packet;}
	public int getBytesRead() {return bytesRead;}
	public void setBytesRead(int bytesRead) {this.bytesRead = bytesRead;}
}
