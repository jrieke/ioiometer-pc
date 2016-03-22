// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.impl;

import ioio.lib.api.PulseInput;
import ioio.lib.api.SpiMaster;
import ioio.lib.api.IcspMaster;
import ioio.lib.api.TwiMaster;
import ioio.lib.api.Uart;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.DigitalInput;
import java.io.IOException;
import ioio.lib.api.exception.IncompatibilityException;
import ioio.lib.spi.Log;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.api.IOIOConnection;
import ioio.lib.api.IOIO;

public class IOIOImpl implements IOIO, IncomingState.DisconnectListener
{
    private static final String TAG = "IOIOImpl";
    private boolean disconnect_;
    private static final byte[] REQUIRED_INTERFACE_ID;
    private IOIOConnection connection_;
    private IncomingState incomingState_;
    private boolean[] openPins_;
    private boolean[] openTwi_;
    private boolean openIcsp_;
    private ModuleAllocator pwmAllocator_;
    private ModuleAllocator uartAllocator_;
    private ModuleAllocator spiAllocator_;
    private ModuleAllocator incapAllocatorDouble_;
    private ModuleAllocator incapAllocatorSingle_;
    IOIOProtocol protocol_;
    private State state_;
    private Board.Hardware hardware_;
    
    static {
        REQUIRED_INTERFACE_ID = new byte[] { 73, 79, 73, 79, 48, 48, 48, 51 };
    }
    
    public IOIOImpl(final IOIOConnection con) {
        this.disconnect_ = false;
        this.incomingState_ = new IncomingState();
        this.state_ = State.INIT;
        this.connection_ = con;
    }
    
    @Override
    public void waitForConnect() throws ConnectionLostException, IncompatibilityException {
        if (this.state_ == State.CONNECTED) {
            return;
        }
        if (this.state_ == State.DEAD) {
            throw new ConnectionLostException();
        }
        this.addDisconnectListener(this);
        Log.d("IOIOImpl", "Waiting for IOIO connection");
        try {
            try {
                Log.v("IOIOImpl", "Waiting for underlying connection");
                this.connection_.waitForConnect();
                synchronized (this) {
                    if (this.disconnect_) {
                        throw new ConnectionLostException();
                    }
                    this.protocol_ = new IOIOProtocol(this.connection_.getInputStream(), this.connection_.getOutputStream(), this.incomingState_);
                }
            }
            catch (ConnectionLostException e) {
                this.incomingState_.handleConnectionLost();
                throw e;
            }
            Log.v("IOIOImpl", "Waiting for handshake");
            this.incomingState_.waitConnectionEstablished();
            this.initBoard();
            Log.v("IOIOImpl", "Querying for required interface ID");
            this.checkInterfaceVersion();
            Log.v("IOIOImpl", "Required interface ID is supported");
            this.state_ = State.CONNECTED;
            Log.i("IOIOImpl", "IOIO connection established");
        }
        catch (ConnectionLostException e) {
            Log.d("IOIOImpl", "Connection lost / aborted");
            this.state_ = State.DEAD;
            throw e;
        }
        catch (IncompatibilityException e2) {
            throw e2;
        }
        catch (InterruptedException e3) {
            Log.e("IOIOImpl", "Unexpected exception", e3);
        }
    }
    
    @Override
    public synchronized void disconnect() {
        Log.d("IOIOImpl", "Client requested disconnect.");
        if (this.disconnect_) {
            return;
        }
        this.disconnect_ = true;
        try {
            if (this.protocol_ != null && !this.connection_.canClose()) {
                this.protocol_.softClose();
            }
        }
        catch (IOException e) {
            Log.e("IOIOImpl", "Soft close failed", e);
        }
        this.connection_.disconnect();
    }
    
    @Override
    public synchronized void disconnected() {
        this.state_ = State.DEAD;
        if (this.disconnect_) {
            return;
        }
        Log.d("IOIOImpl", "Physical disconnect.");
        this.disconnect_ = true;
        this.connection_.disconnect();
    }
    
    @Override
    public void waitForDisconnect() throws InterruptedException {
        this.incomingState_.waitDisconnect();
    }
    
    @Override
    public State getState() {
        return this.state_;
    }
    
