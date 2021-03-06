package kvstore;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A set-associate cache which has a fixed maximum number of sets (numSets).
 * Each set has a maximum number of elements (MAX_ELEMS_PER_SET).
 * If a set is full and another entry is added, an entry is dropped based on
 * the eviction policy.
 */
public class KVCache implements KeyValueInterface {

    /**
     * Constructs a second-chance-replacement cache.
     *
     * @param numSets the number of sets this cache will have
     * @param maxElemsPerSet the size of each set
     */
    @SuppressWarnings("unchecked")
    public KVCache(int numSets, int maxElemsPerSet) {
        // implement me
    	this.numSets = numSets;
    	this.maxElemsPerSet = maxElemsPerSet;
    	caches = new ArrayList<ArrayList<CacheEntry>>();
    	locks = new ArrayList<ReentrantLock>();
    	for(int i = 0; i < numSets; i++)
    	{
    		caches.add(new ArrayList<CacheEntry>());
    		locks.add(new ReentrantLock());
    	}
    }

    /**
     * Retrieves an entry from the cache.
     * Assumes access to the corresponding set has already been locked by the
     * caller of this method.
     *
     * @param  key the key whose associated value is to be returned.
     * @return the value associated to this key or null if no value is
     *         associated with this key in the cache
     */
    @Override
    public String get(String key) {
    	
        // implement me
    	ArrayList<CacheEntry> set = caches.get(getSetId(key));
    	for(int i = 0; i < set.size(); i++)
    	{
    		CacheEntry entry = set.get(i);
    		if(entry.key.equals(key))
    		{
    			entry.ref = true;
    			return entry.value;
    		}
    	}
    	
        return null;
    }

    /**
     * Adds an entry to this cache.
     * If an entry with the specified key already exists in the cache, it is
     * replaced by the new entry. When an entry is replaced, its reference bit
     * will be set to True. If the set is full, an entry is removed from
     * the cache based on the eviction policy. If the set is not full, the entry
     * will be inserted behind all existing entries. For this policy, we suggest
     * using a LinkedList over an array to keep track of entries in a set since
     * deleting an entry in an array will leave a gap in the array, likely not
     * at the end. More details and explanations in the spec. Assumes access to
     * the corresponding set has already been locked by the caller of this
     * method.
     *
     * @param key the key with which the specified value is to be associated
     * @param value a value to be associated with the specified key
     */
    @Override
    public void put(String key, String value) {
        // implement me
    	ArrayList<CacheEntry> set = caches.get(getSetId(key));
    	for(int i = 0; i < set.size(); i++)
    	{
    		CacheEntry entry = set.get(i);
    		if(entry.key.equals(key))
    		{
    			entry.ref = true;
    			entry.value = value;
    			//System.out.println("cachset " + getSetId(key) + " has " +  set.size() + " entries " + sameEntry(set));
    			return;
    		}
    	}
    	if(set.size() < maxElemsPerSet)
    	{
    		set.add(new CacheEntry(key, value, true));
    		//System.out.println("cachset " + getSetId(key) + " has " +  set.size() + " entries " + sameEntry(set));
    		return;
    	}
    	while(true)
    	{
    		CacheEntry entry = set.get(0);
    		if(entry.ref)
    		{
    			entry.ref = false;
    			set.remove(entry);
    			set.add(entry);
    			continue;
    		}
    		if(!entry.ref)
    		{
    			set.remove(entry);
    			set.add(new CacheEntry(key, value, true));
    			//System.out.println("cachset " + getSetId(key) + " has " +  set.size() + " entries " + sameEntry(set));
    			return;
    		}
    	}
    	
    	
    	
    	
    }

    /**
     * Removes an entry from this cache.
     * Assumes access to the corresponding set has already been locked by the
     * caller of this method. Does nothing if called on a key not in the cache.
     *
     * @param key key with which the specified value is to be associated
     */
    @Override
    public void del(String key) {
        // implement me
    	ArrayList<CacheEntry> set = caches.get(getSetId(key));
    	for(int i = 0; i < set.size(); i++)
    	{
    		CacheEntry entry = set.get(i);
    		if(entry.key.equals(key))
    		{
    			set.remove(entry);
    			return;
    		}
    	}
    }

    /**
     * Get a lock for the set corresponding to a given key.
     * The lock should be used by the caller of the get/put/del methods
     * so that different sets can be modified in parallel.
     *
     * @param  key key to determine the lock to return
     * @return lock for the set that contains the key
     */
    public Lock getLock(String key) {
        // implement me
        return locks.get(getSetId(key));
    }

    /**
     * Get the id of the set for a particular key.
     *
     * @param  key key of interest
     * @return set of the key
     */
    private int getSetId(String key) {
        return Math.abs(key.hashCode()) % numSets;
    }

    /**
     * Serialize this store to XML. See spec for details on output format.
     * This method is best effort. Any exceptions that arise can be dropped.
     */
    public String toXML() {
        // implement me
        String result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
        String space = "    ";
        result += "<KVCache>\n";
        for(int i = 0; i < numSets; i++)
        {
        	result += space + "<Set Id=\"" + i + "\">\n";
        	ArrayList<CacheEntry> set = caches.get(i);
        	for(int j = 0; j < set.size(); j++)
        	{
        		CacheEntry entry = set.get(j);
        		result += space + space + "<CacheEntry isReferenced=\"" + entry.ref + "\">\n";
        		result += space + space + space + "<Key>" + entry.key + "</Key>\n";
        		result += space + space + space + "<Value>" + entry.value + "</Value>\n";
        		result += space + space + "</CacheEntry>\n";
        	}
        	result += "</Set>\n";
        }
        result += "</KVCache>\n";
        return result;
        
    }

    @Override
    public String toString() {
        return this.toXML();
    }
    
    public boolean sameEntry(ArrayList<CacheEntry> set)
    {
    	for(int i = 0; i < set.size(); i++)
    	{
    		for(int j = i+1; j < set.size(); j++)
    		{
    			if(set.get(i).key.equals(set.get(j).key))
    				return true;
    		}
    	}
    	return false;
    }
    
    
    // Here:
    
    public int numSets, maxElemsPerSet;
    public ArrayList<ArrayList<CacheEntry>> caches;
    public ArrayList<ReentrantLock> locks;
    
    
    class CacheEntry
    {
    	public String key, value;
    	public boolean ref;
    	
    	
    	public CacheEntry(String key, String value, boolean ref)
    	{
    		this.key = key;
    		this.value = value;
    		this.ref = ref;
    	}
    }

}
