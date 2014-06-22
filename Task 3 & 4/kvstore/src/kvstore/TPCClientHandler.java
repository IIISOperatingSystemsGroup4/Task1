package kvstore;

import static kvstore.KVConstants.*;

import java.io.IOException;
import java.net.Socket;
import java.util.Collections;

/**
 * This NetworkHandler will asynchronously handle the socket connections.
 * It uses a threadPool to ensure that none of it's methods are blocking.
 */
public class TPCClientHandler implements NetworkHandler {

    private TPCMaster tpcMaster;
    private ThreadPool threadPool;

    /**
     * Constructs a TPCClientHandler with ThreadPool of a single thread.
     *
     * @param tpcMaster TPCMaster to carry out requests
     */
    public TPCClientHandler(TPCMaster tpcMaster) {
        this(tpcMaster, 1);
    }

    /**
     * Constructs a TPCClientHandler with ThreadPool of a single thread.
     *
     * @param tpcMaster TPCMaster to carry out requests
     * @param connections number of threads in threadPool to service requests
     */
    public TPCClientHandler(TPCMaster tpcMaster, int connections) {
        // implement me
    	this.tpcMaster = tpcMaster;
    	this.threadPool = new ThreadPool(connections);
    }

    /**
     * Creates a job to service the request on a socket and enqueues that job
     * in the thread pool. Ignore InterruptedExceptions.
     *
     * @param client Socket connected to the client with the request
     */
    @Override
    public void handle(Socket client) {
        // implement me
    	//System.out.println("Received Registration Request");
    	Runnable r = new ClientHandler(client);
    	try
    	{
    		threadPool.addJob(r);
    	}
    	catch (InterruptedException e)
    	{
    		return;
    	}
    }

    /**
     * Runnable class containing routine to service a request from the client.
     */
    private class ClientHandler implements Runnable {

        private Socket client = null;

        /**
         * Construct a ClientHandler.
         *
         * @param client Socket connected to client with the request
         */
        public ClientHandler(Socket client) {
            this.client = client;
        }

        /**
         * Processes request from client and sends back a response with the
         * result. The delivery of the response is best-effort. If we are
         * unable to return any response, there is nothing else we can do.
         */
//        @Override
//        public void run() {
//            // implement me
//        	
//        	System.out.println("Running Registration for SlaveServer");
//        	try
//        	{
//        		KVMessage resp = new KVMessage("resp");
//        		try
//        		{
//        			KVMessage reg = new KVMessage(client);
//        			TPCSlaveInfo slave = new TPCSlaveInfo(reg.getMessage());
//        			tpcMaster.lock.lock();
//        			
//        			tpcMaster.registerSlave(slave);
//        			resp.setMessage("Successfully registered " + reg.getMessage());
//        			tpcMaster.lock.unlock();
//        		}
//        		catch(Exception e)
//        		{
//        			resp.setMessage("UNknown Error: Registeration failed");
//        		}
//        		resp.sendMessage(client);
//        	}
//        	catch(KVException e)
//        	{}
//        }
        
        @Override
        public void run() {
            // implement me
            try {
                final KVMessage req = new KVMessage(client);
                String type = req.getMsgType();
                if (type.equals(GET_REQ)) {
                    threadPool.addJob(new Thread() {
                        @Override
                        public void run() {
                            try {
                                KVMessage resp = new KVMessage(RESP);
                                resp.setValue(tpcMaster.handleGet(req));
                                resp.setKey(req.getKey());
                                resp.sendMessage(client);
                            } catch (KVException e) {
                                try {
                                    e.getKVMessage().sendMessage(client);
                                } catch (KVException e1) {
                                    e1.printStackTrace();
                                }
                            }
                        }
                    });
                }
                else if (type.equals(PUT_REQ)) {
                    threadPool.addJob(new Thread() {
                        @Override
                        public void run() {
                            try {
                                tpcMaster.handleTPCRequest(req, true);
                                KVMessage resp = new KVMessage(RESP, SUCCESS);
                                resp.sendMessage(client);
                            } catch (KVException e) {
                                try {
                                    e.getKVMessage().sendMessage(client);
                                } catch (KVException e1) {
                                    e1.printStackTrace();
                                }
                            }
                        }
                    });
                }
                else {
                    threadPool.addJob(new Thread() {
                        @Override
                        public void run() {
                            try {
                                tpcMaster.handleTPCRequest(req, false);
                                KVMessage resp = new KVMessage(RESP, SUCCESS);
                                resp.sendMessage(client);
                            } catch (KVException e) {
                                try {
                                    e.getKVMessage().sendMessage(client);
                                } catch (KVException e1) {
                                    e1.printStackTrace();
                                }
                            }
                        }
                    });
                }
            } catch (KVException | InterruptedException e) {
                try {
                    new KVMessage(RESP, e.getMessage()).sendMessage(client);
                } catch (KVException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

}
