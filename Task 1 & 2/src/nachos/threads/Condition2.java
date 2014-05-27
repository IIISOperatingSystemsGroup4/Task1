//ytxh{binofudofxhgdofyh`nridojdptkh`o`ov`ofrhvdh
//PART OF THE NACHOS. DON'T CHANGE CODE OF THIS LINE
package nachos.threads;

import nachos.machine.*;
import java.util.LinkedList;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
	this.conditionLock = conditionLock;
	
	this.waitingQueue = ThreadedKernel.scheduler.newThreadQueue(false);
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
	
	boolean prevStatus = Machine.interrupt().disable();
	waitingQueue.waitForAccess(KThread.currentThread());

	conditionLock.release();
	KThread.sleep();
	conditionLock.acquire();
	
	Machine.interrupt().restore(prevStatus);
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
	
	boolean prevStatus = Machine.interrupt().disable();
	KThread nextThread = waitingQueue.nextThread();
	if(nextThread != null)
	{
		nextThread.ready();
	}
	Machine.interrupt().restore(prevStatus);
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
	boolean prevStatus = Machine.interrupt().disable();
	KThread nextThread;
	while((nextThread = waitingQueue.nextThread()) != null)
	{
		nextThread.ready();
	}
	Machine.interrupt().restore(prevStatus);
    }
    
    public static void selfTest() {

		final Lock lock = new Lock();
		final Condition condition = new Condition(lock);
		final LinkedList<String> list = new LinkedList<String>();
		final int listMaxSize = 3, throughput = 3, numProducers = 5;

		Runnable consuming = new Runnable() {
			public void run() {
				String name = KThread.currentThread().getName();
				for (int i = 0; i < throughput; i ++) {
					lock.acquire();
					while (list.isEmpty()) {
						System.out.println(name
								+ " waits for something to be put into the list");
						condition.sleep();
					}
					String entry = list.removeFirst();
					System.out.println(name + " removes " + entry
							+ " from the list");

					condition.wakeAll();
					lock.release();
				}
			}
		};

		Runnable producing = new Runnable() {
			public void run() {
				String name = KThread.currentThread().getName();
				for (int i = 1; i <= throughput; i ++) {
					lock.acquire();
					while (list.size() >= listMaxSize) {
						System.out.println(name
								+ " waits for listsize to be smaller than "
								+ listMaxSize);
						condition.sleep();
					}
					String entry = "(" + name + "_" + i + ")";

					System.out.println(name + " adds " + entry
							+ " to the list");
					list.addLast(entry);
					condition.wakeAll();
					lock.release();
				}
			}
		};

		// case 1
		System.out.println("\tTestcase 1:");
		KThread consumer = new KThread(consuming);
		KThread producer = new KThread(producing);

		consumer.setName("consumer").fork();
		producer.setName("producer").fork();

		producer.join();
		consumer.join();

		// case 2
		System.out.println("\tTestase 2:");
		LinkedList<KThread> consumers = new LinkedList<KThread>();
		LinkedList<KThread> producers = new LinkedList<KThread>();
		for (int i = 0; i < numProducers; i ++) {
			consumers.addLast(new KThread(consuming));
			producers.addLast(new KThread(producing));
		}
		for (int i = 0; i < numProducers; i ++) {
			consumers.get(i).setName("consumer" + i).fork();
			producers.get(i).setName("producer" + i).fork();
		}
		for (int i = 0; i < numProducers; i ++) {
			producers.get(i).join();
			consumers.get(i).join();
		}

	}

    private Lock conditionLock;
    private ThreadQueue waitingQueue;
}
