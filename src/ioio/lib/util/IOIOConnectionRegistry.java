// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.util;

import ioio.lib.spi.NoRuntimeSupportException;
import ioio.lib.spi.Log;
import java.util.Iterator;
import ioio.lib.spi.IOIOConnectionFactory;
import java.util.LinkedList;
import ioio.lib.spi.IOIOConnectionBootstrap;
import java.util.Collection;

public class IOIOConnectionRegistry
{
    private static final String TAG = "IOIOConnectionRegistry";
    private static Collection<IOIOConnectionBootstrap> bootstraps_;
    
    static {
        IOIOConnectionRegistry.bootstraps_ = new LinkedList<IOIOConnectionBootstrap>();
    }
    
    public static Collection<IOIOConnectionFactory> getConnectionFactories() {
        final Collection<IOIOConnectionFactory> result = new LinkedList<IOIOConnectionFactory>();
        for (final IOIOConnectionBootstrap bootstrap : IOIOConnectionRegistry.bootstraps_) {
            bootstrap.getFactories(result);
        }
        return result;
    }
    
    public static Collection<IOIOConnectionBootstrap> getBootstraps() {
        return IOIOConnectionRegistry.bootstraps_;
    }
    
    public static void addBootstraps(final String[] classNames) {
        for (final String className : classNames) {
            addBootstrap(className);
        }
    }
    
    private static void addBootstrap(final String className) {
        try {
            final Class<? extends IOIOConnectionBootstrap> bootstrapClass = Class.forName(className).asSubclass(IOIOConnectionBootstrap.class);
            IOIOConnectionRegistry.bootstraps_.add((IOIOConnectionBootstrap)bootstrapClass.newInstance());
            Log.d("IOIOConnectionRegistry", "Successfully added bootstrap class: " + className);
        }
        catch (ClassNotFoundException e2) {
            Log.d("IOIOConnectionRegistry", "Bootstrap class not found: " + className + ". Not adding.");
        }
        catch (NoRuntimeSupportException e3) {
            Log.d("IOIOConnectionRegistry", "No runtime support for: " + className + ". Not adding.");
        }
        catch (Throwable e) {
            Log.e("IOIOConnectionRegistry", "Exception caught while attempting to initialize connection factory", e);
        }
    }
}
