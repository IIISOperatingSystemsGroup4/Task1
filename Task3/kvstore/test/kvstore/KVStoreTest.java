package kvstore;

import static kvstore.KVConstants.*;
import static org.junit.Assert.*;

import org.junit.*;

public class KVStoreTest {

    KVStore store;

    @Before
    public void setupStore() {
        store = new KVStore();
    }

    @Test
    public void putAndGetOneKey() throws KVException {
        String key = "this is the key.";
        String val = "this is the value.";
        store.put(key, val);
        assertEquals(val, store.get(key));
    }

    @Test
    public void test() throws KVException {
        String key = "a";
        String value = "b";
        for (int i = 0; i <= 10; i++) {
            store.put(key, value);
            key += "a";
            value += "b";
        }
        store.dumpToFile("out1.xml");
        store.put("aa", "b");
        store.dumpToFile("out2.xml");
        store.restoreFromFile("out1.xml");
        store.dumpToFile("out3.xml");
    }

}
