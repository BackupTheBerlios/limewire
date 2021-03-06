package com.limegroup.gnutella.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import junit.framework.Test;

public class FixedSizeSortedSetTest extends BaseTestCase {
    
    private FixedSizeSortedSet _set;

    public void setUp() {
        Comparator intComp = new Comparator() {
            public int compare(Object oa, Object ob) {
                Integer a = (Integer) oa;
                Integer b = (Integer) ob;
                return a.compareTo(b);
            }
            public boolean equals(Object o) {
                return o.equals(this);
                }
        };
        _set = new FixedSizeSortedSet(intComp, 9);        
    }

    public void tearDown() {
        _set = null;
    }


    public FixedSizeSortedSetTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(FixedSizeSortedSetTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testHighestElementThrownWhenFull() throws Exception { 
        for(int i=0; i<11; i++) {
            Integer I = new Integer(i);
            try {
                _set.add(I);
            } catch(Exception e) {
                System.out.println("mistake while adding:"+i);
            }
        }
        // make sure 8 was knocked off
        assertFalse("set removed wrong element",_set.contains(new Integer(8)));
        assertEquals("wrong highest",new Integer(10),_set.last());
        assertEquals("wrong lowest", new Integer(0),_set.first());
        //remove 10, make sure second highest was 7
        _set.remove(new Integer(10));
        assertEquals("wrong new highest",new Integer(7),_set.last());
        //does size work?
        assertEquals("Wrong size",8,_set.size());
    }
    
    public void testOrdering() throws Exception {
        for(int i=0; i<11; i++) { //add 0-10 knock off 8,9
            Integer I = new Integer(i);
            _set.add(I);
        }        
        _set.add(new Integer(8));//get rid of 10
        Iterator iter = _set.iterator();
        int j=0;
        while(iter.hasNext()) {            
            assertEquals("problem in ordering",iter.next(),new Integer(j));
            j++;
        }//end of while
        _set.clear();
        assertTrue("either clear is broken or isEmpty is",_set.isEmpty());
        List list = new ArrayList();
        for(int i=0; i<10;i++) 
            list.add(new Integer(i));
        _set.addAll(list);
        assertEquals("addAll broken", 9, _set.size());
    }

    public void testRepeatElementsNotReadded() {
        for(int i=0; i< 10; i++) {
            boolean added =  _set.add(new Integer(i)); //8 will be knocked off
            //knocking off 8 should still return true
            assertTrue("added method returned unexpected",added);
        }
        boolean add = _set.add(new Integer(9));
        assertFalse("re-added element added",add);
        add = _set.add(new Integer(10));
        assertTrue("new element not added properly",add);
    }

}
