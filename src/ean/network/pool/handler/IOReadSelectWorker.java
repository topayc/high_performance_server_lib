package ean.network.pool.handler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import ean.network.event.Job;
import ean.network.event.NetworkEvent;
import ean.network.event.callback.IoCallbackListener;
import ean.network.pool.PoolServiceProvider;
import ean.network.queue.Queue;

class RequestChannelRegisterObject {
	public SocketChannel socketChannel;
	public Object object;

	public RequestChannelRegisterObject(SocketChannel socketChannel,
			Object object) {
		this.socketChannel = socketChannel;
		this.object = object;
	}
}

public class IOReadSelectWorker extends HandlerAdaptor {

	private Queue queue = null;
	private Selector selector = null;
	private String name = "IOReadSelectWorker-";
	private IoCallbackListener listener = null;

	private ArrayList<RequestChannelRegisterObject> newClients = new ArrayList<RequestChannelRegisterObject>();

	public IOReadSelectWorker(Queue queue, Selector selector, int index) {
		this.queue = queue;
		this.selector = selector;
		setName(name + index);
	}

	public void run() {
		try {
			while (!Thread.currentThread().isInterrupted()) {
				processNewConnection();
				int keysReady = selector.select(1000);
				// System.out.println("@RequestHandler(" + getName() +
				// ") selected : " + keysReady);
				if (keysReady > 0) {
					processRequest();
				}
			}
		} catch (Exception e) {
		}
	}

	public synchronized void processNewConnection()
			throws ClosedChannelException {
		Iterator<RequestChannelRegisterObject> iter = newClients.iterator();
		while (iter.hasNext()) {
			RequestChannelRegisterObject ro = (RequestChannelRegisterObject) iter.next();
			ro.socketChannel.register(selector, SelectionKey.OP_READ, ro.object);
			System.out.println("["+ getName()+ "]  registered successfully  / "
					+ ro.socketChannel.socket().getInetAddress()
							.getHostAddress() + " : "
					+ ro.socketChannel.socket().getPort());
		}
		newClients.clear();
	}

	private void processRequest() throws IOException {
		
		Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
		while (iter.hasNext()) {
			SelectionKey key = iter.next();
			SocketChannel sc = (SocketChannel) key.channel();
			iter.remove();
			if (key.isValid() && sc.socket().isConnected()
					&& sc.keyFor(selector).attachment() != null && sc.isOpen()) {
				pushMyJob(key);
			} else {
				continue;
			}
		}
		
	}

	private void pushMyJob(SelectionKey key) {
		Map<String, Object> session = new HashMap<String, Object>();
		session.put("SelectionKey", key);
		
		Job job = PoolServiceProvider.getJobPool().getObject();
		job.setEventType(NetworkEvent.READ_EVENT);
		job.setSession(session);
	
		/*Job job = new Job(NetworkEvent.READ_EVENT, session);*/
		queue.push(job);
		
	}

	public synchronized void addClient(SocketChannel socketChannel,
			Object object) {
		newClients.add(new RequestChannelRegisterObject(socketChannel, object));
	}

	public synchronized void setOnIoCallbackListener(IoCallbackListener listener) {
		this.listener = listener;
	}

}
