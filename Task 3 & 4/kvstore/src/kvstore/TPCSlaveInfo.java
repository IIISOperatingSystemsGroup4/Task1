package kvstore;

import static kvstore.KVConstants.*;

import java.io.IOException;
import java.net.*;
import java.util.regex.*;

/**
 * Data structure to maintain information about SlaveServers
 */
public class TPCSlaveInfo {

	// 64-bit globally unique ID of the SlaveServer
    private long slaveID = -1;
    // Name of the host this SlaveServer is running on
    private String hostname = null;
    // Port which SlaveServer is listening to
    private int port = -1;
    // Regex to parse slave info
    private static final Pattern SLAVE_INFO_REGEX = Pattern.compile("^(.*)@(.*):(.*)$");
//    // Timeout value used during 2PC operations
//    public static final int TIMEOUT_MILLISECONDS = 2000;

    /**
     * Construct a TPCSlaveInfo to represent a slave server.
     *
     * @param info as "SlaveServerID@Hostname:Port"
     * @throws KVException ERROR_INVALID_FORMAT if info string is invalid
     */
    public TPCSlaveInfo(String info) throws KVException {
        // implement me
    	Matcher infoMatcher = SLAVE_INFO_REGEX.matcher(info);
   		if (! infoMatcher.matches()){
   			throw new KVException(ERROR_INVALID_FORMAT);
   		}
   		slaveID = Long.parseLong(infoMatcher.group(1));
   		hostname = infoMatcher.group(2);
   		port = Integer.parseInt(infoMatcher.group(3));
    }

    public long getSlaveID() {
        return slaveID;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    /**
     * Create and connect a socket within a certain timeout.
     *
     * @return Socket object connected to SlaveServer, with timeout set
     * @throws KVException ERROR_SOCKET_TIMEOUT, ERROR_COULD_NOT_CREATE_SOCKET,
     *         or ERROR_COULD_NOT_CONNECT
     */
    public Socket connectHost(int timeout) throws KVException {
        // implement me
    	try {
        	Socket sock = new Socket();
        	sock.setSoTimeout(timeout);
        	sock.connect(new InetSocketAddress(hostname, port), timeout);
        	return sock;
        } catch (UnknownHostException e) {
            throw new KVException(ERROR_COULD_NOT_CONNECT);
        } catch (SocketTimeoutException e){
        	throw new KVException(ERROR_SOCKET_TIMEOUT);
        } catch (IOException e){
        	throw new KVException(ERROR_COULD_NOT_CREATE_SOCKET);
        }
    }

    /**
     * Closes a socket.
     * Best effort, ignores error since the response has already been received.
     *
     * @param sock Socket to be closed
     */
    public void closeHost(Socket sock) {
        // implement me
    	try {
    		sock.close();
    	} catch (IOException e){
    		e.printStackTrace();
    	}
    }
}
