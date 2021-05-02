package ean.network.event.callback;

import java.nio.channels.SocketChannel;

public class IoAcceptEventArgs {
	public SocketChannel socketChannel;
	public IoAcceptEventArgs(SocketChannel socketChannel) {
		this.socketChannel = socketChannel;
	}

	public SocketChannel getSocketChannel() {
		return socketChannel;
	}
	public void setSocketChannel(SocketChannel socketChannel) {
		this.socketChannel = socketChannel;
	}
}
