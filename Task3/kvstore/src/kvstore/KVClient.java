package kvstore;

import static kvstore.KVConstants.*;

import java.net.Socket;
import java.io.IOException;

/**
 * Client API used to issue requests to key-value server.
 */
public class KVClient implements KeyValueInterface {

    private String server = null;
    private int port = 0;

    /**
     * Constructs a KVClient connected to a server.
     *
     * @param server is the DNS reference to the server
     * @param port is the port on which the server is listening
     */
    public KVClient(String server, int port) {
        this.server = server;
        this.port = port;
    }

    /**
     * Creates a socket connected to the server to make a request.
     *
     * @return Socket connected to server
     * @throws KVException if unable to create or connect socket
     */
    private Socket connectHost() throws KVException {
        // implement me
    	Socket socket = null;
    	try {
			socket = new Socket(server, port);
		} catch (IOException ex) {
    		throw new KVException(KVConstants.ERROR_COULD_NOT_CONNECT);
    	}
    	catch (Exception ex) {
    		throw new KVException(KVConstants.ERROR_COULD_NOT_CREATE_SOCKET);
    	}
        return socket;
    }

    /**
     * Closes a socket.
     * Best effort, ignores error since the response has already been received.
     *
     * @param  sock Socket to be closed
     */
    private void closeHost(Socket sock) {
        // implement me
    	try {
			sock.close();
	    } catch (Exception e) {
	    	
	    }
    }

    /**
     * Issues a PUT request to the server.
     *
     * @param  key String to put in server as key
     * @throws KVException if the request was not successful in any way
     */
    @Override
    public void put(String key, String value) throws KVException {
        // implement me
    	
    	if (key == null || key.length() == 0)
        	throw new KVException(KVConstants.ERROR_INVALID_KEY);
        if (value == null || value.length() == 0)
        	throw new KVException(KVConstants.ERROR_INVALID_VALUE);
        
    	Socket socket = null;
		KVMessage message = null;
		KVMessage response = null;

		try {
			socket = connectHost();
			message = new KVMessage("putreq");
			message.setKey(key);
			message.setValue(value);
			message.sendMessage(socket);
			
			response = new KVMessage(socket);
			closeHost(socket);
		} catch (Exception e) {
			throw new KVException(new KVMessage("resp", "Unknown Error: " + e.getMessage()));
		}

		if (! response.getMsgType().equals("resp")) {
			throw new KVException(new KVMessage("resp", "Uknown Error: Recieved a message not of type 'resp' from server"));
		}

		if (! response.getMessage().equals("Success")) {
			throw new KVException(response);
		}

		return;
    }

    /**
     * Issues a GET request to the server.
     *
     * @param  key String to get value for in server
     * @return String value associated with key
     * @throws KVException if the request was not successful in any way
     */
    @Override
    public String get(String key) throws KVException {
        // implement me
    	
    	 if (key == null || key.length() == 0)
         	throw new KVException(KVConstants.ERROR_INVALID_KEY);
    	 
    	Socket socket = null;
		KVMessage message = null;
		KVMessage response = null;

		try {
			// Connect to server
			socket = connectHost();

			// Initialize KVMessage of type getreq
			message = new KVMessage("getreq");

			// Set key and send message to server via socket
			message.setKey(key);
			message.sendMessage(socket);

			// Get the response from the server
			response = new KVMessage(socket);
			closeHost(socket);
		} catch (Exception e) {
			throw new KVException(new KVMessage("resp", "Unknown Error: " + e.getMessage()));
		}

		// If we don't recieve a KVMessage of type "resp" throw unknown error
		if (! response.getMsgType().equals("resp")) {
			throw new KVException(new KVMessage("resp", "Unknown Error: Recieved a message not of type 'resp' from server"));
		}

		// If KVMessage response doesn't have a value of type String, throw KVException with response
		if ( response.getValue() == null) {
			throw new KVException(response);
		} 

		return response.getValue();

    }

    /**
     * Issues a DEL request to the server.
     *
     * @param  key String to delete value for in server
     * @throws KVException if the request was not successful in any way
     */
    @Override
    public void del(String key) throws KVException {
        // implement me
    	if (key == null || key.length() == 0)
        	throw new KVException(KVConstants.ERROR_INVALID_KEY);
    	
    	Socket socket = null;
		KVMessage message = null;
		KVMessage response = null;

		try {
			// Connect to server
			socket = connectHost();

			// Initialize a KVMessage of type delreq
			message = new KVMessage("delreq");

			// Set key and value then send message
			message.setKey(key);
			message.sendMessage(socket);

			// Get response from server
			response = new KVMessage(socket);
			closeHost(socket);
		} catch (Exception e) {
			throw new KVException(new KVMessage("resp", "Unknown Error: " + e.getMessage()));
		}

		// If we don't recieve a KVMessage of type "resp" throw unknown error
		if (! response.getMsgType().equals("resp")) {
			throw new KVException(new KVMessage("resp", "Uknown Error: Recieved a message not of type 'resp' from server"));
		}

		// If KVMessage response isn't successful, throw KVException with response
		if (! response.getMessage().equals("Success")) {
			throw new KVException(response);
		} 

		return;

    }


}
