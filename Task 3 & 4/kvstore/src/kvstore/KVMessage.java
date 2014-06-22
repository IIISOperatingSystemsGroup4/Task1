package kvstore;

import static kvstore.KVConstants.*;

import java.io.*;
import java.net.*;
import java.util.Scanner;

import javax.xml.parsers.*;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 * This is the object that is used to generate the XML based messages
 * for communication between clients and servers.
 */
public class KVMessage implements Serializable {

    private String msgType;
    private String key;
    private String value;
    private String message;
//    private Lock lock=new Lock
    public static final long serialVersionUID = 6473128480951955693L;

    /**
     * Construct KVMessage with only a type.
     *
     * @param msgType the type of this KVMessage
     */
    public KVMessage(String msgType) {
    	this(msgType, null);
    }

    /**
     * Construct KVMessage with type and message.
     *
     * @param msgType the type of this KVMessage
     * @param message the content of this KVMessage
     */
    public KVMessage(String msgType, String message) {
    	this.msgType = msgType;
        this.message = message;
    }

    /**
     * Construct KVMessage from the InputStream of a socket.
     * Parse XML from the InputStream with unlimited timeout.
     *
     * @param  sock Socket to receive serialized KVMessage through
     * @throws KVException if we fail to create a valid KVMessage. Please see
     *         KVConstants.java for possible KVException messages.
     */
    public KVMessage(Socket sock) throws KVException {
        this(sock, 0);
    }

    /**
     * Construct KVMessage from the InputStream of a socket.
     * This constructor parses XML from the InputStream within a certain timeout
     * or with an unlimited timeout if the provided argument is 0.
     *
     * @param  sock Socket to receive serialized KVMessage through
     * @param  timeout total allowable receipt time, in milliseconds
     * @throws KVException if we fail to create a valid KVMessage. Please see
     *         KVConstants.java for possible KVException messages.
     */
    private boolean verify(String type)
    {
    	return (type.equals("commit")||type.equals("abort")||
    			type.equals("ready")||type.equals("putreq")||
    			type.equals("getreq")||type.equals("delreq")||
    			type.equals("resp")||type.equals("ack")||type.equals("register"));
    }
    public KVMessage(Socket sock, int timeout) throws KVException {
        // implement me
    	try {
    		sock.setSoTimeout(timeout);
    		NoCloseInputStream input=new NoCloseInputStream(sock.getInputStream());
    		DocumentBuilderFactory fac=DocumentBuilderFactory.newInstance();
    		DocumentBuilder db=fac.newDocumentBuilder();
    		while (input.available()==0);
    		Document doc=db.parse(input);
    		NodeList list=doc.getElementsByTagName("KVMessage");
    		if (list.getLength()!=1)
    			throw new KVException(new KVMessage("resp","XML Error: Message format incorrect"));
    		Node root=list.item(0);
    		NamedNodeMap attributes=root.getAttributes();
    		if (attributes.getLength()!=1)
    			throw new KVException(new KVMessage("resp","XML Error: Message format incorrect"));
    		if (!attributes.item(0).getNodeName().equals("type"))
    			throw new KVException(new KVMessage("resp","XML Error: Message format incorrect"));
    		String type=attributes.item(0).getNodeValue();
    		msgType=type;
    		if (!verify(type))
    			throw new KVException(new KVMessage("resp","XML Error: Message format incorrect"));
    		NodeList keyNodeList=doc.getElementsByTagName("Key");
			NodeList valNodeList=doc.getElementsByTagName("Value");
			NodeList msgNodeList=doc.getElementsByTagName("Message");
			if (type.equals("getreq")||type.equals("delreq")||type.equals("putreq")) {
    			if (keyNodeList.getLength()!=1)
        			throw new KVException(new KVMessage("resp","XML Error: Message format incorrect"));
    			key=keyNodeList.item(0).getTextContent();
    			if ((key==null)||(key.equals("")))
    				throw new KVException(new KVMessage("resp","Data Error: Null or empty key"));
    		}
    		if (type.equals("putreq")) {
    			if (valNodeList.getLength()!=1)
        			throw new KVException(new KVMessage("resp","XML Error: Message format incorrect"));
    			value=valNodeList.item(0).getTextContent();
    			if ((value==null)||(value.equals("")))
    				throw new KVException(new KVMessage("resp","Data Error: Null or empty value"));
    		}
    		if (type.equals("resp")) {
    			if ((valNodeList.getLength()==1)&&(keyNodeList.getLength()==1)&&(msgNodeList.getLength()==0)) {//successful getreq
    				key=keyNodeList.item(0).getTextContent();
    				value=valNodeList.item(0).getTextContent();
        			if ((key==null)||(key.equals("")))
        				throw new KVException(new KVMessage("resp","Data Error: Null or empty key"));
        			if ((value==null)||(value.equals("")))
        				throw new KVException(new KVMessage("resp","Data Error: Null or empty value"));
    			}
    			else if ((valNodeList.getLength()==0)&&(keyNodeList.getLength()==0)&&(msgNodeList.getLength()==1)) {//other resp
    				message=msgNodeList.item(0).getTextContent();
    			}
    			else
    				throw new KVException(new KVMessage("resp","XML Error: Message format incorrect"));
    		}
    		if (type.equals("abort")) {
    			if ((valNodeList.getLength()>0)||(keyNodeList.getLength()>0)) 
    				throw new KVException(new KVMessage("resp","XML Error: Message format incorrect"));
    			if (msgNodeList.getLength()>1)
    				throw new KVException(new KVMessage("resp","XML Error: Message format incorrect"));
    			message=msgNodeList.item(0).getTextContent();
    		}
    		if (type.equals("register")) {
    			if ((valNodeList.getLength()>0)||(keyNodeList.getLength()>0)) 
    				throw new KVException(new KVMessage("resp","XML Error: Message format incorrect"));
    			if (msgNodeList.getLength()!=1)
    				throw new KVException(new KVMessage("resp","XML Error: Message format incorrect"));
    			message=msgNodeList.item(0).getTextContent();    			
    		}
    		if (type.equals("ack")||type.equals("commit")||type.equals("ready")) {
    			if ((valNodeList.getLength()>0)||(keyNodeList.getLength()>0)||(msgNodeList.getLength()>0)) 
    				throw new KVException(new KVMessage("resp","XML Error: Message format incorrect"));
    		}
        		
    	}
    	catch(SocketTimeoutException e) {
    		System.out.println("SocketException");
    		throw new KVException(new KVMessage("resp","Network Error: Socket timeout"));
    	}
    	catch (IOException e) {
    		System.out.println("IOException");
    		throw new KVException(new KVMessage("resp","Network Error: Could not connect"));
    	}
    	catch (ParserConfigurationException e) {
    		System.out.println("ParserException");
    		throw new KVException(new KVMessage("resp","XML Error: Parser Error"));
    	}
    	catch (SAXException e) {
    		System.out.println("SAXException");
    		throw new KVException(new KVMessage("resp","XML Error: Parser Error"));
    	}
    	
    }

