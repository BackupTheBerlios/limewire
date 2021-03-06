package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.util.BaseTestCase;

import com.limegroup.gnutella.util.IntervalSet;
import com.limegroup.gnutella.util.PrivilegedAccessor;

import java.util.Random;
import java.util.NoSuchElementException;

public class RandomDownloadStrategyTest extends BaseTestCase {

    /** Smallest number that can be subtracted from 1.0f 
     * and get something different from 1.0f.  Note that
     * this differs slightly from the standard IEE 754
     * definition of EPSILON by using subtraction instead
     * of addition.
     */
    private static float EPSILON;
    static {
        float epsilon = 1.0f;
        while((1.0f-epsilon)!=1.0f) {
            EPSILON = epsilon;
            epsilon /= 2.0f;
        }
    }
    
    public RandomDownloadStrategyTest(String s) {
        super(s);
    }

    private long fileSize;

    private long blockSize;

    private TestPredeterminedRandom prng;

    private SelectionStrategy strategy;

    private IntervalSet availableBytes;

    public void setUp() throws Exception {
        fileSize = 12345;
        blockSize = 1234;
        prng = new TestPredeterminedRandom();
        strategy = createRandomStrategy(fileSize, prng);
        availableBytes = IntervalSet.createSingletonSet(0, fileSize - 1);
    }

    public void testSequentialLocations() throws Exception {
        // Set up prng so our ideal location is always the 5th block boundary
        prng.setLongs(new long[] {5, 5, 5, 5});
        // Set ints so that the strategy picks two chunks 
        // after idealLocation (two odd ints)
        // followed by two chunks before idealLocation (two even ints)
        prng.setInts(new int[] {-1,487137, -2, 65538});

        // Set the expected assignments
        Interval[] expectations = new Interval[4];
        expectations[0] = new Interval(5 * blockSize, 6 * blockSize - 1);
        expectations[1] = new Interval(6 * blockSize, 7 * blockSize - 1);
        expectations[2] = new Interval(4 * blockSize, 5 * blockSize - 1);
        expectations[3] = new Interval(3 * blockSize, 4 * blockSize - 1);
        
        // Test
        testAssignments(strategy, availableBytes, fileSize, blockSize,
                expectations);
    }

    /** Tests that an interval spanning idealLocation gets split in two */
    public void testSplitInterval() {
        // Break the neededBytes into 3 intervals
        availableBytes.delete(new Interval(fileSize/3));
        availableBytes.delete(new Interval((2*fileSize)/3));
        
        
        // Set things up to ask for a chunk bothe before and after
        // the middle of the file
        long[] idealLocations = new long[2];
        idealLocations[0] = (fileSize/2)/blockSize;
        idealLocations[1] = idealLocations[0];
        prng.setLongs(idealLocations);
        // Return something before the idealLocation, then
        // something after the idealLocation
        prng.setInts(new int[] {0,1});
        
        Interval assignment = strategy.pickAssignment(availableBytes, 
                availableBytes, blockSize);
        
        assertEquals("Failed to break Interval before idealLocation",
                new Interval((idealLocations[0]-1)*blockSize,
                        idealLocations[0]*blockSize-1), 
                assignment);
        
        assignment = strategy.pickAssignment(availableBytes, 
                availableBytes, blockSize);
        
        assertEquals("Failed to break Interval after idealLocation",
                new Interval(idealLocations[1]*blockSize,
                        (idealLocations[1]+1)*blockSize-1), 
                assignment);
    }
    
    public void testSingleByteChunk() throws Exception {
        // Remove two single bytes
        availableBytes.delete(new Interval(5 * blockSize + 1));
        availableBytes.delete(new Interval(6 * blockSize - 2));
        
        // Set up the random number generator so that
        // it tries both above and below our single byte
        prng.setLongs(new long[] {5,6});
        prng.setInts(new int[] {3,0}); //go forward, then go backward

        // We expect a single byte assignment
        Interval[] expectation = new Interval[2];
        expectation[0] = new Interval(5 * blockSize);
        expectation[1] = new Interval(6 * blockSize-1);

        testAssignments(strategy, availableBytes, fileSize, blockSize,
                expectation);
    }
    
