package ean.network.server;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.text.DecimalFormat;

import ean.network.event.Job;
import ean.network.event.callback.IoCallbackListener;
import ean.network.packet.TCPPacket;
import ean.network.pool.ObjectPool;
import ean.network.pool.PoolObjectFactory;
import ean.network.pool.PoolServiceProvider;
import ean.network.pool.buffer.ByteBufferPool;
import ean.network.pool.buffer.IByteBufferPool;
import ean.network.protocol.C_PT_KEEPALIVE;
import ean.network.protocol.C_TRANSACTION_TCP_PROTOCOL;
import ean.network.queue.IOEventQueue;
import ean.network.queue.Queue;
import ean.network.selector.IOAcceptSelectPool;
import ean.network.selector.IOReadtSelectPool;
import ean.network.selector.ISelectorPool;
import ean.network.server.config.Config;
import ean.network.server.config.Configuration;
import ean.network.session.NetworkSession;
import ean.network.session.manager.ConnectedSessionManager;
import ean.network.thread.IThreadPool;
import ean.network.thread.ThreadPool;

public abstract class BaseSelectServer implements IoCallbackListener {

	protected Queue queue = null;

	private ISelectorPool IOAcceptSelectorWorkerThreadPool = null;
	private ISelectorPool IORequestSelectorWrokerThreadPool = null;
	
	private IByteBufferPool byteBufferPool = null;
	
	private IThreadPool IOAcceptProcessorWokerThreadPool = null;
	private IThreadPool OoReadProcessorWorkerThreadPool = null;

	protected NetworkSession listenSession;
	protected ConnectedSessionManager connectedsessionManager;

	Thread keepAliveThread;
	protected int listenPort;
	protected int maxConnection;
	protected int memoryBolckSize;
	protected int memoryBlockCount;

	protected int fileBlockSize;
	protected int fileBlockCount;
	protected String bufferFileName;

	protected ByteOrder byteOrder;
	protected int sessionBufferLength;

	protected int acceptSelectThreadCount;
	protected int readSelectThreadCount;

	protected int acceptProcessorThreadMinCount;
	protected int acceptProcessorTheadMaxCount;
	protected String acceptProcessorThreadClassname;

	protected int readWriteProcessorThreadMinCount;
	protected int readWriteProcessorThreadMaxCount;
	protected String readWriteProcessorThreadClassname;

	protected boolean useCircularQueue;
	protected int circularQueueSize;

	protected String encodingSetString;
	protected String decodingSetString;

	protected CharsetEncoder encoder;
	protected CharsetDecoder decoder;
	
	protected int objectPoolLimit; 

	protected boolean isConsoleMessage;

	boolean canKeepAliveThread = false;
	int keepAliveInterval;
	byte[] keepAlivePacket;

	public BaseSelectServer() {
	}

	public class KeepAliveThread extends Thread {
		private  ByteBuffer keepSendBuffer;
		private CharsetDecoder decoder;
		private CharsetEncoder encoder;
		
		private String encodingSetString;
		private String decodingSetString;
		private byte[] alivePacket;
		
		private int keepAliveInterval;
		private C_TRANSACTION_TCP_PROTOCOL ct;

		public KeepAliveThread() {
			this("cp949", "cp949",3000);		
		}

		public KeepAliveThread(String encodingSetString, String decodingSetString, int keepAliveInterval) {
			this.keepSendBuffer = ByteBuffer.allocateDirect(1000);
			this.encodingSetString = encodingSetString;
			this.decodingSetString = decodingSetString;
			
			this.encoder = Charset.forName(this.encodingSetString).newEncoder();
			this.decoder = Charset.forName(this.decodingSetString).newDecoder();
			this.keepAliveInterval = keepAliveInterval;
			try {
				alivePacket = "\r\nkeep alive packet".getBytes(encodingSetString);
			} catch (UnsupportedEncodingException e) {e.printStackTrace();}
			ct = new C_TRANSACTION_TCP_PROTOCOL("utf-8");
		}

