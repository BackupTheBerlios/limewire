package com.limegroup.gnutella.simpp;

import java.io.File;
import java.io.IOException;

import junit.framework.Test;

import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.SimppSettingsManager;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.PrivilegedAccessor;


public class SimppManagerTest extends BaseTestCase {
    
    static final int OLD = 1;
    
    static final int MIDDLE = 2;

    static final int NEW = 3;
    
    static final int DEF_SIGNATURE = 4;

    static final int DEF_MESSAGE = 5;

    static final int BAD_XML = 6;
    
    static final int RANDOM_BYTES = 7;

    static final int ABOVE_MAX = 8;

    static final int BELOW_MIN = 9;    

    private static File OLD_SIMPP_FILE;
    
    private static File MIDDLE_SIMPP_FILE;

    private static File NEW_SIMPP_FILE;

    private static File DEF_SIG_FILE;
    
    private static File DEF_MESSAGE_FILE;

    private static File BAD_XML_FILE;

    private static File RANDOM_BYTES_FILE;

    static final int PORT = 6346;

    private static File _simppFile;

    private static int _simppMessageNumber = -1;

    public SimppManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(SimppManagerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static void globalSetUp() throws Exception {
        setSettings();
        RouterService rs = new RouterService(new ActivityCallbackStub());
        rs.start();
    }

    private static void setSettings() throws Exception {
        String simppDir = "com/limegroup/gnutella/simpp/";

        OLD_SIMPP_FILE = CommonUtils.getResourceFile(simppDir+"oldFile.xml");
        MIDDLE_SIMPP_FILE = CommonUtils.getResourceFile
                                                 (simppDir+"middleFile.xml");
        NEW_SIMPP_FILE = CommonUtils.getResourceFile(simppDir+"newFile.xml");
        DEF_SIG_FILE = CommonUtils.getResourceFile(simppDir+"defSigFile.xml");
        DEF_MESSAGE_FILE = CommonUtils.getResourceFile
                                               (simppDir+"defMessageFile.xml");
        BAD_XML_FILE = CommonUtils.getResourceFile(simppDir+"badXmlFile.xml");
        RANDOM_BYTES_FILE = CommonUtils.getResourceFile
                                                    (simppDir+"randFile.xml");

        assertTrue(OLD_SIMPP_FILE.exists());
        assertTrue(MIDDLE_SIMPP_FILE.exists());
        assertTrue(NEW_SIMPP_FILE.exists());
        assertTrue(DEF_SIG_FILE.exists());
        assertTrue(BAD_XML_FILE.exists());
        assertTrue(RANDOM_BYTES_FILE.exists());
        
        // TODO: make simpp more than 1 class with 1 method.
        File pub = CommonUtils.getResourceFile(simppDir+"pub1.key");
        File pub2 = new File(".", "pub1.key");
        pub2.delete();
        CommonUtils.copy(pub, pub2);
        assertTrue("local public key doesn't exist", pub2.exists());

        _simppFile = new File(_settingsDir, "simpp.xml");

        //set up the correct simpp version
        _simppMessageNumber = OLD;
        changeSimppFile();


        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(
                                                   new String[] {"*.*.*.*"} );
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(
                                                   new String[] {"127.*.*.*"});
		ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
        ConnectionSettings.PORT.setValue(PORT);
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(true);
        UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
		UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(false);
		UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
        UltrapeerSettings.MAX_LEAVES.setValue(5);
		ConnectionSettings.NUM_CONNECTIONS.setValue(5);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);	
		ConnectionSettings.USE_GWEBCACHE.setValue(false);
    }
    
    public void setUp() throws Exception {
        Thread.sleep(4000);
        setSettings();
    }

    ////////////////////////////////tests/////////////////////////////////////

    public void testOldVersion() throws Exception{
        //Note: we have already set the version to be old in setSettings
        SimppManager man = SimppManager.instance();
        assertEquals("problem reading/verifying old version file", 1,
                                                              man.getVersion());
    }
    
    public void testMiddleVersion() throws Exception {
        _simppMessageNumber = MIDDLE;
        changeSimppFile();
        SimppManager man = SimppManager.instance();
        assertEquals("problem reading/verifying middle version file", 2,
                                                             man.getVersion());
    }
    
