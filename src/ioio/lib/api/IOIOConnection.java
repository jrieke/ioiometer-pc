// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.api;

import java.io.OutputStream;
import java.io.InputStream;
import ioio.lib.api.exception.ConnectionLostException;

public interface IOIOConnection
{
    void waitForConnect() throws ConnectionLostException;
    
    void disconnect();
    
    InputStream getInputStream() throws ConnectionLostException;
    
    OutputStream getOutputStream() throws ConnectionLostException;
    
    boolean canClose();
}
