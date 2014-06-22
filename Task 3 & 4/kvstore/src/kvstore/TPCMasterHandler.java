package kvstore;

import static kvstore.KVConstants.*;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
/**
 * Implements NetworkHandler to handle 2PC operation requests from the Master/
 * Coordinator Server
 */
public class TPCMasterHandler implements NetworkHandler {

    private long slaveID;
    private KVServer kvServer;
    private TPCLog tpcLog;
    private ThreadPool threadpool;

    /**
     * Constructs a TPCMasterHandler with one connection in its ThreadPool
     *
     * @param slaveID the ID for this slave server
     * @param kvServer KVServer for this slave
     * @param log the log for this slave
     */
    public TPCMasterHandler(long slaveID, KVServer kvServer, TPCLog log) {
        this(slaveID, kvServer, log, 1);
    }
    
  
    /**
     * Constructs a TPCMasterHandler with a variable number of connections
     * in its ThreadPool
     *
     * @param slaveID the ID for this slave server
     * @param kvServer KVServer for this slave
     * @param log the log for this slave
     * @param connections the number of connections in this slave's ThreadPool
     */
    public TPCMasterHandler(long slaveID, KVServer kvServer, TPCLog log, int connections) {
        this.slaveID = slaveID;
        this.kvServer = kvServer;
        this.tpcLog = log;
        this.threadpool = new ThreadPool(connections);
    }

    /**
     * Registers this slave server with the master.
     *
     * @param masterHostname
     * @param server SocketServer used by this slave server (which contains the
     *               hostname and port this slave is listening for requests on
     * @throws KVException with ERROR_INVALID_FORMAT if the response from the
     *         master is received and parsed but does not correspond to a
     *         success as defined in the spec OR any other KVException such
     *         as those expected in KVClient in project 3 if unable to receive
     *         and/or parse message
     */
    public void registerWithMaster(String masterHostname, SocketServer server)
            throws KVException {
        // implement me
    	Socket master = null;
		try {
			master = new Socket(masterHostname, 9090);
			KVMessage regMessage = new KVMessage("register", slaveID + "@" + server.getHostname() 
					+ ":" + server.getPort());
			regMessage.sendMessage(master);
			master.close();
		} catch (UnknownHostException e) {
			// ignore
		} catch (IOException e) {
			// ignore
		}
    }

    /**
     * Creates a job to service the request on a socket and enqueues that job
     * in the thread pool. Ignore any InterruptedExceptions.
     *
     * @param master Socket connected to the master with the request
     */
    @Override
    public void handle(Socket master) {
        // implement me
    	MasterHandler handler = new MasterHandler(master);
    	try {
			threadpool.addJob(handler);
		} catch (InterruptedException e) {
			// ignore
		}
    }

    /**
     * Runnable class containing routine to service a message from the master.
     */
    private class MasterHandler implements Runnable {

        private Socket master;

        /**
         * Construct a MasterHandler.
         *
         * @param master Socket connected to master with the message
         */
        public MasterHandler(Socket master) {
            this.master = master;
        }

        /**
         * Processes request from master and sends back a response with the
         * result. This method needs to handle both phase1 and phase2 messages
         * from the master. The delivery of the response is best-effort. If
         * we are unable to return any response, there is nothing else we can do.
         */
        @Override
        public void run() {
            // implement me
        	try {
        		KVMessage prepareOrDecision = new KVMessage(master);
        		KVMessage responseOrAck = new KVMessage(READY);
        		
        		switch(prepareOrDecision.getMsgType()) {
        			// phase 1
        			case PUT_REQ:
        				// key size no larger than 256, value size no larger than 256*1024
        				if (prepareOrDecision.getKey().length() > 256)
        					responseOrAck = new KVMessage(ABORT, ERROR_OVERSIZED_KEY);
        				if (prepareOrDecision.getValue().length() > 256 * 1024)
        					responseOrAck = new KVMessage(ABORT, ERROR_OVERSIZED_VALUE);
        				break;
        			case DEL_REQ:
        			    break;
        			case GET_REQ:
        				try {
        					responseOrAck = new KVMessage(RESP);
        					responseOrAck.setKey(prepareOrDecision.getKey());
							responseOrAck.setValue(kvServer.get(prepareOrDecision.getKey()));
        				} catch (KVException e) {
							responseOrAck = new KVMessage(ABORT, e.getKVMessage().getMessage());
        				}
        				break;
        				
        			// phase 2
        			case COMMIT:
        				responseOrAck = new KVMessage(ACK);
        				KVMessage prepare = tpcLog.getLastEntry();
						switch(prepare.getMsgType()) {
							case PUT_REQ:
								kvServer.put(prepare.getKey(), prepare.getValue());
								break;
							case DEL_REQ:
								kvServer.del(prepare.getKey());
								break;
							default:
								break;
						}
					case ABORT:
						responseOrAck = new KVMessage(ACK);
						break;
					
					// wrong format
					default:
						throw new KVException(ERROR_INVALID_FORMAT);
        		}
        		tpcLog.appendAndFlush(prepareOrDecision);
        		responseOrAck.sendMessage(master);
		
        	} catch (KVException e) {
		    	try {
					e.getKVMessage().sendMessage(master);
				} catch (KVException e1) {
					// ignore
				}
        	}		
        }

    }

}
