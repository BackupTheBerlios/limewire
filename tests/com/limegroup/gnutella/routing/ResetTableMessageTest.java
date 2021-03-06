package com.limegroup.gnutella.routing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import junit.framework.Test;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.util.BaseTestCase;

/**
 * Unit tests for ResetTableMessage
 */
public class ResetTableMessageTest extends BaseTestCase {
        
	public ResetTableMessageTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(ResetTableMessageTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

    public void testLegacy() throws Exception {
        //From scratch.  Check encode/decode.
        ResetTableMessage m=new ResetTableMessage(1024, (byte)10);
        assertEquals(ResetTableMessage.RESET_VARIANT, m.getVariant());
        assertEquals((byte)1, m.getTTL());
        assertEquals(1024, m.getTableSize());
        assertEquals(10, m.getInfinity());
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        m.write(out);
        out.flush();
        
        m=read(out.toByteArray());
        assertEquals(ResetTableMessage.RESET_VARIANT, m.getVariant());
        assertEquals((byte)1, m.getTTL());
        assertEquals(1024, m.getTableSize());
        assertEquals(10, m.getInfinity());

        //Read from bytes
        byte[] message=new byte[23+6];
        message[16]=Message.F_ROUTE_TABLE_UPDATE;            //function code
        message[17]=(byte)3;                                 //TTL
        message[19]=(byte)6;                                 //payload length
        message[23+0]=(byte)RouteTableMessage.RESET_VARIANT; //reset variant
        message[23+2]=(byte)1;                               //size==256
        message[23+5]=(byte)10;                              //infinity
        m=read(message);
        assertEquals(RouteTableMessage.RESET_VARIANT, m.getVariant());
        assertEquals((byte)10, m.getInfinity());
        assertEquals(256, m.getTableSize());
    }

    static ResetTableMessage read(byte[] bytes) throws Exception {
        InputStream in=new ByteArrayInputStream(bytes);
        return (ResetTableMessage)Message.read(in);
    }
}