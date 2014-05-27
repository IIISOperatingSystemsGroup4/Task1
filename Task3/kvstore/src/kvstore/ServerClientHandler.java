package kvstore;

import static kvstore.KVConstants.*;

import java.net.Socket;
import java.io.*;

/**
 * This NetworkHandler will asynchronously handle the socket connections.
 * Uses a thread pool to ensure that none of its methods are blocking.
 */
public class ServerClientHandler implements NetworkHandler {

    private KVServer kvServer;
    private ThreadPool threadPool;

    /**
     * Constructs a ServerClientHandler with ThreadPool of a single thread.
     *
     * @param kvServer KVServer to carry out requests
     */
    public ServerClientHandler(KVServer kvServer) {
        this(kvServer, 1);
    }

    /**
     * Constructs a ServerClientHandler with ThreadPool of thread equal to
     * the number passed in as connections.
     *
     * @param kvServer KVServer to carry out requests
     * @param connections number of threads in threadPool to service requests
     */
    public ServerClientHandler(KVServer kvServer, int connections) {
        // implement me
    	this.kvServer = kvServer;
		threadPool = new ThreadPool(connections);
    }

    /**
     * Creates a job to service the request for a socket and enqueues that job
     * in the thread pool. Ignore any InterruptedExceptions.
     *
     * @param client Socket connected to the client with the request
     */
    @Override
    public void handle(Socket client) {
        // implement me
    	Runnable r = new ClientHandler(kvServer, client);
		try {
			threadPool.addJob(r);
		}
		catch (InterruptedException e) {
			// ignore error
			return;
		}
    }

    /**
     * Runnable class with routine to service a request from the client.
     */
    private class ClientHandler implements Runnable {

        private Socket client;
        private KVServer kvServer = null;
        
        /**
         * Construct a ClientHandler.
         *
         * @param client Socket connected to client with the request
         */
        public ClientHandler(Socket client) {
            this.client = client;
        }        
        
        public ClientHandler(KVServer kvServer, Socket client) {
			this.kvServer = kvServer;
			this.client = client;
		}

        /**
         * Processes request from client and sends back a response with the
         * result. The delivery of the response is best-effort. If we are
         * unable to return a response, there is nothing else we can do.
         */
        @Override
        public void run() {
            // implement me
        	KVMessage request = null;
        	KVMessage response = null;
        	try {
        		request = new KVMessage(client);
        	}
            catch (KVException e) {
                response = e.getKVMessage();
            }
        	try {
        		if (response == null) {
            		String requestType = request.getMsgType();
            		String requestKey = request.getKey();
            		String requestValue = request.getValue();
            		response = new KVMessage(RESP);
            		if (requestType.equals(GET_REQ)) {
            			response.setKey(requestKey);
            			response.setValue(kvServer.get(requestKey));
            		}
            		else if (requestType.equals(PUT_REQ)) {
            			kvServer.put(requestKey, requestValue);
            			response.setMessage(SUCCESS);
            		}
            		else if (requestType.equals(DEL_REQ)) {
            			kvServer.del(requestKey);
            			response.setMessage(SUCCESS);
            		}
            		else
            			response.setMessage(ERROR_INVALID_FORMAT);
            	}
        	}
        	catch (KVException e) {
        		response = e.getKVMessage();
        	}
        	try {
				response.sendMessage(client);
                client.close();
			} catch (KVException e) {
				// ignore error
			} 
            catch (IOException e) {}
        }
    }

}
