/**
 * A simple custom lock that allows simultaneously read operations, but
 * disallows simultaneously write and read/write operations.
 */
public class MultiReaderLock {
	private int readers;
	private int writers;

	/**
	 * Initializes a multi-reader (single-writer) lock.
	 */
	public MultiReaderLock() {
		readers = 0;
		writers = 0;
	}

	/**
	 * Will wait until there are no active writers in the system, and then will
	 * increase the number of active readers.
	 */
	public synchronized void lockRead() {
		while (writers > 0) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				System.err.println("There was a thread interrupted " + e);
			}
		}
		readers += 1;
	}

	/**
	 * Will decrease the number of active readers, and notify any waiting
	 * threads if necessary.
	 */
	public synchronized void unlockRead() {
		readers -= 1;
		if (readers == 0) {
			this.notifyAll();
		}
	}

	/**
	 * Will wait until there are no active readers or writers in the system, and
	 * then will increase the number of active writers.
	 */
	public synchronized void lockWrite() {

		while (readers > 0 || writers > 0) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				System.err.println("The thread was interrupded here " + e);
			}
		}
		writers += 1;
	}

	/**
	 * Will decrease the number of active writers, and notify any waiting
	 * threads if necessary.
	 */
	public synchronized void unlockWrite() {
		writers -= 1;
		this.notifyAll();
	}
}