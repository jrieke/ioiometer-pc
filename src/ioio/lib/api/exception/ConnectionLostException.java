// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.api.exception;

public class ConnectionLostException extends Exception
{
    private static final long serialVersionUID = 7422862446246046772L;
    
    public ConnectionLostException(final Exception e) {
        super(e);
    }
    
    public ConnectionLostException() {
        super("Connection lost");
    }
}
