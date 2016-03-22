// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.api;

import ioio.lib.impl.IOIOImpl;
import java.util.Collection;
import java.util.NoSuchElementException;
import ioio.lib.spi.Log;
import ioio.lib.spi.IOIOConnectionFactory;
import ioio.lib.util.IOIOConnectionRegistry;

public class IOIOFactory
{
    private static final String TAG = "IOIOFactory";
    
    public static IOIO create() {
        final Collection<IOIOConnectionFactory> factories = IOIOConnectionRegistry.getConnectionFactories();
        try {
            return create(factories.iterator().next().createConnection());
        }
        catch (NoSuchElementException e) {
            Log.e("IOIOFactory", "No connection is available. This shouldn't happen.");
            throw e;
        }
    }
    
    public static IOIO create(final IOIOConnection connection) {
        return new IOIOImpl(connection);
    }
}
