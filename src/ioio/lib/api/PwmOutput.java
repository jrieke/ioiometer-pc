// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.api;

import ioio.lib.api.exception.ConnectionLostException;

public interface PwmOutput extends Closeable
{
    void setDutyCycle(final float p0) throws ConnectionLostException;
    
    void setPulseWidth(final int p0) throws ConnectionLostException;
    
    void setPulseWidth(final float p0) throws ConnectionLostException;
}