    private void initBoard() throws IncompatibilityException {
        if (this.incomingState_.board_ == null) {
            throw new IncompatibilityException("Unknown board: " + this.incomingState_.hardwareId_);
        }
        this.hardware_ = this.incomingState_.board_.hardware;
        this.openPins_ = new boolean[this.hardware_.numPins()];
        this.openTwi_ = new boolean[this.hardware_.numTwiModules()];
        this.openIcsp_ = false;
        this.pwmAllocator_ = new ModuleAllocator(this.hardware_.numPwmModules(), "PWM");
        this.uartAllocator_ = new ModuleAllocator(this.hardware_.numUartModules(), "UART");
        this.spiAllocator_ = new ModuleAllocator(this.hardware_.numSpiModules(), "SPI");
        this.incapAllocatorDouble_ = new ModuleAllocator(this.hardware_.incapDoubleModules(), "INCAP_DOUBLE");
        this.incapAllocatorSingle_ = new ModuleAllocator(this.hardware_.incapSingleModules(), "INCAP_SINGLE");
    }
    
    private void checkInterfaceVersion() throws IncompatibilityException, ConnectionLostException, InterruptedException {
        try {
            this.protocol_.checkInterface(IOIOImpl.REQUIRED_INTERFACE_ID);
        }
        catch (IOException e) {
            throw new ConnectionLostException(e);
        }
        if (!this.incomingState_.waitForInterfaceSupport()) {
            this.state_ = State.INCOMPATIBLE;
            Log.e("IOIOImpl", "Required interface ID is not supported");
            throw new IncompatibilityException("IOIO firmware does not support required firmware: " + new String(IOIOImpl.REQUIRED_INTERFACE_ID));
        }
    }
    
    synchronized void removeDisconnectListener(final IncomingState.DisconnectListener listener) {
        this.incomingState_.removeDisconnectListener(listener);
    }
    
    synchronized void addDisconnectListener(final IncomingState.DisconnectListener listener) throws ConnectionLostException {
        this.incomingState_.addDisconnectListener(listener);
    }
    
    synchronized void closePin(final int pin) {
        try {
            this.checkState();
            if (!this.openPins_[pin]) {
                throw new IllegalStateException("Pin not open: " + pin);
            }
            this.protocol_.setPinDigitalIn(pin, DigitalInput.Spec.Mode.FLOATING);
            this.openPins_[pin] = false;
        }
        catch (IOException ex) {}
        catch (ConnectionLostException ex2) {}
    }
    
    synchronized void closePwm(final int pwmNum) {
        try {
            this.checkState();
            this.pwmAllocator_.releaseModule(pwmNum);
            this.protocol_.setPwmPeriod(pwmNum, 0, IOIOProtocol.PwmScale.SCALE_1X);
        }
        catch (IOException ex) {}
        catch (ConnectionLostException ex2) {}
    }
    
    synchronized void closeUart(final int uartNum) {
        try {
            this.checkState();
            this.uartAllocator_.releaseModule(uartNum);
            this.protocol_.uartClose(uartNum);
        }
        catch (IOException ex) {}
        catch (ConnectionLostException ex2) {}
    }
    
    synchronized void closeTwi(final int twiNum) {
        try {
            this.checkState();
            if (!this.openTwi_[twiNum]) {
                throw new IllegalStateException("TWI not open: " + twiNum);
            }
            this.openTwi_[twiNum] = false;
            final int[][] twiPins = this.hardware_.twiPins();
            this.openPins_[twiPins[twiNum][0]] = false;
            this.openPins_[twiPins[twiNum][1]] = false;
            this.protocol_.i2cClose(twiNum);
        }
        catch (IOException ex) {}
        catch (ConnectionLostException ex2) {}
    }
    
    synchronized void closeIcsp() {
        try {
            this.checkState();
            if (!this.openIcsp_) {
                throw new IllegalStateException("ICSP not open");
            }
            this.openIcsp_ = false;
            final int[] icspPins = this.hardware_.icspPins();
            this.openPins_[icspPins[0]] = false;
            this.openPins_[icspPins[1]] = false;
            this.protocol_.icspClose();
        }
        catch (ConnectionLostException ex) {}
        catch (IOException ex2) {}
    }
    
