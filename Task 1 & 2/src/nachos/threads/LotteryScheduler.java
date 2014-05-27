//ytxh{binofudofxhgdofyh`nridojdptkh`o`ov`ofrhvdh
//PART OF THE NACHOS. DON'T CHANGE CODE OF THIS LINE
package nachos.threads;

import nachos.machine.*;
import nachos.threads.PriorityScheduler.PriorityQueue;
import nachos.threads.PriorityScheduler.ThreadState;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A scheduler that chooses threads using a lottery.
 *
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 *
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking
 * the maximum).
 */
public class LotteryScheduler extends PriorityScheduler {
//	@Override
	public static void selfTestRun(KThread t1, int p1, KThread t2, int p2, KThread t3, int p3) {
		boolean int_state = Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(t1, p1);
		ThreadedKernel.scheduler.setPriority(t2, p2);
		ThreadedKernel.scheduler.setPriority(t3, p3);
		Machine.interrupt().restore(int_state);
		t1.setName("Thread 1").fork();		
		t2.setName("Thread 2").fork();		
		t3.setName("Thread 3").fork();
		t1.join();
		t2.join();
		t3.join();
	}
	
//	@Override
	public static void selfTestRun(KThread t1, int p1, KThread t2, int p2, KThread t3, int p3, KThread t4, int p4) {

		boolean int_state = Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(t1, p1);
		ThreadedKernel.scheduler.setPriority(t2, p2);
		ThreadedKernel.scheduler.setPriority(t3, p3);
		ThreadedKernel.scheduler.setPriority(t4, p4);
		Machine.interrupt().restore(int_state);
		t1.setName("Thread 1").fork();		
		t2.setName("Thread 2").fork();		
		t3.setName("Thread 3").fork();
		t4.setName("Thread 4").fork();
		t1.join();
		t2.join();
		t3.join();
		t4.join();
	}	

