package ean.network.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.DecimalFormat;

import ean.network.event.callback.IoAcceptEventArgs;
import ean.network.event.callback.IoDiconnectedEventArgs;
import ean.network.event.callback.IoReadEventArgs;
import ean.network.event.callback.IoServerBindingEventArgs;
import ean.network.packet.TCPPacket;
import ean.network.pool.PoolServiceProvider;
import ean.network.pool.handler.HandlerAdaptor;
import ean.network.protocol.C_PT_CHATTING_MESSAGE;
import ean.network.protocol.C_PT_USER_INFO;
import ean.network.protocol.C_TCP_PROTOCOL_DEFINITION;
import ean.network.protocol.C_TRANSACTION_TCP_PROTOCOL;
import ean.network.session.extended.ConnectedSession;
import ean.network.thread.processor.IOAcceptProcessorWorker;
import ean.network.thread.processor.IOReadProcessorWorker;

public class DefaultSelectServer extends BaseSelectServer {
	
	public boolean begin() {
		return super.begin();
	}

	@Override
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// 서버가 정상적으로 바인딩 되었을 때 호출되는 메서드 
	// 서버 세션을 초기화 및 시작하고, 관련 객체를 초기화 한다.
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public void onIoChannelBinding(Object object, IoServerBindingEventArgs event) throws IOException {
		ServerSocketChannel serverSocketChannel = event.getServerSocketChannel();
		listenSession.setServerSocketChannel(serverSocketChannel);
		System.out.println("Server binding ........successfully" + "  :  "+ serverSocketChannel.toString());
		System.out.println();
	}

	@Override
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// 클라이언트의 연결 요청이 올 경우 호출되는 메서드로 accept 된 소켓 채널등이 전달된다
	// 전달되는 채널등 기타 클라이언트 객체(세션)를  생성하고, 세션 매니져등에 등록하고, 채널을 read 셀렉터에 등록한다.
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public void onIoAccept(Object object, IoAcceptEventArgs event) throws IOException {
		
		//이벤트가 발생된 스레드가 object 로전달됨 
		IOAcceptProcessorWorker thread = (IOAcceptProcessorWorker)object;
		SocketChannel sc = event.socketChannel;
		sc.configureBlocking(false);

		ConnectedSession connectedSession = new ConnectedSession();
		connectedSession.setClientSocketChannel(sc);
		connectedSession.setSessinId(connectedSession.hashCode());

		// SessionManager 등록
		if (!connectedsessionManager.addSession(connectedSession)) {
			connectedSession.end();
			sc.close();
			
			System.out.println("");
			System.out.println("max limit excceded ..... disconneting");
			System.out.println("접속 최대 인원을 초과했습니다. 접속을 끊습니다");
			System.out.println("");
		} else {
			// ReadSelector 등록
			HandlerAdaptor handler = (HandlerAdaptor) PoolServiceProvider.getRequestSelectorPool().get();
			handler.addClient(sc, connectedSession);
		}
	}
	

	@Override
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// read 이벤트가 발생할 경우 이 메서드도 관련 정보 객체가 전달되며, 실제적으로 상황에 맞게 오버라이딩 해야 한다.
	// (아래의 코드는 임시적으로 작성한 것임) 
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public void onIoRead(Object object, IoReadEventArgs event) throws IOException {
		
		//이벤트가 발생한 스레드가 objcet 변수로 넘어옴 
		IOReadProcessorWorker thread = (IOReadProcessorWorker)object;
		
		ConnectedSession session = (ConnectedSession)event.object;
		
		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// 이후 부터 세션의 버퍼에 버퍼링 되어 있는 패킷을 패킷 길이단위 별로 유효성을 검사한 후, 패킷을 잘라와서 TCPPacket 객체
		// 에 담게 되며, 프로토콜에 따라 각각의 처리를 한다.
		// 루프는 패킷에 이상이 없을 때 까지 계속 반복처리되며, 문제가 있는 패킷의 경우 버려진다.
		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		
		//풀로부터 TCPPacket 객체를 하나 가져온다.
		TCPPacket tcpPacket = PoolServiceProvider.getTCPPakcetPool().getObject();
		// 기존에 사용된 객체일 수도 있기 때문에 초기화 한다.
		tcpPacket.initialize();	
		//버퍼링 된 패킷을 루프를 돌면서 처리한다.
		
		while (session.getPacket(tcpPacket)){	
			switch (tcpPacket.getProtocol()){
			case C_TCP_PROTOCOL_DEFINITION.PT_CHATTING_MESSAGE:
				onPT_CHANNEL_CHATTING_MESSAGE(session, tcpPacket);
				break;
			case C_TCP_PROTOCOL_DEFINITION.PT_USER_INFO:
				onPT_CHANNEL_USER_INFO(session, tcpPacket);
				break;
			}
			tcpPacket.initialize();	
		}
		// 사용한 TCPPacket 객체는 풀에 반납한디다.
		PoolServiceProvider.getTCPPakcetPool().freeObject(tcpPacket);
	}