    synchronized void closeSpi(final int spiNum) {
        try {
            this.checkState();
            this.spiAllocator_.releaseModule(spiNum);
            this.protocol_.spiClose(spiNum);
        }
        catch (IOException ex) {}
        catch (ConnectionLostException ex2) {}
    }
    
    synchronized void closeIncap(final int incapNum, final boolean doublePrecision) {
        try {
            this.checkState();
            if (doublePrecision) {
                this.incapAllocatorDouble_.releaseModule(incapNum);
            }
            else {
                this.incapAllocatorSingle_.releaseModule(incapNum);
            }
            this.protocol_.incapClose(incapNum, doublePrecision);
        }
        catch (IOException ex) {}
        catch (ConnectionLostException ex2) {}
    }
    
    @Override
    public synchronized void softReset() throws ConnectionLostException {
        this.checkState();
        try {
            this.protocol_.softReset();
        }
        catch (IOException e) {
            throw new ConnectionLostException(e);
        }
    }
    
    @Override
    public synchronized void hardReset() throws ConnectionLostException {
        this.checkState();
        try {
            this.protocol_.hardReset();
        }
        catch (IOException e) {
            throw new ConnectionLostException(e);
        }
    }
    
    @Override
    public String getImplVersion(final VersionType v) throws ConnectionLostException {
        if (this.state_ == State.INIT) {
            throw new IllegalStateException("Connection has not yet been established");
        }
        switch (v) {
            case HARDWARE_VER: {
                return this.incomingState_.hardwareId_;
            }
            case BOOTLOADER_VER: {
                return this.incomingState_.bootloaderId_;
            }
            case APP_FIRMWARE_VER: {
                return this.incomingState_.firmwareId_;
            }
            case IOIOLIB_VER: {
                return "IOIO0326";
            }
            default: {
                return null;
            }
        }
    }
    
    @Override
    public DigitalInput openDigitalInput(final int pin) throws ConnectionLostException {
        return this.openDigitalInput(new DigitalInput.Spec(pin));
    }
    
    @Override
    public DigitalInput openDigitalInput(final int pin, final DigitalInput.Spec.Mode mode) throws ConnectionLostException {
        return this.openDigitalInput(new DigitalInput.Spec(pin, mode));
    }
    
    @Override
    public synchronized DigitalInput openDigitalInput(final DigitalInput.Spec spec) throws ConnectionLostException {
        this.checkState();
        this.hardware_.checkValidPin(spec.pin);
        this.checkPinFree(spec.pin);
        final DigitalInputImpl result = new DigitalInputImpl(this, spec.pin);
        this.addDisconnectListener(result);
        this.openPins_[spec.pin] = true;
        this.incomingState_.addInputPinListener(spec.pin, result);
        try {
            this.protocol_.setPinDigitalIn(spec.pin, spec.mode);
            this.protocol_.setChangeNotify(spec.pin, true);
        }
        catch (IOException e) {
            result.close();
            throw new ConnectionLostException(e);
        }
        return result;
    }
    
    @Override
    public DigitalOutput openDigitalOutput(final int pin, final DigitalOutput.Spec.Mode mode, final boolean startValue) throws ConnectionLostException {
        return this.openDigitalOutput(new DigitalOutput.Spec(pin, mode), startValue);
    }
    
    @Override
    public synchronized DigitalOutput openDigitalOutput(final DigitalOutput.Spec spec, final boolean startValue) throws ConnectionLostException {
        this.checkState();
        this.hardware_.checkValidPin(spec.pin);
        this.checkPinFree(spec.pin);
        final DigitalOutputImpl result = new DigitalOutputImpl(this, spec.pin, startValue);
        this.addDisconnectListener(result);
        this.openPins_[spec.pin] = true;
        try {
            this.protocol_.setPinDigitalOut(spec.pin, startValue, spec.mode);
        }
        catch (IOException e) {
            result.close();
            throw new ConnectionLostException(e);
        }
        return result;
    }
    
    @Override
    public DigitalOutput openDigitalOutput(final int pin, final boolean startValue) throws ConnectionLostException {
        return this.openDigitalOutput(new DigitalOutput.Spec(pin), startValue);
    }
    
