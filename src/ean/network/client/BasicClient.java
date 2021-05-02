package ean.network.client;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import ean.network.packet.TCPPacket;
import ean.network.packet.UDPPacket;
import ean.network.pool.PoolServiceProvider;
import ean.network.pool.buffer.ByteBufferPool;
import ean.network.pool.buffer.IByteBufferPool;
import ean.network.protocol.C_PT_KEEPALIVE;
import ean.network.protocol.C_TCP_PROTOCOL_DEFINITION;
import ean.network.protocol.C_TRANSACTION_TCP_PROTOCOL;
import ean.network.queue.CircularQueue;
import ean.network.session.NetworkSession;
import ean.network.session.PacketSession;

public class BasicClient extends EventSelect {

	//현재의 테스트 소스에서는 외부의 다른 객체나 다른 스레드에서 접근할 일이 없는 변수들이라 getter 메서드는 생성하지 않음 
	
	protected  PacketSession session;
	protected  boolean isConnected;
	//udp로 작동할 것인지의 여부를 나타내는 변수이나, 현재는 지원하지 않기 때문에 사용될 일이 없음
	protected boolean isUdp;
	//패킷의 일시적 저장을 위한 순환큐 생성 
	protected CircularQueue<TCPPacket> tcpPacketQueue;
	protected CircularQueue<UDPPacket> udpPacketQueue;
	//메모리 버퍼와 mappedbuffer(파일버퍼)를 관리하기 위한 버퍼 풀  
	protected IByteBufferPool byteBufferPool;
	
	protected int memoryBolckSize;
	protected int memoryBlockCount;

	protected int fileBlockSize;
	protected int fileBlockCount;
	protected String bufferFileName;
	
	/*
	 * 프로토콜 관련 클래스 선언( 이 클래스는 PacketGenerator 에 의해 생성된 클래스임) 
	 * 클라이언트의 단일 스레드에서 작동하는 세션이기 때문에, 풀등을 사용할 필요 없이 직접 멤버 변수로 선언하여
	 * GC 의 부담을 줄인다. ==> 차이는 미미할 것으로 보임
	 */
	protected C_TRANSACTION_TCP_PROTOCOL ct;

	public BasicClient() throws IOException{
		initialize();
	}
	
	public void initialize() throws IOException{
		this.session =  new PacketSession();
		this.tcpPacketQueue = new CircularQueue<TCPPacket>();
		
		this.isUdp = false;
		this.isConnected = false;
		this.bufferFileName =  "C:/fileBuffer.tmp";
		
		File mappedBufferFile = new File(bufferFileName);
		if (!mappedBufferFile.exists()) mappedBufferFile.createNewFile();
		mappedBufferFile.deleteOnExit();
		
		this.memoryBolckSize = 4096;
		this.memoryBlockCount = 20;
		
		this.fileBlockSize = 4096;
		this.fileBlockCount= 40;
		
		ct = new C_TRANSACTION_TCP_PROTOCOL("utf-8");
		this.byteBufferPool  = new ByteBufferPool(memoryBlockCount* memoryBolckSize, fileBlockCount * fileBlockSize,mappedBufferFile);
		PoolServiceProvider.regisertByteBufferPool(byteBufferPool);
	}
	
	public boolean begingTcp(String remoteAddress, int remotePort){
		if ( remoteAddress == null  || remotePort <=0 ) return false;
		
		if (!session.begin()){
			end();
			return false;
		}
		
		if (! session.createSession(NetworkSession.CLIENT_SESSION)){
			end();
			return false;
		}
	
		if (!super.begin(session.getClientSocketChannel())){
			end();
			return false;
		}
		
		if (!session.connect(remoteAddress, remotePort)){
			end();
			return false;
		}
		return true;	
	}
	
	public boolean end(){
		session.end();
		return super.end();
	}
	
