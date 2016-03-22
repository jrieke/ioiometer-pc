// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.impl;

import java.io.IOException;
import ioio.lib.spi.Log;
import java.util.Iterator;
import ioio.lib.api.exception.ConnectionLostException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Map;
import java.util.Queue;
import ioio.lib.api.SpiMaster;

class SpiMasterImpl extends AbstractResource implements SpiMaster, IncomingState.DataModuleListener, FlowControlledPacketSender.Sender
{
    private final Queue<SpiResult> pendingRequests_;
    private final FlowControlledPacketSender outgoing_;
    private final int spiNum_;
    private final Map<Integer, Integer> ssPinToIndex_;
    private final int[] indexToSsPin_;
    private final int mosiPinNum_;
    private final int misoPinNum_;
    private final int clkPinNum_;
    
    SpiMasterImpl(final IOIOImpl ioio, final int spiNum, final int mosiPinNum, final int misoPinNum, final int clkPinNum, final int[] ssPins) throws ConnectionLostException {
        super(ioio);
        this.pendingRequests_ = new ConcurrentLinkedQueue<SpiResult>();
        this.outgoing_ = new FlowControlledPacketSender(this);
        this.spiNum_ = spiNum;
        this.mosiPinNum_ = mosiPinNum;
        this.misoPinNum_ = misoPinNum;
        this.clkPinNum_ = clkPinNum;
        this.indexToSsPin_ = ssPins.clone();
        this.ssPinToIndex_ = new HashMap<Integer, Integer>(ssPins.length);
        for (int i = 0; i < ssPins.length; ++i) {
            this.ssPinToIndex_.put(ssPins[i], i);
        }
    }
    
    @Override
    public synchronized void disconnected() {
        super.disconnected();
        this.outgoing_.kill();
        for (final SpiResult tr : this.pendingRequests_) {
            synchronized (tr) {
                tr.notify();
            }
            // monitorexit(tr)
        }
    }
    
    @Override
    public void writeRead(final int slave, final byte[] writeData, final int writeSize, final int totalSize, final byte[] readData, final int readSize) throws ConnectionLostException, InterruptedException {
        final Result result = this.writeReadAsync(slave, writeData, writeSize, totalSize, readData, readSize);
        result.waitReady();
    }
    
    @Override
    public SpiResult writeReadAsync(final int slave, final byte[] writeData, final int writeSize, final int totalSize, final byte[] readData, final int readSize) throws ConnectionLostException {
        this.checkState();
        final SpiResult result = new SpiResult(readData);
        final OutgoingPacket p = new OutgoingPacket();
        p.writeSize_ = writeSize;
        p.writeData_ = writeData;
        p.readSize_ = readSize;
        p.ssPin_ = this.indexToSsPin_[slave];
        p.totalSize_ = totalSize;
        Label_0104: {
            if (p.readSize_ > 0) {
                synchronized (this) {
                    this.pendingRequests_.add(result);
                    break Label_0104;
                }
            }
            result.ready_ = true;
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
    public void writeRead(final byte[] writeData, final int writeSize, final int totalSize, final byte[] readData, final int readSize) throws ConnectionLostException, InterruptedException {
        this.writeRead(0, writeData, writeSize, totalSize, readData, readSize);
    }
    
    @Override
    public void dataReceived(final byte[] data, final int size) {
        final SpiResult result = this.pendingRequests_.remove();
        synchronized (result) {
            result.ready_ = true;
            System.arraycopy(data, 0, result.data_, 0, size);
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
        this.ioio_.closeSpi(this.spiNum_);
        this.ioio_.closePin(this.mosiPinNum_);
        this.ioio_.closePin(this.misoPinNum_);
        this.ioio_.closePin(this.clkPinNum_);
        int[] indexToSsPin_;
        for (int length = (indexToSsPin_ = this.indexToSsPin_).length, i = 0; i < length; ++i) {
            final int pin = indexToSsPin_[i];
            this.ioio_.closePin(pin);
        }
    }
    
    @Override
    public void send(final FlowControlledPacketSender.Packet packet) {
        final OutgoingPacket p = (OutgoingPacket)packet;
        try {
            this.ioio_.protocol_.spiMasterRequest(this.spiNum_, p.ssPin_, p.writeData_, p.writeSize_, p.totalSize_, p.readSize_);
        }
        catch (IOException e) {
            Log.e("SpiImpl", "Caught exception", e);
        }
    }
    
    public class SpiResult implements Result
    {
        boolean ready_;
        final byte[] data_;
        
        SpiResult(final byte[] data) {
            this.data_ = data;
        }
        
        @Override
        public synchronized void waitReady() throws ConnectionLostException, InterruptedException {
            while (!this.ready_ && SpiMasterImpl.this.state_ != State.DISCONNECTED) {
                this.wait();
            }
            SpiMasterImpl.this.checkState();
        }
    }
    
    class OutgoingPacket implements FlowControlledPacketSender.Packet
    {
        int writeSize_;
        byte[] writeData_;
        int ssPin_;
        int readSize_;
        int totalSize_;
        
        @Override
        public int getSize() {
            return this.writeSize_ + 4;
        }
    }
}
