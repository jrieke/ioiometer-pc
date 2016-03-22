// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.impl;

import ioio.lib.api.exception.ConnectionLostException;

abstract class AbstractPin extends AbstractResource
{
    protected final int pinNum_;
    
    AbstractPin(final IOIOImpl ioio, final int pinNum) throws ConnectionLostException {
        super(ioio);
        this.pinNum_ = pinNum;
    }
    
    @Override
    public synchronized void close() {
        super.close();
        this.ioio_.closePin(this.pinNum_);
    }
}
