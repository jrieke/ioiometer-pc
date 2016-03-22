// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.util.pc;

import ioio.lib.util.IOIOLooperProvider;
import ioio.lib.util.IOIOConnectionRegistry;
import ioio.lib.util.IOIOConnectionManager;
import ioio.lib.util.IOIOBaseApplicationHelper;

public class IOIOPcApplicationHelper extends IOIOBaseApplicationHelper
{
    private final IOIOConnectionManager manager_;
    
    static {
        IOIOConnectionRegistry.addBootstraps(new String[] { "ioio.lib.pc.SerialPortIOIOConnectionBootstrap" });
    }
    
    public IOIOPcApplicationHelper(final IOIOLooperProvider provider) {
        super(provider);
        this.manager_ = new IOIOConnectionManager(this);
    }
    
    public void start() {
        this.manager_.start();
    }
    
    public void stop() {
        this.manager_.stop();
    }
}