    @Override
    public DigitalOutput openDigitalOutput(final int pin) throws ConnectionLostException {
        return this.openDigitalOutput(new DigitalOutput.Spec(pin), false);
    }
    
    @Override
    public synchronized AnalogInput openAnalogInput(final int pin) throws ConnectionLostException {
        this.checkState();
        this.hardware_.checkSupportsAnalogInput(pin);
        this.checkPinFree(pin);
        final AnalogInputImpl result = new AnalogInputImpl(this, pin);
        this.addDisconnectListener(result);
        this.openPins_[pin] = true;
        this.incomingState_.addInputPinListener(pin, result);
        try {
            this.protocol_.setPinAnalogIn(pin);
            this.protocol_.setAnalogInSampling(pin, true);
        }
        catch (IOException e) {
            result.close();
            throw new ConnectionLostException(e);
        }
        return result;
    }
    
    @Override
    public PwmOutput openPwmOutput(final int pin, final int freqHz) throws ConnectionLostException {
        return this.openPwmOutput(new DigitalOutput.Spec(pin), freqHz);
    }
    
    @Override
    public synchronized PwmOutput openPwmOutput(final DigitalOutput.Spec spec, final int freqHz) throws ConnectionLostException {
        this.checkState();
        this.hardware_.checkSupportsPeripheralOutput(spec.pin);
        this.checkPinFree(spec.pin);
        final int pwmNum = this.pwmAllocator_.allocateModule();
        int scale = 0;
        do {
            final int clk = 16000000 / IOIOProtocol.PwmScale.values()[scale].scale;
            final int period = clk / freqHz;
            if (period <= 65536) {
                final float baseUs = 1000000.0f / clk;
                final PwmImpl pwm = new PwmImpl(this, spec.pin, pwmNum, period, baseUs);
                this.addDisconnectListener(pwm);
                this.openPins_[spec.pin] = true;
                try {
                    this.protocol_.setPinDigitalOut(spec.pin, false, spec.mode);
                    this.protocol_.setPinPwm(spec.pin, pwmNum, true);
                    this.protocol_.setPwmPeriod(pwmNum, period - 1, IOIOProtocol.PwmScale.values()[scale]);
                }
                catch (IOException e) {
                    pwm.close();
                    throw new ConnectionLostException(e);
                }
                return pwm;
            }
        } while (++scale < IOIOProtocol.PwmScale.values().length);
        throw new IllegalArgumentException("Frequency too low: " + freqHz);
    }
    
    @Override
    public Uart openUart(final int rx, final int tx, final int baud, final Uart.Parity parity, final Uart.StopBits stopbits) throws ConnectionLostException {
        return this.openUart((rx == -1) ? null : new DigitalInput.Spec(rx), (tx == -1) ? null : new DigitalOutput.Spec(tx), baud, parity, stopbits);
    }
    
    @Override
    public synchronized Uart openUart(final DigitalInput.Spec rx, final DigitalOutput.Spec tx, final int baud, final Uart.Parity parity, final Uart.StopBits stopbits) throws ConnectionLostException {
        this.checkState();
        if (rx != null) {
            this.hardware_.checkSupportsPeripheralInput(rx.pin);
            this.checkPinFree(rx.pin);
        }
        if (tx != null) {
            this.hardware_.checkSupportsPeripheralOutput(tx.pin);
            this.checkPinFree(tx.pin);
        }
        final int rxPin = (rx != null) ? rx.pin : -1;
        final int txPin = (tx != null) ? tx.pin : -1;
        final int uartNum = this.uartAllocator_.allocateModule();
        final UartImpl uart = new UartImpl(this, txPin, rxPin, uartNum);
        this.addDisconnectListener(uart);
        this.incomingState_.addUartListener(uartNum, uart);
        try {
            if (rx != null) {
                this.openPins_[rx.pin] = true;
                this.protocol_.setPinDigitalIn(rx.pin, rx.mode);
                this.protocol_.setPinUart(rx.pin, uartNum, false, true);
            }
            if (tx != null) {
                this.openPins_[tx.pin] = true;
                this.protocol_.setPinDigitalOut(tx.pin, true, tx.mode);
                this.protocol_.setPinUart(tx.pin, uartNum, true, true);
            }
            boolean speed4x = true;
            int rate = Math.round(4000000.0f / baud) - 1;
            if (rate > 65535) {
                speed4x = false;
                rate = Math.round(1000000.0f / baud) - 1;
            }
            this.protocol_.uartConfigure(uartNum, rate, speed4x, stopbits, parity);
        }
        catch (IOException e) {
            uart.close();
            throw new ConnectionLostException(e);
        }
        return uart;
    }
    
