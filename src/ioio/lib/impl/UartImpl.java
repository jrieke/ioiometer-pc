// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.impl;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;
import ioio.lib.spi.Log;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.api.Uart;

class UartImpl extends AbstractResource implements IncomingState.DataModuleListener, FlowControlledOutputStream.Sender, Uart
{
    private static final int MAX_PACKET = 64;
    private final int uartNum_;
    private final int rxPinNum_;
    private final int txPinNum_;
    private final FlowControlledOutputStream outgoing_;
    private final QueueInputStream incoming_;
    
    public UartImpl(final IOIOImpl ioio, final int txPin, final int rxPin, final int uartNum) throws ConnectionLostException {
        super(ioio);
        this.outgoing_ = new FlowControlledOutputStream(this, 64);
        this.incoming_ = new QueueInputStream();
        this.uartNum_ = uartNum;
        this.rxPinNum_ = rxPin;
        this.txPinNum_ = txPin;
    }
    
    @Override
    public void dataReceived(final byte[] data, final int size) {
        this.incoming_.write(data, size);
    }
    
    @Override
    public void send(final byte[] data, final int size) {
        try {
            this.ioio_.protocol_.uartData(this.uartNum_, size, data);
        }
        catch (IOException e) {
            Log.e("UartImpl", e.getMessage());
        }
    }
    
    @Override
    public synchronized void close() {
        super.close();
        this.incoming_.close();
        this.outgoing_.close();
        this.ioio_.closeUart(this.uartNum_);
        if (this.rxPinNum_ != -1) {
            this.ioio_.closePin(this.rxPinNum_);
        }
        if (this.txPinNum_ != -1) {
            this.ioio_.closePin(this.txPinNum_);
        }
    }
    
    @Override
    public synchronized void disconnected() {
        super.disconnected();
        this.incoming_.kill();
        this.outgoing_.close();
    }
    
    @Override
    public InputStream getInputStream() {
        return this.incoming_;
    }
    
    @Override
    public OutputStream getOutputStream() {
        return this.outgoing_;
    }
    
    @Override
    public void reportAdditionalBuffer(final int bytesRemaining) {
        this.outgoing_.readyToSend(bytesRemaining);
    }
}
