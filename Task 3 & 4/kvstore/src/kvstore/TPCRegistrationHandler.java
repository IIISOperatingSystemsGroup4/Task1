package kvstore;

import static kvstore.KVConstants.*;

import java.io.IOException;
import java.net.Socket;

/**
 * This NetworkHandler will asynchronously handle the socket connections.
 * Uses a thread pool to ensure that none of its methods are blocking.
 */
public class TPCRegistrationHandler implements NetworkHandler {

    private ThreadPool threadpool;
    private TPCMaster master;

    /**
     * Constructs a TPCRegistrationHandler with a ThreadPool of a single thread.
     *
     * @param master TPCMaster to register slave with
     */
    public TPCRegistrationHandler(TPCMaster master) {
        this(master, 1);
    }

    /**
     * Constructs a TPCRegistrationHandler with ThreadPool of thread equal to the
     * number given as connections.
     *
     * @param master TPCMaster to carry out requests
     * @param connections number of threads in threadPool to service requests
     */
    public TPCRegistrationHandler(TPCMaster master, int connections) {
        this.threadpool = new ThreadPool(connections);
        this.master = master;
    }

    /**
     * Creates a job to service the request on a socket and enqueues that job
     * in the thread pool. Ignore any InterruptedExceptions.
     *
     * @param slave Socket connected to the slave with the request
     */
    @Override
    public void handle(Socket slave) {
        // implement me
    	try {
    		Runnable r = new RegistrationHandler(slave);
    		threadpool.addJob(r);
    	} catch (InterruptedException e){
    		e.printStackTrace();
    	}
    }

    /**
     * Runnable class containing routine to service a registration request from
     * a slave.
     */
    public class RegistrationHandler implements Runnable {

        public Socket slave = null;

        public RegistrationHandler(Socket slave) {
            this.slave = slave;
        }

        /**
         * Parse registration request from slave and add register with TPCMaster.
         * If able to successfully parse request and register slave, send back
         * a successful response according to spec. If not, send back a response
         * with ERROR_INVALID_FORMAT.
         */
        @Override
        public void run() {
            // implement me
        	KVMessage ackMsg, regMsg;
        	String response = "";
        	try{
        		regMsg = new KVMessage(slave);
        		if (!regMsg.getMsgType().equals("register")){
        			ackMsg = new KVMessage(ERROR_INVALID_FORMAT);
        			ackMsg.sendMessage(slave);
        			return;
        		}
        		String msg = regMsg.getMessage();
        		TPCSlaveInfo sinfo = new TPCSlaveInfo(msg);
                master.registerSlave(sinfo);
        		response += "Successfully registered " + msg;
        		ackMsg = new KVMessage(RESP, response);
        		ackMsg.sendMessage(slave);
        	} catch (Exception e){
        		e.printStackTrace();
        	}
        	
        }
    }
}
