// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.impl;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;
import java.util.List;
import ioio.lib.spi.Log;
import java.util.Iterator;
import ioio.lib.api.exception.ConnectionLostException;
import java.util.HashSet;
import java.util.Set;

class IncomingState implements IOIOProtocol.IncomingHandler
{
    private static final String TAG = "IncomingState";
    private InputPinState[] intputPinStates_;
    private DataModuleState[] uartStates_;
    private DataModuleState[] twiStates_;
    private DataModuleState[] spiStates_;
    private DataModuleState[] incapStates_;
    private DataModuleState icspState_;
    private final Set<DisconnectListener> disconnectListeners_;
    private ConnectionState connection_;
    public String hardwareId_;
    public String bootloaderId_;
    public String firmwareId_;
    public Board board_;
    
    IncomingState() {
        this.disconnectListeners_ = new HashSet<DisconnectListener>();
        this.connection_ = ConnectionState.INIT;
    }
    
    public synchronized void waitConnectionEstablished() throws InterruptedException, ConnectionLostException {
        while (this.connection_ == ConnectionState.INIT) {
            this.wait();
        }
        if (this.connection_ == ConnectionState.DISCONNECTED) {
            throw new ConnectionLostException();
        }
    }
    
    public synchronized boolean waitForInterfaceSupport() throws InterruptedException, ConnectionLostException {
        if (this.connection_ == ConnectionState.INIT) {
            throw new IllegalStateException("Have to connect before waiting for interface support");
        }
        while (this.connection_ == ConnectionState.ESTABLISHED) {
            this.wait();
        }
        if (this.connection_ == ConnectionState.DISCONNECTED) {
            throw new ConnectionLostException();
        }
        return this.connection_ == ConnectionState.CONNECTED;
    }
    
    public synchronized void waitDisconnect() throws InterruptedException {
        while (this.connection_ != ConnectionState.DISCONNECTED) {
            this.wait();
        }
    }
    
    public void addInputPinListener(final int pin, final InputPinListener listener) {
        this.intputPinStates_[pin].pushListener(listener);
    }
    
    public void addUartListener(final int uartNum, final DataModuleListener listener) {
        this.uartStates_[uartNum].pushListener(listener);
    }
    
    public void addTwiListener(final int twiNum, final DataModuleListener listener) {
        this.twiStates_[twiNum].pushListener(listener);
    }
    
    public void addIncapListener(final int incapNum, final DataModuleListener listener) {
        this.incapStates_[incapNum].pushListener(listener);
    }
    
    public void addIcspListener(final DataModuleListener listener) {
        this.icspState_.pushListener(listener);
    }
    
    public void addSpiListener(final int spiNum, final DataModuleListener listener) {
        this.spiStates_[spiNum].pushListener(listener);
    }
    
    public synchronized void addDisconnectListener(final DisconnectListener listener) throws ConnectionLostException {
        this.checkNotDisconnected();
        this.disconnectListeners_.add(listener);
    }
    
    public synchronized void removeDisconnectListener(final DisconnectListener listener) {
        if (this.connection_ != ConnectionState.DISCONNECTED) {
            this.disconnectListeners_.remove(listener);
        }
    }
    
    @Override
    public void handleConnectionLost() {
        synchronized (this) {
            this.connection_ = ConnectionState.DISCONNECTED;
        }
        for (final DisconnectListener listener : this.disconnectListeners_) {
            listener.disconnected();
        }
        this.disconnectListeners_.clear();
        synchronized (this) {
            this.notifyAll();
        }
    }
    
    @Override
    public void handleSoftReset() {
        InputPinState[] intputPinStates_;
        for (int length = (intputPinStates_ = this.intputPinStates_).length, i = 0; i < length; ++i) {
            final InputPinState pinState = intputPinStates_[i];
            pinState.closeCurrentListener();
        }
        DataModuleState[] uartStates_;
        for (int length2 = (uartStates_ = this.uartStates_).length, j = 0; j < length2; ++j) {
            final DataModuleState uartState = uartStates_[j];
            uartState.closeCurrentListener();
        }
        DataModuleState[] twiStates_;
        for (int length3 = (twiStates_ = this.twiStates_).length, k = 0; k < length3; ++k) {
            final DataModuleState twiState = twiStates_[k];
            twiState.closeCurrentListener();
        }
        DataModuleState[] spiStates_;
        for (int length4 = (spiStates_ = this.spiStates_).length, l = 0; l < length4; ++l) {
            final DataModuleState spiState = spiStates_[l];
            spiState.closeCurrentListener();
        }
        DataModuleState[] incapStates_;
        for (int length5 = (incapStates_ = this.incapStates_).length, n = 0; n < length5; ++n) {
            final DataModuleState incapState = incapStates_[n];
            incapState.closeCurrentListener();
        }
        this.icspState_.closeCurrentListener();
    }
    
