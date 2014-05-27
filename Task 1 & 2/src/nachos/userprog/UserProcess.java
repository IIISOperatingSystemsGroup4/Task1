//ytxh{binofudofxhgdofyh`nridojdptkh`o`ov`ofrhvdh
//PART OF THE NACHOS. DON'T CHANGE CODE OF THIS LINE
package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.userprog.UserKernel.FileAttribute;

import java.io.EOFException;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
    public OpenFile[] descriptorTable;
    int maxOpenFiles = 16;
    /**
     * Allocate a new process.
     */
    public UserProcess() {
    	processID = UserKernel.getKernel().processManager.newProcess(this, -1);
	int numPhysPages = Machine.processor().getNumPhysPages();
	pageTable = new TranslationEntry[numPhysPages];
	for (int i=0; i<numPhysPages; i++)
	    pageTable[i] = new TranslationEntry(i,i, true,false,false,false);
	
	descriptorTable = new OpenFile[maxOpenFiles];
	descriptorTable[0] = UserKernel.console.openForReading();
	descriptorTable[1] = UserKernel.console.openForWriting();
    }

    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return	a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
	return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
	if (!load(name, args))
	    return false;
	thisThread = new UThread(this);
	thisThread.setName(name).fork();
	//new UThread(this).setName(name).fork();

	return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
	Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
	Lib.assertTrue(maxLength >= 0);

	byte[] bytes = new byte[maxLength+1];

	int bytesRead = readVirtualMemory(vaddr, bytes);

	for (int length=0; length<bytesRead; length++) {
	    if (bytes[length] == 0)
		return new String(bytes, 0, length);
	}

	return null;
    }

    int getPhyAddr(int vAddr) {
        int vPage=Processor.pageFromAddress(vAddr);
        int offset=Processor.offsetFromAddress(vAddr);
        if (vPage>numPages) return -1;
        if (!pageTable[vPage].valid) return -1;
        int phyPage=pageTable[vPage].ppn;
        return Processor.makeAddress(phyPage, offset);
    }
    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
	return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
                 int length) {
    Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

    byte[] memory = Machine.processor().getMemory();
    
    int copied=0;
    while (copied<length) {
        int page=Processor.pageFromAddress(vaddr+copied);
        if ((page>numPages)||(!pageTable[page].valid)) {
            if (page>numPages) System.out.println("1:Not proper virtual page:"+page+">numPages="+numPages);
            if (!pageTable[page].valid) System.out.println("1:Not proper virtual page:"+page+"invalid");
            handleException(Processor.exceptionPageFault);
            break;
        }
        int phyAddr=getPhyAddr(vaddr+copied);
        int amount=Math.min(Processor.pageSize, length-copied);
        System.arraycopy(memory, phyAddr, data, copied+offset, amount);
        copied+=amount;
    }

    return copied;
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
	return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
                  int length) {
    Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

    byte[] memory = Machine.processor().getMemory();
    
    int copied=0;
    while (copied<length) {
        int page=Processor.pageFromAddress(vaddr+copied);
        if ((page>numPages)||(!pageTable[page].valid)) {
            if (page>numPages) System.out.println("2:Not proper virtual page:"+page+">numPages="+numPages);
            if (!pageTable[page].valid) System.out.println("2:Not proper virtual page:"+page+"invalid");
            handleException(Processor.exceptionPageFault);
            break;
        }
        if (pageTable[page].readOnly) {
            handleException(Processor.exceptionReadOnly);
            break;
        }
        int phyAddr=getPhyAddr(vaddr+copied);
        int amount=Math.min(Processor.pageSize, length-copied);
        System.arraycopy(data, copied+offset, memory, phyAddr, amount);
        copied+=amount;
    }
    return copied;
    }


    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
	Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
	OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
	if (executable == null) {
	    Lib.debug(dbgProcess, "\topen failed");
	    return false;
	}
	try {
	    coff = new Coff(executable);
	}
	catch (EOFException e) {
	    executable.close();
	    Lib.debug(dbgProcess, "\tcoff load failed");
	    return false;
	}
	
	// make sure the sections are contiguous and start at page 0
	numPages = 0;
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    if (section.getFirstVPN() != numPages) {
		coff.close();
		Lib.debug(dbgProcess, "\tfragmented executable");
		return false;
	    }
	    numPages += section.getLength();
	}

	// make sure the argv array will fit in one page
	byte[][] argv = new byte[args.length][];
	int argsSize = 0;
	for (int i=0; i<args.length; i++) {
	    argv[i] = args[i].getBytes();
	    // 4 bytes for argv[] pointer; then string plus one for null byte
	    argsSize += 4 + argv[i].length + 1;
	}
	if (argsSize > pageSize) {
	    coff.close();
	    Lib.debug(dbgProcess, "\targuments too long");
	    return false;
	}

	// program counter initially points at the program entry point
	savedPC = coff.getEntryPoint();

	// next comes the stack; stack pointer initially points to top of it
	numPages += stackPages;
	savedSP = numPages*pageSize;

	// and finally reserve 1 page for arguments
	numPages++;
	
	if (!loadSections())
	{
		System.out.println("LoadSection returned false!");
	    return false;
	}

	// store arguments in last page
	int entryOffset = (numPages-1)*pageSize;
	int stringOffset = entryOffset + args.length*4;

	this.argc = args.length;
	this.argv = entryOffset;

	for (int i=0; i<argv.length; i++) {
	    byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
	    Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
	    entryOffset += 4;
	    Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
		       argv[i].length);
	    stringOffset += argv[i].length;
	    Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
	    stringOffset += 1;
	}
	System.out.println("Load finish.");
	return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
    pageTable=((UserKernel)(Kernel.kernel)).allocatePageTable(numPages);
    if (pageTable==null)
    {
        coff.close();
        Lib.debug(dbgProcess, "\tinsufficient physical memory");
        return false;       
    }
    // load sections
    for (int s=0; s<coff.getNumSections(); s++) {
        CoffSection section = coff.getSection(s);

        Lib.debug(dbgProcess, "\tinitializing " + section.getName()
              + " section (" + section.getLength() + " pages)");

        for (int i=0; i<section.getLength(); i++) {
        int vpn = section.getFirstVPN()+i;
        if ((vpn>numPages)||(!pageTable[vpn].valid))
        {
            if (vpn>numPages) System.out.println("3:Not proper virtual page:"+vpn+">numPages="+numPages);
            if (!pageTable[vpn].valid) System.out.println("3:Not proper virtual page:"+vpn+"invalid");
            coff.close();
            handleException(Processor.exceptionPageFault);
            return false;
        }
        int ppn=pageTable[vpn].ppn;
        
        // for now, just assume virtual addresses=physical addresses
        section.loadPage(i, ppn);
        }
    }

    return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
        ((UserKernel)(Kernel.kernel)).releasePageTable(pageTable);
    }

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
	Processor processor = Machine.processor();

	// by default, everything's 0
	for (int i=0; i<processor.numUserRegisters; i++)
	    processor.writeRegister(i, 0);

	// initialize PC and SP according
	processor.writeRegister(Processor.regPC, savedPC);
	processor.writeRegister(Processor.regSP, savedSP);

	// initialize the first two argument registers to argc and argv
	processor.writeRegister(Processor.regA0, argc);
	processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Handle the halt() system call.
     */
    private int handleHalt() {
    	if(processID == 0) {
    		Machine.halt();
    	} else {
    		System.out.println("didn't halt"+processID);
    		return -1;
    	}
	//Machine.halt();

	Lib.assertNotReached("Machine.halt() did not halt machine!");
	return 0;
    }
    
    private int handleExit(int returnCode){
		unloadSections();
		UserKernel.getKernel().processManager.removeParent(processID);
		Lib.assertTrue(UserKernel.getKernel().processManager.checkNoChildren(processID), "Children not disowned");
		for(int i = 0; i < maxOpenFiles; i++) {
			if(descriptorTable[i] != null) {
				handleClose(i);
			}
		}
		UserKernel.getKernel().processManager.setReturn(processID, returnCode);
		UserKernel.getKernel().processManager.setFinished(processID);
		if(UserKernel.getKernel().processManager.isLastProcess(processID)) {
			Kernel.kernel.terminate();
		}
		KThread.currentThread().finish();
		Lib.assertNotReached("syscall exit didn't exit");
		return 0;
	}

    private int handleExec(String fileName,int argc, int argvAddr){
		UserProcess newProcess = newUserProcess();
		UserKernel.getKernel().processManager.changeParent(newProcess.processID, processID);
		String[] args = new String[argc];
		byte[] argAddresses = new byte[argc * bytesInInt];
		readVirtualMemory(argvAddr, argAddresses);
		for(int i = 0; i < argc; i++) {	
			args[i] = readVirtualMemoryString(Lib.bytesToInt(argAddresses, i * bytesInInt), 256);
		}
		if(newProcess.execute(fileName, args) == false) {
			UserKernel.getKernel().processManager.setFinished(newProcess.processID);
			UserKernel.getKernel().processManager.setError(processID);
			UserKernel.getKernel().processManager.setReturn(processID, -1);
			return -1;
		}
		return newProcess.processID;
	}

	private int handleJoin(int childID, int statusAddr){
		if(UserKernel.getKernel().processManager.exists(childID) == false) {
			return -1;
		}
		if(UserKernel.getKernel().processManager.getParent(childID) != processID) {
			return -1;
		}
		if(UserKernel.getKernel().processManager.isRunning(childID)) {
			UserKernel.getKernel().processManager.getProcess(childID).thisThread.join();
		}
		if(UserKernel.getKernel().processManager.checkError(childID)) {
			return 0;
		}
		UserKernel.getKernel().processManager.changeParent(childID, -1);		
		int status = UserKernel.getKernel().processManager.getReturn(childID);
		writeVirtualMemory(statusAddr, Lib.bytesFromInt(status));
		return 1;
	}
	
    private int handleCreate(String filename) {
        OpenFile openFile = Machine.stubFileSystem().open(filename, true);
        if (openFile == null) return -1;
        openFile.close();
        return 0;
    }
    
    private int handleOpen(String filename) {
        //ensure the file is not unlinked.
        FileAttribute attribute = UserKernel.getKernel().fileTable.get(filename);
        if (attribute != null && attribute.unlinked == true) { 
            return -1;
        }

        OpenFile openFile = Machine.stubFileSystem().open(filename, false);
        if (openFile == null) return -1;
        for (int i = 2; i < maxOpenFiles; i++) {
            if (descriptorTable[i] == null) {
                if (attribute == null) {
                    UserKernel.getKernel().fileTable.put(filename, UserKernel.getKernel().new FileAttribute());
                }
                else attribute.count++;
                descriptorTable[i] = openFile;
                return i;
            }
        }
        return -1;
    }   

    private int handleRead(int fd, int buffer, int size)  {
        //check for invalid cases
        if (fd < 0 || fd >= maxOpenFiles || size < 0) return -1;
        OpenFile openFile = descriptorTable[fd];
        if (openFile == null 
            || fd == 1 && openFile.getName() == null /* stdout */) {
            return -1;
        }

        byte[] temp = new byte[size];
        int actual_size = openFile.read(temp, 0, size);
        if (actual_size < 0) return -1;
        return writeVirtualMemory(buffer, temp, 0, actual_size);
    }

    private int handleWrite(int fd, int buffer, int size) {
        //check for invalid cases
        if (fd < 0 || fd >= maxOpenFiles || size < 0) return -1;
        OpenFile openFile = descriptorTable[fd];
        if (openFile == null 
            || fd == 0 && openFile.getName() == null /* stdin */) {
            return -1;
        }

        byte[] temp = new byte[size];
        if (readVirtualMemory(buffer, temp) < size) return -1;
        return openFile.write(temp, 0, size);
    }

    private int handleClose(int fd) {
        //check for invalid cases
        if (fd < 0 || fd >= maxOpenFiles) return -1;
        OpenFile openFile = descriptorTable[fd];
        if (openFile == null) return -1;

        openFile.close();
        descriptorTable[fd] = null;

        String filename = openFile.getName();
        if (filename != null) { 
            FileAttribute attribute = UserKernel.getKernel().fileTable.get(filename);
            if (attribute == null) return -1;
            attribute.count--;
            if (attribute.count == 0) {
                UserKernel.getKernel().fileTable.remove(filename);
                if (attribute.unlinked) {
                    //now it¡¯s time to remove it
                    Machine.stubFileSystem().remove(filename);
                }
            }
        }
        
        
        return 0;
    }

    private int handleUnlink(String filename) {
        FileAttribute attribute = UserKernel.getKernel().fileTable.get(filename);

        //if no openFile access to this file
        if (attribute == null) {
            if (Machine.stubFileSystem().remove(filename)) return 0;
            else return -1;
        }

        //otherwise
        attribute.unlinked = true;
        return 0;
    }



    private static final int
    syscallHalt = 0,
	syscallExit = 1,
	syscallExec = 2,
	syscallJoin = 3,
	syscallCreate = 4,
	syscallOpen = 5,
	syscallRead = 6,
	syscallWrite = 7,
	syscallClose = 8,
	syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     *
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
	switch (syscall) {
	case syscallHalt:
	    return handleHalt();
	case syscallCreate:
        return handleCreate(readVirtualMemoryString(a0, 256));
	case syscallOpen:
	    return handleOpen(readVirtualMemoryString(a0, 256));
	case syscallRead:
        return handleRead(a0, a1, a2);
	case syscallWrite:
        return handleWrite(a0, a1, a2);
	case syscallClose:
        return handleClose(a0);
	case syscallUnlink:
        return handleUnlink(readVirtualMemoryString(a0, 256));
	case syscallExit:
		return handleExit(a0);
	case syscallExec:
		return handleExec(readVirtualMemoryString(a0,256),a1,a2);
	case syscallJoin:
		return handleJoin(a0,a1);


	default:
	    Lib.debug(dbgProcess, "Unknown syscall " + syscall);
	    Lib.assertNotReached("Unknown system call!");
	}
	return 0;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
	Processor processor = Machine.processor();

	switch (cause) {
	case Processor.exceptionSyscall:
	    int result = handleSyscall(processor.readRegister(Processor.regV0),
				       processor.readRegister(Processor.regA0),
				       processor.readRegister(Processor.regA1),
				       processor.readRegister(Processor.regA2),
				       processor.readRegister(Processor.regA3)
				       );
	    processor.writeRegister(Processor.regV0, result);
	    processor.advancePC();
	    break;
    case Processor.exceptionPageFault:
        System.out.println("Fault:Invalid page accessed.\n");
        break;
    case Processor.exceptionReadOnly:
        System.out.println("Fault:Try to write a read-only page.\n");
        break;      
	default:
	    Lib.debug(dbgProcess, "Unexpected exception: " +
		      Processor.exceptionNames[cause]);
	    Lib.assertNotReached("Unexpected exception");
	}
    }

    public static void selfTest() {
        UserProcess testProcess=new UserProcess();
        testProcess.pageTable=((UserKernel)(Kernel.kernel)).allocatePageTable(15);
        testProcess.numPages=15;
        int vaddr=Processor.makeAddress(14, Processor.pageSize-2);
        byte[] toWrite=new byte[12];
        byte[] toRead=new byte[12];
        for (int i=0;i<12;i++) toWrite[i]=(byte) i;
        testProcess.writeVirtualMemory(vaddr, toWrite, 4, 8);
        testProcess.readVirtualMemory(vaddr, toRead, 4, 8);
        for (int i=4;i<12;i++) 
            if (toWrite[i]!=toRead[i])
                System.out.println("Test failed: The write and read didn't provide the same result.");
        testProcess.unloadSections();
        
        int pageBefore=((UserKernel)(Kernel.kernel)).freePhysicalPages.size();
        testProcess.load("halt.coff", new String[]{});
        int pageAfter=((UserKernel)(Kernel.kernel)).freePhysicalPages.size();
        if (pageBefore!=pageAfter+testProcess.numPages)
            System.out.println("Test failed: pageBefore!=pageAfter+numPages.");
        System.out.println("Test finish!");
        
        
    }
    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;

    private int initialPC, initialSP;
    private int savedPC, savedSP;
    private int argc, argv;

    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    
    private static final int bytesInInt = 4;
    private UThread thisThread;
    private int processID;
}
