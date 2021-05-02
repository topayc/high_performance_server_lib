package ean.network.session;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import ean.network.packet.TCPPacket;
import ean.network.pool.PoolServiceProvider;
import ean.network.pool.buffer.IByteBufferPool;
import ean.network.queue.CircularQueue;
import ean.network.server.config.Config;
import ean.network.server.config.Configuration;

/*
 * 패킷 구조 
 * 패킷 길이(int 4바이트) + 패킷 번호 (int 4바이트) + 프토토콜 ( int 4바이트) + 데이타  
 */
public class PacketSession extends NetworkSession {

	private ByteBuffer packetBuffer;
	private int remainLength;
	private int currentPacketNumber;
	private int lastReadPacketNumber;
	private int bytesToAdd;
	private static final int SESSION_BUFFER_LENGTH = 4096;
	private static final int INT_SIZE = 4;

	CircularQueue writeQueue;

	public ByteBuffer getPacketBuffer() {return packetBuffer;}
	public int getRemainLength() {return remainLength;}
	public int getCurrentPacketNumber() {return currentPacketNumber;}
	public int getLastReadPacketNumber() {return lastReadPacketNumber;}
	public int getBytesToAdd() {return bytesToAdd;}
	public CircularQueue getWriteQueue() {return writeQueue;}

	public PacketSession(){
		packetBuffer = ByteBuffer.allocateDirect(SESSION_BUFFER_LENGTH * 6);
		writeQueue = new CircularQueue();
		packetBuffer.order(NetworkSession.byteOrder);
	}
	
	public boolean begin() {
		remainLength = 0;
		currentPacketNumber = 0;
		lastReadPacketNumber = 0;
		// if (!writeQueue.begin()) return false;
		return super.begin();
	}

	public boolean end() {
		packetBuffer = null;
		/* if (!writeQueue.end()) return false; */
		return super.end();
	}

	public  boolean getPacket(TCPPacket tcpPacket) {
		//패킷에 현재 남아있는 바이트수가 4바이트가 안되면, 길이 정보가 없다는 것이므로 일단 false 를 리턴한다.
	    if (remainLength < INT_SIZE) {
			return false;
		}
		// 패킷의 길이를 구함
		int packetLength = packetBuffer.getInt(0);
		//구한 패킷 길이가 총 세션버퍼 길이보다 크가나 0보다 작으면 잘못된 패킷이기 때문에 버린다.
		if (packetLength > SESSION_BUFFER_LENGTH || packetLength <= 0) {
			remainLength = 0;
			packetBuffer.clear();
			return false;
		}
		/*
		 *구한 패킷 길이보다 버퍼에 있는 데이타의 길이가 크다면 파싱 작업을 시작
		 * 버퍼에 있는 데이타의 길이자 패킷 길이보다 작다면 아직 데이타가 다 도작하지 않은 것이기 때문에 파싱 작업을 하지 않고
		 * false를 리턴하여, 데이타를 더 읽을 준비를 한다.
		 */
		if (packetLength <= remainLength) {
			//버퍼의 리미트를 임시로 저장 
			int tempLimit = packetBuffer.limit();
			
			//패킷 번호를 구함 
			int packetNumber = packetBuffer.getInt(4);
			//패킷 프로토콜을 구함 
			int protocol = packetBuffer.getInt(8);
			//패킷의 실제 사이즈를 구함 
			int realDataLength = packetLength - (INT_SIZE * 3);
			
			//버퍼의 포지션을 버퍼에서 실제 데이타가 시작하는 부분으로 이동 
			packetBuffer.position(INT_SIZE * 3);
			//버퍼의 리미트를 실제 데이타가 시작하는 부분에서 실제 데이타 길이만큼을 더해 , 데이타 부분만 읽어올 수 있도록 조정한다.
			packetBuffer.limit(packetBuffer.position() + realDataLength);
			
			/*
			 * 인자로 넘어온 tcpPacket을 세팅하는 코드로
			 * 각각 현재 세션 객체, 데이타 부분, 그리고 실제 데이타 길이, 프로토콜 번호를 세팅한다. 
			 */
			tcpPacket.setObject(this);
			tcpPacket.setPacketBuffer(packetBuffer);
			tcpPacket.setPacketLength(realDataLength);
			tcpPacket.setProtocol(protocol);
			
			//현재 패킷에 대한 파싱작업이  완료 되었으므로, 버퍼의 포지션을 완료된 길이만큼 이동시켜서 다음 패킷을 처리할 수 있도록 한다.
			packetBuffer.position(packetLength);
			//버퍼의 리미트를 원래의 리미트로 변경
			packetBuffer.limit(tempLimit);
			
			/*
			 * 버퍼에서 하나의 패킷을 처리한 후  남은 패킷이 더 있다면 다음번 처리를 위해서 남은 데이타를 버퍼의 
			 * 앞으로 이동시켜준다. 
			 */
			if (remainLength - packetLength > 0) {
				packetBuffer.compact();
			}
			//버퍼에 있는 데이타의 길이를 처리된 패킷 길이만큼 마이너스 
			remainLength -= packetLength;
			
			/*
			 * 여기까지 처리 한후 버퍼에 남아있는 패킷이 하나도 없다면, 정확히 받아서 처리된 것이기 때문에
			 * 버퍼와 관련 변수를 초기화 해준다. 
			 */
			if (remainLength <= 0) {
				remainLength = 0;
				packetBuffer.clear();
			}

			/*
			 * 수신된 패킷의 패킷 번호가 기존 번호와 동일하다면 같은 패킷이 재 전송된 것이다.
			 * 따라서 이 패킷에 대해서는 false 를 리턴하여 처리하지 않게 하며,
			 * 만약 패킷 번호가 다를 경우, 최근 수신 패킷 번호변수 (lastReadPacketNumber)을 지금 처리한 패킷의 번호로 갱신
			 * 한다.
			 */
			
			if (lastReadPacketNumber >= packetNumber)
				return false;
			else {
				lastReadPacketNumber = packetNumber;
				return true;
			}
		}
		return false;
	}

