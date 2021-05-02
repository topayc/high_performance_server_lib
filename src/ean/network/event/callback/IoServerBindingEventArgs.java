package ean.network.event.callback;

import java.nio.channels.ServerSocketChannel;

public class IoServerBindingEventArgs {
	public ServerSocketChannel serverSocketChannel;

	public IoServerBindingEventArgs(ServerSocketChannel serverSocketChannel) {
		this.serverSocketChannel = serverSocketChannel;
	}

	public ServerSocketChannel getServerSocketChannel() {
		return serverSocketChannel;
	}

	public void setServerSocketChannel(ServerSocketChannel serverSocketChannel) {
		this.serverSocketChannel = serverSocketChannel;
	}

}