    public void testNewVersion() throws Exception {
        _simppMessageNumber = NEW;
        changeSimppFile();
        SimppManager man = SimppManager.instance();
        assertEquals("problem reading/verifying new version file", 3,
                                                            man.getVersion());
    }
    
    public void testBadSignatureFails() throws Exception {
        _simppMessageNumber = DEF_SIGNATURE;
        changeSimppFile();
        SimppManager man = SimppManager.instance();
        assertEquals("bad signature accepted", 0, man.getVersion());
    }
    
    public void testBadMessageFails() throws Exception {
        _simppMessageNumber = DEF_MESSAGE;
        changeSimppFile();
        SimppManager man = SimppManager.instance();
        assertEquals("tampered message accepted", 0, man.getVersion());
    }
    
    public void testBadXMLFails() throws Exception {
        _simppMessageNumber = BAD_XML;
        changeSimppFile();
        SimppManager man = SimppManager.instance();
        assertEquals("malformed xml accepted", 0, man.getVersion());
    }
    
    public void testRandomBytesFails() throws Exception {
        _simppMessageNumber = RANDOM_BYTES;
        changeSimppFile();
        SimppManager man = SimppManager.instance();
        assertEquals("garbage bytes accepted", 0, man.getVersion());
    }

    public void testOlderSimppNotRequested() throws Exception {
        //1. Set up LimeWire
        _simppMessageNumber = MIDDLE;
        changeSimppFile();

        //2. Set up the TestConnection to have the old version, and expect to
        //not receive a simpprequest
        TestConnection conn = new TestConnection(OLD, false, false);//!expect, !respond
        conn.start();
        //6s = 2s * 3 (timeout in TestConnection == 2s)
        Thread.sleep(6000);//let messages be exchanged, 
        
        //3. let the test run and make sure state is OK, for this test this part
        //is just a formality, the real testing is on the TestConnection in step
        //2 above.
        SimppManager man = SimppManager.instance();
        assertEquals("SimppManager should not have updated", MIDDLE, 
                                                              man.getVersion());
        conn.killConnection();
    }
    
    public void testOlderSimppNotRequestedUnsolicitedAccepted() throws Exception {
        //1. Set up LimeWire 
        _simppMessageNumber = MIDDLE;
        changeSimppFile();

        //2. Set up the TestConnection to advertise same version, not expect a
        //SimppReq, and to send an unsolicited newer SimppResponse
        TestConnection conn = new TestConnection(NEW ,false, true, OLD);
        conn.start();
        //6s = 2s * 3 (timeout in TestConnection == 2s)
        Thread.sleep(6000);//let messages be exchanged, 
        
        //3. let the test run and make sure state is OK, 
        SimppManager man = SimppManager.instance();
        assertEquals("SimppManager should not have updated", NEW, 
                                                              man.getVersion());
        conn.killConnection();
    }

    public void testSameSimppNotRequested() throws Exception {
        //1. Set up LimeWire 
        _simppMessageNumber = MIDDLE;
        changeSimppFile();

        //2. Set up the TestConnection to advertise same version, not expect a
        //SimppReq, and to send an unsolicited same SimppResponse
        TestConnection conn = new TestConnection(MIDDLE,false, true);
        conn.start();
        //6s = 2s * 3 (timeout in TestConnection == 2s)
        Thread.sleep(6000);//let messages be exchanged, 

        //3. let the test run and make sure state is OK, 
        SimppManager man = SimppManager.instance();
        assertEquals("SimppManager should not have updated", MIDDLE, 
                                                              man.getVersion());
        conn.killConnection();
    }
    
    public void testNewSimppAdvOldActualRejected() throws Exception {
        //1. Set up LimeWire 
        _simppMessageNumber = MIDDLE;
        changeSimppFile();

        //2. Set up the TestConnection to advertise same version, not expect a
        //SimppReq, and to send an unsolicited older SimppResponse
        TestConnection conn = new TestConnection(MIDDLE,false, true, OLD);
        conn.start();
        
        //6s = 2s * 3 (timeout in TestConnection == 2s)
        Thread.sleep(6000);//let messages be exchanged, 

        //3. let the test run and make sure state is OK, 
        SimppManager man = SimppManager.instance();
        assertEquals("SimppManager should not have updated", MIDDLE, 
                                                              man.getVersion());
        conn.killConnection();
    }


