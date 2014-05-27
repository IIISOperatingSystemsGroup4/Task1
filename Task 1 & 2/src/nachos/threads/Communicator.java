//ytxh{binofudofxhgdofyh`nridojdptkh`o`ov`ofrhvdh
//PART OF THE NACHOS. DON'T CHANGE CODE OF THIS LINE
package nachos.threads;

import java.util.LinkedList;

import nachos.machine.*;
import nachos.threads.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    int numspeak,numlisten,message;
//  nummessage,tosend,messages[]=new int[100],
    boolean dataready;
    Lock lock=new Lock();
    Condition ready=new Condition(lock);
    public Communicator() {
        numspeak=0;
        numlisten=0;
//      ready=Condition(lock);
//      nummessage=0;
//      tosend=0;
        
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param   word    the integer to transfer.
     */
//  int no=nummessage;
//  messages[nummessage]=word;
//  nummessage++;
//  while (tosend!=no)
//  {
//      ready.sleep();
//  }
    public void speak(int word) {
        System.out.println("begin to speak");
        lock.acquire();
        numspeak++;
        while ((numlisten==0)||(dataready))
        {
            ready.sleep();
        }
        message=word;
        dataready=true;
        ready.wakeAll();
        numspeak--;
        lock.release();
        
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return  the integer transferred.
     */
    public int listen() {
        lock.acquire();
        numlisten++;
        ready.wake();
        while (!dataready)
        {
            ready.sleep();
        }
        int result=message;
        dataready=false;
        numlisten--;
        lock.release();
        return result;
    }
    public static void selfTest() {
        final Communicator com=new Communicator();
        int numSpeaker=5,numListener=5;
        System.out.println("Beginning test");
        Runnable speaker = new Runnable() {
            public void run() {
                int a=(int) (Math.random()*100);
                System.out.println("I speak "+ a+" !\n");
                com.speak(a);
            }
        };
        Runnable listener = new Runnable() {
            public void run() {
                System.out.println("Beginning to listen");
                int b=com.listen();
                System.out.println("I heard "+ b+" !\n");
                
            }
        };
        LinkedList<KThread> speakers = new LinkedList<KThread>();
        LinkedList<KThread> listeners = new LinkedList<KThread>();
        for (int i = 0; i < numSpeaker; i++) {
            speakers.addLast(new KThread(speaker));
            listeners.addLast(new KThread(listener));
        }
        for (int i = 0; i < numListener; i++) {
            speakers.get(i).fork();
            listeners.get(i).fork();
        }
        for (int i = 0; i < numListener; i++) {
            System.out.println("going to fork "+i);
            speakers.get(i).join();
            listeners.get(i).join();
        }

    }
}
