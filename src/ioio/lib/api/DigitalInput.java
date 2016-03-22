// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.api;

import ioio.lib.api.exception.ConnectionLostException;

public interface DigitalInput extends Closeable
{
    boolean read() throws InterruptedException, ConnectionLostException;
    
    void waitForValue(final boolean p0) throws InterruptedException, ConnectionLostException;
    
    public static class Spec
    {
        public int pin;
        public Mode mode;
        
        public Spec(final int pin, final Mode mode) {
            this.pin = pin;
            this.mode = mode;
        }
        
        public Spec(final int pin) {
            this(pin, Mode.FLOATING);
        }
        
        public enum Mode
        {
            FLOATING("FLOATING", 0), 
            PULL_UP("PULL_UP", 1), 
            PULL_DOWN("PULL_DOWN", 2);
            
            private Mode(final String s, final int n) {
            }
        }
    }
}