    public void testNewerSimppRequested() throws Exception {
        //1. Set up limewire correctly
        _simppMessageNumber = MIDDLE;
        changeSimppFile();

        //2. Set up the test connection, to have the new version, and to expect
        //a simpp request from limewire
        TestConnection conn = new TestConnection(NEW, true, true);//expect, respond
        conn.start();
        
        Thread.sleep(6000);//let the message exchange take place

        //3. OK. LimeWire should have upgraded now. 
        SimppManager man = SimppManager.instance();
        assertEquals("Simpp manager did not update simpp version", 
                                                         NEW, man.getVersion());
        conn.killConnection();
    }

    public void testTamperedSimppSigRejected() throws Exception {
        //1. Set up limewire correctly
        _simppMessageNumber = MIDDLE;
        changeSimppFile();

        //2. Set up the test connection, to advertise the new version, and to
        //expect a simpp request from limewire and send a defective signature
        //msg
        TestConnection conn = new TestConnection(DEF_SIGNATURE, true, true, NEW);
        conn.start();
        
        Thread.sleep(6000);//let the message exchange take place

        //3. OK. LimeWire should have upgraded now. 
        SimppManager man = SimppManager.instance();
        assertEquals("Simpp manager did not update simpp version", 
                                                     MIDDLE, man.getVersion());
        conn.killConnection();
    }

    public void testTamperedSimppDataRejected() throws Exception  {
       //1. Set up limewire correctly
        _simppMessageNumber = MIDDLE;
        changeSimppFile();

        //2. Set up the test connection, to advertise the new version, and to
        //expect a simpp request from limewire and send a defective message msg
        TestConnection conn = new TestConnection(DEF_MESSAGE, true, true, NEW);
        conn.start();
        
        Thread.sleep(6000);//let the message exchange take place

        //3. OK. LimeWire should have upgraded now. 
        SimppManager man = SimppManager.instance();
        assertEquals("Simpp manager did not update simpp version", 
                                                     MIDDLE, man.getVersion());
        conn.killConnection();
    }

    public void testBadSimppXMLRejected() throws Exception  {
        //1. Set up limewire correctly
        _simppMessageNumber = MIDDLE;
        changeSimppFile();

        //2. Set up the test connection, to advertise the new version, and to
        //expect a simpp request from limewire and send a bad_xml msg
        TestConnection conn = new TestConnection(BAD_XML, true, true, NEW);
        conn.start();
        Thread.sleep(6000);//let the message exchange take place

        //3. OK. LimeWire should have upgraded now. 
        SimppManager man = SimppManager.instance();
        assertEquals("Simpp manager did not update simpp version", 
                                                     MIDDLE, man.getVersion());
        conn.killConnection();
    }

    public void testGargabeDataRejected() throws Exception {
        //1. Set up limewire correctly
        _simppMessageNumber = MIDDLE;
        changeSimppFile();

        //2. Set up the test connection, to advertise the new version, and to
        //expect a simpp request from limewire and send a garbage msg
        TestConnection conn = new TestConnection(RANDOM_BYTES, true, true, NEW);
        conn.start();
        Thread.sleep(6000);//let the message exchange take place

        //3. OK. LimeWire should have upgraded now. 
        SimppManager man = SimppManager.instance();
        assertEquals("Simpp manager did not update simpp version", 
                                                     MIDDLE, man.getVersion());
        conn.killConnection();
    }

    public void testSimppTakesEffect() throws Exception {        
        assertEquals("base case did not revert to defaults",4, 
                              UploadSettings.TEST_UPLOAD_SETTING.getValue());
        //1. Test that Simpp files read off disk take effect. 
        _simppMessageNumber = OLD;
        changeSimppFile();
        resetSettingmanager();
        Thread.sleep(1000);

        assertEquals("base case did not revert to defaults",12, 
                              UploadSettings.TEST_UPLOAD_SETTING.getValue());
        //2. Test that simpp messages read off the network take effect
        //Get a new message from a connection and make sure the value is changed
        TestConnection conn = new TestConnection(NEW, true, true);
        conn.start();

        Thread.sleep(6000);//let the message exchange take place

        assertEquals("test_upload setting not changed to simpp value", 15,
                                 UploadSettings.TEST_UPLOAD_SETTING.getValue());
    }