    @Override
    public synchronized void handleCheckInterfaceResponse(final boolean supported) {
        this.connection_ = (supported ? ConnectionState.CONNECTED : ConnectionState.UNSUPPORTED_IID);
        this.notifyAll();
    }
    
    @Override
    public void handleSetChangeNotify(final int pin, final boolean changeNotify) {
        if (changeNotify) {
            this.intputPinStates_[pin].openNextListener();
        }
        else {
            this.intputPinStates_[pin].closeCurrentListener();
        }
    }
    
    @Override
    public void handleRegisterPeriodicDigitalSampling(final int pin, final int freqScale) {
        assert false;
    }
    
    @Override
    public void handleAnalogPinStatus(final int pin, final boolean open) {
        if (open) {
            this.intputPinStates_[pin].openNextListener();
        }
        else {
            this.intputPinStates_[pin].closeCurrentListener();
        }
    }
    
    @Override
    public void handleUartData(final int uartNum, final int numBytes, final byte[] data) {
        this.uartStates_[uartNum].dataReceived(data, numBytes);
    }
    
    @Override
    public void handleUartOpen(final int uartNum) {
        this.uartStates_[uartNum].openNextListener();
    }
    
    @Override
    public void handleUartClose(final int uartNum) {
        this.uartStates_[uartNum].closeCurrentListener();
    }
    
    @Override
    public void handleSpiOpen(final int spiNum) {
        this.spiStates_[spiNum].openNextListener();
    }
    
    @Override
    public void handleSpiClose(final int spiNum) {
        this.spiStates_[spiNum].closeCurrentListener();
    }
    
    @Override
    public void handleI2cOpen(final int i2cNum) {
        this.twiStates_[i2cNum].openNextListener();
    }
    
    @Override
    public void handleI2cClose(final int i2cNum) {
        this.twiStates_[i2cNum].closeCurrentListener();
    }
    
    @Override
    public void handleIcspOpen() {
        this.icspState_.openNextListener();
    }
    
    @Override
    public void handleIcspClose() {
        this.icspState_.closeCurrentListener();
    }
    
    @Override
    public void handleEstablishConnection(final byte[] hardwareId, final byte[] bootloaderId, final byte[] firmwareId) {
        this.hardwareId_ = new String(hardwareId);
        this.bootloaderId_ = new String(bootloaderId);
        this.firmwareId_ = new String(firmwareId);
        Log.i("IncomingState", "IOIO Connection established. Hardware ID: " + this.hardwareId_ + " Bootloader ID: " + this.bootloaderId_ + " Firmware ID: " + this.firmwareId_);
        try {
            this.board_ = Board.valueOf(this.hardwareId_);
        }
        catch (IllegalArgumentException e) {
            Log.e("IncomingState", "Unknown board: " + this.hardwareId_);
        }
        if (this.board_ != null) {
            final Board.Hardware hw = this.board_.hardware;
            this.intputPinStates_ = new InputPinState[hw.numPins()];
            for (int i = 0; i < this.intputPinStates_.length; ++i) {
                this.intputPinStates_[i] = new InputPinState();
            }
            this.uartStates_ = new DataModuleState[hw.numUartModules()];
            for (int i = 0; i < this.uartStates_.length; ++i) {
                this.uartStates_[i] = new DataModuleState();
            }
            this.twiStates_ = new DataModuleState[hw.numTwiModules()];
            for (int i = 0; i < this.twiStates_.length; ++i) {
                this.twiStates_[i] = new DataModuleState();
            }
            this.spiStates_ = new DataModuleState[hw.numSpiModules()];
            for (int i = 0; i < this.spiStates_.length; ++i) {
                this.spiStates_[i] = new DataModuleState();
            }
            this.incapStates_ = new DataModuleState[2 * hw.incapDoubleModules().length + hw.incapSingleModules().length];
            for (int i = 0; i < this.incapStates_.length; ++i) {
                this.incapStates_[i] = new DataModuleState();
            }
            this.icspState_ = new DataModuleState();
        }
        synchronized (this) {
            this.connection_ = ConnectionState.ESTABLISHED;
            this.notifyAll();
        }
    }
    
    @Override
    public void handleUartReportTxStatus(final int uartNum, final int bytesRemaining) {
        this.uartStates_[uartNum].reportAdditionalBuffer(bytesRemaining);
    }
    
    @Override
    public void handleI2cReportTxStatus(final int i2cNum, final int bytesRemaining) {
        this.twiStates_[i2cNum].reportAdditionalBuffer(bytesRemaining);
    }
    
