// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.api;

import ioio.lib.api.exception.ConnectionLostException;

public interface TwiMaster extends Closeable
{
    boolean writeRead(final int p0, final boolean p1, final byte[] p2, final int p3, final byte[] p4, final int p5) throws ConnectionLostException, InterruptedException;
    
    Result writeReadAsync(final int p0, final boolean p1, final byte[] p2, final int p3, final byte[] p4, final int p5) throws ConnectionLostException;
    
    public enum Rate
    {
        RATE_100KHz("RATE_100KHz", 0), 
        RATE_400KHz("RATE_400KHz", 1), 
        RATE_1MHz("RATE_1MHz", 2);
        
        private Rate(final String s, final int n) {
        }
    }
    
    public interface Result
    {
        boolean waitReady() throws ConnectionLostException, InterruptedException;
    }
}
