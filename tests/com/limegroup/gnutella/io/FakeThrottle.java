package com.limegroup.gnutella.io;

import java.nio.channels.SelectionKey;
import java.nio.channels.CancelledKeyException;

/** A fake throttle, for testing ThrottleWriter. */
public class FakeThrottle implements Throttle {
    private int available;
    private int interests = 0;
    private boolean didRequest;
    private boolean didRelease;
    
    public void interest(ThrottleListener writer) {
        interests++;
    }
    
    public int request() {
        didRequest = true;
        int av = available;
        available = 0;
        return av;
    }
    
    public void release(int amount) {
        didRelease = true;
        available += amount;
    }
    
    void setAvailable(int av) { available = av; }
    int getAvailable() { return available; }
    int interests() { return interests; }
    public boolean didRequest() { return didRequest; }
    public boolean didRelease() { return didRelease; }
    void clear() { available = 0; interests = 0; didRequest = false; didRelease = false; }
}
    
    