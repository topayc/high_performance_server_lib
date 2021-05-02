package ean.network.threadsync;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;




/**
 * 세마포어의 관리와 교착상태를 피하는 락
 * 락을 그룹으로 관리하기 위해 만들어진 클래스
 * 이 클래스를 사용하면 일련의 락을 교착상태 없이 안전하게 얻을 수 있다.
 */
class Lock_manager {
	private static Object m_oLock = new int[] { 0 };
	private static int m_iIDPool = 0;

	private Lock_manager() {
	};

	/**
	 * 새로운 식별자를 반환한다.(세마포어의 id()에 사용하기 위해 필요)
	 * 어떤 스레드에서 new_id()를 호출하는 동안에도 acquire_multiple()을 호출할 수 있도록 하기 위해서 </br>
	 * 일부러 new_id() 메서드 자체를 동기화하는 대신 , 별도의 락 객체를 사용했다.
	 * @return 정수형의 식별자 
	 */
	public static int new_id() {
		synchronized (m_oLock) {
			return m_iIDPool++;
		}
	}

	/**
	 * ID 순서대로 락을 정렬하기 위해서 사용
	 */
	private static final Comparator compare_strategy = new Comparator() {
		public int compare(Object a, Object b) {
			return ((Semaphore) a).id() - ((Semaphore) b).id();
		}

		public boolean equals(Object obj) {
			return obj == this;
		}
	};

	/**
	 * 인자로부터 받은 모든 락이 얻어지면 복귀한다.
	 * 교착상태가 일어나지 않게 하기 위해 락은 언제나 ID의 오름차순으로 얻어진다. 만약 타임아웃이나 인터럽트로 인해 락을 얻지 못했을 경우 </br>
	 * 그 때까지 얻은 모든 락을 놓아준다.</br>
	 * 메서드가 실행중에 locks 배열 안의 내용이 바뀌지 않도록 해주어야 한다.
	 * @param locks - 락을 얻을 객체 배열
	 * @param lTimeout - Timeout
	 * @throws InterruptedException
	 * @throws Semaphore.Timed_out
	 */
	public static void acquire_multiple(Semaphore[] locks, long lTimeout)
			throws InterruptedException, Semaphore.Timed_out {
		acquire(locks, lTimeout);
	}

	/**
	 * 인자로부터 받은 모든 락이 얻어지면 복귀한다. 교착상태가 일어나지 않게 하기 위해 락은 언제나 ID의 오름차순으로 얻어진다. </br>
	 * 만약 타임아웃이나 인터럽트로 인해 락을 얻지 못했을 경우 그 때까지 얻은 모든 락을 놓아준다.
	 * @param semaphores - 락을 얻을 객체 배열
	 * @param lTimeout - Timeout
	 * @throws InterruptedException
	 * @throws Semaphore.Timed_out
	 */

	public static void acquire_multiple(Collection semaphores, long lTimeout)
			throws InterruptedException, Semaphore.Timed_out {
		acquire(semaphores.toArray(), lTimeout);
	}

	/**
	 * 실제로 락을 얻는 구현부분. 이런식으로 구현부를 따로 둔 이유는 acquire_multiple()에서 컬렉션 클래스에 대해 호출하는 toArray()가 </br>
	 * 오브젝트의 배열을 반환하기 때문이다. 메서드가 실행중에 locks 배열 안의 내용이 바뀌지 않도록 해주어야 한다.
	 * @param locks - 락을 얻을 객체 배열
	 * @param lTimeout - Timeout
	 * @throws InterruptedException
	 * @throws Semaphore.Timed_out
	 */
	private static void acquire(Object[] locks, long lTimeout)
			throws InterruptedException, Semaphore.Timed_out {
		int current_lock = 0;

		try {
			// 확실히 복사한 락을 가지고 작업하는 것이 아니라
			// 인자로 주어진 값들을 그대로 사용하는 것은 잠재적으로 위험할 수 있다.
			// 하지만 쓸데없는 오버헤드가 생기는 것을 원치 않아 락을 복사하지 않았다.
			long expiration = (lTimeout == Semaphore.FOREVER) ? Semaphore.FOREVER
					: System.currentTimeMillis() + lTimeout;
			;

			Arrays.sort(locks, compare_strategy); // sort
			for (; current_lock < locks.length; ++current_lock) {
				long time_remaining = expiration - System.currentTimeMillis();
				if (time_remaining <= 0)
					throw new Semaphore.Timed_out(
							"Timed out waiting to acquire multiple locks");

				((Semaphore) locks[current_lock]).acquire(time_remaining);
			}
		}
		// locks[current_lock]을 얻는 도중 인터럽트 되었다.
		// locks[current_lock]이전에 얻어진 모든 락들을 놓는다.
		catch (InterruptedException exception) {
			while (--current_lock >= 0)
				((Semaphore) locks[current_lock]).release();
			throw exception;
		}
		// locks[current_lock]을 얻는 도중 타임아웃이 발생한다.
		// locks[current_lock]이전에 얻어진 모든 락들을 놓는다.
		catch (Semaphore.Timed_out exception) {
			while (--current_lock >= 0)
				((Semaphore) locks[current_lock]).release();
			throw exception;
		}
	}
}

