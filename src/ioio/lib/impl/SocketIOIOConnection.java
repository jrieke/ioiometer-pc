// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.impl;

import java.io.OutputStream;
import java.io.InputStream;
import java.net.SocketException;
import java.io.IOException;
import ioio.lib.spi.Log;
import ioio.lib.api.exception.ConnectionLostException;
import java.net.Socket;
import java.net.ServerSocket;
import ioio.lib.api.IOIOConnection;

public class SocketIOIOConnection implements IOIOConnection
{
    private static final String TAG = "SocketIOIOConnection";
    private final int port_;
    private ServerSocket server_;
    private Socket socket_;
    private boolean disconnect_;
    private boolean server_owned_by_connect_;
    private boolean socket_owned_by_connect_;
    
    public SocketIOIOConnection(final int port) {
        this.server_ = null;
        this.socket_ = null;
        this.disconnect_ = false;
        this.server_owned_by_connect_ = true;
        this.socket_owned_by_connect_ = true;
        this.port_ = port;
    }
    
    @Override
    public void waitForConnect() throws ConnectionLostException {
        try {
            synchronized (this) {
                if (this.disconnect_) {
                    throw new ConnectionLostException();
                }
                Log.v("SocketIOIOConnection", "Creating server socket");
                this.server_ = new ServerSocket(this.port_);
                this.server_owned_by_connect_ = false;
            }
            Log.v("SocketIOIOConnection", "Waiting for TCP connection");
            this.socket_ = this.server_.accept();
            Log.v("SocketIOIOConnection", "TCP connected");
            synchronized (this) {
                if (this.disconnect_) {
                    this.socket_.close();
                    throw new ConnectionLostException();
                }
                this.socket_owned_by_connect_ = false;
            }
        }
        catch (IOException e2) {
            synchronized (this) {
                this.disconnect_ = true;
                if (this.server_owned_by_connect_ && this.server_ != null) {
                    try {
                        this.server_.close();
                    }
                    catch (IOException e1) {
                        Log.e("SocketIOIOConnection", "Unexpected exception", e1);
                    }
                }
                if (this.socket_owned_by_connect_ && this.socket_ != null) {
                    try {
                        this.socket_.close();
                    }
                    catch (IOException e1) {
                        Log.e("SocketIOIOConnection", "Unexpected exception", e1);
                    }
                }
                if (e2 instanceof SocketException && e2.getMessage().equals("Permission denied")) {
                    Log.e("SocketIOIOConnection", "Did you forget to declare uses-permission of android.permission.INTERNET?");
                }
                throw new ConnectionLostException(e2);
            }
        }
    }
    
    @Override
    public synchronized void disconnect() {
        if (this.disconnect_) {
            return;
        }
        Log.v("SocketIOIOConnection", "Client initiated disconnect");
        this.disconnect_ = true;
        if (!this.server_owned_by_connect_) {
            try {
                this.server_.close();
            }
            catch (IOException e1) {
                Log.e("SocketIOIOConnection", "Unexpected exception", e1);
            }
        }
        if (!this.socket_owned_by_connect_) {
            try {
                this.socket_.shutdownOutput();
            }
            catch (IOException ex) {}
        }
    }
    
    @Override
    public InputStream getInputStream() throws ConnectionLostException {
        try {
            return this.socket_.getInputStream();
        }
        catch (IOException e) {
            throw new ConnectionLostException(e);
        }
    }
    
    @Override
    public OutputStream getOutputStream() throws ConnectionLostException {
        try {
            return this.socket_.getOutputStream();
        }
        catch (IOException e) {
            throw new ConnectionLostException(e);
        }
    }
    
    @Override
    public boolean canClose() {
        return true;
    }
}
