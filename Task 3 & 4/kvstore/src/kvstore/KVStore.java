package kvstore;

import static kvstore.KVConstants.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This is a basic key-value store. Ideally this would go to disk, or some other
 * backing store.
 */
public class KVStore implements KeyValueInterface {

    private ConcurrentHashMap<String, String> store;
    
    private static final int MAX_KEY_SIZE = 256;
    private static final int MAX_VAL_SIZE = 256 * 1024;

    /**
     * Construct a new KVStore.
     */
    public KVStore() {
        resetStore();
    }

    private void resetStore() {
        this.store = new ConcurrentHashMap<String, String>();
    }

    /**
     * Insert key, value pair into the store.
     *
     * @param  key String key
     * @param  value String value
     */
    @Override
    public void put(String key, String value) {
        store.put(key, value);
    }

    /**
     * Retrieve the value corresponding to the provided key
     * @param  key String key
     * @throws KVException with ERROR_NO_SUCH_KEY if key does not exist in store
     */
    @Override
    public String get(String key) throws KVException {
        String retVal = this.store.get(key);
        if (retVal == null) {
            KVMessage msg = new KVMessage(KVConstants.RESP, ERROR_NO_SUCH_KEY);
            throw new KVException(msg);
        }
        return retVal;
    }

    /**
     * Delete the value corresponding to the provided key.
     *
     * @param  key String key
     * @throws KVException with ERROR_NO_SUCH_KEY if key does not exist in store
     */
    @Override
    public void del(String key) throws KVException {
        if(key != null) {
            if (!this.store.containsKey(key)) {
                KVMessage msg = new KVMessage(KVConstants.RESP, ERROR_NO_SUCH_KEY);
                throw new KVException(msg);
            }
            this.store.remove(key);
        }
    }

    /**
     * Serialize the store to XML. See the spec for specific output format.
     * This method is best effort. Any exceptions that arise can be dropped.
     */
    public String toXML() {
        // implement me
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            return null;
        }
        Document doc = builder.newDocument();
        
        //start to edit doc
        Element root = doc.createElement("KVStore");
        doc.appendChild(root);
        for (Entry<String, String> entry: store.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            Element pairEle = doc.createElement("KVPair");
            root.appendChild(pairEle);
            Element keyEle = doc.createElement("Key");
            keyEle.appendChild(doc.createTextNode(key));
            pairEle.appendChild(keyEle);
            Element valueEle = doc.createElement("Value");
            valueEle.appendChild(doc.createTextNode(value));
            pairEle.appendChild(valueEle);
        }
        
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = null;
        try {
          transformer = transformerFactory.newTransformer();
        } catch (TransformerConfigurationException e) {
          return null; 
        }
        StringWriter writer = new StringWriter();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(writer);
        try {
            transformer.transform(source, result);
        } catch (TransformerException e) {
            return null;
        }
        return writer.toString();
    }

    @Override
    public String toString() {
        return this.toXML();
    }

    /**
     * Serialize to XML and write the output to a file.
     * This method is best effort. Any exceptions that arise can be dropped.
     *
     * @param fileName the file to write the serialized store
     */
    public void dumpToFile(String fileName) {
        // implement me
        try {
            PrintWriter writer = new PrintWriter(fileName);
            writer.print(toXML());
            writer.close();
        } catch (FileNotFoundException e) {}
    }

    /**
     * Replaces the contents of the store with the contents of a file
     * written by dumpToFile; the previous contents of the store are lost.
     * The store is cleared even if the file does not exist.
     * This method is best effort. Any exceptions that arise can be dropped.
     *
     * @param fileName the file containing the serialized store data
     */
    public void restoreFromFile(String fileName) {
        resetStore();

        // implement me
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        Document doc = null;
        try {
            builder = factory.newDocumentBuilder();
            doc = builder.parse(fileName);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            return;
        }
        Node root = doc.getFirstChild();
        if (root == null || root.getNodeName() != "KVStore") {
            return;
        }
        NodeList pairs = root.getChildNodes();
        for (int i = 0; i < pairs.getLength(); i++) {
            Node pairNode = pairs.item(i);
            if (pairNode.getNodeName() != "KVPair") return;
            Node keyNode = pairNode.getFirstChild();
            Node valueNode = pairNode.getLastChild();
            if (keyNode == null || keyNode.getNodeName() != "Key" || 
                valueNode == null || valueNode.getNodeName() != "Value") {
                return;
            }
            Node keyTextNode = keyNode.getFirstChild();
            Node valueTextNode = valueNode.getFirstChild();
            if (keyTextNode == null || valueTextNode == null) {
                return;
            }
            String key = keyTextNode.getTextContent();
            String value = valueTextNode.getTextContent();
            try {
                check(key, value);
            } catch (KVException e) {
                return;
            }
            put(key, value);
        }
    }
    
    public void check(String key, String value) throws KVException {
        if (key == null || key.length() <= 0) {
            KVMessage msg = new KVMessage(RESP, ERROR_INVALID_KEY);
            throw new KVException(msg);
        }
        if (value == null || value.length() <= 0) {
            KVMessage msg = new KVMessage(RESP, ERROR_INVALID_VALUE);
            throw new KVException(msg);
        }
        if (key.length() > MAX_KEY_SIZE) {
            KVMessage msg = new KVMessage(RESP, ERROR_OVERSIZED_KEY);
            throw new KVException(msg);
        }
        if (value.length() > MAX_VAL_SIZE) {
            KVMessage msg = new KVMessage(RESP, ERROR_OVERSIZED_VALUE);
            throw new KVException(msg);
        } 
    }
}
