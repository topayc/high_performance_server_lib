package ean.network.thread;

import ean.network.event.callback.IoCallbackListener;

public interface IThreadPool {
	
	public void addThread();
	public void removeThread();
	public void startAll();	
	public void stopAll();
	public void setOnIoCallbakcListener (IoCallbackListener listener);

}