	@Override
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// 연결이 정상종료, 혹은 원격호스트에 의한 강제 종료가 발생하는 경우 호출되는 메서드 
	// 채널 종료 , 세션 종료 및 관련 객체의 종료처리를 한다.
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	synchronized public void onIoDisconnected(Object object,IoDiconnectedEventArgs event) throws IOException {
		
		SelectionKey key = event.key;
		ConnectedSession session = (ConnectedSession) object;
		SocketChannel sc = (SocketChannel) key.channel();
		
		/*클라이언트가 close() 를 한 정상 종료건, 강제 종료건 간에 그 시점에서는 채널은 열려 있다.(단지 종료 요청을 한 것뿐이다)
		그러니 실제적으로 채널을 닫아주어야 한다.
		이는 채널이 아니라 socket 을 직접 사용했을 경우도 동일한다.( 즉 클라이언트가 종료를 하면 서버는 해당 소켓을 역시 close() 해주어야
		종료처리가 완전해지는 것이다.*/
		sc.close();
		key.attach(null);
		key.cancel();
		
		if (session != null) {
			session.end();
			connectedsessionManager.removeSession(session);
		}
	}
	
	private void onPT_CHANNEL_USER_INFO(ConnectedSession session,TCPPacket tcpPacket) {
		C_TRANSACTION_TCP_PROTOCOL ct = new C_TRANSACTION_TCP_PROTOCOL("utf-8");
		C_PT_USER_INFO parameter = new C_PT_USER_INFO();
		ct.READ_PT_USER_INFO(tcpPacket.getPacketBuffer(), parameter);;
		
		System.out.println("[Protocol] => PT_CHANNEL_USER_INFO  :  [ID] " + parameter.id +
				"  [NAME] "+parameter.name + "  [AGE] " + parameter.age );
		
	}
	
	private  void onPT_CHANNEL_CHATTING_MESSAGE(ConnectedSession session, TCPPacket tcpPacket) {
		C_TRANSACTION_TCP_PROTOCOL ct = new C_TRANSACTION_TCP_PROTOCOL("utf-8");
		C_PT_CHATTING_MESSAGE message = new C_PT_CHATTING_MESSAGE();
		ct.READ_PT_CHATTING_MESSAGE(tcpPacket.getPacketBuffer(), message);
		
		System.out.println("[Protocol]PT_CHATTING  :  [MESSAGE] " + message.message);
		
	}
	
	private void broadCasting() throws UnsupportedEncodingException {
		
		ByteBuffer buffer = ByteBuffer.allocateDirect(1000);
		buffer.put("테스트".getBytes("cp949"));
		buffer.flip();
		//프로토콜을 구성하지 않고, 그대로 전송함
		connectedsessionManager.writeRowPacketToAll(buffer);
	}

	@Override
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// 채널에 쓸 수 있는 때가 되면 호출되는 메서드로 실제적으로 사용될 일이 거의 없다.
	// 이유는 필요할 때 패킷을 전송하면 되기 때문임.
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public void onIoWrote() {}

	public static void main(String[] arg) {
		DefaultSelectServer server = new DefaultSelectServer();
		DecimalFormat df = new DecimalFormat("#,##0.00");
		System.out.println("max memory :  " +df.format((int)(Runtime.getRuntime().maxMemory())) + " Byte");
		if (!server.begin())
			return;
	}

}
