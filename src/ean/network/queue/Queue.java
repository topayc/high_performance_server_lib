package ean.network.queue;

import java.util.ArrayList;

import ean.network.event.Job;



public interface Queue {
	
	public Job pop(int eventType);
	public void push(Job job);
	public void popAllReadEvent(ArrayList<Job> list);
	public void popAllAccpetEvent(ArrayList<Job> list);
	public int geRemainedReadEventQueueSize();
	public int getRemainedAcceptQueueSize();

}
