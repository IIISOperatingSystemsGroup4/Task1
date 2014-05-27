//ytxh{binofudofxhgdofyh`nridojdptkh`o`ov`ofrhvdh
//PART OF THE NACHOS. DON'T CHANGE CODE OF THIS LINE
package nachos.threads;
import nachos.ag.BoatGrader;
import nachos.machine.Machine;

public class Boat
{
    static BoatGrader bg;
    static int P = 1, C = 0, OCRide = 1, OCRow = 1, OA = 1, M = 0, land = 0, Row = 0, Ride = 0;
    static int count = 0;
    static Lock lockForCount = new Lock(),
    			lockForSetPriority = new Lock(),
    			lockForO = new Lock(),
    			lockForM = new Lock(),
    			lockForRideAndRow = new Lock(),
    			lockForAdult = new Lock(),
    			lockForLand = new Lock(),
    			lockForMain = new Lock();
    
    static Condition Cpriority = new Condition(lockForSetPriority),
    		  		 Cadult = new Condition(lockForAdult),
    		  		 CO = new Condition(lockForO),
    		  		 CM = new Condition(lockForM),
    		  		 Cride = new Condition(lockForRideAndRow),
    		  		 Crow = new Condition(lockForRideAndRow),
    				 CMain = new Condition(lockForMain);
    
    static int[] sequence = new int[10000];
    static int index = 0;
    		  
    

    public static void selfTest()
    {
	BoatGrader b = new BoatGrader();
	int c = 100;
	int a = 100;
	int adultO = a;
	int childO = c;
	int adultM = 0;
	int childM = 0;
	int pre = 1;

	//System.out.println("\n ***Testing Boats with only 2 children***");
	begin(a, c, b);
	
	/* The testing idea is to store the behaviors in sequence[] and repeat them */
	/* 0 represents child rowing from O to M, 1 represents child riding from O to M */
	/* 2 represents child from M  to O, 3 represents adult from O to M */
	
	for (int i = 0; i < index; i++) {
		switch (sequence[i]) {
		case 0:
			if (childO <= 0) {
				System.out.println("Error1!");
				return;
			}
			childO--;
			childM++;
			break;
		case 1:
			if (childO <= 0 || pre != 0) {
				System.out.println("Error2!");
				System.out.println(pre);
				return;
			}
			childO--;
			childM++;
			break;
		case 2:
			if (childM <= 0) {
				System.out.println("Error3!");
				return;
			}
			childM--;
			childO++;
			break;
		case 3:
			if (adultO <= 0) {
				System.out.println("Error4!");
				return;
			}
			adultO--;
			adultM++;
			break;
		}
		pre = sequence[i];
	}
	if (adultO == 0 && childO == 0) {
		System.out.println("Right!");
    }
    else {
    	System.out.println("Error5");
    }
	}

    public static void begin( int adults, int children, BoatGrader b )
    {
	// Store the externally generated autograder in a class
	// variable to be accessible by children.
	bg = b;

	// Instantiate global variables here

	// Create threads here. See section 3.4 of the Nachos for Java
	// Walkthrough linked from the projects page.

	Runnable ra = new Runnable() {
	    public void run() {
	    	AdultItinerary();
            }
        };
    Runnable rc = new Runnable() {
	    public void run() {
	    	ChildItinerary();
            }
        };
        for (int i = 1; i <= adults; i++) {
        	KThread t = new KThread(ra);
        	t.setName("Adult " + Integer.toString(i));
        	t.fork();
        }
        for (int i = 1; i <= children; i++) {
        	KThread t = new KThread(rc);
        	t.setName("Child " + Integer.toString(i));
        	t.fork();
        }
        lockForMain.acquire();
        CMain.sleep();
        lockForMain.release();
        return;
    }

    static void AdultItinerary()
    {
	bg.initializeAdult(); //Required for autograder interface. Must be the first thing called.
	//DO NOT PUT ANYTHING ABOVE THIS LINE.

	/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
	*/
		lockForCount.acquire();
		count++;
		lockForCount.release();
		
		boolean int_state = Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(KThread.currentThread(), 1);
		Machine.interrupt().restore(int_state);
	
		lockForAdult.acquire();
		while (C == 0) {
			Cadult.sleep();
		}
		lockForAdult.release();

		lockForO.acquire();
		while (OA == 0) {
			CO.sleep();
		}
		OA = 0;
		lockForO.release();
		bg.AdultRowToMolokai();
		sequence[index] = 3;
		index++;
		count--;
		M++;
		lockForM.acquire();
		CM.wakeAll();
		lockForM.release();
		return;
    }

