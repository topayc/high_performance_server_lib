package ean.network.selector;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractSelectorPool implements ISelectorPool {
	
	protected int size = 2;
	
	private int roundRobinIndex = 0;
	
	private final Object monitor = new Object();
	protected final List<Thread> pool = new ArrayList<Thread>();
	
	protected abstract Thread createHandler(int index);
	public abstract void startAll();
	public abstract void stopAll();


	public Thread get() {
		synchronized (monitor) {
			return  pool.get( roundRobinIndex++ % size );
		}
	}

	
	public void put(Thread handler) {
		synchronized (monitor) {
			if (handler != null) {
				pool.add(handler);				
			}
			monitor.notify();
		}
	}

	/* (non-Javadoc)
	 * @see net.daum.javacafe.pool.SelectorPoolIF#size()
	 */
	public int size() {
		synchronized (monitor) {
			return pool.size();
		}
	}

	/* (non-Javadoc)
	 * @see net.daum.javacafe.pool.SelectorPoolIF#isEmpty()
	 */
	public boolean isEmpty() {
		synchronized (monitor) {
			return pool.isEmpty();
		}
	}
	
	

}
