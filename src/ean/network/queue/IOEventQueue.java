package ean.network.queue;

import java.util.ArrayList;
import java.util.List;

import ean.network.event.Job;
import ean.network.event.NetworkEvent;

public class IOEventQueue implements Queue {

	private final Object acceptMonitor = new Object();
	private final Object readMonitor = new Object();
	
	private final List<Job> acceptQueue = new ArrayList<Job>();
	private final List<Job> readQueue = new ArrayList<Job>();
	
	private static IOEventQueue instance = new IOEventQueue();
	
	public static IOEventQueue getInstance() {
		if (instance == null) {
			synchronized (IOEventQueue.class) {
				instance = new IOEventQueue();
			}
		}
		return instance;
	}
	
	private IOEventQueue() {}
	
	public int geRemainedReadEventQueueSize(){
		synchronized (readMonitor) {
			return readQueue.size();
		}
	}
	
	public int getRemainedAcceptQueueSize(){
		synchronized (acceptMonitor) {
			return acceptQueue.size();
		}
			
		
	}
	public Job pop(int eventType) {
		switch (eventType) {
			case NetworkEvent.ACCEPT_EVENT : return getAcceptJob();
			case NetworkEvent.READ_EVENT   : return getReadJob();
			default : throw new IllegalArgumentException("Illegal EventType..");
		}
	}
	
  
	private Job getAcceptJob() {
		synchronized (acceptMonitor) {
			if (acceptQueue.isEmpty()) {
				try {
					acceptMonitor.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			acceptMonitor.notify();
			return  acceptQueue.remove(0);
		}
	}

	private Job getReadJob() {
		synchronized (readMonitor) {
			if (readQueue.isEmpty()) {
				try {
					readMonitor.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			readMonitor.notify();
			return  readQueue.remove(0);
		}
	}

	public void push(Job job) {
		if (job != null) {
			int eventType = job.getEventType();
			switch (eventType) {
				case NetworkEvent.ACCEPT_EVENT : putAcceptJob(job); break;
				case NetworkEvent.READ_EVENT   : putReadJob(job); break;
				default : throw new IllegalArgumentException("Illegal EventType..");
			}
		}
	}

	private void putAcceptJob(Job job) {
		synchronized (acceptMonitor) {
		if (!acceptQueue.isEmpty()){
				try{
					acceptMonitor.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}			
			acceptQueue.add(job);
			acceptMonitor.notify();
		}
	}

	private void putReadJob(Job job) {
		synchronized (readMonitor) {
			if (!readQueue.isEmpty()){
				try{
					readMonitor.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}	
			readQueue.add(job);
			readMonitor.notify();
		}
	}
	
	public void popAllReadEvent(ArrayList<Job> list){
		synchronized (readMonitor) {
		if (readQueue.isEmpty()){
				try {
					readMonitor.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			int size = readQueue.size();
			for (int i = size -1; i >=0 ; i--){
				list.add(readQueue.remove(i));
			}
			readQueue.clear();
			readMonitor.notify();
		}
	}
	
	public void popAllAccpetEvent(ArrayList<Job> list){
		synchronized (acceptMonitor) {
			if (acceptQueue.isEmpty()){
				try {
					acceptMonitor.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			int size = acceptQueue.size();
			for (int i = size -1; i >=0 ; i--){
				list.add(acceptQueue.remove(i));
			}
			acceptQueue.clear();
			acceptMonitor.notify();
		}
	}
}
