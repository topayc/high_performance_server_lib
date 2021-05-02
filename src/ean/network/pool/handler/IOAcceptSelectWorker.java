package ean.network.pool.handler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import ean.network.event.Job;
import ean.network.event.NetworkEvent;
import ean.network.event.callback.IoCallbackListener;
import ean.network.event.callback.IoServerBindingEventArgs;
import ean.network.pool.PoolServiceProvider;
import ean.network.queue.Queue;


public class IOAcceptSelectWorker extends Thread {
	
	private IoCallbackListener listener = null;
	private Queue queue = null;
	private Selector selector = null;
	private int port = 9090;
	private String name = "IOAccpetSelectWorker-";
	ServerSocketChannel ssc;
	
	public IOAcceptSelectWorker(Queue queue, Selector selector, int port, int index) {
		this.queue = queue;
		this.selector = selector;
		this.port = port;
		setName(name + index);
		init();
	}
	
	private void init() {
		try {
			ssc = ServerSocketChannel.open();
			ssc.socket().setReuseAddress(true);
			ssc.configureBlocking(false);
			
			InetSocketAddress address = new InetSocketAddress("localhost", port);
			ssc.socket().bind(address,100);
			
			ssc.register(this.selector, SelectionKey.OP_ACCEPT);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		try {
			if (listener != null){
				IoServerBindingEventArgs event = new IoServerBindingEventArgs(ssc);
				listener.onIoChannelBinding(this,event);
			}
		}catch(Exception e){}
		
		try {
			while (!Thread.currentThread().isInterrupted()) {
				/*int keysReady =*/ selector.select();
				acceptPendingConnections();
			}
		} catch (Exception e) {
			//e.printStackTrace();
		}
	}
	
	private void acceptPendingConnections() throws Exception {
		Iterator<SelectionKey> iter = selector.selectedKeys().iterator();	
		while (iter.hasNext()) {
			SelectionKey key = iter.next();
			iter.remove();
			
			ServerSocketChannel readyChannel = (ServerSocketChannel) key.channel();
			SocketChannel sc = readyChannel.accept();
			
			System.out.println("[" + getName() + "]  is asking to be connected from  " + sc.socket().getInetAddress() + ":" + sc.socket().getPort() );
			
			pushMyJob(sc);
		}
	}
	
	private void pushMyJob(SocketChannel sc) {
		Map<String, Object> session = new HashMap<String, Object>();
		session.put("SocketChannel", sc);

		Job job = PoolServiceProvider.getJobPool().getObject();
		job.setEventType(NetworkEvent.ACCEPT_EVENT);
		job.setSession(session);
/*		Job job = new Job(NetworkEvent.ACCEPT_EVENT, session);*/
		queue.push(job);
	}
	
	public void setOnIoCallbackListener(IoCallbackListener listener){
		this.listener = listener;
	}

}
