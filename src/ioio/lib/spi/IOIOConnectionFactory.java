// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.spi;

import ioio.lib.api.IOIOConnection;

public interface IOIOConnectionFactory
{
    String getType();
    
    Object getExtra();
    
    IOIOConnection createConnection();
}
