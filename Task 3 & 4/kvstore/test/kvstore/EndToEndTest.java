package kvstore;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EndToEndTest extends EndToEndTemplate {

//    @Test
//    public void testPutGet() throws KVException {
//        client.put("foo", "bar");
//        System.out.println("put finished.");
//        assertEquals(client.get("foo"), "bar");
//    }
    
    @Test
    public void testPutGetMultipleTimes() throws KVException {
    	for (int i = 0; i < 100; i++) {
    		client.put("foo" , Integer.toString(i));
    	}
    	assertEquals(client.get("foo"), "99");
    }

}
