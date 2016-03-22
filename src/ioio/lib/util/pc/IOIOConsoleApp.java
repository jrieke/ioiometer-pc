// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.util.pc;

import ioio.lib.util.IOIOLooperProvider;

public abstract class IOIOConsoleApp implements IOIOLooperProvider
{
    protected final void go(final String[] args) throws Exception {
        final IOIOPcApplicationHelper helper = new IOIOPcApplicationHelper(this);
        helper.start();
        try {
            this.run(args);
        }
        catch (Exception e) {
            throw e;
        }
        finally {
            helper.stop();
        }
        helper.stop();
    }
    
    protected abstract void run(final String[] p0) throws Exception;
}
