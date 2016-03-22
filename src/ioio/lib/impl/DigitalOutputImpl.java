// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.impl;

import java.io.IOException;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.api.DigitalOutput;

class DigitalOutputImpl extends AbstractPin implements DigitalOutput
{
    boolean value_;
    
    DigitalOutputImpl(final IOIOImpl ioio, final int pin, final boolean startValue) throws ConnectionLostException {
        super(ioio, pin);
        this.value_ = startValue;
    }
    
    @Override
    public synchronized void write(final boolean val) throws ConnectionLostException {
        this.checkState();
        if (val != this.value_) {
            try {
                this.ioio_.protocol_.setDigitalOutLevel(this.pinNum_, val);
                this.value_ = val;
            }
            catch (IOException e) {
                throw new ConnectionLostException(e);
            }
        }
    }
}
