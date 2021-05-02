package ean.network.pool.buffer;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import ean.network.server.config.Config;
import ean.network.server.config.Configuration;

public class ByteBufferPool implements IByteBufferPool {

	private static final int MEMORY_BLOCKSIZE = 1024;
	private static final int FILE_BLOCKSIZE = 2048;
	
	private static final int MEMORY_BUFFER_SIZE = 20 * 1024;
	private static final int FILE_BUFFER_SIZE = 40 * 2048;
	private static final String DEFAULT_MEMORY_FILE  = "buffer.tmp";
	
	private final List<ByteBuffer> memoryQueue = new ArrayList<ByteBuffer>();
	private final List<ByteBuffer> fileQueue = new ArrayList<ByteBuffer>();
	
	private boolean isWait = false;
	
	public ByteBufferPool(int memorySize, int fileSize, File file) throws IOException {
		if (memorySize > 0) 
			createMemoryBuffer(memorySize);

		if (fileSize > 0)
			createFileBuffer(fileSize, file);
	}
	
	public ByteBufferPool(){
		createMemoryBuffer(MEMORY_BUFFER_SIZE );
		try {
			createFileBuffer(FILE_BUFFER_SIZE, new File(DEFAULT_MEMORY_FILE ));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void createMemoryBuffer(int size) {
		int bufferCount = size / MEMORY_BLOCKSIZE;
		size = bufferCount * MEMORY_BLOCKSIZE;
		ByteBuffer directBuffer = ByteBuffer.allocateDirect(size);
		setByteOrder(directBuffer);
		divideBuffer(directBuffer, MEMORY_BLOCKSIZE, memoryQueue);
	}
	

	private void createFileBuffer(int size, File f) throws IOException {
		int bufferCount = size / FILE_BLOCKSIZE;
		size = bufferCount * FILE_BLOCKSIZE;
		RandomAccessFile file = new RandomAccessFile(f, "rw");
		try {
			file.setLength(size);
			ByteBuffer fileBuffer = file.getChannel().map(FileChannel.MapMode.READ_WRITE, 0L, size);
			setByteOrder(fileBuffer);
			divideBuffer(fileBuffer, FILE_BLOCKSIZE, fileQueue);
		} finally {
			file.close();
		}
	}
	
	private void setByteOrder(ByteBuffer buffer) {
		ByteOrder byteOrder = Configuration.getInstance().getString(Config.BYTE_ORDER).toLowerCase().trim().equals("big")? 
				ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
		buffer.order(byteOrder);
		
	}

	private void divideBuffer(ByteBuffer buf, int blockSize, List<ByteBuffer> list) {
		int bufferCount = buf.capacity() / blockSize;
		int position = 0;
		for (int i = 0; i < bufferCount; i++) {
			int max = position + blockSize;
			buf.limit(max);
			list.add(buf.slice());
			position = max;
			buf.position(position);
		}
	}
	
	public ByteBuffer getMemoryBuffer() {
		return getBuffer(memoryQueue, fileQueue);
	}

	public ByteBuffer getFileBuffer() {
		return getBuffer(fileQueue, memoryQueue);
	}

	private ByteBuffer getBuffer(List<ByteBuffer> firstQueue, List<ByteBuffer> secondQueue) {
		ByteBuffer buffer = getBuffer(firstQueue, false);
		if (buffer == null) {
			buffer = getBuffer(secondQueue, false);
			if (buffer == null) {
				if (isWait)
					buffer = getBuffer(firstQueue, true);
				else
					buffer = ByteBuffer.allocate(MEMORY_BLOCKSIZE);
			}
		}
		return buffer;
	}

	private ByteBuffer getBuffer(List<ByteBuffer> queue, boolean wait) {
		synchronized (queue) {
			if (queue.isEmpty()) {
				if (wait) {
					try {
						queue.wait();
					} catch (InterruptedException e) {
						return null;
					}
				} else {
					return null;
				}
			}
			return (ByteBuffer) queue.remove(0);
		}
	}

	public void putBuffer(ByteBuffer buffer) {
		if (buffer.isDirect()) {
			switch (buffer.capacity()) {
				case MEMORY_BLOCKSIZE :
					putBuffer(buffer, memoryQueue);
					break;
				case FILE_BLOCKSIZE :
					putBuffer(buffer, fileQueue);
					break;
			}
		}
	}
	
	private void putBuffer(ByteBuffer buffer, List<ByteBuffer> queue) {
		buffer.clear();
		synchronized (queue) {
			queue.add(buffer);
			queue.notify();
		}
	}
	
	public synchronized void setWait(boolean wait) { this.isWait = wait; }	
	public synchronized boolean isWait() { return isWait; }
}
