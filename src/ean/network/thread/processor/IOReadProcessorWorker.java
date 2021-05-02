package ean.network.thread.processor;

import java.io.IOException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

import ean.network.event.Job;
import ean.network.event.callback.IoCallbackListener;
import ean.network.event.callback.IoDiconnectedEventArgs;
import ean.network.event.callback.IoReadEventArgs;
import ean.network.pool.PoolServiceProvider;
import ean.network.queue.Queue;
import ean.network.session.extended.ConnectedSession;

public class IOReadProcessorWorker extends Thread {

	private static int theadIndex = 0;
	private Queue queue = null;
	private IoCallbackListener listener = null;
	ArrayList<Job> readJobList = new ArrayList<Job>();
	private String name = "IOReadProcessorWorker-";
	
	public IOReadProcessorWorker(Queue queue) {
		this.queue = queue;
		theadIndex++;
		setName(name+theadIndex);
	}

	public void run() {
		while (!Thread.currentThread().isInterrupted()) {
			// transactionOneReadEvent();
			// 메모리 부족을 방지하기 위해서 큐에서 이벤트를 모두 한번에 가져 온 후 원본 큐의 이벤트는 제거함
			transactionAllreadEventEx();
		}
	}

	public void transactionAllreadEventEx() {
		queue.popAllReadEvent(readJobList);
		int size = readJobList.size();
		for (int i = size - 1; i >= 0; i--) {
			IoDiconnectedEventArgs diconnectedEvent = null;
			IoReadEventArgs readEvent = null;

			Job job = readJobList.remove(i);
			SelectionKey key = (SelectionKey) job.getSession().get(
					"SelectionKey");
			SocketChannel sc = (SocketChannel) key.channel();
			ConnectedSession session = (ConnectedSession) key.attachment();

			// 세션이 널이라면 정상정료(readbytes < 0 인경우) 및 강제종료에 대한 처리를 완료한 상태이기 때문에
			// continue 한다.
			// 종료를 한다고 하더라도, 실제 처리되기 까지 계속 read 이벤트가 발생하기 때문에 이 처리는 반드시 필요하다.
			try {
				if (sc.isConnected() && sc.isOpen() && key.isValid() && sc.socket().isConnected() && key.attachment() != null){
				}else {
					continue;
				}
				int readBytes = session.readPacket();
				if (readBytes < 0 ) {
					if (listener != null && key.attachment() != null && sc.isOpen()) {
						System.out.println("ConnectedSession disconnected ["+ session.getSessinId() + "]");
						
						diconnectedEvent = new IoDiconnectedEventArgs(key);
						listener.onIoDisconnected(session, diconnectedEvent);
					}
				} else {
					if (readBytes != 0) {
						if (listener != null) {
							readEvent = new IoReadEventArgs(session,key,session.getBytesToAdd());
							System.out.println("["+getName()+ "] : The Length of ReadBytes from SESSION_ID [" + 
									session.getSessinId() + "] " +" :  " + readBytes + " byte");
							
							this.listener.onIoRead(this, readEvent);
						}
					}
				}
			} catch (ClosedByInterruptException e) {
				/*
				 * System.out.println("ClosedByInterruptException발생");
				 * e.printStackTrace();
				 */
			} catch (AsynchronousCloseException e) {
				/*
				 * 하나의 스레드에서 채널 I/O 작업을 하고 있는 중에 다른 스레드에서 같은 채널을 닫았을 때 현재의 스레드에서
				 * 이 익셉션이 발생한다. 닫은 스레드에서 채널에 대한 종료 작업을 했을 것이기 때문에 아무 처리를 하지 않는다.
				 * System.out.println("AsynchronousCloseException 발생");
				 * e.printStackTrace();
				 */
			} catch (NotYetConnectedException e) {
				/*
				 * 연결되지 않은 채널에 대해서 I/O 작업을 할 경우 발생. 아무 처리도 할 필요 없다.
				 * System.out.println("NotYetConnectedException 발생");
				 * e.printStackTrace();
				 */
			} catch (ClosedChannelException e) {
				/*
				 * 닫힌 채널에 대해서 I/O 작업을 할 경우 발생한다. 이경우는 종료처리가 된 상태이기 때문에 아무런 처리를
				 * 해주지 않는다. System.out.println("ClosedChannelException 발생");
				 * e.printStackTrace();
				 */
			} catch (IOException e) {
				/*
				 * 클라이언트가 프로그램의 소켓 close 절차가 있는 정상종료 가 아니라 강제 종료했을 경우, read 하면
				 * "원격에서 강제종료했습니다" 라는 에러메시이와 함께 IOException 이 발생한다. 이때 종료처리를 해주게
				 * 된다.
				 */
				if (sc.isOpen()){
					System.out.println("[원격호스트에 의한 강제 접속 종료] " + session.getSessinId() );
				}
				/* e.printStackTrace(); */
				if (listener != null) {
					IoDiconnectedEventArgs event = new IoDiconnectedEventArgs(
							key);
					event.threadName = getName();
					try {
						listener.onIoDisconnected(session, event);
					} catch (IOException e1) {
					}
				}
			} finally {
				diconnectedEvent = null;
				readEvent = null;
			}
			PoolServiceProvider.getJobPool().freeObject(job);
		}

		if (readJobList.size() > 0)
			readJobList.clear();

	}

