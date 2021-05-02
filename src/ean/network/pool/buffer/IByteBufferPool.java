package ean.network.pool.buffer;

import java.nio.ByteBuffer;

public interface IByteBufferPool {

	public ByteBuffer getMemoryBuffer();

	public ByteBuffer getFileBuffer();

	public void putBuffer(ByteBuffer buffer);

	public void setWait(boolean wait);

	public boolean isWait();

}
