package ean.network.threadsync;



public  class Mutex implements Semaphore {
	// Mutex의 Owner, null이면 아무것도 없다.
	private Thread m_owner = null;
	private int m_iLockcount = 0;

	private final int m_id = Lock_manager.new_id();

	/**
	 * 객체를 위한 식별 ID를 반환한다.
	 */
	public int id() {
		return m_id;
	}

	/**
	 * 매개변수의 값에 따라 적당한 뮤텍스를 얻는다. </br>같은 스레드에 의해서라면 뮤텍스는 여러번 얻을 수 있다.</br>
	 * 또한, 얻은만큼의 해당 뮤텍스를 놓을 수도 있다.뮤텍스를 얻으려한 스레드는 뮤텍스를 얻기 전까지 블로킹 된다.
	 * 
	 * @param lTimeout
	 *            - blocking timeout
	 * @return 뮤텍스 얻기 유무, 인자가 0일때 false 이면 뮤텍스 얻기 실패이다.
	 * @throws InterruptedException
	 *             - 타임아웃이 다 지나기 전에 인터럽트가 걸린 경우 발생한다.
	 * @throws Timed_out
	 *             - HSemaphore.Timed_out 얻고자하는 뮤텍스를 타임아웃 내에 얻을 수 없다면 발생한다.
	 */
	public synchronized boolean acquire(long lTimeout)
			throws InterruptedException, Semaphore.Timed_out {
		if (lTimeout == 0) {
			return acquire_without_blocking();
		}
		// 락을 획득할 때까지 영원이 블럭된다.
		else if (lTimeout == FOREVER) {
			while (!acquire_without_blocking())
				// spin_lock
				this.wait(FOREVER);
		}
		// 타임아웃 만큼 블럭된다.
		else {
			long lExpiration = System.currentTimeMillis() + lTimeout;
			while (!acquire_without_blocking()) {
				long lTime_remaining = lExpiration - System.currentTimeMillis();
				if (lTime_remaining <= 0)
					throw new Semaphore.Timed_out(
							"Timed out waiting for Mutex");

				this.wait(lTime_remaining);
			}
		}
		return true;
	}

	/**
	 * 뮤텍스를 얻되, 뮤텍스를 얻을 때까지 영원히 블럭킹이 된다
	 * 
	 * @throws InterruptedException
	 * @throws Semaphore.Timed_out
	 */
	public void acquire() throws InterruptedException, Semaphore.Timed_out {
		acquire(FOREVER);
	}

	/**
	 * non-blocking 뮤텍스를 얻는다.
	 * 
	 * @return 뮤텍스 얻기 여부
	 */
	private boolean acquire_without_blocking() {
		Thread current = Thread.currentThread();

		if (m_owner == null) {
			m_owner = current;
			m_iLockcount = 1;
		} else if (m_owner == current) {
			++m_iLockcount;
		}

		return m_owner == current;
	}

	/**
	 * 뮤텍스를 놓는다. 뮤텍스는 얻은 만큼 놓아야만 하고, 뮤텍스는 자신이 얻은 스레드로부터 놓아져야만 한다.
	 * 
	 * @exception Semaphore.Ownership
	 *                - HSemaphore.Ownership : 만약 뮤텍스를 얻은 스레드가 아닌 다른 스레드에서 뮤텍스를
	 *                놓으려 하면 RuntimeException 을 상속받은 예외가 발생. 또한 뮤텍스를 얻지 않은 상태에서
	 *                놓으려 해도 발생한다.
	 */

	public synchronized void release() {
		if (m_owner != Thread.currentThread())
			throw new Semaphore.Ownership();

		if (--m_iLockcount <= 0) {
			m_owner = null;
			notify();
		}
	}
}