	/*public void transactionOneReadEvent() {
		readJobList = new ArrayList<Job>();
		IByteBufferPool bufferPool = PoolServiceProvider.getByteBufferPool();
		ByteBuffer buffer = bufferPool.getMemoryBuffer();
		SelectionKey key = null;
		Iterator<Job> it = null;

		SocketChannel sc = null;
		int readBytes;
		Job job = null;

		IoDiconnectedEventArgs diconnectedEvent = null;
		IoReadEventArgs readEvent = null;
		queue.popAllReadEvent(readJobList);
		it = readJobList.iterator();

		while (it.hasNext()) {
			try {
				job = it.next();
				key = (SelectionKey) job.getSession().get("SelectionKey");
				sc = (SocketChannel) key.channel();
				if (sc.isOpen() && key.isValid() && sc.isConnected()) {
					readBytes = sc.read(buffer);
				} else
					continue;

				if (readBytes < 0) { // 읽은 바이트 수가 -1 이면 정상 종료한 상태
					if (listener != null) {
						diconnectedEvent = new IoDiconnectedEventArgs(key);
						listener.onIoDisconnected(this, diconnectedEvent);
					}
				} else {
					if (listener != null) {
						buffer.flip();
						readEvent = new IoReadEventArgs(session,key, readBytes);
						this.listener.onIoRead(this, readEvent);
					}
				}
				buffer.rewind();
			} catch (ClosedByInterruptException e) {
				System.out.println("ClosedByInterruptException발생");
				e.printStackTrace();
			} catch (AsynchronousCloseException e) {
				System.out.println("AsynchronousCloseException 발생");
				e.printStackTrace();
			} catch (NotYetConnectedException e) {
				System.out.println("NotYetConnectedException 발생");
				e.printStackTrace();
			} catch (ClosedChannelException e) {
				System.out.println("ClosedChannelException 발생");
				e.printStackTrace();
			} catch (IOException e) {
				
				 * 클라이언트가 프로그램의 소켓 close 절차가 있는 정상종료 가 아니라 강제 종료했을 경우, read 하면
				 * "원격에서 강제종료했습니다" 라는 에러메시이와 함께 IOException 이 발생한다.
				 
				System.out.println("IOException 발생 [원격호스트에 의한 강제 종료] ");
				if (listener != null) {
					IoDiconnectedEventArgs event = new IoDiconnectedEventArgs(
							key);
					event.threadName = getName();
					try {
						listener.onIoDisconnected(this, event);
					} catch (IOException e1) {
					}
				}
			} finally {
				buffer.rewind();
			}
		}
		readJobList.clear();
		readJobList = null;
		bufferPool.putBuffer(buffer);
	}*/

	/*public void transactionAllReadEvent() {
		readJobList = new ArrayList<Job>();
		IByteBufferPool bufferPool = PoolServiceProvider.getByteBufferPool();
		ByteBuffer buffer = bufferPool.getMemoryBuffer();
		SelectionKey key = null;
		Iterator<Job> it = null;

		SocketChannel sc = null;
		int readBytes;

		IoDiconnectedEventArgs diconnectedEvent = null;
		IoReadEventArgs readEvent = null;
		queue.popAllReadEvent(readJobList);
		it = readJobList.iterator();

		while (it.hasNext()) {
			Job job =  it.next();
			try {
				key = (SelectionKey) job.getSession().get("SelectionKey");
				sc = (SocketChannel) key.channel();
				if (sc.isOpen() && key.isValid() && sc.isConnected()) {
					readBytes = sc.read(buffer);
				} else
					continue;

				if (readBytes < 0) { // 읽은 바이트 수가 -1 이면 정상 종료한 상태
					if (listener != null) {
						diconnectedEvent = new IoDiconnectedEventArgs(key);
						listener.onIoDisconnected(this, diconnectedEvent);
					}
				} else {
					if (listener != null) {
						buffer.flip();
						readEvent = new IoReadEventArgs(key, readBytes);
						this.listener.onIoRead(this, readEvent);
					}
				}
				buffer.rewind();
			} catch (ClosedByInterruptException e) {
				System.out.println("ClosedByInterruptException발생");
				e.printStackTrace();
			} catch (AsynchronousCloseException e) {
				System.out.println("AsynchronousCloseException 발생");
				e.printStackTrace();
			} catch (NotYetConnectedException e) {
				System.out.println("NotYetConnectedException 발생");
				e.printStackTrace();
			} catch (ClosedChannelException e) {
				System.out.println("ClosedChannelException 발생");
				e.printStackTrace();
			} catch (IOException e) {
				
				 * 클라이언트가 프로그램의 소켓 close 절차가 있는 정상종료 가 아니라 강제 종료했을 경우, read 하면
				 * "원격에서 강제종료했습니다" 라는 에러메시이와 함께 IOException 이 발생한다.
				 
				System.out.println("IOException 발생 [원격호스트에 의한 강제 종료] ");
				if (listener != null) {
					IoDiconnectedEventArgs event = new IoDiconnectedEventArgs(
							key);
					event.threadName = getName();
					try {
						listener.onIoDisconnected(this, event);
					} catch (IOException e1) {
					}
				}
			} finally {
				buffer.rewind();
			}
		}
		readJobList.clear();
		readJobList = null;
		bufferPool.putBuffer(buffer);
	}*/

	public void setOnIoCallbackListener(IoCallbackListener listener) {
		this.listener = listener;
	}

}
