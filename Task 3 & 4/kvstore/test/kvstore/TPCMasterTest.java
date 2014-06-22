package kvstore;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

import org.junit.Test;

public class TPCMasterTest {
    
    Master master = null;
    
    /*@Test
    public void test() {
        Slave[] slaves = new Slave[10];
        try {
            master = new Master();
            for (int i = 0; i < 10; i++) {
                slaves[i] = new Slave();
            }
        } catch (IOException | InterruptedException | KVException e) {}
        String key = "a";
        String value = "b";
        for (int i = 0; i <= 100; i++) {
            put(master, key, value);
            key += "a";
            value += "b";
        }
        key = "a";
        value = "b";
        for (int i = 0; i <= 100; i++) {
            assertEquals(get(master, key), value);
            key += "a";
            value += "b";
        }
        //test for replace
        put(master, "a", "bb");
        assertEquals(get(master, "a"), "bb");
        //test for del and hasKey
        del(master, "aa");
        assertNull(get(master, "aa"));
    }*/
    
    /* test for concurrent */
    @Test
    public synchronized void testForConcurrent() throws KVException {
        Slave[] slaves = new Slave[10];
        try {
            master = new Master();
            for (int i = 0; i < 10; i++) {
                slaves[i] = new Slave();
            }
        } catch (IOException | InterruptedException | KVException e) {}
        Thread[] threads1 = new Thread[100];
        Thread[] threads2 = new Thread[100];
        Thread[] threads3 = new Thread[100];
        String temp1 = "", temp2 = "";
        for (int i = 0; i < 100; i++) {
            temp1 = temp1 + "a";
            temp2 = temp2 + "b";
        }
        final String meaningfulValue1 = temp1;
        final String meaningfulValue2 = temp2;
        put(master, "a", meaningfulValue1);
        for (int i = 0; i < 100; i++) {
            threads1[i] = new Thread() {public void run() {
                String value = get(master, "a");
                assertTrue(value == meaningfulValue1 || 
                        value == meaningfulValue2);
            }};
        }
        for (int i = 0; i < 100; i++) {
            threads2[i] = new Thread() {public void run() {
                put(master, "a", meaningfulValue1);
            }};
        }
        for (int i = 0; i < 100; i++) {
            threads3[i] = new Thread() {public void run() {
                put(master, "a", meaningfulValue2);
            }};
        }
        for (int i = 0; i < 100; i++) {
            threads1[i].start();
            threads2[i].start();
            threads3[i].start();
        }
        try {
            for (int i = 0; i < 100; i++) {
                threads1[i].join();
                threads2[i].join();
                threads3[i].join();
            }
        } catch (InterruptedException e) {}
    }
    
    public void put(Master master, String key, String value) {
        KVMessage msg = new KVMessage(KVConstants.PUT_REQ);
        msg.setKey(key);
        msg.setValue(value);
        while (true) {
            try {
                master.tpcMaster.handleTPCRequest(msg, true);
                return;
            } catch (KVException e) {
                assertNull(get(master, key));
            }
        }
    }
    
    public void del(Master master, String key) {
        KVMessage msg = new KVMessage(KVConstants.DEL_REQ);
        msg.setKey(key);
        if (get(master, key) == null) return;
        while (true) {
            try {
                master.tpcMaster.handleTPCRequest(msg, false);
                return;
            } catch (KVException e) {
                assertNotNull(get(master, key));
            }
        }
    }
    
    public String get(Master master, String key) {
        KVMessage msg = new KVMessage(KVConstants.GET_REQ);
        msg.setKey(key);
        try {
            return master.tpcMaster.handleGet(msg);
        } catch (KVException e) {
            return null;
        }
    }
    
    public class Master {

//        SocketServer clientSocketServer;
        SocketServer slaveSocketServer;
        TPCMaster tpcMaster;

        public Master() throws IOException, InterruptedException {
            final String hostname = InetAddress.getLocalHost().getHostAddress();
            tpcMaster = new TPCMaster(2, new KVCache(1, 4));
            
            slaveSocketServer = new SocketServer(hostname, 9090);
            NetworkHandler slaveHandler = new TPCRegistrationHandler(tpcMaster);
            slaveSocketServer.addHandler(slaveHandler);
            slaveSocketServer.connect();
            System.out.println("Starting registration server in background...");

            new Thread() {
                @Override
                public void run() {
                    try {
                        slaveSocketServer.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.start();

            Thread.sleep(100);
/*
            clientSocketServer = new SocketServer(hostname, 8080);
            NetworkHandler clientHandler = new TPCClientHandler(tpcMaster);
            clientSocketServer.addHandler(clientHandler);
            clientSocketServer.connect();
               
            System.out.println("fuck0");
            System.out.println("Master listening for clients and slaves at " +
                clientSocketServer.getHostname());
            System.out.println("fuck1");
            clientSocketServer.start();
*/
        }

    }
    
    public class Slave {

        String logPath;
        TPCLog log;

        KVServer keyServer;
        SocketServer server;

        long slaveID;
        String masterHostname;

        int masterPort = 8080;
        int registrationPort = 9090;

        public Slave() throws IOException, KVException, InterruptedException {
            Random rand = new Random();
            slaveID = rand.nextLong();

            masterHostname = InetAddress.getLocalHost().getHostAddress();
            System.out.println("Looking for master at " + masterHostname);

            keyServer = new KVServer(100, 10);
            server = new SocketServer(InetAddress.getLocalHost().getHostAddress());
            logPath = "bin/log." + slaveID + "@" + server.getHostname();
            log = new TPCLog(logPath, keyServer);
            TPCMasterHandler handler = new TPCMasterHandler(slaveID, keyServer, log);
            server.addHandler(handler);
            server.connect();
            handler.registerWithMaster(masterHostname, server);
            System.out.println("Starting SlaveServer " + slaveID + " at " +
                    server.getHostname() + ":" + server.getPort());
            
            new Thread() {
                @Override
                public void run() {
                    try {
                        server.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
            
            Thread.sleep(100);
        }

    }
}
