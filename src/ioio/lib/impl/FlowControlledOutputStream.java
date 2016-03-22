// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.impl;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.io.OutputStream;

class FlowControlledOutputStream extends OutputStream
{
    private final Sender sender_;
    private final BlockingQueue<Byte> queue_;
    private final FlushThread thread_;
    private final int maxPacket_;
    private final byte[] packet_;
    private int readyToSend_;
    private boolean closed_;
    
    public FlowControlledOutputStream(final Sender sender, final int maxPacket) {
        this.queue_ = new ArrayBlockingQueue<Byte>(1024);
        this.thread_ = new FlushThread();
        this.readyToSend_ = 0;
        this.closed_ = false;
        this.sender_ = sender;
        this.maxPacket_ = maxPacket;
        this.packet_ = new byte[maxPacket];
        this.thread_.start();
    }
    
    @Override
    public synchronized void flush() throws IOException {
        try {
            while (!this.closed_) {
                if (this.queue_.isEmpty()) {
                    break;
                }
                this.wait();
            }
        }
        catch (InterruptedException e) {
            throw new IOException("Interrupted");
        }
        if (this.closed_) {
            throw new IOException("Stream has been closed");
        }
    }
    
    @Override
    public synchronized void write(final int oneByte) throws IOException {
        try {
            while (!this.closed_) {
                if (this.queue_.offer((byte)oneByte)) {
                    break;
                }
                this.wait();
            }
        }
        catch (InterruptedException e) {
            throw new IOException("Interrupted");
        }
        if (this.closed_) {
            throw new IOException("Stream has been closed");
        }
        this.notifyAll();
    }
    
    public synchronized void readyToSend(final int numBytes) {
        this.readyToSend_ += numBytes;
        this.notifyAll();
    }
    
    @Override
    public synchronized void close() {
        if (this.closed_) {
            return;
        }
        this.closed_ = true;
        this.notifyAll();
        this.thread_.interrupt();
    }
    
    static /* synthetic */ void access$4(final FlowControlledOutputStream flowControlledOutputStream, final int readyToSend_) {
        flowControlledOutputStream.readyToSend_ = readyToSend_;
    }
    
    class FlushThread extends Thread
    {
        @Override
        public void run() {
            super.run();
            try {
                while (true) {
                    final int toSend;
                    synchronized (FlowControlledOutputStream.this) {
                        while (FlowControlledOutputStream.this.readyToSend_ == 0 || FlowControlledOutputStream.this.queue_.isEmpty()) {
                            FlowControlledOutputStream.this.wait();
                        }
                        toSend = Math.min(FlowControlledOutputStream.this.maxPacket_, Math.min(FlowControlledOutputStream.this.readyToSend_, FlowControlledOutputStream.this.queue_.size()));
                        for (int i = 0; i < toSend; ++i) {
                            FlowControlledOutputStream.this.packet_[i] = (byte)FlowControlledOutputStream.this.queue_.remove();
                        }
                        final FlowControlledOutputStream this$0 = FlowControlledOutputStream.this;
                        FlowControlledOutputStream.access$4(this$0, this$0.readyToSend_ - toSend);
                        FlowControlledOutputStream.this.notifyAll();
                    }
                    // monitorexit(this.this$0)
                    FlowControlledOutputStream.this.sender_.send(FlowControlledOutputStream.this.packet_, toSend);
                }
            }
            catch (InterruptedException ex) {}
        }
    }
    
    interface Sender
    {
        void send(final byte[] p0, final int p1);
    }
}
