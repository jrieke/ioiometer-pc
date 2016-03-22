// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.impl;

import java.io.IOException;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.api.AnalogInput;

class AnalogInputImpl extends AbstractPin implements AnalogInput, IncomingState.InputPinListener
{
    private int value_;
    private boolean valid_;
    short[] buffer_;
    int bufferSize_;
    int bufferCapacity_;
    int bufferReadCursor_;
    int bufferWriteCursor_;
    int bufferOverflowCount_;
    
    AnalogInputImpl(final IOIOImpl ioio, final int pin) throws ConnectionLostException {
        super(ioio, pin);
        this.valid_ = false;
        this.bufferOverflowCount_ = 0;
    }
    
    @Override
    public float getVoltage() throws InterruptedException, ConnectionLostException {
        return this.read() * this.getReference();
    }
    
    @Override
    public float getReference() {
        return 3.3f;
    }
    
    @Override
    public synchronized void setValue(final int value) {
        assert value >= 0 && value < 1024;
        this.value_ = value;
        if (!this.valid_) {
            this.valid_ = true;
            this.notifyAll();
        }
        this.bufferPush((short)value);
    }
    
    @Override
    public synchronized float read() throws InterruptedException, ConnectionLostException {
        this.checkState();
        while (!this.valid_ && this.state_ == State.OPEN) {
            this.wait();
        }
        this.checkState();
        return this.value_ / 1023.0f;
    }
    
    @Override
    public synchronized void disconnected() {
        super.disconnected();
        this.notifyAll();
    }
    
    @Override
    public synchronized void close() {
        super.close();
        try {
            this.ioio_.protocol_.setAnalogInSampling(this.pinNum_, false);
        }
        catch (IOException ex) {}
    }
    
    @Override
    public synchronized void setBuffer(final int capacity) throws ConnectionLostException {
        this.checkState();
        if (capacity <= 0) {
            this.buffer_ = null;
        }
        else {
            this.buffer_ = new short[capacity];
        }
        this.bufferCapacity_ = capacity;
        this.bufferSize_ = 0;
        this.bufferReadCursor_ = 0;
        this.bufferWriteCursor_ = 0;
        this.bufferOverflowCount_ = 0;
    }
    
    @Override
    public float readBuffered() throws InterruptedException, ConnectionLostException {
        this.checkState();
        return this.bufferPull() / 1023.0f;
    }
    
    @Override
    public float getVoltageBuffered() throws InterruptedException, ConnectionLostException {
        return this.readBuffered() * this.getReference();
    }
    
    private void bufferPush(final short value) {
        if (this.buffer_ == null) {
            return;
        }
        if (this.bufferSize_ == this.bufferCapacity_) {
            ++this.bufferOverflowCount_;
        }
        else {
            ++this.bufferSize_;
        }
        this.buffer_[this.bufferWriteCursor_++] = value;
        if (this.bufferWriteCursor_ == this.bufferCapacity_) {
            this.bufferWriteCursor_ = 0;
        }
        this.notifyAll();
    }
    
    private synchronized short bufferPull() throws InterruptedException, ConnectionLostException {
        if (this.buffer_ == null) {
            throw new IllegalStateException("Need to call setBuffer() before reading buffered values.");
        }
        while (this.bufferSize_ == 0 && this.state_ == State.OPEN) {
            this.wait();
        }
        this.checkState();
        final short result = this.buffer_[this.bufferReadCursor_++];
        if (this.bufferReadCursor_ == this.bufferCapacity_) {
            this.bufferReadCursor_ = 0;
        }
        --this.bufferSize_;
        return result;
    }
    
    @Override
    public int getOverflowCount() throws ConnectionLostException {
        return this.bufferOverflowCount_;
    }
    
    @Override
    public float getSampleRate() throws ConnectionLostException {
        return 1000.0f;
    }
    
    @Override
    public int available() throws ConnectionLostException {
        return this.bufferSize_;
    }
}
