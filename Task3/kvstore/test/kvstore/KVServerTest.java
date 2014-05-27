package kvstore;

import static kvstore.KVConstants.ERROR_NO_SUCH_KEY;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.*;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(KVServer.class)
public class KVServerTest {

    KVServer server;
    static KVCache realCache;
    KVCache mockCache;
    static KVStore realStore;
    KVStore mockStore;

    /**
     * Nick: This is necessary because once I start mocking constructors, I
     * haven't figured out a way to use the real ones again.  Also, this sucks
     * because the cache doesn't get reset between tests -_- there is no method
     * for that, though and I can't construct a new cache with the actual
     * constructor.  Only fix I thought of was creating an array of new caches
     * and using a different one for each test, but that's even worse imo :(
     */
    @BeforeClass
    public static void setupRealDependencies() {
        realCache = new KVCache(10, 10);
        realStore = new KVStore();
    }

    public void setupRealServer() {
        try {
            whenNew(KVCache.class).withArguments(anyInt(), anyInt()).thenReturn(realCache);
            whenNew(KVStore.class).withNoArguments().thenReturn(realStore);
            server = new KVServer(10, 10);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setupMockServer() {
        try {
            mockCache = mock(KVCache.class);
            mockStore = mock(KVStore.class);
            whenNew(KVCache.class).withAnyArguments().thenReturn(mockCache);
            whenNew(KVStore.class).withAnyArguments().thenReturn(mockStore);
            server = new KVServer(10, 10);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void fuzzTest() throws KVException {
        setupRealServer();
        Random rand = new Random(8); // no reason for 8
        Map<String, String> map = new HashMap<String, String>(10000);
        String key, val;
        for (int i = 0; i < 10000; i++) {
            key = Integer.toString(rand.nextInt());
            val = Integer.toString(rand.nextInt());
            server.put(key, val);
            map.put(key, val);
        }
        Iterator<Map.Entry<String, String>> mapIter = map.entrySet().iterator();
        Map.Entry<String, String> pair;
        while(mapIter.hasNext()) {
            pair = mapIter.next();
            assertTrue(server.hasKey(pair.getKey()));
            assertEquals(pair.getValue(), server.get(pair.getKey()));
            mapIter.remove();
        }
        assertTrue(map.size() == 0);
    }

    @Test
    public void testNonexistentGetFails() {
        setupRealServer();
        try {
            server.get("this key shouldn't be here");
            fail("get with nonexistent key should error");
        } catch (KVException e) {
            assertEquals(KVConstants.RESP, e.getKVMessage().getMsgType());
            assertEquals(KVConstants.ERROR_NO_SUCH_KEY, e.getKVMessage().getMessage());
        }
    }

    @Test
    public void test() throws KVException {
        setupRealServer();
        try {
            server.get("a");
            fail();
        } catch (KVException e) {}
        try {
            server.del("a");
            fail();
        } catch (KVException e) {}
        try {
            server.put("", "b");
            fail();
        } catch (KVException e) {}
        String key = "a";
        String value = "b";
        //test for put and get
        for (int i = 0; i <= 100; i++) {
            server.put(key, value);
            key += "a";
            value += "b";
        }
        key = "a";
        value = "b";
        for (int i = 0; i <= 100; i++) {
            assertEquals(server.get(key), value);
            key += "a";
            value += "b";
        }
        //test for replace
        server.put("a", "bb");
        assertEquals(server.get("a"), "bb");
        //test for del and hasKey
        server.del("aa");
        assertFalse(server.hasKey("aa"));
    }

    @Test
    public synchronized void testForConcurrent() throws KVException {
        //test for concurrent
        setupRealServer();
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
        server.put("a", meaningfulValue1);
        for (int i = 0; i < 100; i++) {
            threads1[i] = new Thread() {public void run() {
                try {
                    String value = server.get("a");
                    assertTrue(value == meaningfulValue1 ||
                               value == meaningfulValue2);
                } catch (KVException e) {
                    fail();
                }
            }};
        }
        for (int i = 0; i < 100; i++) {
            threads2[i] = new Thread() {public void run() {
                try {
                    server.put("a", meaningfulValue1);
                } catch (KVException e) {
                    fail();
                }
            }};
        }
        for (int i = 0; i < 100; i++) {
            threads3[i] = new Thread() {public void run() {
                try {
                    server.put("a", meaningfulValue2);
                } catch (KVException e) {
                    fail();
                }
            }};
        }
        for (int i = 0; i < 100; i++) {
            threads1[i].start();
            threads2[i].start();
            threads3[i].start();
        }
        try {
            wait(5000);
        } catch (InterruptedException e) {}
    }
}