    @Override
    public synchronized TwiMaster openTwiMaster(final int twiNum, final TwiMaster.Rate rate, final boolean smbus) throws ConnectionLostException {
        this.checkState();
        this.checkTwiFree(twiNum);
        final int[][] twiPins = this.hardware_.twiPins();
        this.checkPinFree(twiPins[twiNum][0]);
        this.checkPinFree(twiPins[twiNum][1]);
        this.openPins_[twiPins[twiNum][0]] = true;
        this.openPins_[twiPins[twiNum][1]] = true;
        this.openTwi_[twiNum] = true;
        final TwiMasterImpl twi = new TwiMasterImpl(this, twiNum);
        this.addDisconnectListener(twi);
        this.incomingState_.addTwiListener(twiNum, twi);
        try {
            this.protocol_.i2cConfigureMaster(twiNum, rate, smbus);
        }
        catch (IOException e) {
            twi.close();
            throw new ConnectionLostException(e);
        }
        return twi;
    }
    
    @Override
    public synchronized IcspMaster openIcspMaster() throws ConnectionLostException {
        this.checkState();
        this.checkIcspFree();
        final int[] icspPins = this.hardware_.icspPins();
        this.checkPinFree(icspPins[0]);
        this.checkPinFree(icspPins[1]);
        this.checkPinFree(icspPins[2]);
        this.openPins_[icspPins[0]] = true;
        this.openPins_[icspPins[1]] = true;
        this.openPins_[icspPins[2]] = true;
        this.openIcsp_ = true;
        final IcspMasterImpl icsp = new IcspMasterImpl(this);
        this.addDisconnectListener(icsp);
        this.incomingState_.addIcspListener(icsp);
        try {
            this.protocol_.icspOpen();
        }
        catch (IOException e) {
            icsp.close();
            throw new ConnectionLostException(e);
        }
        return icsp;
    }
    
    @Override
    public SpiMaster openSpiMaster(final int miso, final int mosi, final int clk, final int slaveSelect, final SpiMaster.Rate rate) throws ConnectionLostException {
        return this.openSpiMaster(miso, mosi, clk, new int[] { slaveSelect }, rate);
    }
    
    @Override
    public SpiMaster openSpiMaster(final int miso, final int mosi, final int clk, final int[] slaveSelect, final SpiMaster.Rate rate) throws ConnectionLostException {
        final DigitalOutput.Spec[] slaveSpecs = new DigitalOutput.Spec[slaveSelect.length];
        for (int i = 0; i < slaveSelect.length; ++i) {
            slaveSpecs[i] = new DigitalOutput.Spec(slaveSelect[i]);
        }
        return this.openSpiMaster(new DigitalInput.Spec(miso, DigitalInput.Spec.Mode.PULL_UP), new DigitalOutput.Spec(mosi), new DigitalOutput.Spec(clk), slaveSpecs, new SpiMaster.Config(rate));
    }
    
