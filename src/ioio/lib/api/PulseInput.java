// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.api;

import ioio.lib.api.exception.ConnectionLostException;

public interface PulseInput extends Closeable
{
    float getDuration() throws InterruptedException, ConnectionLostException;
    
    float waitPulseGetDuration() throws InterruptedException, ConnectionLostException;
    
    float getFrequency() throws InterruptedException, ConnectionLostException;
    
    public enum ClockRate
    {
        RATE_16MHz("RATE_16MHz", 0, 16000000), 
        RATE_2MHz("RATE_2MHz", 1, 2000000), 
        RATE_250KHz("RATE_250KHz", 2, 250000), 
        RATE_62KHz("RATE_62KHz", 3, 62500);
        
        public final int hertz;
        
        private ClockRate(final String s, final int n, final int h) {
            this.hertz = h;
        }
    }
    
    public enum PulseMode
    {
        POSITIVE("POSITIVE", 0, 1), 
        NEGATIVE("NEGATIVE", 1, 1), 
        FREQ("FREQ", 2, 1), 
        FREQ_SCALE_4("FREQ_SCALE_4", 3, 4), 
        FREQ_SCALE_16("FREQ_SCALE_16", 4, 16);
        
        public final int scaling;
        
        private PulseMode(final String s2, final int n, final int s) {
            this.scaling = s;
        }
    }
}