    /**
     * Constructs a KVMessage by copying another KVMessage.
     *
     * @param kvm KVMessage with fields to copy
     */
    public KVMessage(KVMessage kvm) {
        // implement me
    	if (kvm.msgType!=null) msgType=new String(kvm.msgType);
    	else msgType=null;
    	if (kvm.key!=null) key=new String(kvm.key);
    	else key=null;
    	if (kvm.value!=null) value=new String(kvm.value);
    	else value=null;
    	if (kvm.message!=null) message=new String(kvm.message);
    	else message=null;
    	
    }

    /**
     * Generate the serialized XML representation for this message. See
     * the spec for details on the expected output format.
     *
     * @return the XML string representation of this KVMessage
     * @throws KVException with ERROR_INVALID_FORMAT or ERROR_PARSER
     */
    public String toXML() throws KVException {
        // implement me
    	try {
    		DocumentBuilderFactory fac=DocumentBuilderFactory.newInstance();
    		DocumentBuilder db=fac.newDocumentBuilder();
    		Document doc=db.newDocument();
    		doc.setXmlStandalone(true);
    		Element root=doc.createElement("KVMessage");
    		root.setAttribute("type",msgType);
    		doc.appendChild(root);
    		if (!verify(msgType)) 
    			throw new KVException(new KVMessage("resp","XML Error: Message format incorrect"));
    		if (msgType.equals("getreq")||msgType.equals("delreq")||msgType.equals("putreq")) {
    			if ((key==null)||(key.equals("")))
        			throw new KVException(new KVMessage("resp","XML Error: Message format incorrect"));
    			Element keyElement=doc.createElement("Key");
    			keyElement.appendChild(doc.createTextNode(key));
    			root.appendChild(keyElement);
    		}
    		if (msgType.equals("putreq")) {
    			if ((value==null)||(value.equals("")))
        			throw new KVException(new KVMessage("resp","XML Error: Message format incorrect"));
    			Element valElement=doc.createElement("Value");
    			valElement.appendChild(doc.createTextNode(value));
    			root.appendChild(valElement);
    		}
    		if (msgType.equals("resp")) {
    			if ((key!=null)&&(value!=null)&&(message==null)) {
        			if (key.equals(""))
            			throw new KVException(new KVMessage("resp","XML Error: Message format incorrect"));
        			Element keyElement=doc.createElement("Key");
        			keyElement.appendChild(doc.createTextNode(key));
        			root.appendChild(keyElement);
        			if (value.equals(""))
            			throw new KVException(new KVMessage("resp","XML Error: Message format incorrect"));
        			Element valElement=doc.createElement("Value");
        			valElement.appendChild(doc.createTextNode(value));
        			root.appendChild(valElement);
    			}
    			else if ((key==null)&&(value==null)&&(message!=null)) {
        			Element msgElement=doc.createElement("Message");
        			msgElement.appendChild(doc.createTextNode(message));
        			root.appendChild(msgElement);
    			}
    			else
    				throw new KVException(new KVMessage("resp","XML Error: Message format incorrect"));	
    		}
    		if (msgType.equals("abort")) {
    			if ((key!=null)||(value!=null))
        			throw new KVException(new KVMessage("resp","XML Error: Message format incorrect"));
    			else if (message!=null) {
        			Element msgElement=doc.createElement("Message");
        			msgElement.appendChild(doc.createTextNode(message));
        			root.appendChild(msgElement);    				
    			}
    		}
    		if (msgType.equals("ack")||msgType.equals("commit")||msgType.equals("ready")) {
    			if ((key!=null)||(value!=null)||(message!=null))
        			throw new KVException(new KVMessage("resp","XML Error: Message format incorrect"));
    		}
    		if (msgType.equals("register")) {
    			if ((key==null)&&(value==null)&&(message!=null)) {
        			Element msgElement=doc.createElement("Message");
        			msgElement.appendChild(doc.createTextNode(message));
        			root.appendChild(msgElement);
    			}
    			else
    				throw new KVException(new KVMessage("resp","XML Error: Message format incorrect"));	
    		}
    		StringWriter stringWriter=new StringWriter();
    		Transformer transformer=TransformerFactory.newInstance().newTransformer();
    		transformer.transform(new DOMSource(doc), new StreamResult(stringWriter));
    		String ans=stringWriter.toString();
            return ans;
    	}
    	catch (ParserConfigurationException e) {
    		throw new KVException(new KVMessage("resp","XML Error: Parser Error"));
    	}
    	catch (TransformerException e) {
    		throw new KVException(new KVMessage("resp","XML Error: Parser Error"));
    	}
    }