	/*
	 * 인자인 프로토콜과 버퍼를 패킷 길이(int 4바이트) + 패킷 번호 (int 4바이트) + 프토토콜 ( int 4바이트) + 데이타 
	 * 형태의 구조로 변경하고, 패킷을 전송하는 메서드 
	 */
	public boolean writePacket(int protocol, ByteBuffer packet)
			throws Exception {
		
		IByteBufferPool bufferPool = PoolServiceProvider.getByteBufferPool();
		ByteBuffer writeBuffer = bufferPool.getMemoryBuffer();


		// 패킷의 총 길이를 구함
		int packetLength = INT_SIZE + INT_SIZE + INT_SIZE + packet.remaining();
		// 총길이가 기본 최대 버퍼 길이를 넘어가면 잘못된 패킷임
		if (packetLength >= SESSION_BUFFER_LENGTH)
			return false;
		// 전송할 패킷 번호 증가
		currentPacketNumber++;

		// 보낼 버퍼에 패킷 길이를 씀
		writeBuffer.putInt(packetLength);
		// 보낼 버퍼에 패킷 번호를 씀
		writeBuffer.putInt(currentPacketNumber);
		// 보낼 버퍼에 프로토콜을 씀
		writeBuffer.putInt(protocol);
		// 보낼 버퍼에 데이타를 씀
		writeBuffer.put(packet);
		writeBuffer.flip();

		// 이미 버퍼를 플립했으니 false를 줌. 플립을 하지 않고 호출할 경우, true 를 줌
		boolean succede = super.write(writeBuffer, false);
		bufferPool.putBuffer(writeBuffer);
	
		return succede;
	}

	/*
	 * 이 PacketSession 클래스는 버퍼링과 파싱 작업을 담당하며 실제 통신과정은 부모클래스인 NetworkSession 클래스에서 담당함 
	 *  readPacket 메서드는 NetworkSession 을 이용해서 수신받은 데이타를 PacketSession 클래스의 버퍼에 버퍼링하는 기능을 제공함 
	 */
	public int readPacket() throws IOException {
		int readBytes =0;
		try {
			readBytes = super.read(packetBuffer);
			if (readBytes > 0) {
				remainLength += readBytes;
				bytesToAdd = readBytes;
			}
			
		} catch (IOException e) {
			throw e;
		}
		return readBytes;
	}

	/*
	 * 테스트 혹은 프로토콜을 구성하지 않은 문자열등의 통신에 사용
	 */
	public boolean writeRawPacket(ByteBuffer packet, boolean mustFlip) {
		boolean succede = super.write(packet, mustFlip);
		return succede;
	}
}
