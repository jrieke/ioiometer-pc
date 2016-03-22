// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.impl;

import java.io.IOException;
import ioio.lib.api.exception.ConnectionLostException;
import java.util.LinkedList;
import java.util.Queue;
import ioio.lib.api.IcspMaster;

class IcspMasterImpl extends AbstractResource implements IcspMaster, IncomingState.DataModuleListener
{
    private Queue<Integer> resultQueue_;
    private int rxRemaining_;
    
    public IcspMasterImpl(final IOIOImpl ioio) throws ConnectionLostException {
        super(ioio);
        this.resultQueue_ = new LinkedList<Integer>();
        this.rxRemaining_ = 0;
    }
    
    @Override
    public synchronized void dataReceived(final byte[] data, final int size) {
        assert size == 2;
        final int result = byteToInt(data[1]) << 8 | byteToInt(data[0]);
        this.resultQueue_.add(result);
        this.notifyAll();
    }
    
    @Override
    public synchronized void reportAdditionalBuffer(final int bytesToAdd) {
        this.rxRemaining_ += bytesToAdd;
        this.notifyAll();
    }
    
    @Override
    public synchronized void enterProgramming() throws ConnectionLostException {
        this.checkState();
        try {
            this.ioio_.protocol_.icspEnter();
        }
        catch (IOException e) {
            throw new ConnectionLostException(e);
        }
    }
    
    @Override
    public synchronized void exitProgramming() throws ConnectionLostException {
        this.checkState();
        try {
            this.ioio_.protocol_.icspExit();
        }
        catch (IOException e) {
            throw new ConnectionLostException(e);
        }
    }
    
    @Override
    public synchronized void executeInstruction(final int instruction) throws ConnectionLostException {
        this.checkState();
        try {
            this.ioio_.protocol_.icspSix(instruction);
        }
        catch (IOException e) {
            throw new ConnectionLostException(e);
        }
    }
    
    @Override
    public synchronized void readVisi() throws ConnectionLostException, InterruptedException {
        this.checkState();
        while (this.rxRemaining_ < 2 && this.state_ == State.OPEN) {
            this.wait();
        }
        this.checkState();
        this.rxRemaining_ -= 2;
        try {
            this.ioio_.protocol_.icspRegout();
        }
        catch (IOException e) {
            throw new ConnectionLostException(e);
        }
    }
    
    @Override
    public synchronized void close() {
        super.close();
        this.ioio_.closeIcsp();
    }
    
    @Override
    public synchronized void disconnected() {
        super.disconnected();
        this.notifyAll();
    }
    
    private static int byteToInt(final byte b) {
        return b & 0xFF;
    }
    
    @Override
    public synchronized int waitVisiResult() throws ConnectionLostException, InterruptedException {
        this.checkState();
        while (this.resultQueue_.isEmpty() && this.state_ == State.OPEN) {
            this.wait();
        }
        this.checkState();
        return this.resultQueue_.remove();
    }
}
