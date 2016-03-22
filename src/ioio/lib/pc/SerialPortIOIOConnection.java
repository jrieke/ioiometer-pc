// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.pc;

import java.io.IOException;
import purejavacomm.CommPort;
import ioio.lib.api.exception.ConnectionLostException;
import purejavacomm.NoSuchPortException;
import purejavacomm.CommPortIdentifier;
import java.io.OutputStream;
import java.io.InputStream;
import purejavacomm.SerialPort;
import ioio.lib.api.IOIOConnection;

class SerialPortIOIOConnection implements IOIOConnection
{
    private boolean abort_;
    private final String name_;
    private SerialPort serialPort_;
    private InputStream inputStream_;
    private OutputStream outputStream_;
    
    public SerialPortIOIOConnection(final String name) {
        this.abort_ = false;
        this.name_ = name;
    }
    
    @Override
    public void waitForConnect() throws ConnectionLostException {
        while (!this.abort_) {
            try {
                final CommPortIdentifier identifier = CommPortIdentifier.getPortIdentifier(this.name_);
                final CommPort commPort = identifier.open(this.getClass().getName(), 1000);
                synchronized (this) {
                    if (!this.abort_) {
                        (this.serialPort_ = (SerialPort)commPort).enableReceiveThreshold(1);
                        this.serialPort_.enableReceiveTimeout(500);
                        this.serialPort_.setDTR(true);
                        Thread.sleep(100L);
                        this.inputStream_ = new GracefullyClosingInputStream(this.serialPort_.getInputStream());
                        this.outputStream_ = this.serialPort_.getOutputStream();
                        // monitorexit(this)
                        return;
                    }
                    continue;
                }
            }
            catch (NoSuchPortException e) {
                try {
                    Thread.sleep(1000L);
                }
                catch (InterruptedException ex) {}
            }
            catch (Exception e2) {
                if (this.serialPort_ == null) {
                    continue;
                }
                this.serialPort_.close();
            }
        }
        throw new ConnectionLostException();
    }
    
    @Override
    public synchronized void disconnect() {
        this.abort_ = true;
        if (this.serialPort_ != null) {
            try {
                this.inputStream_.close();
            }
            catch (IOException ex) {}
            this.serialPort_.close();
        }
    }
    
    @Override
    public InputStream getInputStream() throws ConnectionLostException {
        return this.inputStream_;
    }
    
    @Override
    public OutputStream getOutputStream() throws ConnectionLostException {
        return this.outputStream_;
    }
    
    @Override
    public boolean canClose() {
        return true;
    }
    
    private static class GracefullyClosingInputStream extends InputStream
    {
        private final InputStream underlying_;
        private boolean closed_;
        
        public GracefullyClosingInputStream(final InputStream is) {
            this.closed_ = false;
            this.underlying_ = is;
        }
        
        @Override
        public int read(final byte[] b) throws IOException {
            while (!this.closed_) {
                final int i = this.underlying_.read(b);
                if (i > 0) {
                    return i;
                }
            }
            return -1;
        }
        
        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            while (!this.closed_) {
                final int i = this.underlying_.read(b, off, len);
                if (i > 0) {
                    return i;
                }
            }
            return -1;
        }
        
        @Override
        public long skip(final long n) throws IOException {
            return this.underlying_.skip(n);
        }
        
        @Override
        public int available() throws IOException {
            return this.underlying_.available();
        }
        
        @Override
        public void close() throws IOException {
            this.closed_ = true;
            this.underlying_.close();
        }
        
        @Override
        public synchronized void mark(final int readlimit) {
            this.underlying_.mark(readlimit);
        }
        
        @Override
        public synchronized void reset() throws IOException {
            this.underlying_.reset();
        }
        
        @Override
        public boolean markSupported() {
            return this.underlying_.markSupported();
        }
        
        @Override
        public int read() throws IOException {
            while (!this.closed_) {
                final int i = this.underlying_.read();
                if (i >= 0) {
                    return i;
                }
            }
            return -1;
        }
    }
}
