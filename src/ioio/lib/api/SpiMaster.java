// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.api;

import ioio.lib.api.exception.ConnectionLostException;

public interface SpiMaster extends Closeable
{
    void writeRead(final int p0, final byte[] p1, final int p2, final int p3, final byte[] p4, final int p5) throws ConnectionLostException, InterruptedException;
    
    void writeRead(final byte[] p0, final int p1, final int p2, final byte[] p3, final int p4) throws ConnectionLostException, InterruptedException;
    
    Result writeReadAsync(final int p0, final byte[] p1, final int p2, final int p3, final byte[] p4, final int p5) throws ConnectionLostException;
    
    public enum Rate
    {
        RATE_31K("RATE_31K", 0), 
        RATE_35K("RATE_35K", 1), 
        RATE_41K("RATE_41K", 2), 
        RATE_50K("RATE_50K", 3), 
        RATE_62K("RATE_62K", 4), 
        RATE_83K("RATE_83K", 5), 
        RATE_125K("RATE_125K", 6), 
        RATE_142K("RATE_142K", 7), 
        RATE_166K("RATE_166K", 8), 
        RATE_200K("RATE_200K", 9), 
        RATE_250K("RATE_250K", 10), 
        RATE_333K("RATE_333K", 11), 
        RATE_500K("RATE_500K", 12), 
        RATE_571K("RATE_571K", 13), 
        RATE_666K("RATE_666K", 14), 
        RATE_800K("RATE_800K", 15), 
        RATE_1M("RATE_1M", 16), 
        RATE_1_3M("RATE_1_3M", 17), 
        RATE_2M("RATE_2M", 18), 
        RATE_2_2M("RATE_2_2M", 19), 
        RATE_2_6M("RATE_2_6M", 20), 
        RATE_3_2M("RATE_3_2M", 21), 
        RATE_4M("RATE_4M", 22), 
        RATE_5_3M("RATE_5_3M", 23), 
        RATE_8M("RATE_8M", 24);
        
        private Rate(final String s, final int n) {
        }
    }
    
    public static class Config
    {
        public Rate rate;
        public boolean invertClk;
        public boolean sampleOnTrailing;
        
        public Config(final Rate rate, final boolean invertClk, final boolean sampleOnTrailing) {
            this.rate = rate;
            this.invertClk = invertClk;
            this.sampleOnTrailing = sampleOnTrailing;
        }
        
        public Config(final Rate rate) {
            this(rate, false, false);
        }
    }
    
    public interface Result
    {
        void waitReady() throws ConnectionLostException, InterruptedException;
    }
}
