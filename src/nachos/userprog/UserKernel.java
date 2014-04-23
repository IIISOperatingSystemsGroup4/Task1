//ytxh{binofudofxhgdofyh`nridojdptkh`o`ov`ofrhvdh
//PART OF THE NACHOS. DON'T CHANGE CODE OF THIS LINE
package nachos.userprog;

import java.util.*;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {
    public class FileAttribute {
        int count;
        boolean unlinked;
        FileAttribute() {
            count = 1;
            unlinked = false;
        }
    }
    HashMap<String, FileAttribute> fileTable = new HashMap<String, FileAttribute>();
    
    public LinkedList<Integer> freePhysicalPages = new LinkedList<Integer>();
    
    public static UserKernel getKernel() {
        if (kernel instanceof UserKernel) return (UserKernel)kernel;
        else return null;
    }
    
    /**
     * Allocate a new user kernel.
     */
    public UserKernel() {
	super();
    }

    /**
     * Initialize this kernel. Creates a synchronized console and sets the
     * processor's exception handler.
     */
    public void initialize(String[] args) {
	super.initialize(args);

	for (int i=0;i<Machine.processor().getNumPhysPages();i++)
		freePhysicalPages.addLast(i);
	visitingPages=new Semaphore(0);
	visitingPages.V();
	console = new SynchConsole(Machine.console());
	
	processManager = new ProcessManager();

	Machine.processor().setExceptionHandler(new Runnable() {
		public void run() { exceptionHandler(); }
	    });
    }

    private Semaphore visitingPages;
    public TranslationEntry[] allocatePageTable(int numPages) {
//    	System.out.println("Acquiring lock");
    	visitingPages.P();
//    	System.out.println("Lock obtained.");
    	if (freePhysicalPages.size()<numPages) {
    		visitingPages.V();
    		return null;
    	}
    	TranslationEntry[] answer=new TranslationEntry[numPages];
    	for (int i=0;i<numPages;i++) {
    		int newPhysicalPage=freePhysicalPages.pollFirst();
    		answer[i]=new TranslationEntry(i,newPhysicalPage,true,false,false,false);
    	}
    	visitingPages.V();
//    	System.out.println("Lock released.");
    	return answer;
    }
    // Return the number of pages released.
    public int releasePageTable(TranslationEntry[] pageTable)
    {
//    	System.out.println("Acquiring lock");
    	visitingPages.P();
//    	System.out.println("Lock obtained.");
    	int ans=0;
    	for (int i=0;i<pageTable.length;i++) {
    		if (pageTable[i].valid)	freePhysicalPages.addLast(pageTable[i].ppn);
    		pageTable[i]=null;
    		ans=ans+1;
    	}
    	visitingPages.V();	
//    	System.out.println("Lock released.");
    	return ans;
    }
    /**
     * Test the console device.
     */
    public void selfTest() {
	super.selfTest();

	System.out.println("Testing the console device. Typed characters");
	System.out.println("will be echoed until q is typed.");

	char c;

	do {
	    c = (char) console.readByte(true);
	    console.writeByte(c);
	}
	while (c != 'q');

	System.out.println("");
//	System.out.println("Allocating the first one.");
//	TranslationEntry[] entry1=allocatePageTable(15);
//	System.out.println("Allocating the second one.");
//	TranslationEntry[] entry2=allocatePageTable(15);
//	TranslationEntry[] entry3=allocatePageTable(15);
//	releasePageTable(entry2);
//	TranslationEntry[] entry4=allocatePageTable(34);
//	if (entry4==null) System.out.println("Error:failed to allocate pages!");
//	for (int i=0;i<15;i++)
//		for (int j=0;j<34;j++)
//			if (entry1[i].ppn==entry4[j].ppn)
//				System.out.println("Error: allocate a page twice!");
//	releasePageTable(entry1);
//	releasePageTable(entry3);
//	releasePageTable(entry4);
//	System.out.println(freePhysicalPages.size());
//	UserProcess.selfTest();
    }

    /**
     * Returns the current process.
     *
     * @return	the current process, or <tt>null</tt> if no process is current.
     */
    public static UserProcess currentProcess() {
	if (!(KThread.currentThread() instanceof UThread))
	    return null;

	return ((UThread) KThread.currentThread()).process;
    }

    /**
     * The exception handler. This handler is called by the processor whenever
     * a user instruction causes a processor exception.
     *
     * <p>
     * When the exception handler is invoked, interrupts are enabled, and the
     * processor's cause register contains an integer identifying the cause of
     * the exception (see the <tt>exceptionZZZ</tt> constants in the
     * <tt>Processor</tt> class). If the exception involves a bad virtual
     * address (e.g. page fault, TLB miss, read-only, bus error, or address
     * error), the processor's BadVAddr register identifies the virtual address
     * that caused the exception.
     */
    public void exceptionHandler() {
	Lib.assertTrue(KThread.currentThread() instanceof UThread);

	UserProcess process = ((UThread) KThread.currentThread()).process;
	int cause = Machine.processor().readRegister(Processor.regCause);
	process.handleException(cause);
    }

    /**
     * Start running user programs, by creating a process and running a shell
     * program in it. The name of the shell program it must run is returned by
     * <tt>Machine.getShellProgramName()</tt>.
     *
     * @see	nachos.machine.Machine#getShellProgramName
     */
    public void run() {
	super.run();

	UserProcess process = UserProcess.newUserProcess();

	String shellProgram = Machine.getShellProgramName();
	
	Lib.assertTrue(process.execute(shellProgram, new String[] { }));

	KThread.currentThread().finish();
    }

    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
	super.terminate();
    }

    public class ProcessManager {
		public ProcessManager() {
			processList = new TreeMap<Integer, ProcessNode>();
		}
		
		//checks if a processID(PID) is valid
		public boolean exists(int processID) {
			return processList.containsKey(processID);
		}
		
		//create a new ProcessNode and assign it a processID
		public int newProcess(UserProcess process, int parent) {
			ProcessNode newProcessNode = new ProcessNode(process, parent, nextProcessID);
			processList.put(newProcessNode.pid, newProcessNode);
			nextProcessID++;
			return newProcessNode.pid;
		}
		
		//get the process which corresponds to the PID
		public UserProcess getProcess(int processID) {
			return processList.get(processID).process;
		}
		
		//change the parent of the process
		public void changeParent(int childPID, int parentPID) {
			processList.get(childPID).parent = parentPID;
		}
		
		//check whether a corresponding process has any children
		public boolean checkNoChildren(int parentPID) {
			Iterator iter = processList.keySet().iterator();
			ProcessNode processNode;
			while(iter.hasNext()) {
				processNode = processList.get(iter.next());
				if(processNode.parent == parentPID) {
					return false;
				}
			}
			return true;
		}
		
		//check if the process is the parent of others, if true, let it no longer be the parent
		public void removeParent(int parentPID) {
			Iterator iter = processList.keySet().iterator();
			ProcessNode processNode;
			while(iter.hasNext()) {
				processNode = processList.get(iter.next());
				if(processNode.parent == parentPID) {
					changeParent(processNode.pid, -1);
				}
			}
		}
		
		//return the PID of the process' parent
		public int getParent(int childPID) {
			return processList.get(childPID).parent;
		}
		
		//set return status of a process when it terminates
		public void setReturn(int pid, int returnCode) {
			processList.get(pid).exitStatus = returnCode;
		}
		
		//check whether a process is running or finished
		public boolean isRunning(int processID) {
			return processList.get(processID).running;
		}
		
		//set the running state to be false
		public void setFinished(int processID) {
			processList.get(processID).running = false;
		}
		
		//allows a parent to get the return status of a child
		public int getReturn(int processID) {
			ProcessNode process = processList.get(processID);
			return process.exitStatus;
		}
		
		//check whether the process exits due to an error
		public boolean checkError(int processID) {
			return processList.get(processID).error;
		}
		
		//notify that a process was terminated because of an error
		public void setError(int processID) {
			processList.get(processID).error = true;
		}
		
		//check if the process is the last process
		public boolean isLastProcess(int processID) {
			Iterator iter = processList.keySet().iterator();
			ProcessNode processNode;
			int count = 0;
			while(iter.hasNext()) {
				processNode = processList.get(iter.next());
				if(processNode.pid != processID && processNode.running) {
					count++;
				}
			}
			return (count == 0);
		}
		
		private class ProcessNode {
			public ProcessNode(UserProcess process, int parent, int pid) {
				this.pid = pid;
				this.parent = parent;
				this.process = process;
				this.running = true;
				this.joined = false;
				this.error = false;
			}
			
			int pid, parent;
			int exitStatus;
			boolean running, joined, error;
			UserProcess process;
		}
		
		private int nextProcessID = 0;
		private TreeMap<Integer, ProcessNode> processList;
	}
    
    /** Globally accessible reference to the synchronized console. */
    public static SynchConsole console;
    
    public ProcessManager processManager;

    // dummy variables to make javac smarter
    private static Coff dummy1 = null;
}
