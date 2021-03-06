package com.limegroup.gnutella.altlocs;

import java.io.IOException;
import java.util.Iterator;
import java.util.StringTokenizer;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.http.HTTPHeaderValue;
import com.limegroup.gnutella.util.FixedSizeSortedSet;

/**
 * This class holds a collection of <tt>AlternateLocation</tt> instances,
 * providing type safety for alternate location data. 
 * <p>
 * @see AlternateLocation
 */
public class AlternateLocationCollection 
	implements HTTPHeaderValue {
	    
    private static final int MAX_SIZE = 100;
    
    public static final AlternateLocationCollection EMPTY;
    static {
        AlternateLocationCollection col = null;
        try {
            col = new EmptyCollection();
        } catch (IOException bad) {
            ErrorService.error(bad);
        }
        EMPTY = col;
    }

	/**
	 * This uses a <tt>FixedSizeSortedSet</tt> so that the highest * entry
     * inserted is removed when the limit is reached.  
     * <p>
     * LOCKING: obtain this' monitor when iterating. Note that all modifications
     * to LOCATIONS are synchronized on this.  
     *
     * LOCKING: Never grab the lock on AlternateLocationCollection.class if you
     * have this' monitor. If both locks are needed, always lock on
     * AlternateLocationCollection.class first, never the other way around.
     */
 
	private final FixedSizeSortedSet LOCATIONS=new FixedSizeSortedSet(MAX_SIZE);
	
        
    /**
     * SHA1 <tt>URN</tt> for this collection.
     */
	private final URN SHA1;
	
    /**
     * Factory constructor for creating a new 
     * <tt>AlternateLocationCollection</tt> for this <tt>URN</tt>.
     *
     * @param sha1 the SHA1 <tt>URN</tt> for this collection
     * @return a new <tt>AlternateLocationCollection</tt> instance for
     *  this SHA1
     */
	public static AlternateLocationCollection create(URN sha1) {
		return new AlternateLocationCollection(sha1);
	}

	/**
	 * Creates a new <tt>AlternateLocationCollection</tt> with all alternate
	 * locations contained in the given comma-delimited HTTP header value
	 * string.  The returned <tt>AlternateLocationCollection</tt> may be empty.
	 *
	 * @param value the HTTP header value containing alternate locations
	 * @return a new <tt>AlternateLocationCollection</tt> with any valid
	 *  <tt>AlternateLocation</tt>s from the HTTP string, or <tt>null</tt>
	 *  if no valid locations could be found
	 * @throws <tt>NullPointerException</tt> if <tt>value</tt> is <tt>null</tt>
	 * 
	 * Note: this method requires the full altloc syntax (including the SHA1 in it)
	 * In other words, you cannot use the httpStringValue() output as an input to 
	 * this method if you want to recreate the collection.  It seems to be used only
	 * in downloader.HeadRequester 
	 */
	public static AlternateLocationCollection 
		createCollectionFromHttpValue(final String value) {
		if(value == null) {
			throw new NullPointerException("cannot create an "+
                                           "AlternateLocationCollection "+
										   "from a null value");
		}
		StringTokenizer st = new StringTokenizer(value, ",");
		AlternateLocationCollection alc = null;
		while(st.hasMoreTokens()) {
			String curTok = st.nextToken();
			try {
				AlternateLocation al = AlternateLocation.create(curTok);
				if(alc == null)
					alc = new AlternateLocationCollection(al.getSHA1Urn());

				if(al.getSHA1Urn().equals(alc.getSHA1Urn()))
					alc.add(al);
			} catch(IOException e) {
				continue;
			}
		}
		return alc;
	}

	/**
	 * Creates a new <tt>AlternateLocationCollection</tt> for the specified
	 * <tt>URN</tt>.
	 *
	 * @param sha1 the SHA1 <tt>URN</tt> for this alternate location collection
	 */
	private AlternateLocationCollection(URN sha1) {
		if(sha1 == null)
			throw new NullPointerException("null URN");
		if( sha1 != null && !sha1.isSHA1())
			throw new IllegalArgumentException("URN must be a SHA1");
		SHA1 = sha1;
	}

	/**
	 * Returns the SHA1 for this AlternateLocationCollection.
	 */
	public URN getSHA1Urn() {
	    return SHA1;
	}

	/**
	 * Adds a new <tt>AlternateLocation</tt> to the list.  If the 
	 * alternate location  is already present in the collection,
	 * it's count will be incremented.  
     *
	 * Implements the <tt>AlternateLocationCollector</tt> interface.
	 *
	 * @param al the <tt>AlternateLocation</tt> to add 
     * 
     * @throws <tt>IllegalArgumentException</tt> if the
     * <tt>AlternateLocation</tt> being added does not have a SHA1 urn or if
     * the SHA1 urn does not match the urn  for this collection
	 * 
     * @return true if added, false otherwise.  
     */
	public boolean add(AlternateLocation al) {
		URN sha1 = al.getSHA1Urn();
		if(!sha1.equals(SHA1))
			throw new IllegalArgumentException("SHA1 does not match");
		
		synchronized(this) {
            AlternateLocation alt = (AlternateLocation)LOCATIONS.get(al);
            boolean ret = false;
            if(alt==null) {//it was not in collections.
                ret = true;
                LOCATIONS.add(al);
            } else {
                LOCATIONS.remove(alt);

                alt.increment();
                alt.promote();
                alt.resetSent();
                ret =  false;
                LOCATIONS.add(alt); //add incremented version

            }
            return ret;
        }
    }
	
	        
	/**
	 * Removes this <tt>AlternateLocation</tt> from the active locations
	 * and adds it to the removed locations.
	 */
	 public boolean remove(AlternateLocation al) {
	    URN sha1 = al.getSHA1Urn();
        if(!sha1.equals(SHA1)) 
			return false; //it cannot be in this list if it has a different SHA1
		
		synchronized(this) {
            AlternateLocation loc = (AlternateLocation)LOCATIONS.get(al);
            if(loc==null) //it's not in locations, cannot remove
                return false;
            if(loc.isDemoted()) {//if its demoted remove it
                LOCATIONS.remove(loc);
                return true;         
            } else {
                LOCATIONS.remove(loc);

                loc.demote(); //one more strike and you are out...
                LOCATIONS.add(loc); //make it replace the older loc

                return false;
            }
		}
    }

    public synchronized void clear() {
        LOCATIONS.clear();
    }

	// implements the AlternateLocationCollector interface
	public synchronized boolean hasAlternateLocations() {
		return !LOCATIONS.isEmpty();
	}

    /**
     * @return true is this contains loc
     */
    public synchronized boolean contains(AlternateLocation loc) {
        return LOCATIONS.contains(loc);
    }
        
	/**
	 * Implements the <tt>HTTPHeaderValue</tt> interface.
	 *
	 * @return an HTTP-compliant string of alternate locations, delimited
	 *  by commas, or the empty string if there are no alternate locations
	 *  to report
	 */	
	public String httpStringValue() {
		final String commaSpace = ", "; 
		StringBuffer writeBuffer = new StringBuffer();
		boolean wrote = false;
        synchronized(this) {
	        Iterator iter = LOCATIONS.iterator();
            while(iter.hasNext()) {
            	AlternateLocation current = (AlternateLocation)iter.next();
			    writeBuffer.append(
                           current.httpStringValue());
			    writeBuffer.append(commaSpace);
			    wrote = true;
			}
		}
		
		// Truncate the last comma from the buffer.
		// This is arguably quicker than rechecking hasNext on the iterator.
		if ( wrote )
		    writeBuffer.setLength(writeBuffer.length()-2);
        
		return writeBuffer.toString();
	}
	

    // Implements AlternateLocationCollector interface -- 
    // inherit doc comment
	public synchronized int getAltLocsSize() { 
		return LOCATIONS.size();
    }
    
    public Iterator iterator() {
        return LOCATIONS.iterator();
    }

	/**
	 * Overrides Object.toString to print out all of the alternate locations
	 * for this collection of alternate locations.
	 *
	 * @return the string representation of all alternate locations in 
	 *  this collection
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Alternate Locations: ");
		synchronized(this) {
			Iterator iter = LOCATIONS.iterator();
			while(iter.hasNext()) {
				AlternateLocation curLoc = (AlternateLocation)iter.next();
				sb.append(curLoc.toString());
				sb.append("\n");
			}
		}
		return sb.toString();
	}

    
    public boolean equals(Object o) {
        if(o == this) return true;
        if(!(o instanceof AlternateLocationCollection))
            return false;
        AlternateLocationCollection alc = (AlternateLocationCollection)o;
        boolean ret = SHA1.equals(alc.SHA1);
        if ( !ret )
            return false;
        // This must be synchronized on both LOCATIONS and alc.LOCATIONS
        // because we not using the SynchronizedMap versions, and equals
        // will inherently call methods that would have been synchronized.
        synchronized(AlternateLocationCollection.class) {
            synchronized(this) {
                synchronized(alc) {
                    ret = LOCATIONS.equals(alc.LOCATIONS);
                }
            }
        }
        return ret;
    }
      
    private static class EmptyCollection extends AlternateLocationCollection {
        EmptyCollection() throws IOException {
            super(URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB"));
        }
        
        public boolean add(AlternateLocation loc) {
            throw new UnsupportedOperationException();
        }
    }
}
