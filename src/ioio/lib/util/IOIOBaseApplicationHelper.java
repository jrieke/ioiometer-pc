// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.util;

import ioio.lib.api.exception.IncompatibilityException;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.spi.Log;
import ioio.lib.api.IOIOFactory;
import ioio.lib.api.IOIO;
import ioio.lib.spi.IOIOConnectionFactory;

public abstract class IOIOBaseApplicationHelper implements IOIOConnectionManager.IOIOConnectionThreadProvider
{
    private static final String TAG = "IOIOBaseApplicationHelper";
    protected final IOIOLooperProvider looperProvider_;
    
    public IOIOBaseApplicationHelper(final IOIOLooperProvider provider) {
        this.looperProvider_ = provider;
    }
    
    @Override
    public IOIOConnectionManager.Thread createThreadFromFactory(final IOIOConnectionFactory factory) {
        final IOIOLooper looper = this.looperProvider_.createIOIOLooper(factory.getType(), factory.getExtra());
        if (looper == null) {
            return null;
        }
        return new IOIOThread(looper, factory);
    }
    
    protected static class IOIOThread extends IOIOConnectionManager.Thread
    {
        protected IOIO ioio_;
        private boolean abort_;
        private boolean connected_;
        private final IOIOLooper looper_;
        private final IOIOConnectionFactory connectionFactory_;
        
        IOIOThread(final IOIOLooper looper, final IOIOConnectionFactory factory) {
            this.abort_ = false;
            this.connected_ = false;
            this.looper_ = looper;
            this.connectionFactory_ = factory;
        }
        
        @Override
        public final void run() {
            super.run();
            while (!this.abort_) {
                try {
                    synchronized (this) {
                        if (this.abort_) {
                            // monitorexit(this)
                            break;
                        }
                        this.ioio_ = IOIOFactory.create(this.connectionFactory_.createConnection());
                    }
                }
                catch (Exception e2) {
                    Log.e("IOIOBaseApplicationHelper", "Failed to create IOIO, aborting IOIOThread!");
                    return;
                }
                try {
                    this.ioio_.waitForConnect();
                    this.connected_ = true;
                    this.looper_.setup(this.ioio_);
                    while (!this.abort_) {
                        if (this.ioio_.getState() != IOIO.State.CONNECTED) {
                            break;
                        }
                        this.looper_.loop();
                    }
                }
                catch (ConnectionLostException ex) {}
                catch (InterruptedException e3) {
                    this.ioio_.disconnect();
                }
                catch (IncompatibilityException e) {
                    Log.e("IOIOBaseApplicationHelper", "Incompatible IOIO firmware", e);
                    this.looper_.incompatible();
                }
                catch (Exception e2) {
                    Log.e("IOIOBaseApplicationHelper", "Unexpected exception caught", e2);
                    this.ioio_.disconnect();
                }
                finally {
                    try {
                        this.ioio_.waitForDisconnect();
                    }
                    catch (InterruptedException ex2) {}
                    synchronized (this) {
                        this.ioio_ = null;
                    }
                    if (this.connected_) {
                        this.looper_.disconnected();
                        this.connected_ = false;
                    }
                }
                try {
                    this.ioio_.waitForDisconnect();
                }
                catch (InterruptedException ex3) {}
                synchronized (this) {
                    this.ioio_ = null;
                }
                if (this.connected_) {
                    this.looper_.disconnected();
                    this.connected_ = false;
                }
            }
            Log.d("IOIOBaseApplicationHelper", "IOIOThread is exiting");
        }
        
        @Override
        public final synchronized void abort() {
            this.abort_ = true;
            if (this.ioio_ != null) {
                this.ioio_.disconnect();
            }
            if (this.connected_) {
                this.interrupt();
            }
        }
    }
}
