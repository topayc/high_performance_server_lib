package ean.network.selector;

import java.io.IOException;
import java.nio.channels.Selector;
import java.util.Iterator;

import ean.network.event.callback.IoCallbackListener;
import ean.network.pool.handler.IOAcceptSelectWorker;
import ean.network.queue.Queue;




public class IOAcceptSelectPool extends AbstractSelectorPool {
	
	private int port = 9090;
	private Queue queue = null;
	

	public IOAcceptSelectPool(Queue queue) {
		this(queue, 1, 9090);
	}
	
	public IOAcceptSelectPool(Queue queue, int size, int port) {
		super.size = size;		
		this.queue = queue;
		this.port = port;
		init();
	}
	
	private void init() {
		for (int i = 0; i < size; i++) {
			pool.add(createHandler(i));
		}
	}
	
	
	protected Thread createHandler(int index) {
		Selector selector = null;
		try {
			selector = Selector.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Thread handler = new IOAcceptSelectWorker(queue, selector, port, index);
		return handler;
	}


	public void startAll() {
		Iterator<Thread> iter = pool.iterator();
		while (iter.hasNext()) {
			Thread handler = iter.next();
			handler.start();
		}
	}


	public void stopAll() {
		Iterator<Thread> iter = pool.iterator();
		while (iter.hasNext()) {
			Thread handler = iter.next();
			handler.interrupt();
			handler = null;
		}
	}

	@Override
	public void setOnIoCallbakcListener(IoCallbackListener listener) {
		for (int i = 0; i<pool.size();i++){	
			IOAcceptSelectWorker  worker = (IOAcceptSelectWorker)pool.get(i);
			worker.setOnIoCallbackListener(listener);
		}
	}
	

}
