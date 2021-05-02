package ean.network.thread.processor;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;

import ean.network.event.Job;
import ean.network.event.callback.IoAcceptEventArgs;
import ean.network.event.callback.IoCallbackListener;
import ean.network.pool.PoolServiceProvider;
import ean.network.queue.Queue;

public class IOAcceptProcessorWorker extends Thread {
	private static int theadIndex = 0;
	private IoCallbackListener listener = null;
	private Queue queue = null;
	ArrayList<Job> acceptJobList =  new ArrayList<Job>();
	private String name = "IOAcceptProcessorWorker-";

	public IOAcceptProcessorWorker(Queue queue) {
		this.queue = queue;
		theadIndex++;
		setName(name+theadIndex);
	}

	public void run() {
			while (!Thread.currentThread().isInterrupted()) transactionAllAcceptEventEx();		
	}

	public void transactionAllAcceptEventEx() {
		queue.popAllAccpetEvent(acceptJobList);
		SocketChannel socketChannel = null;
	
		IoAcceptEventArgs acceptEvent = null;

		int size = acceptJobList.size();
		for (int i = size - 1; i >= 0; i--) {
			
			Job job = acceptJobList.remove(i);
			socketChannel = (SocketChannel) (job.getSession().get("SocketChannel"));
			try {
				if (listener != null) {
					acceptEvent = new IoAcceptEventArgs(socketChannel);
					System.out.println("[" + getName()  + "] : " +
							"has finished connection from /" + socketChannel.socket().getInetAddress().toString() + ":" +socketChannel.socket().getPort());
					
					listener.onIoAccept(this, acceptEvent);
				}
			} catch (Exception e) {
			}finally{
			}
			acceptEvent = null;
			socketChannel  = null;
			PoolServiceProvider.getJobPool().freeObject(job);
		}
		if (acceptJobList.size() !=0) acceptJobList.clear();
	}

	public void setOnIoCallbackListener(IoCallbackListener listener) {
		this.listener = listener;
	}

}