    /** Test the case where the availableBytes
     * contains the Interval we return and makes
     * sure a new Interval is not created if not necessary.
     */
    public void testReturnOptimization() {
        availableBytes = IntervalSet.createSingletonSet(blockSize,2*blockSize-1);
        prng.setInt(1);
        prng.setLong(0);
        
        Interval assignment = strategy.pickAssignment(availableBytes, availableBytes, blockSize);
        assertTrue("Return optimization not working", availableBytes.getFirst() == assignment);
    }
    
    /** Test the case where the download is almost finished.
     *  There's only a sliver left.
     */
    public void testSliverDownload() {
        // Set ideal location before the sliver
        prng.setLong(1);
        availableBytes = IntervalSet.createSingletonSet(2475, 2483);
        Interval assignment = strategy.pickAssignment(availableBytes, availableBytes, blockSize);
        assertEquals("Only a sliver of the file left, and something other than the sliver"+
                "was returned", availableBytes.getFirst(), assignment);
        
        // Same thing, except the ideal location is after the sliver
        prng.setLong(8);
        availableBytes = IntervalSet.createSingletonSet(2475,2483);
        assignment = strategy.pickAssignment(availableBytes, availableBytes, blockSize);
        assertEquals("Only a sliver of the file left, and something other than the sliver"+
                "was returned", availableBytes.getFirst(), assignment);
    }
    
    
    /**
     * Test that various invalid inputs throw IllegalArgumentException.
     */
    public void testInvalidInputs() {
        // Try an invalid block size
        try {
            strategy.pickAssignment(availableBytes,
                    availableBytes, 0);
            fail("Failed to complain about invalid block size");
        } catch (IllegalArgumentException e) {
            // Wohoo!  Exception thrown... test passed... do nothing
        }

        // createSingletonSet might throw its own IllegalArgumentException
        // so create it outside of the try-catch
        IntervalSet badNeededBytes = IntervalSet.createSingletonSet(-5,10);
        // Try telling the strategy that we need some bytes
        // before the beginning of the file
        try {
            strategy.pickAssignment(availableBytes, badNeededBytes, blockSize);
            fail("Failed to complain about negative Intervals in neededBytes");
        } catch (IllegalArgumentException e) {
            // Wohoo!  Exception thrown... test passed... do nothing
        }

        badNeededBytes = IntervalSet.createSingletonSet(fileSize,fileSize);
        // Try telling the strategy that we need a byte after the end
        // of the file
        try {
            strategy.pickAssignment(availableBytes,
                    badNeededBytes, blockSize);
            fail("Failed to complain about neededBytes extending past the end of the file");
        } catch (IllegalArgumentException e) {
            // Wohoo!  Exception thrown... test passed... do nothing
        }
    }
    
    /** Make sure we throw NoSuchElement exception if there is nothing to
     * download.
     */
    public void testNoAvailableBytes() {
        availableBytes = new IntervalSet();
        prng.setInt(15);
        prng.setLong(2);
        try {
            strategy.pickAssignment(availableBytes,
                    IntervalSet.createSingletonSet(0,fileSize-1), blockSize);
        } catch (NoSuchElementException e) {
            // Wohoo!  Exception thrown... test passed
            return;
        }
        assertTrue("Failed to complain about no available bytes", false);
    }
    
    /////////////// helper methods ////////////////
    /**
     * A helper method that simulates chosing blocks to download and removes
     * them from availableBytes.
     * 
     * @param strategy
     * @param availableBytes
     * @param fileSize
     * @param blockSize
     * @param blockCount
     * @param expectedAssignments
     */
    private void testAssignments(SelectionStrategy strategy,
            IntervalSet availableBytes, long fileSize, long blockSize,
            Interval[] expectedAssignments) {
        for (int i = 0; i < expectedAssignments.length; i++) {
            Interval assignment = strategy.pickAssignment(availableBytes, 
                    IntervalSet.createSingletonSet(0, fileSize - 1), blockSize);
            availableBytes.delete(assignment);
            assertEquals("Wrong assignment for chunk #" + i,
                    expectedAssignments[i], assignment);
        }
    }

    private SelectionStrategy createRandomStrategy(long fileSize, Random rng)
            throws IllegalAccessException, NoSuchFieldException {
        RandomDownloadStrategy rds = new RandomDownloadStrategy(fileSize);
        PrivilegedAccessor.setValue(rds, "pseudoRandom", rng);
        return rds;
    }
}