    /**
     * Send serialized version of this KVMessage over the network.
     * You must call sock.shutdownOutput() in order to flush the OutputStream
     * and send an EOF (so that the receiving end knows you are done sending).
     * Do not call close on the socket. Closing a socket closes the InputStream
     * as well as the OutputStream, preventing the receipt of a response.
     *
     * @param  sock Socket to send XML through
     * @throws KVException with ERROR_INVALID_FORMAT, ERROR_PARSER, or
     *         ERROR_COULD_NOT_SEND_DATA
     */
    public void sendMessage(Socket sock) throws KVException {
        // implement me
    	try {

    		String toWrite=toXML();
    		OutputStream output=sock.getOutputStream();
        	output.write(toWrite.getBytes("UTF-8"));
        	output.flush();
        	sock.shutdownOutput();
       	}
    	catch(IOException e) {
    		throw new KVException(new KVMessage("resp","Network Error: Could not send data"));
    	}
    	
    	
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMsgType() {
        return msgType;
    }


    @Override
    public String toString() {
        try {
            return this.toXML();
        } catch (KVException e) {
            // swallow KVException
            return e.toString();
        }
    }

    /*
     * InputStream wrapper that allows us to reuse the corresponding
     * OutputStream of the socket to send a response.
     * Please read about the problem and solution here:
     * http://weblogs.java.net/blog/kohsuke/archive/2005/07/socket_xml_pitf.html
     */
    private class NoCloseInputStream extends FilterInputStream {
        public NoCloseInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() {} // ignore close
    }

    /* http://stackoverflow.com/questions/2567416/document-to-string/2567428#2567428 */
    public static String printDoc(Document doc) {
        try {
            StringWriter sw = new StringWriter();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            transformer.transform(new DOMSource(doc), new StreamResult(sw));
            return sw.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Error converting to String", ex);
        }
    }


}
