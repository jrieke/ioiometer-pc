// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.util;

import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.api.IOIO;

public class BaseIOIOLooper implements IOIOLooper
{
    protected IOIO ioio_;
    
    @Override
    public final void setup(final IOIO ioio) throws ConnectionLostException, InterruptedException {
        this.ioio_ = ioio;
        this.setup();
    }
    
    protected void setup() throws ConnectionLostException, InterruptedException {
    }
    
    @Override
    public void loop() throws ConnectionLostException, InterruptedException {
        Thread.sleep(20L);
    }
    
    @Override
    public void disconnected() {
    }
    
    @Override
    public void incompatible() {
    }
}
