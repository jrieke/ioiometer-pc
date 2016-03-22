// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.api;

import ioio.lib.api.exception.ConnectionLostException;

public interface AnalogInput extends Closeable
{
    float getVoltage() throws InterruptedException, ConnectionLostException;
    
    float getReference();
    
    float read() throws InterruptedException, ConnectionLostException;
    
    void setBuffer(final int p0) throws ConnectionLostException;
    
    int getOverflowCount() throws ConnectionLostException;
    
    int available() throws ConnectionLostException;
    
    float readBuffered() throws InterruptedException, ConnectionLostException;
    
    float getVoltageBuffered() throws InterruptedException, ConnectionLostException;
    
    float getSampleRate() throws ConnectionLostException;
}
