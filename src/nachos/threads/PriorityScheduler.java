//ytxh{binofudofxhgdofyh`nridojdptkh`o`ov`ofrhvdh
//PART OF THE NACHOS. DON'T CHANGE CODE OF THIS LINE
package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.LinkedList;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
	
	/**
	 * Generate a test with 3 running threads with given initial priority.
	 */
	public static void selfTestRun(KThread t1, int p1, KThread t2, int p2, KThread t3, int p3) {

		boolean int_state = Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(t1, p1);
		ThreadedKernel.scheduler.setPriority(t2, p2);
		ThreadedKernel.scheduler.setPriority(t3, p3);
		Machine.interrupt().restore( int_state );
		t1.setName("Thread 1").fork();		
		t2.setName("Thread 2").fork();		
		t3.setName("Thread 3").fork();
		t1.join();
		t2.join();
		t3.join();
	}
	
	/**
	 * Generate a test with 4 running threads with given initial priority.
	 */
	public static void selfTestRun(KThread t1, int p1, KThread t2, int p2, KThread t3, int p3, KThread t4, int p4) {

		boolean int_state = Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(t1, p1);
		ThreadedKernel.scheduler.setPriority(t2, p2);
		ThreadedKernel.scheduler.setPriority(t3, p3);
		ThreadedKernel.scheduler.setPriority(t4, p4);
		Machine.interrupt().restore( int_state );
		t1.setName("Thread 1").fork();		
		t2.setName("Thread 2").fork();		
		t3.setName("Thread 3").fork();
		t4.setName("Thread 4").fork();
		t1.join();
		t2.join();
		t3.join();
		t4.join();
	}	

	/**
	 * Tests whether the PriorityScheduler is working as expected.
	 */
	public void selfTest() {

		KThread t1, t2, t3;
		final Lock lock;
		final Condition condition;

		/*
		 * Test 1: without donation and priority change
		 *
		 */

		System.out.println("Test 1");

		t1 = new KThread(new Runnable() {
				public void run() {
					System.out.println(KThread.currentThread().getName());
					for(int i = 0; i < 10; ++i) {
						System.out.println(KThread.currentThread().getName() + " working " + i);	
						KThread.yield();
					}
					System.out.println(KThread.currentThread().getName());
				}
			});
		
		t2 = new KThread(new Runnable() {
			public void run() {
				System.out.println(KThread.currentThread().getName());
				for(int i = 0; i < 10; ++i) {
					System.out.println(KThread.currentThread().getName() + " working " + i);	
					KThread.yield();
				}
				System.out.println(KThread.currentThread().getName());
			}
		});
		
		t3 = new KThread(new Runnable() {
			public void run() {
				System.out.println(KThread.currentThread().getName());
				for(int i = 0; i < 10; ++i) {
					System.out.println(KThread.currentThread().getName() + " working " + i);	
					KThread.yield();
				}
				System.out.println(KThread.currentThread().getName());
			}
		});

		selfTestRun(t1, 6, t2, 5, t3, 4);

		/*
		 * Test 2: without donation but with priority change
		 *
		 */
		
		
		System.out.println("Test 2");

		t1 = new KThread(new Runnable() {
			public void run() {
				System.out.println(KThread.currentThread().getName());
				for(int i = 0; i < 10; ++i) {
					System.out.println(KThread.currentThread().getName() + " working " + i);
					if (i == 2) {
						System.out.println(KThread.currentThread().getName() + " changes priority from 6 to 0");
						boolean int_state = Machine.interrupt().disable();
						ThreadedKernel.scheduler.setPriority(0);
						Machine.interrupt().restore(int_state);
					}
					KThread.yield();
				}
				System.out.println(KThread.currentThread().getName());
			}
		});
	
		t2 = new KThread(new Runnable() {
			public void run() {
				System.out.println(KThread.currentThread().getName());
				for(int i = 0; i < 10; ++i) {
					System.out.println(KThread.currentThread().getName() + " working " + i);	
					if (i == 5) {
						System.out.println(KThread.currentThread().getName() + " changes priority from 5 to 7");
						boolean int_state = Machine.interrupt().disable();
						ThreadedKernel.scheduler.setPriority(7);
						Machine.interrupt().restore(int_state);
					}
					KThread.yield();
				}
				System.out.println(KThread.currentThread().getName());
			}
		});
	
		t3 = new KThread(new Runnable() {
			public void run() {
				System.out.println(KThread.currentThread().getName());
				for(int i = 0; i < 10; ++i) {
					System.out.println(KThread.currentThread().getName() + " working " + i);
					KThread.yield();
				}
				System.out.println(KThread.currentThread().getName());
			}
		});

		selfTestRun(t1, 6, t2, 5, t3, 4);

		/*
		 * Test 3: with donation and priority change
		 *
		 */
		
		System.out.println("Test 3");

		lock = new Lock();
	//	final Lock lock2 = new Lock();
		condition = new Condition( lock );
	//	final Condition condition2 = new Condition(lock2);

		final KThread t01 = new KThread(new Runnable() {
			public void run() {
		//		lock2.acquire();
				KThread.yield();
				KThread.yield();
				System.out.println(KThread.currentThread().getName());
				boolean int_state = Machine.interrupt().disable();
				ThreadedKernel.scheduler.setPriority(1);
				KThread a = KThread.currentThread();
				System.out.println(getEffectivePriority(a));
				Machine.interrupt().restore(int_state);
				System.out.println(KThread.currentThread().getName());
				KThread.yield();
				int_state = Machine.interrupt().disable();
				a = KThread.currentThread();
				System.out.println(getEffectivePriority(a));
				Machine.interrupt().restore(int_state);
				System.out.println(KThread.currentThread().getName());
				KThread.yield();
				System.out.println(KThread.currentThread().getName());
		//		lock2.release();
			}
		});

		final KThread t02 = new KThread(new Runnable() {
			public void run() {
				KThread.yield();
				KThread.yield();
				System.out.println(KThread.currentThread().getName());
				lock.acquire();
				boolean int_state = Machine.interrupt().disable();
				ThreadedKernel.scheduler.setPriority(2);
				KThread a = KThread.currentThread();
				System.out.println(getEffectivePriority(a));
				Machine.interrupt().restore(int_state);
				System.out.println(KThread.currentThread().getName());
				KThread.yield();
			//	lock2.acquire();
				t01.join();
				System.out.println(KThread.currentThread().getName());
				lock.release();
			//	lock2.release();
				System.out.println(KThread.currentThread().getName());
			}
		});

		final KThread t03 = new KThread(new Runnable() {
			public void run() {
				KThread.yield();
				KThread.yield();
				System.out.println(KThread.currentThread().getName());
				lock.acquire();
				System.out.println(KThread.currentThread().getName());
			}
		});
		
		final KThread t04 = new KThread(new Runnable() {
			public void run() {
				KThread.yield();
				KThread.yield();
				System.out.println(KThread.currentThread().getName());
				System.out.println("t01:" + getThreadState(t01).effectivePriority);
				System.out.println("t02:" + getThreadState(t02).effectivePriority);
				System.out.println("t03:" + getThreadState(t03).effectivePriority);
				System.out.println("t04:" + getThreadState(KThread.currentThread()).effectivePriority);
				for( int i = 0; i < 2; ++i ) {
					System.out.println( KThread.currentThread().getName() + " working " + i );	
					KThread.yield();
				}
				System.out.println(KThread.currentThread().getName());
			}
		});

		selfTestRun(t01, 7, t02, 6, t03, 5, t04, 4);
	}

	/**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }

    /**
     * Allocate a new priority thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
	return new PriorityQueue(transferPriority);
    }
    
    //Task 1.5
    public ThreadQueue newThreadQueue(boolean transferPriority, KThread thread) {
    	return new PriorityQueue(transferPriority, thread);
        }

    public int getPriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());

	return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());

	return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
	Lib.assertTrue(Machine.interrupt().disabled());

	Lib.assertTrue(priority >= priorityMinimum &&
		   priority <= priorityMaximum);

	getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
	boolean intStatus = Machine.interrupt().disable();

	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMaximum)
	    return false;

	setPriority(thread, priority+1);

	Machine.interrupt().restore(intStatus);
	return true;
    }

    public boolean decreasePriority() {
	boolean intStatus = Machine.interrupt().disable();

	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMinimum)
	    return false;

	setPriority(thread, priority-1);

	Machine.interrupt().restore(intStatus);
	return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
	if (thread.schedulingState == null)
	    thread.schedulingState = new ThreadState(thread);

	return (ThreadState) thread.schedulingState;
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {
	PriorityQueue(boolean transferPriority) {
	    this.transferPriority = transferPriority;
	}
	
	//Task 1.5
	PriorityQueue(boolean transferPriority, KThread thread) {
	    this.transferPriority = transferPriority;
	    this.owner = getThreadState(thread);
	}

	public void waitForAccess(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    getThreadState(thread).waitForAccess(this);
	}

	public void acquire(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    getThreadState(thread).acquire(this);
	}

	public KThread nextThread() {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    
	    // implement me
	    ThreadState next = pickNextThread();
	    if (next == null) {
	    	owner = null;
	    	return null;
	    }
	    if (owner != null) {
    		owner.donorQueue.remove(this);
    		owner.getEffectivePriority();
    	}
	    this.acquire(next.thread);
		return next.thread;
	}

	/**
	 * Return the next thread that <tt>nextThread()</tt> would return,
	 * without modifying the state of this queue.
	 *
	 * @return	the next thread that <tt>nextThread()</tt> would
	 *		return.
	 */
	protected ThreadState pickNextThread() {
	    // implement me
		if (waitingQueue.isEmpty())
			return null;
		return waitingQueue.peek();
	}

	public void print() {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    
	    // implement me (if you want)
	    
	}

	/**
	 * <tt>true</tt> if this queue should transfer priority from waiting
	 * threads to the owning thread.
	 */
	public boolean transferPriority;
	
	//Task 1.5
	public ThreadState owner = null;
	private java.util.PriorityQueue<ThreadState> waitingQueue = new java.util.PriorityQueue<ThreadState>();
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    protected class ThreadState implements Comparable {
	/**
	 * Allocate a new <tt>ThreadState</tt> object and associate it with the
	 * specified thread.
	 *
	 * @param	thread	the thread this state belongs to.
	 */
	public ThreadState(KThread thread) {
	    this.thread = thread;

	    setPriority(priorityDefault);
	}
	
	/**
	 * Update the priority of a thread and its owner.
	 * 
	 * @param 	A	the ThreadState to update
	 * @param	p	the value we want to assign to the effective priority of A
	 */
	public void update(ThreadState A, int p) {
		A.effectivePriority = p;
		if (A.waitingOn == null)
			return;
		if (A.waitingOn.owner == null)
			return;
		if (A.waitingOn.owner.effectivePriority > p)
			update(A.waitingOn.owner, p);
	}

	/**
	 * Return the priority of the associated thread.
	 *
	 * @return	the priority of the associated thread.
	 */
	public int getPriority() {
	    return priority;
	}

	/**
	 * Return the effective priority of the associated thread.
	 *
	 * @return	the effective priority of the associated thread.
	 */
	public int getEffectivePriority() {
	    // implement me
		effectivePriority = priority;
		for (PriorityQueue queue : donorQueue)
			if (queue.transferPriority)
				for (ThreadState t : queue.waitingQueue)
					if (t.effectivePriority > effectivePriority)
						effectivePriority = t.effectivePriority;
		PriorityQueue queue = (PriorityQueue) thread.joinQueue;
		if (queue != null)
		if (queue.transferPriority)
			for (ThreadState t : queue.waitingQueue)
				if (t.effectivePriority > effectivePriority)
					effectivePriority = t.effectivePriority;
	    return effectivePriority;
	}

	/**
	 * Set the priority of the associated thread to the specified value.
	 *
	 * @param	priority	the new priority.
	 */
	public void setPriority(int priority) {
	    if (this.priority == priority)
		return;

	    this.priority = priority;

	    // implement me
		update(this, getEffectivePriority());
	}

	/**
	 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
	 * the associated thread) is invoked on the specified priority queue.
	 * The associated thread is therefore waiting for access to the
	 * resource guarded by <tt>waitQueue</tt>. This method is only called
	 * if the associated thread cannot immediately obtain access.
	 *
	 * @param	waitQueue	the queue that the associated thread is
	 *				now waiting on.
	 *
	 * @see	nachos.threads.ThreadQueue#waitForAccess
	 */
	public void waitForAccess(PriorityQueue waitQueue) {
	    // implement me
		this.enqueueTime = Machine.timer().getTime();
		waitQueue.waitingQueue.add(this);
		waitingOn = waitQueue;
		if (waitQueue.owner != null)
			waitQueue.owner.getEffectivePriority();
	}

	/**
	 * Called when the associated thread has acquired access to whatever is
	 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
	 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
	 * <tt>thread</tt> is the associated thread), or as a result of
	 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
	 *
	 * @see	nachos.threads.ThreadQueue#acquire
	 * @see	nachos.threads.ThreadQueue#nextThread
	 */
	public void acquire(PriorityQueue waitQueue) {
	    // implement me
		waitQueue.waitingQueue.remove(this);
		waitQueue.owner = this;
		donorQueue.add(waitQueue);
		getEffectivePriority();
	}
	
	//Task 1.5
	public int compareTo(Object t) {
		ThreadState state = (ThreadState) t;
		int thisPriority = effectivePriority;//getEffectivePriority();//
		int statePriotity = state.effectivePriority;//state.getEffectivePriority();//
		if (thisPriority < statePriotity)
			return 1;
		else if (thisPriority > statePriotity) 
			return -1;
		else if (enqueueTime > state.enqueueTime)
			return -1;
		else if (enqueueTime > state.enqueueTime)
			return 1;
		return 0;
	}

	/** The thread with which this object is associated. */
	protected KThread thread;
	/** The priority of the associated thread. */
	protected int priority;
	
	//Task 1.5
	protected LinkedList<PriorityQueue> donorQueue = new LinkedList<PriorityQueue>();
	protected PriorityQueue waitingOn = null;
	protected int effectivePriority = 0;
	protected long enqueueTime;
    }
}
