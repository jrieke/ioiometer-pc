// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.impl;

import java.io.IOException;
import ioio.lib.spi.Log;
import java.util.Iterator;
import ioio.lib.api.exception.ConnectionLostException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;
import ioio.lib.api.TwiMaster;

class TwiMasterImpl extends AbstractResource implements TwiMaster, IncomingState.DataModuleListener, FlowControlledPacketSender.Sender
{
    private final Queue<TwiResult> pendingRequests_;
    private final FlowControlledPacketSender outgoing_;
    private final int twiNum_;
    
    TwiMasterImpl(final IOIOImpl ioio, final int twiNum) throws ConnectionLostException {
        super(ioio);
        this.pendingRequests_ = new ConcurrentLinkedQueue<TwiResult>();
        this.outgoing_ = new FlowControlledPacketSender(this);
        this.twiNum_ = twiNum;
    }
    
    @Override
    public synchronized void disconnected() {
        super.disconnected();
        this.outgoing_.kill();
        for (final TwiResult tr : this.pendingRequests_) {
            synchronized (tr) {
                tr.notify();
            }
            // monitorexit(tr)
        }
    }
    
    @Override
    public boolean writeRead(final int address, final boolean tenBitAddr, final byte[] writeData, final int writeSize, final byte[] readData, final int readSize) throws ConnectionLostException, InterruptedException {
        final Result result = this.writeReadAsync(address, tenBitAddr, writeData, writeSize, readData, readSize);
        return result.waitReady();
    }
    
    @Override
    public Result writeReadAsync(final int address, final boolean tenBitAddr, final byte[] writeData, final int writeSize, final byte[] readData, final int readSize) throws ConnectionLostException {
        this.checkState();
        final TwiResult result = new TwiResult(readData);
        final OutgoingPacket p = new OutgoingPacket();
        p.writeSize_ = writeSize;
        p.writeData_ = writeData;
        p.tenBitAddr_ = tenBitAddr;
        p.readSize_ = readSize;
        p.addr_ = address;
        synchronized (this) {
            this.pendingRequests_.add(result);
            try {
                this.outgoing_.write(p);
            }
            catch (IOException e) {
                Log.e("SpiMasterImpl", "Exception caught", e);
            }
        }
        return result;
    }
    
    @Override
    public void dataReceived(final byte[] data, final int size) {
        final TwiResult result = this.pendingRequests_.remove();
        synchronized (result) {
            result.ready_ = true;
            result.success_ = (size != 255);
            if (result.success_ && size > 0) {
                System.arraycopy(data, 0, result.data_, 0, size);
            }
            result.notify();
        }
        // monitorexit(result)
    }
    
    @Override
    public void reportAdditionalBuffer(final int bytesRemaining) {
        this.outgoing_.readyToSend(bytesRemaining);
    }
    
    @Override
    public synchronized void close() {
        super.close();
        this.outgoing_.close();
        this.ioio_.closeTwi(this.twiNum_);
    }
    
    @Override
    public void send(final FlowControlledPacketSender.Packet packet) {
        final OutgoingPacket p = (OutgoingPacket)packet;
        try {
            this.ioio_.protocol_.i2cWriteRead(this.twiNum_, p.tenBitAddr_, p.addr_, p.writeSize_, p.readSize_, p.writeData_);
        }
        catch (IOException e) {
            Log.e("TwiImpl", "Caught exception", e);
        }
    }
    
    class TwiResult implements Result
    {
        boolean ready_;
        boolean success_;
        final byte[] data_;
        
        public TwiResult(final byte[] data) {
            this.ready_ = false;
            this.data_ = data;
        }
        
        @Override
        public synchronized boolean waitReady() throws ConnectionLostException, InterruptedException {
            while (!this.ready_ && TwiMasterImpl.this.state_ != State.DISCONNECTED) {
                this.wait();
            }
            TwiMasterImpl.this.checkState();
            return this.success_;
        }
    }
    
    class OutgoingPacket implements FlowControlledPacketSender.Packet
    {
        int writeSize_;
        byte[] writeData_;
        boolean tenBitAddr_;
        int addr_;
        int readSize_;
        
        @Override
        public int getSize() {
            return this.writeSize_ + 4;
        }
    }
}
