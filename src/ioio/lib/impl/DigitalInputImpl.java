// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.impl;

import java.io.IOException;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.api.DigitalInput;

class DigitalInputImpl extends AbstractPin implements DigitalInput, IncomingState.InputPinListener
{
    private boolean value_;
    private boolean valid_;
    
    DigitalInputImpl(final IOIOImpl ioio, final int pin) throws ConnectionLostException {
        super(ioio, pin);
        this.valid_ = false;
    }
    
    @Override
    public synchronized void setValue(final int value) {
        assert value == 1;
        this.value_ = (value == 1);
        if (!this.valid_) {
            this.valid_ = true;
        }
        this.notifyAll();
    }
    
    @Override
    public synchronized void waitForValue(final boolean value) throws InterruptedException, ConnectionLostException {
        this.checkState();
        while ((!this.valid_ || this.value_ != value) && this.state_ != State.DISCONNECTED) {
            this.wait();
        }
        this.checkState();
    }
    
    @Override
    public synchronized void close() {
        super.close();
        try {
            this.ioio_.protocol_.setChangeNotify(this.pinNum_, false);
        }
        catch (IOException ex) {}
    }
    
    @Override
    public synchronized boolean read() throws InterruptedException, ConnectionLostException {
        this.checkState();
        while (!this.valid_ && this.state_ != State.DISCONNECTED) {
            this.wait();
        }
        this.checkState();
        return this.value_;
    }
    
    @Override
    public synchronized void disconnected() {
        super.disconnected();
        this.notifyAll();
    }
}
