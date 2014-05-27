//ytxh{binofudofxhgdofyh`nridojdptkh`o`ov`ofrhvdh
//PART OF THE NACHOS. DON'T CHANGE CODE OF THIS LINE
package nachos.threads;

import java.util.TreeSet;

import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
    	long nowTime = Machine.timer().getTime();
    	boolean intStatus = Machine.interrupt().disable();
    	while(!threads.isEmpty())
    	{
    		TThread t = threads.first();
    		if(t.wakeTime <= nowTime)
    		{
    			t.thread.ready();
    			threads.remove(t);
    		}
    		else
    			break;
    	}
    	Machine.interrupt().restore(intStatus);
	KThread.currentThread().yield();
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
	// for now, cheat just to get something working (busy waiting is bad)
    long nowTime = Machine.timer().getTime();
	long wakeTime = nowTime + x;
	//while (wakeTime > Machine.timer().getTime())
    //    KThread.yield();
	
	boolean intStatus = Machine.interrupt().disable();
	threads.add(new TThread(KThread.currentThread(), wakeTime));
	KThread.currentThread().sleep();
	Machine.interrupt().restore(intStatus);
    }
    
    public TreeSet<TThread> threads = new TreeSet<TThread>(); 
    
    public static void selfTest()
    {
    	Alarm alarm = new Alarm();
    	KThread[] ts = new KThread[100];
    	for(int i = 0; i < 100; i++)
    		ts[i] = new KThread(alarm.new Test(alarm));
    	for(int i = 0; i < 100; i++)
    		ts[i].fork();
    	for(int i = 0; i < 100; i++)
    		ts[i].join();
    	System.out.println("All threads are waken");
    }
    
    
    
    
    class TThread implements Comparable
    {
    	public KThread thread;
    	long wakeTime;
    	public TThread(KThread kthread, long wakeTime)
    	{
    		this.thread = kthread;
    		this.wakeTime = wakeTime;
    	}
		@Override
		public int compareTo(Object o) {
			// TODO Auto-generated method stub
			TThread oThread = (TThread) o;
			if(this.wakeTime > oThread.wakeTime)
				return 1;
			else if(this.wakeTime < oThread.wakeTime)
				return -1;
			else
				return this.thread.compareTo(oThread.thread);
		}
    	
    }
    
    
    
    class Test implements Runnable
    {

		public Alarm alarm;
		
		public Test(Alarm alarm)
		{
			this.alarm = alarm;
		}
		public void run() {
			// TODO Auto-generated method stub
			long wait = (long)(Math.random()  * 10000);
			long nowTime = Machine.timer().getTime();
			
			alarm.waitUntil(wait);
			
			long wakeTime = Machine.timer().getTime();
			if(wakeTime - nowTime < wait)
				System.out.println("Error!");
			else
				System.out.println(wait + " < " + (wakeTime - nowTime));
		}
    	
    }
}
