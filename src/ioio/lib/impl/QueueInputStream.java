// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.impl;

import ioio.lib.spi.Log;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.Queue;
import java.io.InputStream;

class QueueInputStream extends InputStream
{
    private final Queue<Byte> queue_;
    private State state_;
    
    QueueInputStream() {
        this.queue_ = new ArrayBlockingQueue<Byte>(1024);
        this.state_ = State.OPEN;
    }
    
    @Override
    public synchronized int read() throws IOException {
        try {
            while (this.state_ == State.OPEN && this.queue_.isEmpty()) {
                this.wait();
            }
            if (this.state_ == State.KILLED) {
                throw new IOException("Stream has been closed");
            }
            if (this.state_ == State.CLOSED && this.queue_.isEmpty()) {
                return -1;
            }
            return this.queue_.remove() & 0xFF;
        }
        catch (InterruptedException e) {
            throw new IOException("Interrupted");
        }
    }
    
    @Override
    public synchronized int read(final byte[] b, int off, int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        try {
            do {
                this.wait();
            } while (this.state_ == State.OPEN && this.queue_.isEmpty());
            if (this.state_ == State.KILLED) {
                throw new IOException("Stream has been closed");
            }
            if (this.state_ == State.CLOSED && this.queue_.isEmpty()) {
                return -1;
            }
            if (len > this.queue_.size()) {
                len = this.queue_.size();
            }
            for (int i = 0; i < len; ++i) {
                b[off++] = this.queue_.remove();
            }
            return len;
        }
        catch (InterruptedException e) {
            throw new IOException("Interrupted");
        }
    }
    
    public synchronized void write(final byte[] data, final int size) {
        for (int i = 0; i < size; ++i) {
            if (this.queue_.size() == 1024) {
                Log.e("QueueInputStream", "Buffer overflow, discarding data");
                break;
            }
            this.queue_.add(data[i]);
        }
        this.notifyAll();
    }
    
    @Override
    public synchronized int available() throws IOException {
        return this.queue_.size();
    }
    
    @Override
    public synchronized void close() {
        if (this.state_ != State.OPEN) {
            return;
        }
        this.state_ = State.CLOSED;
        this.notifyAll();
    }
    
    public synchronized void kill() {
        if (this.state_ != State.OPEN) {
            return;
        }
        this.state_ = State.KILLED;
        this.notifyAll();
    }
    
    private enum State
    {
        OPEN("OPEN", 0), 
        CLOSED("CLOSED", 1), 
        KILLED("KILLED", 2);
        
        private State(final String s, final int n) {
        }
    }
}
