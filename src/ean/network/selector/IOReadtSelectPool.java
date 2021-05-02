
package ean.network.selector;

import java.io.IOException;
import java.nio.channels.Selector;
import java.util.Iterator;
import ean.network.event.callback.IoCallbackListener;
import ean.network.pool.handler.IOReadSelectWorker;
import ean.network.queue.Queue;

public class IOReadtSelectPool extends AbstractSelectorPool {

	private Queue queue = null;

	public IOReadtSelectPool(Queue queue) {
		this(queue, 2);
	}
	
	public IOReadtSelectPool(Queue queue, int size) {
		super.size = size;
		this.queue = queue;
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
		Thread handler = new IOReadSelectWorker(queue, selector, index);
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
			Thread handler =  iter.next();
			handler.interrupt();
			handler = null;
		}
	}

	@Override
	public void setOnIoCallbakcListener(IoCallbackListener listener) {
		for (int i = 0; i<pool.size();i++){	
			IOReadSelectWorker  worker = (IOReadSelectWorker)pool.get(i);
			worker.setOnIoCallbackListener(listener);
		}	
	}

}