    static void ChildItinerary()
    {
	bg.initializeChild(); //Required for autograder interface. Must be the first thing called.
	//DO NOT PUT ANYTHING ABOVE THIS LINE.
	boolean int_state;
		lockForCount.acquire();
		count++;
		lockForCount.release();
	
		lockForSetPriority.acquire();
		if (P > 0) {
			P--;
			int_state = Machine.interrupt().disable();
			ThreadedKernel.scheduler.setPriority(KThread.currentThread(), 0);
			Machine.interrupt().restore(int_state);
			Cpriority.sleep(); 
			lockForSetPriority.release();
			lockForAdult.acquire();
			Cadult.wakeAll(); 
			C = 1;
			lockForAdult.release();
		}
		else {
			int_state = Machine.interrupt().disable();
			ThreadedKernel.scheduler.setPriority(KThread.currentThread(), 2);
			Machine.interrupt().restore(int_state);
			Cpriority.wake(); 
			lockForSetPriority.release();
		}
	
		lockForO.acquire();
		while (OCRide == 0) {
			CO.sleep();
		}
		if (OCRow > 0) {
			OCRow = 0;
			OA = 0;
			lockForO.release();
			lockForRideAndRow.acquire();
			bg.ChildRowToMolokai();
			sequence[index] = 0;
			index++;
			count--;
			Row = 1;
			Cride.wake();  
			while (Ride == 0) {
				Crow.sleep();
			}
			Ride = 0;
			lockForRideAndRow.release();
		}
		else {
			OCRide = 0;
			lockForO.release();
			lockForRideAndRow.acquire();
			while (Row == 0) {
				Crow.sleep();
			}
			Row = 0;
			bg.ChildRideToMolokai();
			sequence[index] = 1;
			index++;
			count--;
			Ride = 1;
			Crow.wake();
			lockForRideAndRow.release();
		}
		
		while (true) {
			lockForLand.acquire();
			if (count == 0) {
				lockForMain.acquire();
				CMain.wakeAll();
				lockForMain.release();
				return;
			}
			int_state = Machine.interrupt().disable();
			if (land > 0 || ThreadedKernel.scheduler.getPriority(KThread.currentThread()) == 0) {
				Machine.interrupt().restore(int_state);
				land = 0;
				lockForLand.release(); 
			}
			else {
				Machine.interrupt().restore(int_state);
				land = 1;
				lockForLand.release();
				lockForM.acquire();
				while (M == 0) {
					CM.sleep();
				}
				M = 0;
				lockForM.release();
			}
			bg.ChildRowToOahu();
			sequence[index] = 2;
			index++;
			count++;
			
			int_state = Machine.interrupt().disable();
			if (ThreadedKernel.scheduler.getPriority(KThread.currentThread()) == 0) {
				Machine.interrupt().restore(int_state);
				lockForO.acquire();
				OCRow = 1;
				OA = 1; 
				OCRide = 1;
				CO.wakeAll();
				do {
					CO.sleep();
				} while (OCRide == 0);
				OCRide = 0;
				lockForO.release();
				lockForRideAndRow.acquire();
				while (Row == 0) {
					Crow.sleep();
				}
				Row = 0;
				bg.ChildRideToMolokai();
				sequence[index] = 1;
				index++;
				count--;
				Ride = 1;
				Crow.wake();
				lockForRideAndRow.release();
			}
			
			else {
				Machine.interrupt().restore(int_state);
				lockForO.acquire();
				OCRide = 1;
				CO.wakeAll();
				lockForO.release();
				lockForRideAndRow.acquire();
				bg.ChildRowToMolokai();
				sequence[index] = 0;
				index++;
				count--;
				Row = 1;
				Cride.wake();
				while (Ride == 0) {
					Crow.sleep();
				}
				Ride = 0;
				lockForRideAndRow.release();
			}
		}

    }

    static void SampleItinerary()
    {
	// Please note that this isn't a valid solution (you can't fit
	// all of them on the boat). Please also note that you may not
	// have a single thread calculate a solution and then just play
	// it back at the autograder -- you will be caught.
	System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
	bg.AdultRowToMolokai();
	bg.ChildRideToMolokai();
	bg.AdultRideToMolokai();
	bg.ChildRideToMolokai();
    }

}
