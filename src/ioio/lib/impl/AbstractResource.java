// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.impl;

import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.api.Closeable;

class AbstractResource implements Closeable, IncomingState.DisconnectListener
{
    protected State state_;
    protected final IOIOImpl ioio_;
    
    public AbstractResource(final IOIOImpl ioio) throws ConnectionLostException {
        this.state_ = State.OPEN;
        this.ioio_ = ioio;
    }
    
    @Override
    public synchronized void disconnected() {
        if (this.state_ != State.CLOSED) {
            this.state_ = State.DISCONNECTED;
        }
    }
    
    @Override
    public synchronized void close() {
        if (this.state_ == State.CLOSED) {
            throw new IllegalStateException("Trying to use a closed resouce");
        }
        if (this.state_ == State.DISCONNECTED) {
            this.state_ = State.CLOSED;
            return;
        }
        this.state_ = State.CLOSED;
        this.ioio_.removeDisconnectListener(this);
    }
    
    protected synchronized void checkState() throws ConnectionLostException {
        if (this.state_ == State.CLOSED) {
            throw new IllegalStateException("Trying to use a closed resouce");
        }
        if (this.state_ == State.DISCONNECTED) {
            throw new ConnectionLostException();
        }
    }
    
    enum State
    {
        OPEN("OPEN", 0), 
        CLOSED("CLOSED", 1), 
        DISCONNECTED("DISCONNECTED", 2);
        
        private State(final String s, final int n) {
        }
    }
}
