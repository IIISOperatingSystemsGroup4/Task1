package kvstore;


import static kvstore.KVConstants.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.net.*;
import java.util.*;

import org.junit.*;

public class TPCLogTest {
	KVServer kvServer;
	@Test
	public void testTPCLog() throws KVException {
		setupServer();
		TPCLog log=new TPCLog("result.txt",kvServer);
		KVMessage m1=new KVMessage("putreq");
		m1.setKey("Key1");
		m1.setValue("v1");
		log.appendAndFlush(m1);
		KVMessage m2=new KVMessage("getreq");
		m2.setKey("Key2");
		log.appendAndFlush(m2);
		KVMessage m3=new KVMessage("commit");
		log.appendAndFlush(m3);
		KVMessage m4=new KVMessage("delreq");
		m4.setKey("Key3");
		log.appendAndFlush(m4);
		KVMessage m5=new KVMessage("abort");
		log.appendAndFlush(m5);
		log.rebuildServer();
		
		
		
		
		
	}
    /* Begin helper methods */

    private void setupServer() {
        kvServer = mock(KVServer.class);
        try {
        	doNothing().when(kvServer).del(anyString());
            doNothing().when(kvServer).put(anyString(),anyString());
        } catch (KVException e) {
            throw new RuntimeException(e);
        }
    }
    

}
