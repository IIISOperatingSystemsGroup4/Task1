package kvstore;

import static org.junit.Assert.*;

import org.junit.*;

public class KVCacheTest {

    /**
     * Verify the cache can put and get a KV pair successfully.
     */
    @Test
    public void singlePutAndGet() {
        KVCache cache = new KVCache(1, 4);
        cache.put("hello", "world");
        assertEquals("world", cache.get("hello"));
    }
     
    
    @Test
    public void singlePutAndDelAndGet()
    {
    	KVCache cache = new KVCache(1, 4);
        cache.put("hello", "world");
        assertEquals("world", cache.get("hello"));
        
        cache.del("hello");
        assertNull(cache.get("hello"));
    }
    
    @Test
    public void doublePutAndGet()
    {
    	KVCache cache = new KVCache(1, 4);
        cache.put("hello", "world");
        assertEquals("world", cache.get("hello"));
        
        cache.put("hello", "m");
        assertEquals("m",cache.get("hello"));
    }
    
    @Test
    public void noMoreElementsThanMax()
    {
    	KVCache cache = new KVCache(1, 4);
    	cache.put("k1", "v1");
    	cache.put("k2", "v2");
    	cache.put("k3", "v3");
    	cache.put("k4", "v4");
    	cache.put("k5", "v5");
    	cache.put("k6", "v6");
    	
    	int t = 0;
    	t += (cache.get("k1") == null) ? 0 : 1;
    	t += (cache.get("k2") == null) ? 0 : 1;
    	t += (cache.get("k3") == null) ? 0 : 1;
    	t += (cache.get("k4") == null) ? 0 : 1;
    	t += (cache.get("k5") == null) ? 0 : 1;
    	t += (cache.get("k6") == null) ? 0 : 1;
    	
    	assertEquals(4,t);
    }
    
    @Test
    public void secondChance()
    {
    	KVCache cache = new KVCache(1, 4);
    	cache.put("k1", "v1");
    	cache.put("k2", "v2");
    	cache.put("k3", "v3");
    	cache.put("k4", "v4");
    	cache.put("k5", "v5");
    	
    	assertNull(cache.get("k1"));
    	
    	cache.put("k2", "v6");
    	cache.put("k6", "v7");
    	
    	assertNull(cache.get("k3"));
    	
    	cache.put("k7", "v8");
    	
    	assertNull(cache.get("k4"));
    	
    	cache.put("k8", "v9");
    	
    	assertNull(cache.get("k2"));
    	
    }
    

}