    @Override
    public void handleSpiData(final int spiNum, final int ssPin, final byte[] data, final int dataBytes) {
        this.spiStates_[spiNum].dataReceived(data, dataBytes);
    }
    
    @Override
    public void handleIcspReportRxStatus(final int bytesRemaining) {
        this.icspState_.reportAdditionalBuffer(bytesRemaining);
    }
    
    @Override
    public void handleReportDigitalInStatus(final int pin, final boolean level) {
        this.intputPinStates_[pin].setValue(level ? 1 : 0);
    }
    
    @Override
    public void handleReportPeriodicDigitalInStatus(final int frameNum, final boolean[] values) {
    }
    
    @Override
    public void handleReportAnalogInStatus(final List<Integer> pins, final List<Integer> values) {
        for (int i = 0; i < pins.size(); ++i) {
            this.intputPinStates_[pins.get(i)].setValue(values.get(i));
        }
    }
    
    @Override
    public void handleSpiReportTxStatus(final int spiNum, final int bytesRemaining) {
        this.spiStates_[spiNum].reportAdditionalBuffer(bytesRemaining);
    }
    
    @Override
    public void handleI2cResult(final int i2cNum, final int size, final byte[] data) {
        this.twiStates_[i2cNum].dataReceived(data, size);
    }
    
    @Override
    public void handleIncapReport(final int incapNum, final int size, final byte[] data) {
        this.incapStates_[incapNum].dataReceived(data, size);
    }
    
    @Override
    public void handleIncapClose(final int incapNum) {
        this.incapStates_[incapNum].closeCurrentListener();
    }
    
    @Override
    public void handleIncapOpen(final int incapNum) {
        this.incapStates_[incapNum].openNextListener();
    }
    
    @Override
    public void handleIcspResult(final int size, final byte[] data) {
        this.icspState_.dataReceived(data, size);
    }
    
    private void checkNotDisconnected() throws ConnectionLostException {
        if (this.connection_ == ConnectionState.DISCONNECTED) {
            throw new ConnectionLostException();
        }
    }
    
    enum ConnectionState
    {
        INIT("INIT", 0), 
        ESTABLISHED("ESTABLISHED", 1), 
        CONNECTED("CONNECTED", 2), 
        DISCONNECTED("DISCONNECTED", 3), 
        UNSUPPORTED_IID("UNSUPPORTED_IID", 4);
        
        private ConnectionState(final String s, final int n) {
        }
    }
    
    class InputPinState
    {
        private Queue<InputPinListener> listeners_;
        private boolean currentOpen_;
        
        InputPinState() {
            this.listeners_ = new ConcurrentLinkedQueue<InputPinListener>();
            this.currentOpen_ = false;
        }
        
        void pushListener(final InputPinListener listener) {
            this.listeners_.add(listener);
        }
        
        void closeCurrentListener() {
            if (this.currentOpen_) {
                this.currentOpen_ = false;
                this.listeners_.remove();
            }
        }
        
        void openNextListener() {
            assert !this.listeners_.isEmpty();
            if (!this.currentOpen_) {
                this.currentOpen_ = true;
            }
        }
        
        void setValue(final int v) {
            assert this.currentOpen_;
            this.listeners_.peek().setValue(v);
        }
    }
    
    class DataModuleState
    {
        private Queue<DataModuleListener> listeners_;
        private boolean currentOpen_;
        
        DataModuleState() {
            this.listeners_ = new ConcurrentLinkedQueue<DataModuleListener>();
            this.currentOpen_ = false;
        }
        
        void pushListener(final DataModuleListener listener) {
            this.listeners_.add(listener);
        }
        
        void closeCurrentListener() {
            if (this.currentOpen_) {
                this.currentOpen_ = false;
                this.listeners_.remove();
            }
        }
        
        void openNextListener() {
            assert !this.listeners_.isEmpty();
            if (!this.currentOpen_) {
                this.currentOpen_ = true;
            }
        }
        
        void dataReceived(final byte[] data, final int size) {
            assert this.currentOpen_;
            this.listeners_.peek().dataReceived(data, size);
        }
        
        public void reportAdditionalBuffer(final int bytesRemaining) {
            assert this.currentOpen_;
            this.listeners_.peek().reportAdditionalBuffer(bytesRemaining);
        }
    }
    
    interface DataModuleListener
    {
        void dataReceived(final byte[] p0, final int p1);
        
        void reportAdditionalBuffer(final int p0);
    }
    
    interface DisconnectListener
    {
        void disconnected();
    }
    
    interface InputPinListener
    {
        void setValue(final int p0);
    }
}
