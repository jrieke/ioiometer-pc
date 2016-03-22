// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.impl;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

class FlowControlledPacketSender
{
    private final Sender sender_;
    private final BlockingQueue<Packet> queue_;
    private final FlushThread thread_;
    private int readyToSend_;
    private boolean closed_;
    
    public FlowControlledPacketSender(final Sender sender) {
        this.queue_ = new ArrayBlockingQueue<Packet>(256);
        this.thread_ = new FlushThread();
        this.readyToSend_ = 0;
        this.closed_ = false;
        this.sender_ = sender;
        this.thread_.start();
    }
    
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
            throw new IllegalStateException("Stream has been closed");
        }
    }
    
    public synchronized void write(final Packet packet) throws IOException {
        try {
            while (!this.closed_) {
                if (this.queue_.offer(packet)) {
                    break;
                }
                this.wait();
            }
        }
        catch (InterruptedException e) {
            throw new IOException("Interrupted");
        }
        if (this.closed_) {
            throw new IllegalStateException("Stream has been closed");
        }
        this.notifyAll();
    }
    
    public synchronized void readyToSend(final int numBytes) {
        this.readyToSend_ += numBytes;
        this.notifyAll();
    }
    
    public synchronized void close() {
        this.closed_ = true;
        this.thread_.interrupt();
    }
    
    public synchronized void kill() {
        this.thread_.interrupt();
    }
    
    static /* synthetic */ void access$2(final FlowControlledPacketSender flowControlledPacketSender, final int readyToSend_) {
        flowControlledPacketSender.readyToSend_ = readyToSend_;
    }
    
    class FlushThread extends Thread
    {
        @Override
        public void run() {
            super.run();
            try {
                while (true) {
                    synchronized (FlowControlledPacketSender.this) {
                        while (FlowControlledPacketSender.this.queue_.isEmpty() || FlowControlledPacketSender.this.readyToSend_ < ((Packet)FlowControlledPacketSender.this.queue_.peek()).getSize()) {
                            FlowControlledPacketSender.this.wait();
                        }
                        FlowControlledPacketSender.this.notifyAll();
                        final FlowControlledPacketSender this$0 = FlowControlledPacketSender.this;
                        FlowControlledPacketSender.access$2(this$0, this$0.readyToSend_ - ((Packet)FlowControlledPacketSender.this.queue_.peek()).getSize());
                    }
                    // monitorexit(this.this$0)
                    FlowControlledPacketSender.this.sender_.send((Packet)FlowControlledPacketSender.this.queue_.remove());
                }
            }
            catch (InterruptedException ex) {}
        }
    }
    
    interface Packet
    {
        int getSize();
    }
    
    interface Sender
    {
        void send(final Packet p0);
    }
}