		public void run() {
			C_PT_KEEPALIVE keep = new C_PT_KEEPALIVE();
			
			int count = 0;
			while (true) {
				/*
				 * sendBuffer.clear(); sendBuffer.put(alivePacket);
				 * sendBuffer.flip();
				 * mConnectedsessionManager.writeRowPacketToAll(sendBuffer);
				 */
				try {
					Thread.sleep(keepAliveInterval);
					keepSendBuffer.clear();
					count++;
					keep.count = count;
					keep.message = " Keep Alive Checking ";
					ct.WRITE_PT_KEEPALIVE(keepSendBuffer, keep);
					keepSendBuffer.flip();
					connectedsessionManager.writeAll(10000004, keepSendBuffer);
					System.out.println("current connection count : " + connectedsessionManager.getCurrentUserCount());
					DecimalFormat df = new DecimalFormat("#,##0.");
					System.out.println("max    memory  :  " +df.format((int)(Runtime.getRuntime().maxMemory())/1024)  + " KB");
					System.out.println("total  memory  :  " +df.format((int)(Runtime.getRuntime().totalMemory())/1024)  + " KB");
					System.out.println("free   memory  :  " +df.format((int)(Runtime.getRuntime().freeMemory())/1024)  + " KB");
					System.out.println("-----------------------------------------------------------------------------------------------------------------");

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public boolean begin() {
		if (!initializeConfig())return false;
		if (!initializeSession())return false;
		if (!initializeServerPool())return false;
		if (!initializeIoCallbackListener())return true;
		if (!initializeKeepAliveThread())return false;
		launchServer();
		return true;
	}

	public boolean initializeConfig() {
		Config config = Configuration.getInstance();//싱글톤 객체 생성 방식
		listenPort = config.getInt(Config.LISTEN_PORT);
		maxConnection = config.getInt(Config.MAX_CONNECTION);
		memoryBlockCount = config.getInt(Config.MEMORY_BLOCK_SIZE);
		memoryBlockCount = config.getInt(Config.MEMORY_BLOCK_COUNT);

		fileBlockSize = config.getInt(Config.FILE_BLOCK_SIZE);
		fileBlockCount = config.getInt(Config.FILE_BLOCK_COUNT);
		bufferFileName = config.getString(Config.BUFFER_FILENAME);

		byteOrder = config.getString(Config.BYTE_ORDER).toLowerCase().trim().equals("big") ? ByteOrder.BIG_ENDIAN
				: ByteOrder.LITTLE_ENDIAN;

		sessionBufferLength = config.getInt(Config.SESSION_BUFFER_LENGTH);

		acceptSelectThreadCount = config.getInt(Config.ACCEPT_SELECT_THREAD_COUNT);
		readSelectThreadCount = config.getInt(Config.READ_SELECT_THREAD_COUNT);

		acceptProcessorTheadMaxCount = config.getInt(Config.ACCEPT_PROCESSOR_THREAD_MAX_COUNT);
		acceptProcessorThreadMinCount = config.getInt(Config.ACCEPT_PROCESSOR_THREAD_MIN_COUNT);
		acceptProcessorThreadClassname = config.getString(Config.ACCEPT_PROCESSOR_THREAD_CLASSNAME);

		readWriteProcessorThreadMaxCount = config.getInt(Config.READWRITE_PROCESSOR_THREAD_MAX_COUNT);
		readWriteProcessorThreadMinCount = config.getInt(Config.READWRITE_PROCESSOR_THREAD_MIN_COUNT);
		readWriteProcessorThreadClassname = config.getString(Config.READWRITE_PROCESSOR_THREAD_CLASSNAME);

		useCircularQueue = config.getBoolean(Config.USE_QUEUE_CIRCULA);
		circularQueueSize = config.getInt(Config.MAX_QUEUE_LENGTH);

		encodingSetString = config.getString(Config.CHARSET_FOR_ENCODING);
		decodingSetString = config.getString(Config.CHARSET_FOR_DECODING);

		if (encodingSetString == null){
			encodingSetString = Charset.defaultCharset().name();
			encoder = Charset.defaultCharset().newEncoder();
			decoder = Charset.defaultCharset().newDecoder();
		}else {
			encoder = Charset.forName(encodingSetString).newEncoder();
			decoder = Charset.forName(decodingSetString).newDecoder();
		}
		
		objectPoolLimit = config.getInt(Config.OBJECT_POOL_LIMIT);
		
		isConsoleMessage = config.getBoolean(Config.CONSOLE_MESSAGE);
		canKeepAliveThread = config.getBoolean(Config.CAN_KEEP_ALIVE);
		keepAliveInterval = config.getInt(Config.KEEP_ALIVE_INTERVAL);

		return true;
	}

	public boolean initializeKeepAliveThread() {
		if (canKeepAliveThread) {
			keepAliveThread = new KeepAliveThread(encodingSetString, decodingSetString, keepAliveInterval);
			keepAliveThread.start();
		}
		return true;
	}

	public boolean initializeSession() {
		listenSession = new NetworkSession();
		if (!listenSession.begin()) {
			return false;
		}

		connectedsessionManager = new ConnectedSessionManager();
		if (!connectedsessionManager.begin(maxConnection)) {
			return false;
		}
		return true;
	}

	public boolean initializeServerPool() {
		try {
			File bufferFile = new File(bufferFileName);
			if (!bufferFile.exists())
				bufferFile.createNewFile();
			bufferFile.deleteOnExit();

			queue = IOEventQueue.getInstance();

			byteBufferPool = new ByteBufferPool(memoryBlockCount* memoryBolckSize, fileBlockCount * fileBlockSize,bufferFile);
			
			IOAcceptSelectorWorkerThreadPool = new IOAcceptSelectPool(queue,acceptSelectThreadCount, listenPort);
			IORequestSelectorWrokerThreadPool = new IOReadtSelectPool(queue,readSelectThreadCount);
			
			IOAcceptProcessorWokerThreadPool = new ThreadPool(queue,
					acceptProcessorThreadClassname,
					acceptProcessorThreadMinCount, 
					acceptProcessorTheadMaxCount);
			
			OoReadProcessorWorkerThreadPool = new ThreadPool(queue,
					readWriteProcessorThreadClassname,
					readWriteProcessorThreadMinCount,
					readWriteProcessorThreadMaxCount);

			
			
			//Job 이벤트 객체 풀
			ObjectPool<Job> jobPool = new ObjectPool<Job>(new PoolObjectFactory<Job>(){
				public Job createObject() {return new Job();}
			}, objectPoolLimit);
		
			ObjectPool<TCPPacket> tcpPacketPool = new ObjectPool<TCPPacket>(new PoolObjectFactory<TCPPacket>(){
				public TCPPacket createObject() { return new TCPPacket();}
			},objectPoolLimit);
			
			PoolServiceProvider.registerJobPool(jobPool);
			PoolServiceProvider.registerTCPPacketPool(tcpPacketPool);
			PoolServiceProvider.regisertByteBufferPool(byteBufferPool);
			PoolServiceProvider.registerAcceptSelectorPool(IOAcceptSelectorWorkerThreadPool);
			PoolServiceProvider.registerRequestSelectorPool(IORequestSelectorWrokerThreadPool);
			
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public void launchServer() {
		IOAcceptSelectorWorkerThreadPool.startAll();
		IORequestSelectorWrokerThreadPool.startAll();
		IOAcceptProcessorWokerThreadPool.startAll();
		OoReadProcessorWorkerThreadPool.startAll();
	}

	public boolean initializeIoCallbackListener() {
		IOAcceptSelectorWorkerThreadPool.setOnIoCallbakcListener(this);
		IORequestSelectorWrokerThreadPool.setOnIoCallbakcListener(this);
		IOAcceptProcessorWokerThreadPool.setOnIoCallbakcListener(this);
		OoReadProcessorWorkerThreadPool.setOnIoCallbakcListener(this);
		return true;
	}
}