    public void testSimppSettingStaysBelowMax() throws Exception {
        assertEquals("base case did not revert to defaults", 4, 
                              UploadSettings.TEST_UPLOAD_SETTING.getValue());
        _simppMessageNumber = OLD;
        changeSimppFile();
        Thread.sleep(1000);
        
        resetSettingmanager();
        assertEquals("base case did not revert to defaults",12, 
                              UploadSettings.TEST_UPLOAD_SETTING.getValue());
        //2. Test that simpp messages read off the network take effect
        //Get a new message from a connection and make sure the value is changed
        TestConnection conn = new TestConnection(ABOVE_MAX, true, true, NEW);
        conn.start();
        Thread.sleep(6000);//let the message exchange take place

        assertEquals("test_upload setting not changed to simpp value", 12,
                                UploadSettings.TEST_UPLOAD_SETTING.getValue());
        
    }

    public void testSimppSettingStaysAboveMin() throws Exception {
        assertEquals("base case did not revert to defaults", 4, 
                              UploadSettings.TEST_UPLOAD_SETTING.getValue());
        _simppMessageNumber = OLD;
        changeSimppFile();
        Thread.sleep(1000);

        resetSettingmanager();
        assertEquals("base case did not revert to defaults",12, 
                              UploadSettings.TEST_UPLOAD_SETTING.getValue());
        //2. Test that simpp messages read off the network take effect
        //Get a new message from a connection and make sure the value is changed
        TestConnection conn = new TestConnection(BELOW_MIN, true, true, NEW);
        conn.start();
        
        Thread.sleep(6000);//let the message exchange take place
        
        assertEquals("test_upload setting not changed to simpp value", 12,
                                UploadSettings.TEST_UPLOAD_SETTING.getValue());
    }

    public void testIOXLeavesSimppUnchanged() throws Exception {
       //1. Set up limewire correctly
        _simppMessageNumber = MIDDLE;
        changeSimppFile();

        //2. Set up the test connection, to have the new version, and to expect
        //a simpp request from limewire, but then close the connection while
        //uploading the simpp message
        TestConnection conn = new TestConnection(NEW, true, true);
        conn.setCauseError(true);
        conn.start();

        Thread.sleep(6000);//let the message exchange take place

        //3. OK. LimeWire should have upgraded now. 
        SimppManager man = SimppManager.instance();
        assertEquals("Simpp manager did not update simpp version", 
                                                     MIDDLE, man.getVersion());
        conn.killConnection();
    }

    ////////////////////////////////private methods///////////////////////////
    
    private static void changeSimppFile() throws Exception {
        if(_simppMessageNumber == OLD) 
            CommonUtils.copy(OLD_SIMPP_FILE, _simppFile);
        else if(_simppMessageNumber == MIDDLE)
            CommonUtils.copy(MIDDLE_SIMPP_FILE, _simppFile);
        else if(_simppMessageNumber == NEW) 
            CommonUtils.copy(NEW_SIMPP_FILE, _simppFile);
        else if(_simppMessageNumber == BAD_XML)
            CommonUtils.copy(BAD_XML_FILE, _simppFile);
        else if(_simppMessageNumber == DEF_SIGNATURE)
            CommonUtils.copy(DEF_SIG_FILE, _simppFile);
        else if(_simppMessageNumber == DEF_MESSAGE)
            CommonUtils.copy(DEF_MESSAGE_FILE, _simppFile);
        else if(_simppMessageNumber == RANDOM_BYTES)
            CommonUtils.copy(RANDOM_BYTES_FILE, _simppFile);
        else 
            fail("simppMessageNumber value is illegal");
        
        PrivilegedAccessor.setValue(SimppManager.class, "INSTANCE", null);
        PrivilegedAccessor.setValue(CapabilitiesVM.class,"_instance", null);
        PrivilegedAccessor.setValue(SimppManager.class, "MIN_VERSION", 
                                    new Integer(0));//so we can use 1,2,3
        //reload the SimppManager and Capabilities VM
        SimppManager.instance();
        CapabilitiesVM.instance();
    }
    
    private static void resetSettingmanager() throws Exception {
        PrivilegedAccessor.setValue(SimppSettingsManager.class,
                                                          "INSTANCE", null);
        SimppSettingsManager.instance();
    }
    
}