	/*
	 * PacketSession 클래스가  인자로 주어진 프로토콜번호와 데이타버퍼를 가지고 
	 * 길이(4바이트) + 패킷 번호 (4바이트) + 프로토콜번호(4바이트) + 데이타 형태로 다시 구성하며, 이렇게 구성된 패킷이 
	 * NetworkSession 클래스에 의해 실제로 전송되게 됨 
	 * 메서드의 인자인 버퍼는 패킷에서 데이타 부분에 해당하며, 호출하기 전에 , buffer 는 반드시 flip 되어야 함.
	 */
	public boolean writePacket(int protocol, ByteBuffer packet) throws Exception{
		return session.writePacket(protocol, packet);
	}
	
	//프로토콜을 구성하지 않는 테스트용의 문자열을 송신하며, 버퍼의 플립여부를 지정
	public boolean writeRawPacket(ByteBuffer packet, boolean isFlip) {
		return session.writeRawPacket(packet, isFlip);
	}
	
	
	@Override
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// 데이타가 수신되었을 때 호출되는  메서드,필요에 따라 적절하게 재정의함 
	// PacketGenerator 클래스를 사용하여 자동 생성된 클래스를 사용하는 방법과 버퍼링된 버퍼에서 패킷을 
	// 가져오는 법에 대하여  간단하게 골격 코드를 작성했으니. 상속을 받을 때 이 코드를 참고하면 됨
	// C_TCP_PROTOCOL_DEFINITION.PT_CHANNEL_KEEPALIVE 프로토콜에 대한 처리코드만 일단 삽입했음 
	// 자동생성된 클래스를 사용하지 않을 경우, 버퍼에서 일일히 원하는 데이타를 추출하는 귀찮은 작업이 필요합니다.
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public void OnIoRead() {
		try{
			if (session.readPacket() < 0) {
				// 접속 종료가 요청된 상태로, OnIoDisconnected() 를 호출해서 접속 종료 처리할 기회를 제공한다.
				OnIoDisconnected();  
			}else {
				/*
				* 패킷을 가져온 후 바로 처리하지 않고 큐에 일단 넣기 때문에 패킷 풀로부터 패킷을 가져오지 않고 new 를 통해서 생성하고, 
				* 가비지 컬렉터가 제거하도록 한다.(서버가 아닌 클라이언트에서는 이 정도 객체 생성은  부담이 없음)
				*/
				TCPPacket tcpPacket = new TCPPacket();		
				
				/*
				 * 원래는 false 가 떨어질 때까지 버퍼링 된 패킷을 루프를 돌며,  어플리케이션의 반응성을 높이거나 패킷 수신시 밀림 현상을 방지하기 위해서 
				 * 패킷을 뽑아와서 큐에 삽입한 후 , 이 큐에 삽입된 패킷은  별도의 처리스레드에서 처리되는 구조로 작성하거나, 
				 * 이 큐를 폴링하여 처리하는 구조로 작성을 하여야 하나, 
				 * 현재의 테스트 코드는 실행상의 편의를 위하여 별도의 처리 스레드를 두지 않고 (즉 큐에 삽입하지 않고) 바로 TCPPacket  의 프로토콜을 
				 * 검사하여 프로토콜   처리를 수행한다. 
				 */
				while (session.getPacket(tcpPacket)){
					// 뽑아온 패킷을 큐에 삽입한다.
					//tcpPacketQueue.push(tcpPacket);
					
					switch (tcpPacket.getProtocol()){
					case C_TCP_PROTOCOL_DEFINITION.PT_KEEPALIVE:
						onPT_KEEPALIVE( tcpPacket);
						break;
					}
				}
			}	
		}catch (IOException e){
			/*
			 * 단일 스레드 상에서 소켓채널을 다루기 때문에 동기화 및 채널과  관련한 익셉션은 발생하지 않는다.
			 * 그래서  IO익셉션이 발생하면 원격 호스트의 강제 종료로 간주한다.
			 */
			System.out.println("[원격 호스트에 의한 강제 종료가 발생했습니다");
			OnIoDisconnected();  
		}
	}

