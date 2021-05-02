package ean.network.client;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public abstract class EventSelect {
	protected Socket socket;
	protected SocketChannel channel;
	protected Selector selector;
	protected SelectionKey selectionKey; 
	protected Object keyAttachment = null;
	protected static int WORKER_THREAD_INDEX = 1;
	
	protected Thread selectWorkerThread;
	
	public EventSelect(){
		this.channel =  null;
		this.socket = null;
		this.selector = null;
	}
	
	public boolean begin(SocketChannel channel){
		return begin(channel,null);
	}
		
	public Socket getSocket() {return socket;}
	public void setSocket(Socket socket) {this.socket = socket;}
	public SocketChannel getChannel() {return channel;}
	public void setChannel(SocketChannel channel) {this.channel = channel;}
	public Selector getSelector() {return selector;}
	public void setSelector(Selector selector) {this.selector = selector;}
	public SelectionKey getSelectionKey() {return selectionKey;}
	public void setSelectionKey(SelectionKey selectionKey) {this.selectionKey = selectionKey;}
	public Object getKeyAttachment() {return keyAttachment;}
	public void setKeyAttachment(Object keyAttachment) {this.keyAttachment = keyAttachment;}
	public Thread getSelectWorkerThread() {return selectWorkerThread;}
	public void setSelectWorkerThread(Thread selectWorkerThread) {this.selectWorkerThread = selectWorkerThread;	}
	
	public boolean begin(SocketChannel channel, Object attachment){
		if (channel == null) return false;
		if (this.channel != null || this.selector !=null) return false;

		try {
			this.selector = Selector.open();
			this.channel = channel;
			this.socket = this.channel.socket();
			//채널이 비블럭킹으로 설정되지 않을 경우, 비블럭킹으로  설정한다.
			if (this.channel.isBlocking())
				this.channel.configureBlocking(false);
			
			/*
			 * 연결이 된 후 어떤 방법으로든 OP_CONNECT 이벤트의 등록을 해제하지 않으면, OP_WRITE 와 혼재될 가능성이 있는데. 이로 인해서
			 * 연속적으로 쓸모없는 이벤트가 발생하게 되며, 결국 이 이벤트로 인하여 selelctor 의 select메서드는  블럭킹 되지 않고 바로 반환하게 됨
			 * OP_WRITE 는 소켓에 데이타를 쓸 때 발생하는 것이 아니라, 데이타를 쓸 수 있는 상태가 되었을 때 발생하기 때문에 
			 * OP_CONNECT 이벤트가 OP_WRITE 와 겹쳐 쉴새 없이 이벤트가 발생하게 되는 것임  
			 * 결국 이는 아무것도 하지 않고 , 쉴새 없이 무한루프를 돌게 되는데. 이로 인해서 CPU의 점유율이 높아지게 되며, 
			 * 아무런 작업도 하지 않음에도 불구하고 연속적인 GC 를 유발하게 된다.
			 * 
			 * 이 소스에서는 아래와 같이 처음에 OP_CONNECT 만 등록하고, 연결이 완료되었을 때 , 해당 소켓 채널에 대해서 OP_READ 를 등록하는 
			 * 방법으로 OP_CONNECT 를 제거하는 방법을 사용한다.
			 */
			this.selectionKey = this.channel.register(this.selector,SelectionKey.OP_CONNECT, attachment);
			this.selectWorkerThread = createSelectWorkerThread();
			selectWorkerThread.start();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;		
	}
	
	/*
	 * 강제 및 정상 연결종료 발생시 스레드를 중지시키기 위한 메서드 
	 */
	public boolean end(){
		this.selectWorkerThread.interrupt();
		return true;
	}
	
	/*
	 * IO이벤트를 감지하는 스레드를 생성 
	 */
	private Thread createSelectWorkerThread() {
		return new IOSelectWorkerThread(this.selector);
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//IO 이벤트가 발생했을 때 호출되는 메서드
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	//데이타가 도착했을 때 호출되는 메서드
	public abstract void OnIoRead();
	//연결이 성공되었을 때 호출되는 메서드
	public abstract void OnIoConnected();
	//연결이 원격 호스트에 의한 정상 종료  및 원격호스트의 강제 종료로 연결이 끊어질 때 호출되는 메서드 
	public abstract void OnIoDisconnected();
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// IO 이벤트를 감지하는 스레드 클래스
	// 단 1개의 스레드만 필요하기 때문에 리스너 방식을 사용하지 않고, 내부 클래스로 선언하여,
	// 이벤트가 발생했을 때 가상함수만을 호출해준다. 
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	class IOSelectWorkerThread extends Thread{
		private String name = "IOSelectWorker-";
		private Selector selector; 

		public IOSelectWorkerThread(Selector selector) {
			this.selector = selector;
			setName(name +WORKER_THREAD_INDEX);
			WORKER_THREAD_INDEX++;
		}
		
		public void run(){
			try {
				while (!Thread.currentThread().isInterrupted()) {
					int keys = this.selector.select();
					/*
					 * keys > 0 인 조건문은 사실상 필요 없음. 왜냐하면 이 소스에서  셀렉터에는 한개의 채널만 등록될 것이기 때문임 
					 * 간혹 클라이언트가 여러개의 서버와 동시에 통신하는 경우가 있는데, 
					 * 이 경우 각기 다른 연결을 가지는 채널을 하나의 셀렉터에서 관리할 수 있으나, 이 소스는 서버가 아닌 클라이언트 용이기 때문에
					 * 굳이 그럴 필요가 없다. 또한 같은 셀렉터에서 각기 다른 연결의 채널을 관리하게 되면, 서버마다 다른 프로토콜과 다른 로직을 
					 * 하나의 공통된 이벤트 메서드에서 처리해야 하기 때문에 , 이는 오히려 가독성만 해치며, 쉽게 찾기 힘든 코드상의 오류도 포함될 수도 있다.
					 * 따라서 각기 다른 연결을 다수 맺어야 할 때는 그냥 이 클래스의 자식클래스인 BasicClient을 각각 상속받아, 각각의 로직을 분리시키는
					 * 것이 좋음. 
					 */
					if (keys > 0) 
						handleRequest();
				}
			} catch (Exception e) {}
		}

		private void handleRequest() throws IOException {
			Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
			while (iter.hasNext()) {
				SelectionKey key = iter.next();
				SocketChannel sc = (SocketChannel) key.channel();
				iter.remove();
					/*
					 * 연결이 완료되거나, 연결이 계류중일 때 key.isConnectable() 은 true를 리턴한다.
					 * 따라서 true 를 리턴한 경우에는 .isConnectionPending() 메서드를 통해서 연결작업이 완료된 것인지
					 * 혹은 계류중인지 확인하여, 계류중일 경우 채널의 finishConnect() 를 호출하여 연결을 마무리 해주어야 한다.
					 * 이 작업이 있지 않다면 연결 완료 이벤트를 제대로 받지 못함.
					 */
					if (key.isConnectable()) {
						if (sc.isConnectionPending()){
							// 연결 작업을 마무리하는 코드로, 반드시 필요하다.
							sc.finishConnect();
							/*
							 * OP_CONNECT 를 등록해제 하기 위해서 다시 해당 채널에 대해 OP_READ 이벤트를 등록한다.
							 * 연결작업이 종료된 후에는 반드시 등록한 OP_CONNECT 를 해제해주어야 한다.
							 * 이 코드가 없다면, 위에서 언급한대로 무한 이벤트 반복으로, 반복적인 GC가 발생하며, 이로 인해 성능은 급격하게 다운된다.
							 */
							key.channel().register(selector,SelectionKey.OP_READ);
							// 연결이 완료되었을 때 처리할 가상함수 호출 
							OnIoConnected();
						}
					}
					// 데이타가 도착했을  처리할 가상함수를 호출
					if (key.isReadable()) OnIoRead();
			}	
		}
	}
}


	
	




























