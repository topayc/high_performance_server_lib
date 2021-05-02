package ean.network.session;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import ean.network.server.config.Config;
import ean.network.server.config.Configuration;
import ean.network.threadsync.Mutex;

public class NetworkSession extends Mutex {
   
	private ByteBuffer readBuffer = null;
	private ServerSocketChannel serverSocketChannel;
	private ServerSocket serverSocket;

	private SocketChannel clientSocketChannel;
	private Socket socket;
	private int currentSessionType;

	public static final int SERVER_SESSION = 1;
	public static final int CLIENT_SESSION = 2;
	public static final int NO_SESSION = 0;
	private static final int SESSION_BUFFER_LENGTH = 4096;
	
	protected static ByteOrder byteOrder;;
	{
		byteOrder = Configuration.getInstance().getString(Config.BYTE_ORDER).toLowerCase().trim().equals("big")? 
				ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
	}
	
	public NetworkSession(){
		readBuffer = ByteBuffer.allocateDirect(SESSION_BUFFER_LENGTH / 2);
		readBuffer.order(NetworkSession.byteOrder);
		begin();
	}
	public ServerSocket getServerSocket() {return serverSocket;}
	public void setServerSocket(ServerSocket serverSocket) {this.serverSocket = serverSocket;}
	public Socket getSocket() {return socket;}
	public void setSocket(Socket socket) {this.socket = socket;}
	public ServerSocketChannel getServerSocketChannel() {return serverSocketChannel;}
	public int getCurrentSessionType() {return currentSessionType;}
	public SocketChannel getClientSocketChannel() {return clientSocketChannel;}
	public void setCurrentSessionType(int currentSessionType) {this.currentSessionType = currentSessionType;}
	
	public void setServerSocketChannel(ServerSocketChannel serverSocketChannel) {
		this.serverSocketChannel = serverSocketChannel;
		currentSessionType = SERVER_SESSION;
	}

	public void setClientSocketChannel(SocketChannel clientSocketChannel) {
		this.clientSocketChannel = clientSocketChannel;
		currentSessionType = CLIENT_SESSION;
	}

	public boolean begin() {
		try {
			if (serverSocketChannel != null || clientSocketChannel != null)
				return false;
			serverSocketChannel = null;
			clientSocketChannel = null;
			currentSessionType = NO_SESSION;
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public boolean end() {
		try {
			switch (currentSessionType) {
			case SERVER_SESSION:
				if (serverSocketChannel.isOpen())
					serverSocketChannel.close();
				currentSessionType = NO_SESSION;
				return true;
			case CLIENT_SESSION:
				if (clientSocketChannel.isOpen())
					clientSocketChannel.close();
				currentSessionType = NO_SESSION;
				return true;
			default:
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}

	public boolean createServerSession() {
		return createSession(SERVER_SESSION,null);
	}

	public boolean createClientSession() {
		return createSession(CLIENT_SESSION, null);
	}
	
	public boolean createSession(int type) {
		boolean returnValue = false;
		try {
			switch (type) {
			case SERVER_SESSION:
				returnValue = createServerSession();
				break;

			case CLIENT_SESSION:
				returnValue = createClientSession();
				break;
			default:
				returnValue = false;
			}
		} catch (Exception e) {
			returnValue = false;
		}
		return returnValue;
	}

	public boolean createSession(int type, Object object) {
		boolean returnValue = false;
		try {
			switch (type) {
			case SERVER_SESSION:
				serverSocketChannel = ServerSocketChannel.open();
				serverSocketChannel.configureBlocking(false);
				serverSocket = serverSocketChannel.socket();
				currentSessionType = type;
				returnValue = true;
				break;

			case CLIENT_SESSION:
				clientSocketChannel = SocketChannel.open();
				clientSocketChannel.configureBlocking(false);
				socket = clientSocketChannel.socket();
				currentSessionType = type;
				returnValue = true;
				break;
			default:
				returnValue = false;
			}
		} catch (Exception e) {
			returnValue = false;
		}
		return returnValue;
	}
	

	public boolean tcpBind(int port, int baglog) {
		try {
			if (currentSessionType == SERVER_SESSION)
				return false;
			InetSocketAddress isa = new InetSocketAddress(port);
			serverSocket = serverSocketChannel.socket();
			serverSocket.bind(isa, baglog);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public boolean registerSelector(Selector selector) {
		boolean returnValue = false;
		try {
			switch (currentSessionType) {
			case SERVER_SESSION:
				serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT,this);
				returnValue = true;
				break;
			case CLIENT_SESSION:
				clientSocketChannel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ, this );
				returnValue = true;
				break;
			default:
				returnValue = false;
			}
		} catch (Exception e) {
			return false;
		}
		return returnValue;
	}

	public SocketChannel accept() throws IOException {
		SocketChannel socketChannel = serverSocketChannel.accept();
		return socketChannel;
	}

	public boolean connect(String address, int port) {
		try {
			if (currentSessionType != CLIENT_SESSION)
				return false;
			boolean connected = clientSocketChannel.connect(new InetSocketAddress(address, port));
			if (connected)
				clientSocketChannel.finishConnect();
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	/*
	 * 데이타를 수신하는 메서드, 데이타를 수신한 후 
	 * 인자로 받은 자식클래스인 PacketSession 클래스의 버퍼링 버퍼에 데이타를 복사한다.(버퍼링)
	 */
	public   int read(ByteBuffer packetBuffer) throws IOException  {
		this.readBuffer.clear();
		int readBytes = this.clientSocketChannel.read(this.readBuffer);
		if (readBytes > 0){
				this.readBuffer.flip();
				packetBuffer.put(this.readBuffer);		
		}
		return readBytes;
	}
	/*
	 * 송신 메서드 
	 * PacketSession 클래스를 통해 생성된 프로토콜 데이타를 송신한다.
	 */
	public boolean write(ByteBuffer buffer, boolean mustFlip)  {
		try {
			if (mustFlip)
				buffer.flip();
			while (buffer.hasRemaining()) {
				clientSocketChannel.write(buffer);
			}
		} catch (Exception e) {
			//e.printStackTrace();
			return false;
		}
		return true;
	}
}
