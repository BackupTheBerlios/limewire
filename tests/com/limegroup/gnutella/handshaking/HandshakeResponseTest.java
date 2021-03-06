package com.limegroup.gnutella.handshaking;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import junit.framework.Test;

import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.HostCatcher;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.MessageTestUtils;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.util.TestConnectionManager;


/**
 * Tests the functionality of the <tt>HandshakeResponse</tt> class.
 */
public final class HandshakeResponseTest extends BaseTestCase {

    private static HandshakeResponse ULTRAPEER_HEADERS;

    private static HandshakeResponse LEAF_HEADERS;

	public HandshakeResponseTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(HandshakeResponseTest.class);
	}

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    /**
     * Individual tests will change these as needed.
     */    
    public void setUp() {
        ConnectionSettings.ACCEPT_DEFLATE.setValue(true);
        ConnectionSettings.ENCODE_DEFLATE.setValue(true);
    }

    public static void globalSetUp(Class name) throws Exception {
        ULTRAPEER_HEADERS = 
            HandshakeResponse.createResponse(
                new UltrapeerHeaders("45.67.89.54"));
        LEAF_HEADERS =
            HandshakeResponse.createResponse(new LeafHeaders("45.67.89.54"));
        RouterService.getHostCatcher().clear();
    }

    public void testLeafRejectIncoming() throws Exception {
        Properties props = new Properties();
        props.put(HeaderNames.X_ULTRAPEER, "false");
        HostCatcher hc = RouterService.getHostCatcher();
        hc.add(MessageTestUtils.createPongWithFreeLeafSlots());
        HandshakeResponse headers = HandshakeResponse.createResponse(props);
        HandshakeResponse hr = 
            HandshakeResponse.createLeafRejectIncomingResponse(headers);
        
        assertTrue(hr.hasXTryUltrapeers());
    }
    
    /**
     * Tests the method that adds Ultrapeer hosts to the X-Try-Ultrapeer
     * header.
     */
    public void testAddXTryHeader() throws Exception {
        Method m = 
            PrivilegedAccessor.getMethod(HandshakeResponse.class, 
                "addXTryHeader",
                new Class[]{HandshakeResponse.class, 
                Properties.class});
        // First, check to make sure that we add connected hosts when we don't
        // have any hosts in the host catcher. 
        RouterService.getHostCatcher().clear();
        assertEquals(0, RouterService.getHostCatcher().getNumHosts());
        List ipPorts = new LinkedList();
        ipPorts.add(new Connection("24.67.85.4", 6346,
                                   new Properties(),
                                   new UltrapeerHandshakeResponder("24.67.85.4")));
        PrivilegedAccessor.setValue(RouterService.getConnectionManager(),
            "_initializedConnections", ipPorts);
        
        Object[] params = new Object[2];
        params[0] = ULTRAPEER_HEADERS;
        params[1] = new Properties();
        Properties props = 
            (Properties)m.invoke(HandshakeResponse.class, params);
        HandshakeResponse hr = HandshakeResponse.createResponse(props);
        String xTry = hr.getXTryUltrapeers().trim();
        assertEquals("unexpected header", "24.67.85.4:6346", xTry);
        
        // Add a bunch of hosts to the host catcher to look for later.
        HostCatcher hc = RouterService.getHostCatcher();
        int limit = 30;
        Set ultrapeersWithLeafSlots = new HashSet();
        Set ultrapeersWithUltrapeerSlots = new HashSet();
        for(int i=0; i<limit; i++) {
            PingReply pr = MessageTestUtils.createPongWithFreeLeafSlots();
            hc.add(pr);
            ultrapeersWithLeafSlots.add(pr.getAddress()+":"+pr.getPort());
            
            pr = MessageTestUtils.createPongWithUltrapeerSlots();
            hc.add(pr);
            ultrapeersWithUltrapeerSlots.add(pr.getAddress()+":"+pr.getPort());
        }
        
        
        
        Properties headers = new Properties();
        headers = 
            (Properties)m.invoke(null, 
                new Object[] {ULTRAPEER_HEADERS, headers});

        // Check that the appropriate hosts are returned to a request from
        // an ultrapeer.
        hr = HandshakeResponse.createResponse(headers);
        
        // Get the ultrapeers
        String hostAddresses = hr.getXTryUltrapeers();
    
         
        // Tokenize to retrieve individual addresses
        StringTokenizer st = new StringTokenizer(hostAddresses,
            Constants.ENTRY_SEPARATOR);
        while(st.hasMoreTokens()) {
            assertTrue(ultrapeersWithUltrapeerSlots.contains(st.nextToken()));
        }
        // Double-check that the hosts are what we expect them to be.
        String[] hostArray = 
            (String[])ultrapeersWithUltrapeerSlots.toArray(new String[0]);
        makeSureHeadersMatch(hostArray, headers);
        
        
        // Now check that the appropriate hosts are returned to a request from
        // a leaf.
        headers = new Properties();
        m.invoke(null, new Object[] {LEAF_HEADERS, headers});
        
        // Make sure the returned list of hosts is what we expect.
        hr = HandshakeResponse.createResponse(headers);
        
        // Get the ultrapeers
        hostAddresses = hr.getXTryUltrapeers();
    
         
        // Tokenize to retrieve individual addresses
        st = new StringTokenizer(hostAddresses, Constants.ENTRY_SEPARATOR);
        while(st.hasMoreTokens()) {
            assertTrue(ultrapeersWithLeafSlots.contains(st.nextToken()));
        }
        
        // Double-check that the hosts are what we expect them to be.
        hostArray = 
            (String[])ultrapeersWithLeafSlots.toArray(new String[0]);
        makeSureHeadersMatch(hostArray, headers);
    }
    
    public void testDefaultCreateEndpointString() throws Exception {
        List leaves = new LinkedList();
        List ultrapeers = new LinkedList();
        String leafAddress = "10.254.0.";
        String ultrapeerAddress = "20.23.0.";
        Properties props = new Properties();
        UltrapeerHandshakeResponder UHR = 
            new UltrapeerHandshakeResponder("20.34.90.1");
        for(int i=0; i<10; i++) {
            Connection conn = 
                new Connection(leafAddress+i, 6346, props, UHR);
            leaves.add(conn);
            conn = 
                new Connection(ultrapeerAddress+i, 6346, props, UHR);
            ultrapeers.add(conn);
        }
        Method m = 
            PrivilegedAccessor.getMethod(HandshakeResponse.class, 
                "createEndpointString", 
                new Class[] {Collection.class});
        
        String leafStr =
            (String)m.invoke(HandshakeResponse.class, 
                new Object[] {leaves});  
        
        List leavesFromString = createListFromIPPortString(leafStr);
        assertAllConnectionsAreEqual(leaves, leavesFromString);
        
        String ultrapeerStr =
            (String)m.invoke(HandshakeResponse.class, 
                new Object[] {ultrapeers});  
        List ultrapeersFromString = createListFromIPPortString(ultrapeerStr);
        assertAllConnectionsAreEqual(ultrapeers, ultrapeersFromString);        
    }
    
	/**
	 * Tests the utility method for creating a string of IP/port pairs from a 
	 * list of connections
	 * 
	 * @throws Exception if anything unexpected occurs
	 */
	public void testCreateEndpointString() throws Exception {
		List leaves = new LinkedList();
		List ultrapeers = new LinkedList();
		String leafAddress = "10.254.0.";
		String ultrapeerAddress = "20.23.0.";
		Properties props = new Properties();
		UltrapeerHandshakeResponder UHR = 
			new UltrapeerHandshakeResponder("20.34.90.1");
		for(int i=0; i<30; i++) {
			Connection conn = 
				new Connection(leafAddress+i, 6346, props, UHR);
			leaves.add(conn);
			conn = 
				new Connection(ultrapeerAddress+i, 6346, props, UHR);
			ultrapeers.add(conn);
		}
		Method m = 
			PrivilegedAccessor.getMethod(HandshakeResponse.class, 
				"createEndpointString", 
                new Class[] {Collection.class, Integer.TYPE});
        
        int limit = leaves.size();
		String leafStr =
			(String)m.invoke(HandshakeResponse.class, 
                new Object[] {leaves, new Integer(limit)});	
		
		List leavesFromString = createListFromIPPortString(leafStr);
		assertAllConnectionsAreEqual(leaves, leavesFromString);
		
        limit = ultrapeers.size();
		String ultrapeerStr =
		    (String)m.invoke(HandshakeResponse.class, 
                new Object[] {ultrapeers, new Integer(limit)});	
		List ultrapeersFromString = createListFromIPPortString(ultrapeerStr);
		assertAllConnectionsAreEqual(ultrapeers, ultrapeersFromString);
		
	}
	
	/**
	 * Asserts that two lists of connections are equal as per having
	 * the same host/ip.
	 */
	private void assertAllConnectionsAreEqual(List one, List two) {
	    Connection c1, c2;
	    boolean found;
	    
	    assertEquals("wrong sizes", one.size(), two.size());
	    
	    for(Iterator a = one.iterator(); a.hasNext(); ) {
            found = false;
            c1 = (Connection)a.next();
            for(Iterator b = two.iterator(); b.hasNext();) {
                c2 = (Connection)b.next();
                if( c1.getAddress().equals(c2.getAddress()) &&
                  c1.getPort() == c2.getPort() )
                    found = true;
            }
            assertTrue("missing " + c1 + " from list two", found);
        }
        
	    for(Iterator a = two.iterator(); a.hasNext(); ) {
            found = false;
            c1 = (Connection)a.next();
            for(Iterator b = one.iterator(); b.hasNext();) {
                c2 = (Connection)b.next();
                if( c1.getAddress().equals(c2.getAddress()) &&
                  c1.getPort() == c2.getPort() )
                    found = true;
            }
            assertTrue("missing " + c1 + " from list one", found);
        }
    }
	        
	
	/**
	 * Helper method for creating a list from a string of ip/port pairs 
	 * separated by commas, as per the Ultrapeer crawler format.
	 * 
	 * @param ipPorts the string of IP/port pairs
	 * @return a new List of Connections from the IP/port pairs
	 */
	private List createListFromIPPortString(String ipPorts) {
		StringTokenizer st = new StringTokenizer(ipPorts, ",");
		List list = new LinkedList();
		Properties props = new Properties();
		UltrapeerHandshakeResponder UHR = 
			new UltrapeerHandshakeResponder("20.34.90.1");
		while(st.hasMoreTokens()) {
			String ipPort = st.nextToken();
			StringTokenizer ipPortST = new StringTokenizer(ipPort, ":");
			Connection conn = new Connection(ipPortST.nextToken(), 
				Integer.parseInt(ipPortST.nextToken()), props, UHR);
			list.add(conn);
		}
		return list;
	}
	
	/**
	 * Tests the method for determining whether or not a given handshake is
	 * from the crawler or not.
	 */
	public void testIsCrawler() throws Exception {
		Properties props = new Properties();
		props.put(HeaderNames.CRAWLER, "0.1");
		HandshakeResponse hr = new HandshakeResponse(200, "OK", props);
		assertTrue("should be crawler", hr.isCrawler());
		hr = new HandshakeResponse(200, "OK", new Properties());
		assertFalse("should not be crawler", hr.isCrawler());
		//props.put()
		//HandshakeResponse hr = HandshakeResponse.createCrawlerResponse();
			
	}

	/**
	 * Tests the method for creating a response to the crawler.
	 */
	public void testCreateCrawlerResponse() throws Exception {
		TestConnectionManager tcm = new TestConnectionManager();
		PrivilegedAccessor.setValue(RouterService.class, "manager", tcm);
		HandshakeResponse hr = HandshakeResponse.createCrawlerResponse();
		Properties headers = hr.props();
		String leaves = headers.getProperty(HeaderNames.LEAVES);
		String ultrapeers = headers.getProperty(HeaderNames.PEERS);
		//System.out.println("leaves: "+leaves);
		//System.out.println("ultrapeers: "+ultrapeers);
		List leafList = createListFromIPPortString(leaves);
		List ultrapeerList = createListFromIPPortString(ultrapeers);
		assertTrue("leaf list should not be empty", !leafList.isEmpty());
		assertTrue("ultrapeer list should not be empty", 
            !ultrapeerList.isEmpty());
	}
	
    /**
     * Tests the method for checking whether or not the specified
     * host supports probe queries.
     */
    public void testSupportsProbeQueries() throws Exception {
        Properties props = new Properties();
        HandshakeResponse hr = HandshakeResponse.createResponse(props);
        assertTrue("should not support probes", !hr.supportsProbeQueries());
        props.put(HeaderNames.X_PROBE_QUERIES, "0.0");
        hr = HandshakeResponse.createResponse(props);
        assertTrue("should not support probes", !hr.supportsProbeQueries());

        props.put(HeaderNames.X_PROBE_QUERIES, "0.1");
        hr = HandshakeResponse.createResponse(props);
        assertTrue("should support probes", hr.supportsProbeQueries());

        props.put(HeaderNames.X_PROBE_QUERIES, "0.2");
        hr = HandshakeResponse.createResponse(props);
        assertTrue("should support probes", hr.supportsProbeQueries());
    }
    
    /**
     * Tests the method for extracting the status message from a connection
     * handshake status line.
     */
    public void testExtractMessage() throws Exception {
		Method codeMethod = 
            PrivilegedAccessor.getMethod(HandshakeResponse.class, 
                                         "extractCode",
                                         new Class[]{String.class});

		Method messageMethod = 
            PrivilegedAccessor.getMethod(HandshakeResponse.class, 
                                         "extractMessage",
                                         new Class[]{String.class});
        String line = "200 OK";
        int code = 
            ((Integer)codeMethod.invoke(null, new Object[]{line})).intValue();
        String message = (String)messageMethod.invoke(null, new Object[]{line});
        assertEquals("unexpected code", 200, code);
        assertEquals("unexpected message", "OK", message);

        line = "503 Service Unavailable";
        code = ((Integer)codeMethod.invoke(null, new Object[]{line})).intValue();
        assertEquals("unexpected code", 503, code);
        message = (String)messageMethod.invoke(null, new Object[]{line});
        assertEquals("unexpected message", "Service Unavailable", message);

        line = "503 Something Totally Different";
        code = ((Integer)codeMethod.invoke(null, new Object[]{line})).intValue();
        assertEquals("unexpected code", 503, code);
        message = (String)messageMethod.invoke(null, new Object[]{line});
        assertEquals("unexpected message", "Something Totally Different", 
            message);

        line = "200 Something Totally Different";
        code = ((Integer)codeMethod.invoke(null, new Object[]{line})).intValue();
        assertEquals("unexpected code", 200, code);
        message = (String)messageMethod.invoke(null, new Object[]{line});
        assertEquals("unexpected message", "Something Totally Different", 
            message);
    }



    /**
     * Helper method to make sure that the X-Try-Ultrapeers header in the
     * given set of headers matches the set of hosts in the given array
     * of hosts.
     */
    private void makeSureHeadersMatch(String[] hosts, Properties headers) {
        String headerHosts = 
            headers.getProperty(HeaderNames.X_TRY_ULTRAPEERS);

        assertNotNull("should have X-Try-Ultrapeer header", headerHosts);
        StringTokenizer st = new StringTokenizer(headerHosts, ",");
        assertEquals("unexpected number of hosts", 
                     Math.min(hosts.length, 10), st.countTokens());
        
        int i = 0;
        while(st.hasMoreTokens()) {
            String curHost = st.nextToken();            
            boolean match = false;
            for(int j=0; j<hosts.length; j++) {
                String host = hosts[j];
                if(curHost.equals(host)) {
                    match = true;
                    break;
                }
            }
            assertTrue("should have been a matching endpoint to: "+curHost, 
                match);
            i++;
        }
    }

    /**
     * Tests the various factory constructors.
     */
    public void testFactoryConstructors() {
        Properties props = new Properties();
        HandshakeResponse hr = 
            HandshakeResponse.createAcceptOutgoingResponse(props);
        assertTrue("should be accepted", hr.isAccepted());
        assertEquals("should not have any properties", 0, hr.props().size());
    }
    
    /**
     * Tests the isDeflateEnabled method.
     */
    public void testIsDeflateEnabled() throws Exception {
        Properties props = new Properties();
        HandshakeResponse hr;

        props.put("Content-Encoding", "deflate");
        hr = HandshakeResponse.createResponse(props);
        assertTrue("should be deflate enabled", hr.isDeflateEnabled());

        props.put("Content-Encoding", "gobblygook");
        hr = HandshakeResponse.createResponse(props);
        assertTrue("should not be deflate enabled", !hr.isDeflateEnabled());

        props.clear();
        hr = HandshakeResponse.createResponse(props);
        assertTrue("should not be deflate enabled", !hr.isDeflateEnabled());
    }
    
    /**
     * Tests that the isOldLimeWire method works correctly
     */
    public void testIsOldLimeWire() throws Exception {
        Properties props = new Properties();
        HandshakeResponse hr;

        props.put("User-Agent","bearshare/1.2.4");
        hr = HandshakeResponse.createResponse(props);
        assertFalse("user agent is not limewire ",hr.isOldLimeWire());
        
        props.put("User-Agent","limewire/@version@");
        hr = HandshakeResponse.createResponse(props);
        assertFalse("this version should not parse ",hr.isOldLimeWire());

        props.put("User-Agent","limewire/2.5.4");
        hr = HandshakeResponse.createResponse(props);
        assertTrue("major lower, minor higher, this is old",hr.isOldLimeWire());

        props.put("User-Agent","limewire/2.3.4");
        hr = HandshakeResponse.createResponse(props);
        assertTrue("major lower, minor lower, this is old",hr.isOldLimeWire());

        props.put("User-Agent","limewire/3.3.3");
        hr = HandshakeResponse.createResponse(props);
        assertTrue("major equal, minor lower, this is old",hr.isOldLimeWire());

        props.put("User-Agent","limewire/3.4.4");
        hr = HandshakeResponse.createResponse(props);
        assertFalse("major equal, minor higher, is  new",hr.isOldLimeWire());

        props.put("User-Agent","limewire/4.0.4");
        hr = HandshakeResponse.createResponse(props);
        assertFalse("major greater, minor lower, is  new",hr.isOldLimeWire());
    }


    /**
     * Tests the isDeflateAccepted method.  Note the wierd condition
     * with ConnectionSettings.ENCODE_DEFLATE
     */
    public void testIsDeflateAccepted() throws Exception {
        Properties props = new Properties();
        HandshakeResponse hr;
        
        // These are all the normal cases, where the servent
        // is allowed to write back a Content-Encoding: deflate
        ConnectionSettings.ENCODE_DEFLATE.setValue(true);

        props.put("Accept-Encoding", "deflate");
        hr = HandshakeResponse.createResponse(props);
        assertTrue("should be deflate enabled", hr.isDeflateAccepted());

        props.put("Accept-Encoding", "gobblygook");
        hr = HandshakeResponse.createResponse(props);
        assertTrue("should not be deflate enabled", !hr.isDeflateAccepted());

        props.clear();
        hr = HandshakeResponse.createResponse(props);
        assertTrue("should not be deflate enabled", !hr.isDeflateAccepted());
        
        // This is the other case, where the servent is now allowed
        // to write back a Content-Encoding: deflate.
        // In order to short-circuit the HandshakeResponders, we pretend
        // that all incoming responses simply do not have an Accept-Encoding
        // line if the servent cannot deflate output.
        ConnectionSettings.ENCODE_DEFLATE.setValue(false);
        props.put("Accept-Encoding", "deflate");
        hr = HandshakeResponse.createResponse(props);
        assertTrue("should not be deflate enabled", !hr.isDeflateAccepted());

        props.put("Accept-Encoding", "gobblygook");
        hr = HandshakeResponse.createResponse(props);
        assertTrue("should not be deflate enabled", !hr.isDeflateAccepted());

        props.clear();
        hr = HandshakeResponse.createResponse(props);
        assertTrue("should not be deflate enabled", !hr.isDeflateAccepted());                
    }    

    /**
     * Test to make sure that Ultrapeer headers are created correctly.
     */
    public void testUltrapeerHeaders() throws Exception {
        
        // Test once with deflate support & once without.
        ConnectionSettings.ACCEPT_DEFLATE.setValue(true);
        
        Properties headers = new Properties();
        headers.put(HeaderNames.X_ULTRAPEER, "false");
        HandshakeResponse client = HandshakeResponse.createResponse(headers);
        HandshakeResponse hr = 
            HandshakeResponse.createUltrapeerRejectIncomingResponse(client);
        runRejectHeadersTest(hr);

        hr = HandshakeResponse.createAcceptIncomingResponse(
            HandshakeResponse.createEmptyResponse(),
                new UltrapeerHeaders("32.9.8.9"));
        runUltrapeerHeadersTest(hr);
        
        headers = new Properties();
        headers.put(HeaderNames.X_ULTRAPEER, "true");
        client = HandshakeResponse.createResponse(headers);
        
        ConnectionSettings.ACCEPT_DEFLATE.setValue(false);
        hr = HandshakeResponse.createUltrapeerRejectIncomingResponse(client);
        runRejectHeadersTest(hr);

        hr = HandshakeResponse.createAcceptIncomingResponse(
            HandshakeResponse.createEmptyResponse(),
                new UltrapeerHeaders("32.9.8.9"));
        runUltrapeerHeadersTest(hr);
    }

    
    /**
     * Test to make sure that leaf headers are created correctly.
     */
    public void testLeafHeaders() throws Exception {
        // don't let the short-circuit take place, we want a real test.
        ConnectionSettings.ENCODE_DEFLATE.setValue(true);        
        
        // Test once with deflate support & once without.
        ConnectionSettings.ACCEPT_DEFLATE.setValue(true);  
        Properties headers = new Properties();
        headers.put(HeaderNames.X_ULTRAPEER, "false");
        HandshakeResponse client = HandshakeResponse.createResponse(headers);
        HandshakeResponse hr = 
            HandshakeResponse.createUltrapeerRejectIncomingResponse(client);
        runRejectHeadersTest(hr);

        hr = HandshakeResponse.createAcceptIncomingResponse(
            HandshakeResponse.createEmptyResponse(),
                new LeafHeaders("32.9.8.9"));
        runLeafHeadersTest(hr);

        ConnectionSettings.ACCEPT_DEFLATE.setValue(false);
        hr = HandshakeResponse.createRejectOutgoingResponse();
        runRejectOutgoingLeafHeadersTest(hr);

        hr = HandshakeResponse.createAcceptOutgoingResponse(
            new LeafHeaders("32.9.8.9"));
        runLeafHeadersTest(hr);
    }

    /**
     * Tests that the give <tt>HandshakeResponse</tt> has all of the headers
     * that it should as a leaf.
     *
     * @param hr the headers to test
     */
    private static void runLeafHeadersTest(HandshakeResponse hr) {
        runCommonHeadersTest(hr);
        assertTrue("should not be an Ultrapeer connection", 
                   !hr.isUltrapeer());
        assertTrue("should be a leaf connection", hr.isLeaf());
        assertTrue("should not be a GUESS Ultrapeer", !hr.isGUESSUltrapeer()); 
    }

    /**
     * Tests that the give <tt>HandshakeResponse</tt> has all of the headers
     * that it should as an Ultrapeer.
     *
     * @param hr the headers to test
     */
    private static void runUltrapeerHeadersTest(HandshakeResponse hr) {
        // don't let the short-circuit take place, we want a real test.        
        ConnectionSettings.ENCODE_DEFLATE.setValue(true);        
        
        runCommonHeadersTest(hr);
        assertTrue("should be an Ultrapeer connection", hr.isUltrapeer());
        assertTrue("should not be a leaf connection", !hr.isLeaf());
        assertTrue("should be a GUESS Ultrapeer", hr.isGUESSUltrapeer());        
    }

    /**
     * Makes sure that we only pass the expected headers when we
     * reject a connection -- in particular, make sure we only
     * pass the X-Try-Ultrapeers header.
     */
    private static void runRejectHeadersTest(HandshakeResponse hr) {
        Properties props = hr.props();
        assertEquals("unexpected props size", 1, props.size());
        assertTrue("should have X try Ultrapeer header", 
                   hr.hasXTryUltrapeers());
    }

    /**
     * Makes sure that we don't pass any headers in the odd case that
     * we're a leaf and we reject the connection that we initiated.
     */
    private static void runRejectOutgoingLeafHeadersTest(HandshakeResponse hr) {
        Properties props = hr.props();

        // if we're a leaf rejecting a connection that we initiated, we 
        // should not pass any headers.
        assertEquals("unexpected props size",0, props.size());
    }
    
    /**
     * Tests to make sure that all of the common headers are present, with the
     * expected values.  This tests headers that are sent by both leaves and
     * Ultrapeers.  If new common headers are added, they should be added to 
     * this list.
     *
     * @param hr the <tt>HandshakeResponse</tt> containing the headers to
     *  check
     */
    private static void runCommonHeadersTest(HandshakeResponse hr) {
        assertTrue("query routing should be enabled", hr.isQueryRoutingEnabled());
        assertTrue("Ultrapeer query routing should be enabled",
                   hr.isUltrapeerQueryRoutingConnection());
        assertEquals("unexpected user agent", 
                     CommonUtils.getHttpServer(), hr.getUserAgent());

        assertTrue("should be a high-degree connection", 
            hr.isHighDegreeConnection());
        assertEquals("unexpected max ttl", 
                     ConnectionSettings.SOFT_MAX.getValue(), 
                     hr.getMaxTTL());
        assertEquals("unexpected degree", 32, 
            hr.getNumIntraUltrapeerConnections());
        assertTrue("should be GUESS capable", hr.isGUESSCapable());
        assertTrue("should support GGEP", hr.supportsGGEP());
        assertTrue("should support vendor messages", 
            hr.supportsVendorMessages() > 0);
        assertTrue("should use dynamic querying", hr.isDynamicQueryConnection());

        //if we added the value, make sure its there.
        if(ConnectionSettings.ACCEPT_DEFLATE.getValue())
            assertTrue("should accept deflate encoding", hr.isDeflateAccepted());
        else 
            assertTrue("should not accept deflate encoding", 
                !hr.isDeflateAccepted());
            
        // no responders have added the Content-Encoding: deflate yet...
        assertTrue("should not be encoding in deflate", !hr.isDeflateEnabled());
    }

    /**
     * Tests to make sure that the ultrapeer needed header is interpretted
     * correctly.
     */
    public void testLeafGuidance() {
        Properties headers = new Properties();
        headers.put(HeaderNames.X_ULTRAPEER_NEEDED, "true");
        HandshakeResponse hr = 
            HandshakeResponse.createAcceptIncomingResponse(
                HandshakeResponse.createEmptyResponse(), headers);
        assertTrue("should not include leaf guidance", !hr.hasLeafGuidance());

        headers.put(HeaderNames.X_ULTRAPEER_NEEDED, "false");
        hr = HandshakeResponse.createAcceptIncomingResponse(
            HandshakeResponse.createEmptyResponse(), headers);
        assertTrue("should include leaf guidance", hr.hasLeafGuidance());

        hr = HandshakeResponse.createAcceptIncomingResponse(
            HandshakeResponse.createEmptyResponse(), new Properties());
        assertTrue("should not include leaf guidance", !hr.hasLeafGuidance());
    }
        

    /**
     * Tests the header utility method that checks if a the version for a 
     * given header is greater than or equal to specific values.
     */
    public void testIsVersionOrHigher() throws Exception {
        Properties headers = new Properties();
        headers.put(HeaderNames.X_ULTRAPEER_QUERY_ROUTING, "0.1");

		Method m = 
            PrivilegedAccessor.getMethod(HandshakeResponse.class, 
                                         "isVersionOrHigher",
                                         new Class[]{Properties.class,
                                                     String.class, 
                                                     Float.TYPE});

        Object[] params = new Object[3];
        params[0] = headers;
        params[1] = HeaderNames.X_ULTRAPEER_QUERY_ROUTING;

        
        params[2] = new Float(0.1);
        Boolean correctVersion = (Boolean)m.invoke(null, params); 
        assertTrue("should have been the correct version", 
                   correctVersion.booleanValue());

        params[2] = new Float(0.2);
        correctVersion = (Boolean)m.invoke(null, params); 
        assertFalse("should not have been the correct version", 
                    correctVersion.booleanValue());

        params[2] = new Float(0.3);
        correctVersion = (Boolean)m.invoke(null, params); 
        assertFalse("should not have been the correct version", 
                    correctVersion.booleanValue());
    }


    /**
     * Test to make sure that the X-Max-TTL header is being created and
     * passed correctly.
     */
    public void testXMaxTTL() throws Exception {
		Method m = 
            PrivilegedAccessor.getMethod(HandshakeResponse.class, 
                                         "extractByteHeaderValue",
                                         new Class[]{Properties.class,
                                                     String.class, 
                                                     Byte.TYPE}); 

        Properties headers = new UltrapeerHeaders("3.7.6.8");
        Object[] params = new Object[3];
        params[0] = headers;
        params[1] = HeaderNames.X_MAX_TTL;         
        params[2] = new Byte((byte)5);
        
        Byte ttl = (Byte)m.invoke(null, params);

        assertEquals("should have X-Max-TTL: 3", (byte)3, ttl.byteValue());
    }

    /**
     * Test to make sure that the extractByteHeaderValue method is 
     * working correctly.
     */
    public void testExtractByteHeaderValue() throws Exception {
		Method m = 
            PrivilegedAccessor.getMethod(HandshakeResponse.class, 
                                         "extractByteHeaderValue",
                                         new Class[]{Properties.class,
                                                     String.class, 
                                                     Byte.TYPE}); 

        Properties headers = new UltrapeerHeaders("3.7.6.8");
        Object[] params = new Object[3];
        params[0] = headers;
        params[1] = HeaderNames.X_MAX_TTL; 
        params[2] = new Byte((byte)5);
        
        Byte ttl = (Byte)m.invoke(null, params);

        // should use contain 4 from the UltrapeerHeaders
        assertEquals("should have X-Max-TTL: 3", 
                     3, ttl.byteValue());

        params[0] = new Properties();
        ttl = (Byte)m.invoke(null, params);

        // should use the default value of 5, since the header is not
        // present
        assertEquals("should have X-Max-TTL: 5", 
                     ((Byte)params[2]).byteValue(), ttl.byteValue());

        ((Properties)params[0]).put(HeaderNames.X_MAX_TTL, "fjajfl");

        ttl = (Byte)m.invoke(null, params);

        // should use the default value of 5, since the header is 
        // gibberish
        assertEquals("should have X-Max-TTL: 5", 
                     ((Byte)params[2]).byteValue(), ttl.byteValue());
    }


    /**
     * Test to make sure that the extractIntHeaderValue method is 
     * working correctly.
     */
    public void testExtractIntHeaderValue() throws Exception {
		Method m = 
            PrivilegedAccessor.getMethod(HandshakeResponse.class, 
                                         "extractIntHeaderValue",
                                         new Class[]{Properties.class,
                                                     String.class, 
                                                     Integer.TYPE}); 

        Properties headers = new UltrapeerHeaders("3.7.6.8");

        // construct the parameters to pass to the method
        Object[] params = new Object[3];
        params[0] = headers;
        params[1] = HeaderNames.X_DEGREE; 
        params[2] = new Integer(20);
        
        Integer degree = (Integer)m.invoke(null, params);

        // should use the default degree from the UltrapeerHeaders
        assertEquals("should have different X-Degree", 
                     32, degree.intValue());

        params[0] = new Properties();
        degree = (Integer)m.invoke(null, params);

        // should use the default value of 20, since the header is not
        // present
        assertEquals("should have different X-Degree", 
                     ((Integer)params[2]).intValue(), degree.intValue());

        ((Properties)params[0]).put(HeaderNames.X_DEGREE, "fjajfl");

        degree = (Integer)m.invoke(null, params);

        // should use the default value of 5, since the header is 
        // gibberish
        assertEquals("should have different X-Degree", 
                     ((Integer)params[2]).intValue(), degree.intValue());
    }


    /**
     * Test to make sure that the extractStringHeaderValue method is 
     * working correctly.
     */
    public void testExtractStringHeaderValue() throws Exception {
		Method m = 
            PrivilegedAccessor.getMethod(HandshakeResponse.class, 
                                         "extractStringHeaderValue",
                                         new Class[]{Properties.class,
                                                     String.class});

        Properties headers = new Properties();

        String host = "54.78.54.8:6346";
        headers.put(HeaderNames.X_TRY_ULTRAPEERS, host);
        
        // construct the parameters to pass to the method
        Object[] params = new Object[2];
        params[0] = headers;
        params[1] = HeaderNames.X_TRY_ULTRAPEERS; 
        
        String hosts = (String)m.invoke(null, params);

        
        assertEquals("should have different X-Try-Ultrapeer", 
                     host, hosts);

        params[0] = new Properties();
        hosts = (String)m.invoke(null, params);

        // should use the default value of "", since the header is not
        // present
        assertEquals("should have different X-Try-Ultrapeer", 
                     "", hosts);
    }


    /**
     * Tests the headerExists method.
     */
    public void testHeaderExists() throws Exception {
		Method m = 
            PrivilegedAccessor.getMethod(HandshakeResponse.class, 
                                         "headerExists",
                                         new Class[]{Properties.class,
                                                     String.class});

        Properties headers = new Properties();

        String host = "54.78.54.8:6346";
        headers.put(HeaderNames.X_TRY_ULTRAPEERS, host);
        
        // construct the parameters to pass to the method
        Object[] params = new Object[2];
        params[0] = headers;
        params[1] = HeaderNames.X_TRY_ULTRAPEERS; 
        
        Boolean exists = (Boolean)m.invoke(null, params);
        assertTrue("header should exist", exists.booleanValue());

        // this header should not be there, so make sure it isn't!
        params[1] = HeaderNames.X_ULTRAPEER_QUERY_ROUTING;

        exists = (Boolean)m.invoke(null, params);
        assertTrue("header should not exists", !exists.booleanValue());        
    }

    /**
     * Tests the isFalseValue method to make sure it's working correctly
     */
    public void testIsFalseValue() throws Exception {
		Method m = 
            PrivilegedAccessor.getMethod(HandshakeResponse.class, 
                                         "isFalseValue",
                                         new Class[]{Properties.class,
                                                     String.class});

        Properties headers = new Properties();

        // construct the parameters to pass to the method
        Object[] params = new Object[2];
        params[0] = headers;
        params[1] = HeaderNames.X_ULTRAPEER; 

        // this header should not exist, and so should not be false
        Boolean isFalse = (Boolean)m.invoke(null, params);
        assertTrue("header should not be false", !isFalse.booleanValue());


        // this should be false
        headers.put(HeaderNames.X_ULTRAPEER, "false");
        isFalse = (Boolean)m.invoke(null, params);
        assertTrue("header should be false", isFalse.booleanValue());


        // this should be false
        headers.put(HeaderNames.X_ULTRAPEER, "False");
        isFalse = (Boolean)m.invoke(null, params);
        assertTrue("header should be false", isFalse.booleanValue());


        // this should not be false
        headers.put(HeaderNames.X_ULTRAPEER, "FIPEUI");
        isFalse = (Boolean)m.invoke(null, params);
        assertTrue("header should not be false", !isFalse.booleanValue());
        
        // this should not be false
        headers.put(HeaderNames.X_ULTRAPEER, "true");
        isFalse = (Boolean)m.invoke(null, params);
        assertTrue("header should not be false", !isFalse.booleanValue());        

        // this should not be false
        headers.put(HeaderNames.X_ULTRAPEER, "True");
        isFalse = (Boolean)m.invoke(null, params);
        assertTrue("header should not be false", !isFalse.booleanValue());        

        // this should not be false
        headers.put(HeaderNames.X_ULTRAPEER, "falsee");
        isFalse = (Boolean)m.invoke(null, params);
        assertTrue("header should not be false", !isFalse.booleanValue()); 

        // this should not be false
        headers.put(HeaderNames.X_ULTRAPEER, "");
        isFalse = (Boolean)m.invoke(null, params);
        assertTrue("header should not be false", !isFalse.booleanValue()); 
    }

    /**
     * Tests the isTrueValue method to make sure it's working correctly
     */
    public void testIsTrueValue() throws Exception {
		Method m = 
            PrivilegedAccessor.getMethod(HandshakeResponse.class, 
                                         "isTrueValue",
                                         new Class[]{Properties.class,
                                                     String.class});

        Properties headers = new Properties();

        // construct the parameters to pass to the method
        Object[] params = new Object[2];
        params[0] = headers;
        params[1] = HeaderNames.X_ULTRAPEER; 

        // this header should not exist, and so should not be true
        Boolean isTrue = (Boolean)m.invoke(null, params);
        assertTrue("header should not be true", !isTrue.booleanValue());


        // this should be true
        headers.put(HeaderNames.X_ULTRAPEER, "true");
        isTrue = (Boolean)m.invoke(null, params);
        assertTrue("header should be true", isTrue.booleanValue());


        // this should be true
        headers.put(HeaderNames.X_ULTRAPEER, "True");
        isTrue = (Boolean)m.invoke(null, params);
        assertTrue("header should be true", isTrue.booleanValue());


        // this should not be true
        headers.put(HeaderNames.X_ULTRAPEER, "FIPEUI");
        isTrue = (Boolean)m.invoke(null, params);
        assertTrue("header should not be true", !isTrue.booleanValue());
        
        // this should not be true
        headers.put(HeaderNames.X_ULTRAPEER, "false");
        isTrue = (Boolean)m.invoke(null, params);
        assertTrue("header should not be true", !isTrue.booleanValue());        

        // this should not be true
        headers.put(HeaderNames.X_ULTRAPEER, "False");
        isTrue = (Boolean)m.invoke(null, params);
        assertTrue("header should not be true", !isTrue.booleanValue());        

        // this should not be true
        headers.put(HeaderNames.X_ULTRAPEER, "truee");
        isTrue = (Boolean)m.invoke(null, params);
        assertTrue("header should not be true", !isTrue.booleanValue()); 

        // this should not be true
        headers.put(HeaderNames.X_ULTRAPEER, "");
        isTrue = (Boolean)m.invoke(null, params);
        assertTrue("header should not be true", !isTrue.booleanValue()); 
    }
    
    /**
     * Tests the isStringValue method to see if it's working correctly.
     */
    public void testIsStringValue() throws Exception {
		Method m = 
            PrivilegedAccessor.getMethod(HandshakeResponse.class, 
                                         "isStringValue",
                                         new Class[]{Properties.class,
                                                     String.class,
                                                     String.class});

        Properties headers = new Properties();

        // construct the parameters to pass to the method
        Object[] params = new Object[3];
        params[0] = headers;
        params[1] = "Content-Encoding";
        params[2] = "deflate";

        //the header with the correct value exists; true
        headers.put("Content-Encoding", "Deflate");
        Boolean isTrue = (Boolean)m.invoke(null, params);
        assertTrue("should contain correct value", isTrue.booleanValue());

        //the header with an incorrect value exists; false
        headers.put("Content-Encoding", "zip");
        isTrue = (Boolean)m.invoke(null, params);
        assertTrue("should not contain correct value", !isTrue.booleanValue());

        //the header does not exist; false
        headers.clear();
        isTrue = (Boolean)m.invoke(null, params);
        assertTrue("should not contain correct value", !isTrue.booleanValue());
    }
    
    /**
     * Tests the containsStringValue method to see if it's working properly.
     */
    public void testContainsStringValue() throws Exception {
		Method m = 
            PrivilegedAccessor.getMethod(HandshakeResponse.class, 
                                         "containsStringValue",
                                         new Class[]{Properties.class,
                                                     String.class,
                                                     String.class});

        Properties headers = new Properties();

        // construct the parameters to pass to the method
        Object[] params = new Object[3];
        params[0] = headers;
        params[1] = "Accept-Encoding";
        params[2] = "deflate";

        //the header with a single correct value exists; true
        headers.put("Accept-Encoding", "deflate");
        Boolean isTrue = (Boolean)m.invoke(null, params);
        assertTrue("should contain correct value", isTrue.booleanValue());
        
        //the header with a multiple values (one correct) exists; true
        headers.put("Accept-Encoding", "deflate, zip, really good zip");
        isTrue = (Boolean)m.invoke(null, params);
        assertTrue("should contain correct value", isTrue.booleanValue());        

        //the header with an incorrect value exists; false
        headers.put("Accept-Encoding", "zip");
        isTrue = (Boolean)m.invoke(null, params);
        assertTrue("should not contain correct value", !isTrue.booleanValue());
        
        //the header with an multiple incorrect value exists; false
        headers.put("Accept-Encoding", "zip, gzip, pack");
        isTrue = (Boolean)m.invoke(null, params);
        assertTrue("should not contain correct value", !isTrue.booleanValue());        

        //the header does not exist; false
        headers.clear();
        isTrue = (Boolean)m.invoke(null, params);        
        assertTrue("should not contain correct value", !isTrue.booleanValue());        
    }
}
