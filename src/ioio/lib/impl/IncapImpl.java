// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.impl;

import ioio.lib.api.exception.ConnectionLostException;
import java.util.LinkedList;
import java.util.Queue;
import ioio.lib.api.PulseInput;

class IncapImpl extends AbstractPin implements IncomingState.DataModuleListener, PulseInput
{
    private static final int MAX_QUEUE_LEN = 32;
    private final PulseMode mode_;
    private final int incapNum_;
    private long lastDuration_;
    private final float timeBase_;
    private final boolean doublePrecision_;
    private boolean valid_;
    private Queue<Long> pulseQueue_;
    
    public IncapImpl(final IOIOImpl ioio, final PulseMode mode, final int incapNum, final int pin, final int clockRate, final int scale, final boolean doublePrecision) throws ConnectionLostException {
        super(ioio, pin);
        this.valid_ = false;
        this.pulseQueue_ = new LinkedList<Long>();
        this.mode_ = mode;
        this.incapNum_ = incapNum;
        this.timeBase_ = 1.0f / (scale * clockRate);
        this.doublePrecision_ = doublePrecision;
    }
    
    @Override
    public float getFrequency() throws InterruptedException, ConnectionLostException {
        if (this.mode_ != PulseMode.FREQ && this.mode_ != PulseMode.FREQ_SCALE_4 && this.mode_ != PulseMode.FREQ_SCALE_16) {
            throw new IllegalStateException("Cannot query frequency when module was not opened in frequency mode.");
        }
        return 1.0f / this.getDuration();
    }
    
    @Override
    public synchronized float getDuration() throws InterruptedException, ConnectionLostException {
        this.checkState();
        while (!this.valid_) {
            this.wait();
            this.checkState();
        }
        return this.timeBase_ * this.lastDuration_;
    }
    
    @Override
    public synchronized float waitPulseGetDuration() throws InterruptedException, ConnectionLostException {
        if (this.mode_ != PulseMode.POSITIVE && this.mode_ != PulseMode.NEGATIVE) {
            throw new IllegalStateException("Cannot wait for pulse when module was not opened in pulse mode.");
        }
        this.checkState();
        while (this.pulseQueue_.isEmpty() && this.state_ == State.OPEN) {
            this.wait();
        }
        this.checkState();
        return this.timeBase_ * this.pulseQueue_.remove();
    }
    
    @Override
    public synchronized void dataReceived(final byte[] data, final int size) {
        this.lastDuration_ = ByteArrayToLong(data, size);
        if (this.pulseQueue_.size() == 32) {
            this.pulseQueue_.remove();
        }
        this.pulseQueue_.add(this.lastDuration_);
        this.valid_ = true;
        this.notifyAll();
    }
    
    private static long ByteArrayToLong(final byte[] data, final int size) {
        long result = 0L;
        int i = size;
        while (i-- > 0) {
            result <<= 8;
            result |= (data[i] & 0xFF);
        }
        if (result == 0L) {
            result = 1 << size * 8;
        }
        return result;
    }
    
    @Override
    public synchronized void reportAdditionalBuffer(final int bytesToAdd) {
    }
    
    @Override
    public synchronized void close() {
        this.ioio_.closeIncap(this.incapNum_, this.doublePrecision_);
        super.close();
    }
    
    @Override
    public synchronized void disconnected() {
        this.notifyAll();
        super.disconnected();
    }
}
