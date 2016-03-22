// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.api;

import java.io.OutputStream;
import java.io.InputStream;

public interface Uart extends Closeable
{
    InputStream getInputStream();
    
    OutputStream getOutputStream();
    
    public enum Parity
    {
        NONE("NONE", 0), 
        EVEN("EVEN", 1), 
        ODD("ODD", 2);
        
        private Parity(final String s, final int n) {
        }
    }
    
    public enum StopBits
    {
        ONE("ONE", 0), 
        TWO("TWO", 1);
        
        private StopBits(final String s, final int n) {
        }
    }
}
