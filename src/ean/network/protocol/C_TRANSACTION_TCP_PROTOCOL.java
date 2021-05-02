package ean.network.protocol;

import java.nio.ByteBuffer;
import ean.network.packet.PacketStream;
import ean.network.protocol.C_PT_NICKNAME;
import ean.network.protocol.C_PT_CHATTING_MESSAGE;
import ean.network.protocol.C_PT_USER_INFO;
import ean.network.protocol.C_PT_KEEPALIVE;
import ean.network.protocol.C_PT_ENTER_ROOM;

public class C_TRANSACTION_TCP_PROTOCOL { 

	public PacketStream stream;

	public C_TRANSACTION_TCP_PROTOCOL(String encoding){
		 stream = new PacketStream(encoding);
	}

	public void READ_PT_NICKNAME(ByteBuffer buffer, C_PT_NICKNAME parameter){
		stream.setBuffer(buffer);
		parameter.userId = stream.readString();
		parameter.nickName = stream.readString();
	}

	public int WRITE_PT_NICKNAME(ByteBuffer buffer, C_PT_NICKNAME parameter){
		stream.setBuffer(buffer);
		stream.writeString(parameter.userId);
		stream.writeString(parameter.nickName);
		return stream.getLength();
	}

	public void READ_PT_CHATTING_MESSAGE(ByteBuffer buffer, C_PT_CHATTING_MESSAGE parameter){
		stream.setBuffer(buffer);
		parameter.message = stream.readString();
	}

	public int WRITE_PT_CHATTING_MESSAGE(ByteBuffer buffer, C_PT_CHATTING_MESSAGE parameter){
		stream.setBuffer(buffer);
		stream.writeString(parameter.message);
		return stream.getLength();
	}

	public void READ_PT_USER_INFO(ByteBuffer buffer, C_PT_USER_INFO parameter){
		stream.setBuffer(buffer);
		parameter.id = stream.readString();
		parameter.name = stream.readString();
		parameter.age = stream.readInt();
	}

	public int WRITE_PT_USER_INFO(ByteBuffer buffer, C_PT_USER_INFO parameter){
		stream.setBuffer(buffer);
		stream.writeString(parameter.id);
		stream.writeString(parameter.name);
		stream.writeInt(parameter.age);
		return stream.getLength();
	}

	public void READ_PT_KEEPALIVE(ByteBuffer buffer, C_PT_KEEPALIVE parameter){
		stream.setBuffer(buffer);
		parameter.count = stream.readInt();
		parameter.message = stream.readString();
	}

	public int WRITE_PT_KEEPALIVE(ByteBuffer buffer, C_PT_KEEPALIVE parameter){
		stream.setBuffer(buffer);
		stream.writeInt(parameter.count);
		stream.writeString(parameter.message);
		return stream.getLength();
	}

	public void READ_PT_ENTER_ROOM(ByteBuffer buffer, C_PT_ENTER_ROOM parameter){
		stream.setBuffer(buffer);
		parameter.roomNo = stream.readInt();
		parameter.sessionId = stream.readInt();
	}

	public int WRITE_PT_ENTER_ROOM(ByteBuffer buffer, C_PT_ENTER_ROOM parameter){
		stream.setBuffer(buffer);
		stream.writeInt(parameter.roomNo);
		stream.writeInt(parameter.sessionId);
		return stream.getLength();
	}

}
