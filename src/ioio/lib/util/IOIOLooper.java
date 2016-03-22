// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.util;

import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.api.IOIO;

public interface IOIOLooper
{
    void setup(final IOIO p0) throws ConnectionLostException, InterruptedException;
    
    void loop() throws ConnectionLostException, InterruptedException;
    
    void disconnected();
    
    void incompatible();
}
