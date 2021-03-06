package com.limegroup.gnutella.connection;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.net.*;

import junit.framework.Test;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.util.*;

/**
 * Tests that MessageReader extracts messages from a source channel correctly.
 */
public final class MessageReaderTest extends BaseTestCase {
    
    private StubMessageReceiver STUB = new StubMessageReceiver();
    private MessageReader READER = new MessageReader(STUB);
    
    private static final byte[] IP = new byte[] { (byte)127, 0, 0, 1 };

	public MessageReaderTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(MessageReaderTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void setUp() throws Exception {
	    STUB.clear();
        RouterService.getAcceptor().setAddress(InetAddress.getLocalHost());	    
	}
	
	public void testSingleMessageRead() throws Exception {
	    Message out = new PingRequest((byte)1);
	    READER.setReadChannel(channel(buffer(out)));
	    assertEquals(0, STUB.size());
	    READER.handleRead();
	    assertEquals(1, STUB.size());
	    Message in = STUB.getMessage();
	    assertEquals(buffer(out), buffer(in));
	    assertFalse(STUB.isClosed());
    }
    
    public void testReadMultipleMessages() throws Exception {
        Message out1 = new PingRequest((byte)1);
        Message out2 = QueryRequest.createQuery("test");
        Message out3 = new QueryReply(GUID.makeGuid(), (byte) 4, 
                                           6346, IP, 0, new Response[0],
                                           GUID.makeGuid(), new byte[0],
                                           false, false, true, true, true, false,
                                           null);
        Message out4 = new PushRequest(GUID.makeGuid(), (byte)0, GUID.makeGuid(), 0, IP, 6346);
        Message out5 = PingReply.create(GUID.makeGuid(),(byte)1, new Endpoint("1.2.3.4", 5));
        Message[] allOut = new Message[] { out1, out2, out3, out4, out5 };
        READER.setReadChannel(channel(buffer(allOut)));
	    assertEquals(0, STUB.size());
	    READER.handleRead();
	    assertEquals(5, STUB.size());
	    Message in1 = STUB.getMessage();
	    Message in2 = STUB.getMessage();
	    Message in3 = STUB.getMessage();
	    Message in4 = STUB.getMessage();
	    Message in5 = STUB.getMessage();
	    Message[] allIn = new Message[] { in1, in2, in3, in4, in5 };
	    assertEquals("out: " + out1 + ", in: " + in1, buffer(out1), buffer(in1));
	    assertEquals("out: " + out2 + ", in: " + in2, buffer(out2), buffer(in2));
	    assertEquals("out: " + out3 + ", in: " + in3, buffer(out3), buffer(in3));
	    assertEquals("out: " + out4 + ", in: " + in4, buffer(out4), buffer(in4));
	    assertEquals("out: " + out5 + ", in: " + in5, buffer(out5), buffer(in5));
	    assertEquals(buffer(allOut), buffer(allIn));
	    assertFalse(STUB.isClosed());
    }
    
    public void testBadPacketIgnored() throws Exception {
        Message out1 = new PingRequest((byte)1);
        Message out2 = QueryRequest.createQuery("test");
        Message out3 = new QueryReply(GUID.makeGuid(), (byte) 4, 
                                           6346, IP, 0, new Response[0],
                                           GUID.makeGuid(), new byte[0],
                                           false, false, true, true, true, false,
                                           null);
        Message out4 = new PushRequest(GUID.makeGuid(), (byte)0, GUID.makeGuid(), 0, IP, 6346);
        Message out5 = PingReply.create(GUID.makeGuid(),(byte)1, new Endpoint("1.2.3.4", 5));
        ByteBuffer b1 = buffer(out1);
        ByteBuffer b2 = buffer(out2);
        ByteBuffer b3 = buffer(out3);
        ByteBuffer b4 = buffer(out4);
        ByteBuffer b5 = buffer(out5);
        b2.put(18, (byte)100);   // change the hops of the query request. to be absurdly high
        READER.setReadChannel(channel(buffer(new ByteBuffer[] { b1, b2, b3, b4, b5 })));
	    assertEquals(0, STUB.size());
	    READER.handleRead();
	    assertEquals(4, STUB.size());
	    Message in1 = STUB.getMessage();
	    Message in3 = STUB.getMessage();
	    Message in4 = STUB.getMessage();
	    Message in5 = STUB.getMessage();
	    assertEquals("out: " + out1 + ", in: " + in1, buffer(out1), buffer(in1));
	    assertEquals("out: " + out3 + ", in: " + in3, buffer(out3), buffer(in3));
	    assertEquals("out: " + out4 + ", in: " + in4, buffer(out4), buffer(in4));
	    assertEquals("out: " + out5 + ", in: " + in5, buffer(out5), buffer(in5));
	    assertFalse(STUB.isClosed());
    }
    
    public void testLargeLengthThrows() throws Exception {
        Message out1 = new PingRequest((byte)1);
        Message out2 = QueryRequest.createQuery("test");
        Message out3 = new QueryReply(GUID.makeGuid(), (byte) 4, 
                                           6346, IP, 0, new Response[0],
                                           GUID.makeGuid(), new byte[0],
                                           false, false, true, true, true, false,
                                           null);
        Message out4 = new PushRequest(GUID.makeGuid(), (byte)0, GUID.makeGuid(), 0, IP, 6346);
        Message out5 = PingReply.create(GUID.makeGuid(),(byte)1, new Endpoint("1.2.3.4", 5));
        ByteBuffer b1 = buffer(out1);
        ByteBuffer b2 = buffer(out2);
        ByteBuffer b3 = buffer(out3);
        ByteBuffer b4 = buffer(out4);
        ByteBuffer b5 = buffer(out5);
        b4.order(ByteOrder.LITTLE_ENDIAN);
        b4.putInt(19, 64 * 1024 + 1);
        READER.setReadChannel(channel(buffer(new ByteBuffer[] { b1, b2, b3, b4, b5 })));
	    assertEquals(0, STUB.size());
	    try {
	        READER.handleRead();
	        fail("didn't throw IOX");
        } catch(IOException expected) {}
	    assertEquals(3, STUB.size());
	    Message in1 = STUB.getMessage();
	    Message in2 = STUB.getMessage();
	    Message in3 = STUB.getMessage();
	    assertEquals("out: " + out1 + ", in: " + in1, buffer(out1), buffer(in1));
	    assertEquals("out: " + out2 + ", in: " + in2, buffer(out2), buffer(in2));
	    assertEquals("out: " + out3 + ", in: " + in3, buffer(out3), buffer(in3));
	    // the close in real life is actually handled by NIODispatcher catching the IOX
	    // and shutting down the NIOSocket, which shuts down this MessageReader, which
	    // forwards the close to the MessageReceiver.  since we don't have that framework
	    // here, the close won't actually happen.
	    //assertTrue(STUB.isClosed());
    }
    
    public void testSmallLengthThrows() throws Exception {
        Message out1 = new PingRequest((byte)1);
        Message out2 = QueryRequest.createQuery("test");
        Message out3 = new QueryReply(GUID.makeGuid(), (byte) 4, 
                                           6346, IP, 0, new Response[0],
                                           GUID.makeGuid(), new byte[0],
                                           false, false, true, true, true, false,
                                           null);
        Message out4 = new PushRequest(GUID.makeGuid(), (byte)0, GUID.makeGuid(), 0, IP, 6346);
        Message out5 = PingReply.create(GUID.makeGuid(),(byte)1, new Endpoint("1.2.3.4", 5));
        ByteBuffer b1 = buffer(out1);
        ByteBuffer b2 = buffer(out2);
        ByteBuffer b3 = buffer(out3);
        ByteBuffer b4 = buffer(out4);
        ByteBuffer b5 = buffer(out5);
        b4.order(ByteOrder.LITTLE_ENDIAN);
        b4.putInt(19, -1);
        READER.setReadChannel(channel(buffer(new ByteBuffer[] { b1, b2, b3, b4, b5 })));
	    assertEquals(0, STUB.size());
	    try {
	        READER.handleRead();
	        fail("didn't throw IOX");
        } catch(IOException expected) {}
	    assertEquals(3, STUB.size());
	    Message in1 = STUB.getMessage();
	    Message in2 = STUB.getMessage();
	    Message in3 = STUB.getMessage();
	    assertEquals("out: " + out1 + ", in: " + in1, buffer(out1), buffer(in1));
	    assertEquals("out: " + out2 + ", in: " + in2, buffer(out2), buffer(in2));
	    assertEquals("out: " + out3 + ", in: " + in3, buffer(out3), buffer(in3));
	    // the close in real life is actually handled by NIODispatcher catching the IOX
	    // and shutting down the NIOSocket, which shuts down this MessageReader, which
	    // forwards the close to the MessageReceiver.  since we don't have that framework
	    // here, the close won't actually happen.
	    //assertTrue(STUB.isClosed());
    }
    
    public void testReadInPasses() throws Exception {
        Message out = QueryRequest.createQuery("this is a really long query");
        ByteBuffer b1 = buffer(out);
        ByteBuffer b2 = b1.duplicate();
        ByteBuffer b3 = b1.duplicate();
        ByteBuffer b4 = b1.duplicate();
        b1.limit(8);
        b2.position(8);
        b2.limit(23);
        b3.position(23);
        b3.limit(30);
        b4.position(30);
        
        READER.setReadChannel(channel(b1));
        assertEquals(0, STUB.size());
        assertTrue(b1.hasRemaining());
        READER.handleRead();
        assertEquals(0, STUB.size());
        assertFalse(b1.hasRemaining());
    
        READER.setReadChannel(channel(b2));
        assertEquals(0, STUB.size());
        assertTrue(b2.hasRemaining());
        READER.handleRead();
        assertEquals(0, STUB.size());
        assertFalse(b2.hasRemaining());
        
        READER.setReadChannel(channel(b3));
        assertEquals(0, STUB.size());
        assertTrue(b3.hasRemaining());
        READER.handleRead();
        assertEquals(0, STUB.size());
        assertFalse(b3.hasRemaining());
        
        READER.setReadChannel(channel(b4));
        assertEquals(0, STUB.size());
        assertTrue(b4.hasRemaining());
        READER.handleRead();
        assertEquals(1, STUB.size());
        assertFalse(b4.hasRemaining());
        
        Message in = STUB.getMessage();
        assertEquals(buffer(out),  buffer(in));
    }
    
    public void testEOFInHeaderThrows() throws Exception {
        Message out = QueryRequest.createQuery("test");
        ByteBuffer b = buffer(out);
        b.limit(20);
        
        READER.setReadChannel(eof(b));
        assertTrue(b.hasRemaining());
        assertEquals(0, STUB.size());
        try {
            READER.handleRead();
            fail("expected IOX");
        } catch(IOException expected) {}
        assertEquals(0, STUB.size());
        assertFalse(b.hasRemaining());
    }
    
    public void testEOFAfterHeaderThrows() throws Exception {
        Message out = QueryRequest.createQuery("test");
        ByteBuffer b = buffer(out);
        b.limit(23);
        
        READER.setReadChannel(eof(b));
        assertTrue(b.hasRemaining());
        assertEquals(0, STUB.size());
        try {
            READER.handleRead();
            fail("expected IOX");
        } catch(IOException expected) {}
        assertEquals(0, STUB.size());
        assertFalse(b.hasRemaining());
    }
    
    public void testEOFInPayloadThrows() throws Exception {
        Message out = QueryRequest.createQuery("test");
        ByteBuffer b = buffer(out);
        b.limit(30);
        
        READER.setReadChannel(eof(b));
        assertTrue(b.hasRemaining());
        assertEquals(0, STUB.size());
        try {
            READER.handleRead();
            fail("expected IOX");
        } catch(IOException expected) {}
        assertEquals(0, STUB.size());
        assertFalse(b.hasRemaining());
    }

    public void testEOFAfterPayloadThrowsButMessageIsRead() throws Exception {
        Message out = QueryRequest.createQuery("test");
        ByteBuffer b = buffer(out);
        
        READER.setReadChannel(eof(b));
        assertTrue(b.hasRemaining());
        assertEquals(0, STUB.size());
        try {
            READER.handleRead();
            fail("expected IOX");
        } catch(IOException expected) {}
        assertEquals(1, STUB.size());
        assertFalse(b.hasRemaining());
        
        Message in = STUB.getMessage();
        assertEquals(buffer(out), buffer(in));
    }
    
    public void testShutdown() throws Exception {
        assertFalse(STUB.isClosed());
        READER.shutdown();
        assertTrue(STUB.isClosed());
    }
    
    public void testChannelMethods() throws Exception {
        try {
            READER.setReadChannel(null);
            fail("expected NPE");
        } catch(NullPointerException expected) {}
        
        ReadableByteChannel channel = new ReadBufferChannel();
        READER.setReadChannel(channel);
        assertSame(channel, READER.getReadChannel());
        
        try {
            new MessageReader(null);
            fail("expected NPE");
        } catch(NullPointerException expected) {}
            
        READER = new MessageReader(channel, STUB);
        assertSame(channel, READER.getReadChannel());
        
        READER = new MessageReader(null, STUB);
        assertNull(READER.getReadChannel());
    }
    
    private ReadableByteChannel channel(ByteBuffer buffer) throws Exception {
        return new ReadBufferChannel(buffer);
    }
    
    private ReadableByteChannel eof(ByteBuffer buffer) throws Exception {
        return new ReadBufferChannel(buffer, true);
    }
    
    private ByteBuffer buffer(Message m) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        m.write(out);
        out.flush();
        return ByteBuffer.wrap(out.toByteArray());
    }
    
    private ByteBuffer buffer(Message m[]) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for(int i = 0; i < m.length; i++)
            m[i].write(out);
        out.flush();
        return ByteBuffer.wrap(out.toByteArray());
    }

    private ByteBuffer buffer(List ms) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for(Iterator i = ms.iterator(); i.hasNext(); )
            ((Message)i.next()).write(out);
        out.flush();
        return ByteBuffer.wrap(out.toByteArray());
    }
    
    private ByteBuffer buffer(ByteBuffer[] bufs) throws Exception {
        int length = 0;
        for(int i = 0; i < bufs.length; i++)
            length += bufs[i].limit();
        ByteBuffer combined = ByteBuffer.allocate(length);
        for(int i = 0; i < bufs.length; i++)
            combined.put(bufs[i]);
        combined.flip();
        return combined;
    }
}