	@Override
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// 연결되었을 때 호출되는 메서드 , 필요에 따라 적절하게 재정의함 
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public void OnIoConnected() {
		System.out.println("서버에 연결되었습니다");
		 isConnected = true;

	}

	@Override
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// 연결이 종료되었을 때 호출되는  메서드 , 필요에 따라 적절하게 재정의함 
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public void OnIoDisconnected() {
		System.out.println("연결이 종료되었습니다.");
		isConnected= false;
		end();
	}
	
	/*
	 * 프로토콜 처리 메서드 
	 */
	private void onPT_KEEPALIVE(TCPPacket tcpPacket) {
		
		//_PT_CHANNEL_KEEPALIVE 패킷을 위한 객체 생성 
		// 프로토콜 관련된 정의는 첨부하는 PacketGenerator 에서 사용하는 프로토콜 정의파일을 참조(TCP_PROTOCOL.ini)
		C_PT_KEEPALIVE parameter = new C_PT_KEEPALIVE(); 
		
		//tcpPacket의 버퍼로 부터 parameter 로 주어진 C_PT_CHANNEL_KEEPALIVE 객체를 채움 
		ct.READ_PT_KEEPALIVE(tcpPacket.getPacketBuffer(), parameter);
		
		//출력 
		System.out.println("[PT__KEEPALIVE]  :  count : " + parameter.count + "   message : " +parameter.message);	
	}
	    
	public static void main(String[] arg) throws Exception{
		/*
		 * 통신을 시작하는데 있어서  객체 생성 과 연결하는 2줄의  코드만 필요하다.
		 * 객체를 생성한 후 begingTcp("127.0.0.1", 9090)를 호출하면, 연결을 시도하며, 별도의 스레드에서 IO 이벤트를 감지하게 된다.
		 * 스레드에서 IO 이벤트가 감지되면, 위에서 정의한 이벤트 관련 메서드가 호출되게 됨 
		 */
		BasicClient client = new BasicClient();
		client.begingTcp("127.0.0.1", 9090);	
		
		//아래는 서버로 패킷을 보내는임시 테스트 코드 
		/*
		 ByteBuffer buffer = ByteBuffer.allocate(1000);
		*/
		
	    /*	
	    while ( !client.isConnected){
			Thread.sleep(10);
		}
		Thread.sleep(1500);
		
		C_TRANSACTION_TCP_PROTOCOL t = new C_TRANSACTION_TCP_PROTOCOL("utf-8");
		long time = System.currentTimeMillis();
		
		for (int i = 1;i <501; i++){
			buffer.clear();
			C_PT_CHANNEL_USER_INFO userInfo = new C_PT_CHANNEL_USER_INFO();
			
			String id = "["+i+ "]" + " worldrambler";
			String name="홍길동";
			int age = 20;
			
			userInfo.id = id;
			userInfo.name = name;
			userInfo.age = age;
			
			t.WRITE_PT_CHANNEL_USER_INFO(buffer, userInfo);
			buffer.flip();
			System.out.println("sendBytes : " + (4 + 4 + 4 + buffer.remaining()));
			client.writePacket(10000003,buffer);
			Thread.sleep(1);
			
			buffer.clear();

			C_PT_CHANNEL_CHATTING_MESSAGE message = new C_PT_CHANNEL_CHATTING_MESSAGE();
			message.message ="안녕하세요. 모두들 반갑습니다 앞으로 잘 부탁드립니다.";
			t.WRITE_PT_CHANNEL_CHATTING_MESSAGE(buffer, message);
			buffer.flip();
			System.out.println("sendBytes : " + (4 + 4 + 4 + buffer.remaining()));
			client.writePacket(10000002,buffer);
			Thread.sleep(0);
		}
		
		long  endTime = System.currentTimeMillis();
		System.out.println("전송 시간 : " +  (endTime - time)/1000);
		client.end();
		*/
	}
}







