package ean.network.threadsync;

public interface Semaphore {
	 /*
		타임아웃 값으로 사용할 수 있는 것으로 영원히 기다린다는
		의미로서 사용된다(실제값으로는 292,271,023년 동안 기다리는 것이다)
	*/
	public static final long FOREVER = Long.MAX_VALUE;

	// 이 객체를 위한 식별 ID를 반환한다.
	public int id();

	/**
	 * 뮤텍스를 얻는다. 같은 스레드에 의해서라면 뮤텍스는 여러번 얻을 수 있다.또한, 얻은 만큼의 해당 뮤텍스를 놓을 수도
	 * 있다.</br> 뮤텍스를 얻으려한 스레드는 뮤텍스를 얻기 전까지 블로킹 된다.
	 * 
	 * @param lTimeout
	 *            - blocking timeout
	 * @return 뮤텍스 얻기 유무, 인자가 0일때 false이면 뮤텍스 얻기 실패이다.
	 * @throws InterruptedException
	 *             - 타임아웃이 다 지나기 전에 인터럽트가 걸린 경우 발생한다.
	 * @throws Timed_out
	 *             - HSemaphore.Timed_out 우리가 얻고자하는 뮤텍스를 타임아웃 내에 얻을 수 없다면 발생한다.
	 */
	public boolean acquire(long lTimeout) throws InterruptedException,Timed_out;

	/**
	 * 뮤텍스를 놓는다. 뮤텍스는 얻은 만큼 놓아야만 한다.
	 * HSemaphore.Ownership : 만약 뮤텍스를 얻은 스레드가 아닌 다른 스레드에서 뮤텍스를 놓으려 하면 </br>
	 * RuntimeException 을 상속받은 예외가 발생.또한 뮤텍스를 얻지 않은 상태에서 놓으려 해도 발생한다.
	 */
	public void release();

	/**
	 * 타임 아웃의 상황에서 발생하는 예외 클래스
	 */
	@SuppressWarnings("serial")
	public static final class Timed_out extends RuntimeException {
		public Timed_out() {
			super("Timed out while waiting to acquire semaphore");
		}

		public Timed_out(String s) {
			super(s);
		}
	}

	/**
	 * 어떤 스레드에서 세마포어를 잘못 놓아주려는 경우 발생하는 예외 클래스 </br>
	 * 즉, 세마포어에 대한 소유권이 없는 상태에서 세마포어를 놓으려는 경우에 발생한다.
	 * @author Administrator
	 */
	@SuppressWarnings("serial")
	public static final class Ownership extends RuntimeException {
		public Ownership() {
			super("Calling Thread doesn't own Semaphore");
		}
	}
}