// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.impl;

import java.io.IOException;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.api.PwmOutput;

class PwmImpl extends AbstractResource implements PwmOutput
{
    private final int pwmNum_;
    private final int pinNum_;
    private final float baseUs_;
    private final int period_;
    
    public PwmImpl(final IOIOImpl ioio, final int pinNum, final int pwmNum, final int period, final float baseUs) throws ConnectionLostException {
        super(ioio);
        this.pwmNum_ = pwmNum;
        this.pinNum_ = pinNum;
        this.baseUs_ = baseUs;
        this.period_ = period;
    }
    
    @Override
    public synchronized void close() {
        super.close();
        this.ioio_.closePwm(this.pwmNum_);
        this.ioio_.closePin(this.pinNum_);
    }
    
    @Override
    public void setDutyCycle(final float dutyCycle) throws ConnectionLostException {
        assert dutyCycle <= 1.0f && dutyCycle >= 0.0f;
        this.setPulseWidthInClocks(this.period_ * dutyCycle);
    }
    
    @Override
    public void setPulseWidth(final int pulseWidthUs) throws ConnectionLostException {
        this.setPulseWidth((float)pulseWidthUs);
    }
    
    @Override
    public void setPulseWidth(final float pulseWidthUs) throws ConnectionLostException {
        assert pulseWidthUs >= 0.0f;
        final float p = pulseWidthUs / this.baseUs_;
        this.setPulseWidthInClocks(p);
    }
    
    private synchronized void setPulseWidthInClocks(float p) throws ConnectionLostException {
        this.checkState();
        if (p > this.period_) {
            p = this.period_;
        }
        --p;
        int pw;
        int fraction;
        if (p < 1.0f) {
            pw = 0;
            fraction = 0;
        }
        else {
            pw = (int)p;
            fraction = ((int)p * 4 & 0x3);
        }
        try {
            this.ioio_.protocol_.setPwmDutyCycle(this.pwmNum_, pw, fraction);
        }
        catch (IOException e) {
            throw new ConnectionLostException(e);
        }
    }
}
