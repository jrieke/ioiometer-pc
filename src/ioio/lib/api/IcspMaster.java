// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.api;

import ioio.lib.api.exception.ConnectionLostException;

public interface IcspMaster extends Closeable
{
    void enterProgramming() throws ConnectionLostException;
    
    void exitProgramming() throws ConnectionLostException;
    
    void executeInstruction(final int p0) throws ConnectionLostException;
    
    void readVisi() throws ConnectionLostException, InterruptedException;
    
    int waitVisiResult() throws ConnectionLostException, InterruptedException;
}