    @Override
    public synchronized SpiMaster openSpiMaster(final DigitalInput.Spec miso, final DigitalOutput.Spec mosi, final DigitalOutput.Spec clk, final DigitalOutput.Spec[] slaveSelect, final SpiMaster.Config config) throws ConnectionLostException {
        this.checkState();
        final int[] ssPins = new int[slaveSelect.length];
        this.checkPinFree(miso.pin);
        this.hardware_.checkSupportsPeripheralInput(miso.pin);
        this.checkPinFree(mosi.pin);
        this.hardware_.checkSupportsPeripheralOutput(mosi.pin);
        this.checkPinFree(clk.pin);
        this.hardware_.checkSupportsPeripheralOutput(clk.pin);
        for (int i = 0; i < slaveSelect.length; ++i) {
            this.checkPinFree(slaveSelect[i].pin);
            ssPins[i] = slaveSelect[i].pin;
        }
        final int spiNum = this.spiAllocator_.allocateModule();
        final SpiMasterImpl spi = new SpiMasterImpl(this, spiNum, mosi.pin, miso.pin, clk.pin, ssPins);
        this.addDisconnectListener(spi);
        this.openPins_[miso.pin] = true;
        this.openPins_[mosi.pin] = true;
        this.openPins_[clk.pin] = true;
        for (int j = 0; j < slaveSelect.length; ++j) {
            this.openPins_[slaveSelect[j].pin] = true;
        }
        this.incomingState_.addSpiListener(spiNum, spi);
        try {
            this.protocol_.setPinDigitalIn(miso.pin, miso.mode);
            this.protocol_.setPinSpi(miso.pin, 1, true, spiNum);
            this.protocol_.setPinDigitalOut(mosi.pin, true, mosi.mode);
            this.protocol_.setPinSpi(mosi.pin, 0, true, spiNum);
            this.protocol_.setPinDigitalOut(clk.pin, config.invertClk, clk.mode);
            this.protocol_.setPinSpi(clk.pin, 2, true, spiNum);
            for (final DigitalOutput.Spec spec : slaveSelect) {
                this.protocol_.setPinDigitalOut(spec.pin, true, spec.mode);
            }
            this.protocol_.spiConfigureMaster(spiNum, config);
        }
        catch (IOException e) {
            spi.close();
            throw new ConnectionLostException(e);
        }
        return spi;
    }
    
    @Override
    public PulseInput openPulseInput(final DigitalInput.Spec spec, final PulseInput.ClockRate rate, final PulseInput.PulseMode mode, final boolean doublePrecision) throws ConnectionLostException {
        this.checkState();
        this.checkPinFree(spec.pin);
        this.hardware_.checkSupportsPeripheralInput(spec.pin);
        final int incapNum = doublePrecision ? this.incapAllocatorDouble_.allocateModule() : this.incapAllocatorSingle_.allocateModule();
        final IncapImpl incap = new IncapImpl(this, mode, incapNum, spec.pin, rate.hertz, mode.scaling, doublePrecision);
        this.addDisconnectListener(incap);
        this.incomingState_.addIncapListener(incapNum, incap);
        this.openPins_[spec.pin] = true;
        try {
            this.protocol_.setPinDigitalIn(spec.pin, spec.mode);
            this.protocol_.setPinIncap(spec.pin, incapNum, true);
            this.protocol_.incapConfigure(incapNum, doublePrecision, mode.ordinal() + 1, rate.ordinal());
        }
        catch (IOException e) {
            incap.close();
            throw new ConnectionLostException(e);
        }
        return incap;
    }
    
    @Override
    public PulseInput openPulseInput(final int pin, final PulseInput.PulseMode mode) throws ConnectionLostException {
        return this.openPulseInput(new DigitalInput.Spec(pin), PulseInput.ClockRate.RATE_16MHz, mode, true);
    }
    
    private void checkPinFree(final int pin) {
        if (this.openPins_[pin]) {
            throw new IllegalArgumentException("Pin already open: " + pin);
        }
    }
    
    private void checkTwiFree(final int twi) {
        if (this.openTwi_[twi]) {
            throw new IllegalArgumentException("TWI already open: " + twi);
        }
    }
    
    private void checkIcspFree() {
        if (this.openIcsp_) {
            throw new IllegalArgumentException("ICSP already open");
        }
    }
    
    private void checkState() throws ConnectionLostException {
        if (this.state_ == State.DEAD) {
            throw new ConnectionLostException();
        }
        if (this.state_ == State.INCOMPATIBLE) {
            throw new IllegalStateException("Incompatibility has been reported - IOIO cannot be used");
        }
        if (this.state_ != State.CONNECTED) {
            throw new IllegalStateException("Connection has not yet been established");
        }
    }
    
    @Override
    public synchronized void beginBatch() throws ConnectionLostException {
        this.checkState();
        this.protocol_.beginBatch();
    }
    
    @Override
    public synchronized void endBatch() throws ConnectionLostException {
        this.checkState();
        try {
            this.protocol_.endBatch();
        }
        catch (IOException e) {
            throw new ConnectionLostException(e);
        }
    }
}