	@Override
	public void selfTest() {

		KThread t1, t2, t3;
		final Lock lock;
		final Condition condition;
		
		
		/*
		 * Test 0: without donation and priority change
		 *
		 */

		System.out.println("Test 0");

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
	
		selfTestRun(t1, 60000, t2, 500, t3, 4);

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
						System.out.println(KThread.currentThread().getName() + " changes priority from 6 to 1");
						boolean int_state = Machine.interrupt().disable();
						ThreadedKernel.scheduler.setPriority(1);
						KThread a = KThread.currentThread();
						System.out.println(getEffectivePriority(a));
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
						ThreadedKernel.scheduler.setPriority(7000000);
						KThread a = KThread.currentThread();
						System.out.println(getEffectivePriority(a));
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

		selfTestRun(t1, 60000, t2, 500, t3, 4);

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
	//			KThread.yield();
		//		KThread.yield();
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
//				KThread.yield();
//				KThread.yield();
				System.out.println(KThread.currentThread().getName());
				lock.acquire();
				boolean int_state = Machine.interrupt().disable();
				ThreadedKernel.scheduler.setPriority(2);
				KThread a = KThread.currentThread();
				System.out.println(getEffectivePriority(a));
				Machine.interrupt().restore(int_state);
				System.out.println(KThread.currentThread().getName()+"yield");
				KThread.yield();
			//	lock2.acquire();
				System.out.println("t01 join t01");
				t01.join();
				System.out.println(KThread.currentThread().getName());
				lock.release();
			//	lock2.release();
				System.out.println(KThread.currentThread().getName());
			}
		});

		final KThread t03 = new KThread(new Runnable() {
			public void run() {
	//			KThread.yield();
	//			KThread.yield();
				System.out.println(KThread.currentThread().getName());
				lock.acquire();
				System.out.println(KThread.currentThread().getName());
			}
		});
		
		final KThread t04 = new KThread(new Runnable() {
			public void run() {
	//			KThread.yield();
	//			KThread.yield();
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

		selfTestRun(t01, 1000000, t02, 10000, t03, 5, t04, 4);
		
		
		
		
		
		
		
		/*
		 * Test 4: with donation and priority change
		 *
		 */
		
		System.out.println("Test 4");

		final Lock lock2 = new Lock();
		final Lock lock3 = new Lock();
		final Condition condition2 = new Condition( lock2 );
		final Condition condition3 = new Condition( lock3 );
	//	final Condition condition2 = new Condition(lock2);

		final KThread t11 = new KThread(new Runnable() {
			public void run() {
		//		lock2.acquire();
	//			KThread.yield();
		//		KThread.yield();
				System.out.println(KThread.currentThread().getName());
				lock2.acquire();
				boolean int_state = Machine.interrupt().disable();
				ThreadedKernel.scheduler.setPriority(1000);
				KThread a = KThread.currentThread();
				System.out.println(getEffectivePriority(a));
				Machine.interrupt().restore(int_state);
				KThread.yield();
				System.out.println(KThread.currentThread().getName());
				lock3.acquire();
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

		final KThread t12 = new KThread(new Runnable() {
			public void run() {
//				lock2.acquire();
				//			KThread.yield();
				//		KThread.yield();
						System.out.println(KThread.currentThread().getName());
						lock3.acquire();
						boolean int_state = Machine.interrupt().disable();
						ThreadedKernel.scheduler.setPriority(100);
						KThread a = KThread.currentThread();
						System.out.println(getEffectivePriority(a));
						Machine.interrupt().restore(int_state);
						KThread.yield();
						System.out.println(KThread.currentThread().getName());
						lock2.acquire();
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

		final KThread t13 = new KThread(new Runnable() {
			public void run() {
	//			System.out.println(KThread.currentThread().getName());
				
			}
		});
		
		final KThread t14 = new KThread(new Runnable() {
			public void run() {
				System.out.println("t11:" + getThreadState(t11).effectivePriority);
				System.out.println("t12:" + getThreadState(t12).effectivePriority);
				System.out.println("t13:" + getThreadState(t03).effectivePriority);
				System.out.println("t14:" + getThreadState(KThread.currentThread()).effectivePriority);
			}
		});

		selfTestRun(t11, 10000000, t12, 100000, t13, 1, t14, 4);
		
	}
	
	
	
	
	
	
	
	
	
	public static final int priorityDefault = 1;
	public static final int priorityMinimum = 1;
	public static final int priorityMaximum = Integer.MAX_VALUE;
	
    /**
     * Allocate a new lottery scheduler.
     */
    public LotteryScheduler() {
    }
    
    @Override
	protected LotteryThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new LotteryThreadState(thread);
		return (LotteryThreadState) thread.schedulingState;
	}
    
    @Override
	public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());
		return getThreadState(thread).getPriority();
	}
    
    @Override
    public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());
		return getThreadState(thread).getEffectivePriority();
	}

	@Override
	public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());
		Lib.assertTrue(priority >= priorityMinimum &&
				   priority <= priorityMaximum);
		getThreadState(thread).setPriority(priority);
	}

	@Override
	public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();
		KThread thread = KThread.currentThread();
		int priority = getPriority(thread);
		if (priority == priorityMaximum)
			return false;
		setPriority(thread, priority + 1);
		Machine.interrupt().restore(intStatus);
		return true;
	}

	@Override
	public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();
		KThread thread = KThread.currentThread();
		int priority = getPriority(thread);
		if (priority == priorityMinimum)
			return false;
		setPriority(thread, priority - 1);
		Machine.interrupt().restore(intStatus);
		return true;
	}

    protected class LotteryQueue extends PriorityScheduler.PriorityQueue {    	
    	LotteryQueue(boolean transferPriority) {
			super(transferPriority);
		}
    	
    	LotteryQueue(boolean transferPriority, KThread thread) {
			super(transferPriority, thread);
		}
    	
    	@Override
		protected LotteryThreadState pickNextThread() {
    		if (waitingQueue.isEmpty())
    			return null;
    		//System.out.println("here");
    		int ticketSum[] = new int[waitingQueue.size()];
    		//System.out.println("here");
    		ticketSum[0] = 0;
    		//System.out.println("here");
    		int i = 0;
    		for (ThreadState t : waitingQueue) {
        		//System.out.println("here");
    			LotteryThreadState lt = getThreadState(t.thread);
    			if (i == 0)
    				ticketSum[i] = lt.getEffectivePriority();
    			else
    				ticketSum[i] = ticketSum[i-1] + lt.getEffectivePriority();
    			++i;
    		}
    		int Lottery = r.nextInt(ticketSum[i-1]);
    		i = 0;
    		for (ThreadState t : waitingQueue) {
    			if (Lottery < ticketSum[i])
    				return getThreadState(t.thread);
    			++i;
    		}
    		
    		return null;
    	}
    }
    
    /**
     * ThreadState for Lottery Scheduler.
     */
    protected class LotteryThreadState extends PriorityScheduler.ThreadState {
    	public LotteryThreadState(KThread thread) {
			super(thread);
		}
    	
    	@Override
    	public int getEffectivePriority() {
    		return getEffectivePriorityHelper(new HashSet<LotteryThreadState>());
    	}
    	
    	@Override
    	public void update(ThreadState A, int p) {
    		//effectivePriority = priorityMinimum;
    		//getEffectivePriority();
    	}
    	
    	/**
    	 * Get the effective priority of all offsprings of threads in Set.
    	 * @param set The set of threads we are searching
    	 * @return effectivePriority
    	 */
    	public int getEffectivePriorityHelper(HashSet<LotteryThreadState> set) {
    		if (set.contains(this))
    			return priority;
    		effectivePriority = priority;
    		for (PriorityQueue queue : donorQueue)
    			if (queue.transferPriority)
    				for (ThreadState t : queue.waitingQueue) {
    					set.add(this);
    					LotteryThreadState lt = getThreadState(t.thread);
    					effectivePriority += lt.getEffectivePriorityHelper(set);
    					set.remove(this);
    				}
    		PriorityQueue queue = (PriorityQueue) thread.joinQueue;
    		if (queue != null)
    		if (queue.transferPriority)
    			for (ThreadState t : queue.waitingQueue) {
					set.add(this);
					LotteryThreadState lt = getThreadState(t.thread);
					effectivePriority += lt.getEffectivePriorityHelper(set);
					set.remove(this);
				}
    	    return effectivePriority;
    	}
    }

    /**
     * Allocate a new lottery thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer tickets from waiting threads
     *					to the owning thread.
     * @return	a new lottery thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
    	return new LotteryQueue(transferPriority);
    }
    
    public ThreadQueue newThreadQueue(boolean transferPriority, KThread thread) {
    	return new LotteryQueue(transferPriority, thread);
    }
    
    private java.util.Random r = new java.util.Random();
}
