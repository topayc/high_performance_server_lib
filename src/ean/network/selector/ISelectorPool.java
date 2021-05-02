package ean.network.selector;

import ean.network.event.callback.IoCallbackListener;

public interface ISelectorPool {
	
	public Thread get();
	public void put(Thread handler);
	public int size();
	public boolean isEmpty();
	public void startAll();
	public void stopAll();
	public void setOnIoCallbakcListener (IoCallbackListener listener);

}
