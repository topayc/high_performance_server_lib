package ean.network.event.callback;

import java.nio.channels.SelectionKey;

public class IoDiconnectedEventArgs {
	public SelectionKey key;
	public String threadName; 
	public IoDiconnectedEventArgs(SelectionKey key) {
		this.key = key;
	}
	public SelectionKey getKey() {
		return key;
	}

	
}
