package kvstore;

import static kvstore.KVConstants.*;

import java.net.Socket;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TPCMaster {

    public int numSlaves;
    private KVCache masterCache;
    ArrayList<TPCSlaveInfo> ts;
    public SocketServer slaveServer;
    volatile private KVMessage error;

    
    Lock lock;
    Condition cv; 

    public static final int TIMEOUT = 3000;

    /**
     * Creates TPCMaster, expecting numSlaves slave servers to eventually register
     *
     * @param numSlaves number of slave servers expected to register
     * @param cache KVCache to cache results on master
     */
    public TPCMaster(int numSlaves, KVCache cache) {
        this.numSlaves = numSlaves;
        this.masterCache = cache;
        // implement me
        ts = new ArrayList<TPCSlaveInfo>();
        slaveServer = new SocketServer("localhost", 9090);
        lock = new ReentrantLock();
        cv = lock.newCondition();
 
    }
    
    


    /**
     * Registers a slave. Drop registration request if numSlaves already
     * registered.Note that a slave re-registers under the same slaveID when
     * it comes back online.
     *
     * @param slave the slaveInfo to be registered
     */
    public void registerSlave(TPCSlaveInfo slave) {
        // implement me
    	{
    		lock.lock();
    		try{
    		int p = -1;
    		boolean exist = false;
    		for(int i = 0; i < ts.size(); i++)
    		{
    			if(ts.get(i).getSlaveID() == slave.getSlaveID())
    			{
    				exist = true;
    				p = i;
    				break;
    			}
    		}
    		if(exist)
    			ts.set(p, slave);
    		else if(ts.size() < numSlaves)
    		{
    			int q = -1;
    			for(int i = 0; i < ts.size(); i++)
    			{
    				if(isLessThanUnsigned(slave.getSlaveID(), ts.get(i).getSlaveID()))
    				{
    					q = i;
    					break;
    				}
    			}
    			if(q == -1)
    			    ts.add(slave);
    			else
    			{
    				TPCSlaveInfo temp = ts.get(q);
    				ts.add(slave);
    				ts.set(q, slave);
    				for(int j = ts.size() - 2; j > q; j--)
    					ts.set(j+1, ts.get(j));
    				ts.set(q+1, temp);
    			}

    		}
    		if(ts.size() == numSlaves)
    			cv.signalAll();
    		}
    		finally{		
    			lock.unlock();
    		}
    		
    	}
    	
    }

    /**
     * Converts Strings to 64-bit longs. Borrowed from http://goo.gl/le1o0W,
     * adapted from String.hashCode().
     *
     * @param string String to hash to 64-bit
     * @return long hashcode
     */
    public static long hashTo64bit(String string) {
        long h = 1125899906842597L;
        int len = string.length();

        for (int i = 0; i < len; i++) {
            h = (31 * h) + string.charAt(i);
        }
        return h;
    }

    /**
     * Compares two longs as if they were unsigned (Java doesn't have unsigned
     * data types except for char). Borrowed from http://goo.gl/QyuI0V
     *
     * @param n1 First long
     * @param n2 Second long
     * @return is unsigned n1 less than unsigned n2
     */
    public static boolean isLessThanUnsigned(long n1, long n2) {
        return (n1 < n2) ^ ((n1 < 0) != (n2 < 0));
    }

    /**
     * Compares two longs as if they were unsigned, uses isLessThanUnsigned
     *
     * @param n1 First long
     * @param n2 Second long
     * @return is unsigned n1 less than or equal to unsigned n2
     */
    public static boolean isLessThanEqualUnsigned(long n1, long n2) {
        return isLessThanUnsigned(n1, n2) || (n1 == n2);
    }

    /**
     * Find primary replica for a given key.
     *
     * @param key String to map to a slave server replica
     * @return SlaveInfo of first replica
     */
    public TPCSlaveInfo findFirstReplica(String key) {
        // implement me
    	lock.lock();
    	if(ts.size() < numSlaves)
			{
    			try {
    				cv.await();
    			} catch (InterruptedException e) {
    				e.printStackTrace();
    			}
			}
    	try{
    		for(int i = 0; i < ts.size(); i++)
    		{
    			if(isLessThanEqualUnsigned(hashTo64bit(key),ts.get(i).getSlaveID()))
    				return ts.get(i);
    		}
    		return ts.get(0);
    	}
    	finally
    	{
    		lock.unlock();
    	}
    }

    /**
     * Find the successor of firstReplica.
     *
     * @param firstReplica SlaveInfo of primary replica
     * @return SlaveInfo of successor replica
     */
    public TPCSlaveInfo findSuccessor(TPCSlaveInfo firstReplica) {
        // implement me
    	for(int i = 0; i < ts.size(); i++)
    	{
    		if(ts.get(i).getSlaveID() == firstReplica.getSlaveID())
    			return ts.get((i + 1) % ts.size());
    	}
    	return null;
    }

    /**
     * Perform 2PC operations from the master node perspective. This method
     * contains the bulk of the two-phase commit logic. It performs phase 1
     * and phase 2 with appropriate timeouts and retries.
     *
     * See the spec for details on the expected behavior.
     *
     * @param msg KVMessage corresponding to the transaction for this TPC request
     * @param isPutReq boolean to distinguish put and del requests
     * @throws KVException if the operation cannot be carried out for any reason
     */
    public synchronized void handleTPCRequest(KVMessage msg, boolean isPutReq)
            throws KVException {
        // implement me
        String key = msg.getKey();
        Lock lock = masterCache.getLock(key);
        lock.lock();
        error = null;
        try {
            Thread threadPhase1_0 = new Thread(
                    new RunnablePhase1(key, true, msg));
            Thread threadPhase1_1 = new Thread(
                    new RunnablePhase1(key, false, msg));
            threadPhase1_0.start();
            threadPhase1_1.start();
            try {
                threadPhase1_0.join();
                threadPhase1_1.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Thread threadPhase2_0 = new Thread(
                    new RunnablePhase2(key, true, error == null));
            Thread threadPhase2_1 = new Thread(
                    new RunnablePhase2(key, false, error == null));
            threadPhase2_0.start();
            threadPhase2_1.start();
            try {
                threadPhase2_0.join();
                threadPhase2_1.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            if (error != null) throw new KVException(error);
            if (isPutReq) masterCache.put(key, msg.getValue());
            else masterCache.del(key);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Perform GET operation in the following manner:
     * - Try to GET from cache, return immediately if found
     * - Try to GET from first/primary replica
     * - If primary succeeded, return value
     * - If primary failed, try to GET from the other replica
     * - If secondary succeeded, return value
     * - If secondary failed, return KVExceptions from both replicas
     *
     * @param msg KVMessage containing key to get
     * @return value corresponding to the Key
     * @throws KVException with ERROR_NO_SUCH_KEY if unable to get
     *         the value from either slave for any reason
     */
    public String handleGet(KVMessage msg) throws KVException {
        // implement me
        String key = msg.getKey();
        Lock lock = masterCache.getLock(key);
        lock.lock();
        try {
            String result = masterCache.get(key);
            if (result != null) return result;
            TPCSlaveInfo firstReplica;
            TPCSlaveInfo secondReplica;
            String value = null, message = null;
            KVMessage response = null;
            Socket sock = null;
            while (true) {
                try {
                    firstReplica = findFirstReplica(key);
                    sock = firstReplica.connectHost(TIMEOUT);
                    break;
                } catch (KVException e) {}
            }
            try {
                msg.sendMessage(sock);
                response = new KVMessage(sock, TIMEOUT);
                firstReplica.closeHost(sock);
                message = response.getMessage();
                if (message == null) 
                    value = response.getValue();
            } catch (KVException e) {}
            if (value == null) {
                while (true) {
                    try {
                        secondReplica = findSuccessor(findFirstReplica(key));
                        sock = secondReplica.connectHost(TIMEOUT);
                        break;
                    } catch (KVException e) {}
                }
                msg.sendMessage(sock);
                response = new KVMessage(sock, TIMEOUT);
                secondReplica.closeHost(sock);
                message = response.getMessage();
                if (message == null) 
                    value = response.getValue();
                else throw new KVException(message);
            }
            masterCache.put(key, value);
            return value;
        } finally {
            lock.unlock();
        }
    }

    private class RunnablePhase1 implements Runnable {
        
        private String key;
        private boolean isPrimarySlave;
        private KVMessage msg;
        
        public RunnablePhase1(
                String key, boolean isPrimarySlave, KVMessage msg) {
            super();
            this.key = key;
            this.isPrimarySlave = isPrimarySlave;
            this.msg = msg;
        }
        
        public void run() {
            TPCSlaveInfo slaveInfo;
            Socket sock = null;
            while (true) {
                slaveInfo = isPrimarySlave ? findFirstReplica(key) : 
                    findSuccessor(findFirstReplica(key));
                try {
                    sock = slaveInfo.connectHost(TIMEOUT);
                    break;
                } catch (KVException e) {}
            }
            try {
                msg.sendMessage(sock);
                KVMessage response = new KVMessage(sock, TIMEOUT);
                slaveInfo.closeHost(sock);
                if (response.getMsgType().equals(ABORT)) 
                    throw new KVException(response);
            } catch (KVException e) {
                error = e.getKVMessage();
            }
        }
        
    }
    
    private class RunnablePhase2 implements Runnable {
        
        private String key;
        private boolean isPrimarySlave;
        private boolean isCommit;
        
        public RunnablePhase2(String key, 
                boolean isPrimarySlave, boolean isCommit) {
            super();
            this.key = key;
            this.isPrimarySlave = isPrimarySlave;
            this.isCommit = isCommit;
        }
        
        public void run() {
            KVMessage response, msg = isCommit ? new KVMessage(COMMIT) : 
                new KVMessage(ABORT, error.getMessage());
            TPCSlaveInfo slaveInfo;
            Socket sock = null;
            while (true) {
                slaveInfo = isPrimarySlave ? findFirstReplica(key) : 
                    findSuccessor(findFirstReplica(key));
                try {
                    sock = slaveInfo.connectHost(TIMEOUT);
                    msg.sendMessage(sock);
                    response = new KVMessage(sock, TIMEOUT);
                    slaveInfo.closeHost(sock);
                    if (response.getMsgType().equals(ACK)) return;
                    else if (response.getMsgType().equals(RESP)) {
                        error = new KVMessage(RESP, response.getMessage());
                        return;
                    }
                    else {
                        error = new KVMessage(RESP, ERROR_INVALID_FORMAT);
                        return;
                    }
                } catch (KVException e) {}
            }
        }
        
    }
    
}
