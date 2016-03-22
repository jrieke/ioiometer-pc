// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.impl;

import ioio.lib.api.IOIOConnection;
import ioio.lib.spi.IOIOConnectionFactory;
import java.util.Collection;
import ioio.lib.spi.IOIOConnectionBootstrap;

public class SocketIOIOConnectionBootstrap implements IOIOConnectionBootstrap
{
    public static final int IOIO_PORT = 4545;
    
    @Override
    public void getFactories(final Collection<IOIOConnectionFactory> result) {
        result.add(new IOIOConnectionFactory() {
            private Integer port_ = new Integer(4545);
            
            @Override
            public String getType() {
                return SocketIOIOConnection.class.getCanonicalName();
            }
            
            @Override
            public Object getExtra() {
                return this.port_;
            }
            
            @Override
            public IOIOConnection createConnection() {
                return new SocketIOIOConnection(4545);
            }
        });
    }
}
