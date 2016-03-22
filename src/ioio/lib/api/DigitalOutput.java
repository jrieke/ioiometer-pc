// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.api;

import ioio.lib.api.exception.ConnectionLostException;

public interface DigitalOutput extends Closeable
{
    void write(final boolean p0) throws ConnectionLostException;
    
    public static class Spec
    {
        public int pin;
        public Mode mode;
        
        public Spec(final int pin, final Mode mode) {
            this.pin = pin;
            this.mode = mode;
        }
        
        public Spec(final int pin) {
            this(pin, Mode.NORMAL);
        }
        
        public enum Mode
        {
            NORMAL("NORMAL", 0), 
            OPEN_DRAIN("OPEN_DRAIN", 1);
            
            private Mode(final String s, final int n) {
            }
        }
    }
}
