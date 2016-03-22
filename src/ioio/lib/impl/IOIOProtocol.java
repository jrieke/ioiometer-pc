// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.impl;

import ioio.lib.spi.Log;
import java.util.Iterator;
import java.util.Collection;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Set;
import java.util.List;
import ioio.lib.api.TwiMaster;
import ioio.lib.api.SpiMaster;
import ioio.lib.api.Uart;
import ioio.lib.api.DigitalInput;
import ioio.lib.api.DigitalOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;

class IOIOProtocol
{
    static final int HARD_RESET = 0;
    static final int ESTABLISH_CONNECTION = 0;
    static final int SOFT_RESET = 1;
    static final int CHECK_INTERFACE = 2;
    static final int CHECK_INTERFACE_RESPONSE = 2;
    static final int SET_PIN_DIGITAL_OUT = 3;
    static final int SET_DIGITAL_OUT_LEVEL = 4;
    static final int REPORT_DIGITAL_IN_STATUS = 4;
    static final int SET_PIN_DIGITAL_IN = 5;
    static final int REPORT_PERIODIC_DIGITAL_IN_STATUS = 5;
    static final int SET_CHANGE_NOTIFY = 6;
    static final int REGISTER_PERIODIC_DIGITAL_SAMPLING = 7;
    static final int SET_PIN_PWM = 8;
    static final int SET_PWM_DUTY_CYCLE = 9;
    static final int SET_PWM_PERIOD = 10;
    static final int SET_PIN_ANALOG_IN = 11;
    static final int REPORT_ANALOG_IN_STATUS = 11;
    static final int SET_ANALOG_IN_SAMPLING = 12;
    static final int REPORT_ANALOG_IN_FORMAT = 12;
    static final int UART_CONFIG = 13;
    static final int UART_STATUS = 13;
    static final int UART_DATA = 14;
    static final int SET_PIN_UART = 15;
    static final int UART_REPORT_TX_STATUS = 15;
    static final int SPI_CONFIGURE_MASTER = 16;
    static final int SPI_STATUS = 16;
    static final int SPI_MASTER_REQUEST = 17;
    static final int SPI_DATA = 17;
    static final int SET_PIN_SPI = 18;
    static final int SPI_REPORT_TX_STATUS = 18;
    static final int I2C_CONFIGURE_MASTER = 19;
    static final int I2C_STATUS = 19;
    static final int I2C_WRITE_READ = 20;
    static final int I2C_RESULT = 20;
    static final int I2C_REPORT_TX_STATUS = 21;
    static final int ICSP_SIX = 22;
    static final int ICSP_REPORT_RX_STATUS = 22;
    static final int ICSP_REGOUT = 23;
    static final int ICSP_RESULT = 23;
    static final int ICSP_PROG_ENTER = 24;
    static final int ICSP_PROG_EXIT = 25;
    static final int ICSP_CONFIG = 26;
    static final int INCAP_CONFIGURE = 27;
    static final int INCAP_STATUS = 27;
    static final int SET_PIN_INCAP = 28;
    static final int INCAP_REPORT = 28;
    static final int SOFT_CLOSE = 29;
    static final int[] SCALE_DIV;
    private static final String TAG = "IOIOProtocol";
    private byte[] outbuf_;
    private int pos_;
    private int batchCounter_;
    private final InputStream in_;
    private final OutputStream out_;
    private final IncomingHandler handler_;
    private final IncomingThread thread_;
    
    static {
        SCALE_DIV = new int[] { 31, 30, 29, 28, 27, 26, 23, 22, 21, 20, 19, 18, 15, 14, 13, 12, 11, 10, 7, 6, 5, 4, 3, 2, 1 };
    }
    
    private void writeByte(final int b) throws IOException {
        assert b >= 0 && b < 256;
        if (this.pos_ == this.outbuf_.length) {
            this.flush();
        }
        this.outbuf_[this.pos_++] = (byte)b;
    }
    
    public synchronized void beginBatch() {
        ++this.batchCounter_;
    }
    
    public synchronized void endBatch() throws IOException {
        final int batchCounter_ = this.batchCounter_ - 1;
        this.batchCounter_ = batchCounter_;
        if (batchCounter_ == 0) {
            this.flush();
        }
    }
    
    private void flush() throws IOException {
        try {
            this.out_.write(this.outbuf_, 0, this.pos_);
        }
        finally {
            this.pos_ = 0;
        }
        this.pos_ = 0;
    }
    
    private void writeTwoBytes(final int i) throws IOException {
        this.writeByte(i & 0xFF);
        this.writeByte(i >> 8);
    }
    
    private void writeThreeBytes(final int i) throws IOException {
        this.writeByte(i & 0xFF);
        this.writeByte(i >> 8 & 0xFF);
        this.writeByte(i >> 16 & 0xFF);
    }
    
    public synchronized void hardReset() throws IOException {
        this.beginBatch();
        this.writeByte(0);
        this.writeByte(73);
        this.writeByte(79);
        this.writeByte(73);
        this.writeByte(79);
        this.endBatch();
    }
    
    public synchronized void softReset() throws IOException {
        this.beginBatch();
        this.writeByte(1);
        this.endBatch();
    }
    
    public synchronized void softClose() throws IOException {
        this.beginBatch();
        this.writeByte(29);
        this.endBatch();
    }
    
    public synchronized void checkInterface(final byte[] interfaceId) throws IOException {
        if (interfaceId.length != 8) {
            throw new IllegalArgumentException("interface ID must be exactly 8 bytes long");
        }
        this.beginBatch();
        this.writeByte(2);
        for (int i = 0; i < 8; ++i) {
            this.writeByte(interfaceId[i]);
        }
        this.endBatch();
    }
    
    public synchronized void setDigitalOutLevel(final int pin, final boolean level) throws IOException {
        this.beginBatch();
        this.writeByte(4);
        this.writeByte(pin << 2 | (level ? 1 : 0));
        this.endBatch();
    }
    
    public synchronized void setPinPwm(final int pin, final int pwmNum, final boolean enable) throws IOException {
        this.beginBatch();
        this.writeByte(8);
        this.writeByte(pin & 0x3F);
        this.writeByte((enable ? 128 : 0) | (pwmNum & 0xF));
        this.endBatch();
    }
    
    public synchronized void setPwmDutyCycle(final int pwmNum, final int dutyCycle, final int fraction) throws IOException {
        this.beginBatch();
        this.writeByte(9);
        this.writeByte(pwmNum << 2 | fraction);
        this.writeTwoBytes(dutyCycle);
        this.endBatch();
    }
    
    public synchronized void setPwmPeriod(final int pwmNum, final int period, final PwmScale scale) throws IOException {
        this.beginBatch();
        this.writeByte(10);
        this.writeByte((scale.encoding & 0x2) << 6 | pwmNum << 1 | (scale.encoding & 0x1));
        this.writeTwoBytes(period);
        this.endBatch();
    }
    
    public synchronized void setPinIncap(final int pin, final int incapNum, final boolean enable) throws IOException {
        this.beginBatch();
        this.writeByte(28);
        this.writeByte(pin);
        this.writeByte(incapNum | (enable ? 128 : 0));
        this.endBatch();
    }
    
    public synchronized void incapClose(final int incapNum, final boolean double_prec) throws IOException {
        this.beginBatch();
        this.writeByte(27);
        this.writeByte(incapNum);
        this.writeByte(double_prec ? 128 : 0);
        this.endBatch();
    }
    
    public synchronized void incapConfigure(final int incapNum, final boolean double_prec, final int mode, final int clock) throws IOException {
        this.beginBatch();
        this.writeByte(27);
        this.writeByte(incapNum);
        this.writeByte((double_prec ? 128 : 0) | mode << 3 | clock);
        this.endBatch();
    }
    
    public synchronized void i2cWriteRead(final int i2cNum, final boolean tenBitAddr, final int address, final int writeSize, final int readSize, final byte[] writeData) throws IOException {
        this.beginBatch();
        this.writeByte(20);
        this.writeByte(address >> 8 << 6 | (tenBitAddr ? 32 : 0) | i2cNum);
        this.writeByte(address & 0xFF);
        this.writeByte(writeSize);
        this.writeByte(readSize);
        for (int i = 0; i < writeSize; ++i) {
            this.writeByte(writeData[i] & 0xFF);
        }
        this.endBatch();
    }
    
    public synchronized void setPinDigitalOut(final int pin, final boolean value, final DigitalOutput.Spec.Mode mode) throws IOException {
        this.beginBatch();
        this.writeByte(3);
        this.writeByte(pin << 2 | ((mode == DigitalOutput.Spec.Mode.OPEN_DRAIN) ? 1 : 0) | (value ? 2 : 0));
        this.endBatch();
    }
    
    public synchronized void setPinDigitalIn(final int pin, final DigitalInput.Spec.Mode mode) throws IOException {
        int pull = 0;
        if (mode == DigitalInput.Spec.Mode.PULL_UP) {
            pull = 1;
        }
        else if (mode == DigitalInput.Spec.Mode.PULL_DOWN) {
            pull = 2;
        }
        this.beginBatch();
        this.writeByte(5);
        this.writeByte(pin << 2 | pull);
        this.endBatch();
    }
    
    public synchronized void setChangeNotify(final int pin, final boolean changeNotify) throws IOException {
        this.beginBatch();
        this.writeByte(6);
        this.writeByte(pin << 2 | (changeNotify ? 1 : 0));
        this.endBatch();
    }
    
    public synchronized void registerPeriodicDigitalSampling(final int pin, final int freqScale) throws IOException {
    }
    
    public synchronized void setPinAnalogIn(final int pin) throws IOException {
        this.beginBatch();
        this.writeByte(11);
        this.writeByte(pin);
        this.endBatch();
    }
    
    public synchronized void setAnalogInSampling(final int pin, final boolean enable) throws IOException {
        this.beginBatch();
        this.writeByte(12);
        this.writeByte((enable ? 128 : 0) | (pin & 0x3F));
        this.endBatch();
    }
    
    public synchronized void uartData(final int uartNum, final int numBytes, final byte[] data) throws IOException {
        if (numBytes > 64) {
            throw new IllegalArgumentException("A maximum of 64 bytes can be sent in one uartData message. Got: " + numBytes);
        }
        this.beginBatch();
        this.writeByte(14);
        this.writeByte(numBytes - 1 | uartNum << 6);
        for (int i = 0; i < numBytes; ++i) {
            this.writeByte(data[i] & 0xFF);
        }
        this.endBatch();
    }
    
    public synchronized void uartConfigure(final int uartNum, final int rate, final boolean speed4x, final Uart.StopBits stopbits, final Uart.Parity parity) throws IOException {
        final int parbits = (parity == Uart.Parity.EVEN) ? 1 : ((parity == Uart.Parity.ODD) ? 2 : 0);
        this.beginBatch();
        this.writeByte(13);
        this.writeByte(uartNum << 6 | (speed4x ? 8 : 0) | ((stopbits == Uart.StopBits.TWO) ? 4 : 0) | parbits);
        this.writeTwoBytes(rate);
        this.endBatch();
    }
    
    public synchronized void uartClose(final int uartNum) throws IOException {
        this.beginBatch();
        this.writeByte(13);
        this.writeByte(uartNum << 6);
        this.writeTwoBytes(0);
        this.endBatch();
    }
    
    public synchronized void setPinUart(final int pin, final int uartNum, final boolean tx, final boolean enable) throws IOException {
        this.beginBatch();
        this.writeByte(15);
        this.writeByte(pin);
        this.writeByte((enable ? 128 : 0) | (tx ? 64 : 0) | uartNum);
        this.endBatch();
    }
    
    public synchronized void spiConfigureMaster(final int spiNum, final SpiMaster.Config config) throws IOException {
        this.beginBatch();
        this.writeByte(16);
        this.writeByte(spiNum << 5 | IOIOProtocol.SCALE_DIV[config.rate.ordinal()]);
        this.writeByte((config.sampleOnTrailing ? 0 : 2) | (config.invertClk ? 1 : 0));
        this.endBatch();
    }
    
    public synchronized void spiClose(final int spiNum) throws IOException {
        this.beginBatch();
        this.writeByte(16);
        this.writeByte(spiNum << 5);
        this.writeByte(0);
        this.endBatch();
    }
    
    public synchronized void setPinSpi(final int pin, final int mode, final boolean enable, final int spiNum) throws IOException {
        this.beginBatch();
        this.writeByte(18);
        this.writeByte(pin);
        this.writeByte(0x10 | mode << 2 | spiNum);
        this.endBatch();
    }
    
    public synchronized void spiMasterRequest(final int spiNum, final int ssPin, final byte[] data, final int dataBytes, final int totalBytes, final int responseBytes) throws IOException {
        final boolean dataNeqTotal = dataBytes != totalBytes;
        final boolean resNeqTotal = responseBytes != totalBytes;
        this.beginBatch();
        this.writeByte(17);
        this.writeByte(spiNum << 6 | ssPin);
        this.writeByte((dataNeqTotal ? 128 : 0) | (resNeqTotal ? 64 : 0) | totalBytes - 1);
        if (dataNeqTotal) {
            this.writeByte(dataBytes);
        }
        if (resNeqTotal) {
            this.writeByte(responseBytes);
        }
        for (int i = 0; i < dataBytes; ++i) {
            this.writeByte(data[i] & 0xFF);
        }
        this.endBatch();
    }
    
    public synchronized void i2cConfigureMaster(final int i2cNum, final TwiMaster.Rate rate, final boolean smbusLevels) throws IOException {
        final int rateBits = (rate == TwiMaster.Rate.RATE_1MHz) ? 3 : ((rate == TwiMaster.Rate.RATE_400KHz) ? 2 : 1);
        this.beginBatch();
        this.writeByte(19);
        this.writeByte((smbusLevels ? 128 : 0) | rateBits << 5 | i2cNum);
        this.endBatch();
    }
    
    public synchronized void i2cClose(final int i2cNum) throws IOException {
        this.beginBatch();
        this.writeByte(19);
        this.writeByte(i2cNum);
        this.endBatch();
    }
    
    public synchronized void icspOpen() throws IOException {
        this.beginBatch();
        this.writeByte(26);
        this.writeByte(1);
        this.endBatch();
    }
    
    public synchronized void icspClose() throws IOException {
        this.beginBatch();
        this.writeByte(26);
        this.writeByte(0);
        this.endBatch();
    }
    
    public synchronized void icspEnter() throws IOException {
        this.beginBatch();
        this.writeByte(24);
        this.endBatch();
    }
    
    public synchronized void icspExit() throws IOException {
        this.beginBatch();
        this.writeByte(25);
        this.endBatch();
    }
    
    public synchronized void icspSix(final int instruction) throws IOException {
        this.beginBatch();
        this.writeByte(22);
        this.writeThreeBytes(instruction);
        this.endBatch();
    }
    
    public synchronized void icspRegout() throws IOException {
        this.beginBatch();
        this.writeByte(23);
        this.endBatch();
    }
    
    public IOIOProtocol(final InputStream in, final OutputStream out, final IncomingHandler handler) {
        this.outbuf_ = new byte[256];
        this.pos_ = 0;
        this.batchCounter_ = 0;
        this.thread_ = new IncomingThread();
        this.in_ = in;
        this.out_ = out;
        this.handler_ = handler;
        this.thread_.start();
    }
    
    enum PwmScale
    {
        SCALE_1X("SCALE_1X", 0, 1, 0), 
        SCALE_8X("SCALE_8X", 1, 8, 3), 
        SCALE_64X("SCALE_64X", 2, 64, 2), 
        SCALE_256X("SCALE_256X", 3, 256, 1);
        
        public final int scale;
        private final int encoding;
        
        private PwmScale(final String s, final int n, final int scale, final int encoding) {
            this.scale = scale;
            this.encoding = encoding;
        }
    }
    
    class IncomingThread extends Thread
    {
        private int readOffset_;
        private int validBytes_;
        private byte[] inbuf_;
        private List<Integer> analogPinValues_;
        private List<Integer> analogFramePins_;
        private List<Integer> newFramePins_;
        private Set<Integer> removedPins_;
        private Set<Integer> addedPins_;
        
        IncomingThread() {
            this.readOffset_ = 0;
            this.validBytes_ = 0;
            this.inbuf_ = new byte[64];
            this.analogPinValues_ = new ArrayList<Integer>();
            this.analogFramePins_ = new ArrayList<Integer>();
            this.newFramePins_ = new ArrayList<Integer>();
            this.removedPins_ = new HashSet<Integer>();
            this.addedPins_ = new HashSet<Integer>();
        }
        
        private void calculateAnalogFrameDelta() {
            this.removedPins_.clear();
            this.removedPins_.addAll(this.analogFramePins_);
            this.addedPins_.clear();
            this.addedPins_.addAll(this.newFramePins_);
            final Iterator<Integer> it = this.removedPins_.iterator();
            while (it.hasNext()) {
                final Integer current = it.next();
                if (this.addedPins_.contains(current)) {
                    it.remove();
                    this.addedPins_.remove(current);
                }
            }
            final List<Integer> temp = this.analogFramePins_;
            this.analogFramePins_ = this.newFramePins_;
            this.newFramePins_ = temp;
        }
        
        private void fillBuf() throws IOException {
            try {
                this.validBytes_ = IOIOProtocol.this.in_.read(this.inbuf_, 0, this.inbuf_.length);
                if (this.validBytes_ <= 0) {
                    throw new IOException("Unexpected stream closure");
                }
                this.readOffset_ = 0;
            }
            catch (IOException e) {
                Log.i("IOIOProtocol", "IOIO disconnected");
                throw e;
            }
        }
        
        private int readByte() throws IOException {
            if (this.readOffset_ == this.validBytes_) {
                this.fillBuf();
            }
            int b = this.inbuf_[this.readOffset_++];
            b &= 0xFF;
            return b;
        }
        
        private void readBytes(final int size, final byte[] buffer) throws IOException {
            for (int i = 0; i < size; ++i) {
                buffer[i] = (byte)this.readByte();
            }
        }
        
        @Override
        public void run() {
            super.run();
            this.setPriority(10);
            final byte[] data = new byte[256];
            try {
                while (true) {
                    int arg1;
                    switch (arg1 = this.readByte()) {
                        case 0: {
                            if (this.readByte() != 73 || this.readByte() != 79 || this.readByte() != 73 || this.readByte() != 79) {
                                throw new IOException("Bad establish connection magic");
                            }
                            final byte[] hardwareId = new byte[8];
                            final byte[] bootloaderId = new byte[8];
                            final byte[] firmwareId = new byte[8];
                            this.readBytes(8, hardwareId);
                            this.readBytes(8, bootloaderId);
                            this.readBytes(8, firmwareId);
                            IOIOProtocol.this.handler_.handleEstablishConnection(hardwareId, bootloaderId, firmwareId);
                            continue;
                        }
                        case 1: {
                            this.analogFramePins_.clear();
                            IOIOProtocol.this.handler_.handleSoftReset();
                            continue;
                        }
                        case 4: {
                            arg1 = this.readByte();
                            IOIOProtocol.this.handler_.handleReportDigitalInStatus(arg1 >> 2, (arg1 & 0x1) == 0x1);
                            continue;
                        }
                        case 6: {
                            arg1 = this.readByte();
                            IOIOProtocol.this.handler_.handleSetChangeNotify(arg1 >> 2, (arg1 & 0x1) == 0x1);
                            continue;
                        }
                        case 7: {
                            continue;
                        }
                        case 5: {
                            continue;
                        }
                        case 12: {
                            final int numPins = this.readByte();
                            this.newFramePins_.clear();
                            for (int i = 0; i < numPins; ++i) {
                                this.newFramePins_.add(this.readByte());
                            }
                            this.calculateAnalogFrameDelta();
                            for (final Integer j : this.removedPins_) {
                                IOIOProtocol.this.handler_.handleAnalogPinStatus(j, false);
                            }
                            for (final Integer j : this.addedPins_) {
                                IOIOProtocol.this.handler_.handleAnalogPinStatus(j, true);
                            }
                            continue;
                        }
                        case 11: {
                            final int numPins = this.analogFramePins_.size();
                            int header = 0;
                            this.analogPinValues_.clear();
                            for (int k = 0; k < numPins; ++k) {
                                if (k % 4 == 0) {
                                    header = this.readByte();
                                }
                                this.analogPinValues_.add(this.readByte() << 2 | (header & 0x3));
                                header >>= 2;
                            }
                            IOIOProtocol.this.handler_.handleReportAnalogInStatus(this.analogFramePins_, this.analogPinValues_);
                            continue;
                        }
                        case 15: {
                            arg1 = this.readByte();
                            final int arg2 = this.readByte();
                            IOIOProtocol.this.handler_.handleUartReportTxStatus(arg1 & 0x3, arg1 >> 2 | arg2 << 6);
                            continue;
                        }
                        case 14: {
                            arg1 = this.readByte();
                            for (int k = 0; k < (arg1 & 0x3F) + 1; ++k) {
                                data[k] = (byte)this.readByte();
                            }
                            IOIOProtocol.this.handler_.handleUartData(arg1 >> 6, (arg1 & 0x3F) + 1, data);
                            continue;
                        }
                        case 13: {
                            arg1 = this.readByte();
                            if ((arg1 & 0x80) != 0x0) {
                                IOIOProtocol.this.handler_.handleUartOpen(arg1 & 0x3);
                                continue;
                            }
                            IOIOProtocol.this.handler_.handleUartClose(arg1 & 0x3);
                            continue;
                        }
                        case 17: {
                            arg1 = this.readByte();
                            final int arg2 = this.readByte();
                            for (int k = 0; k < (arg1 & 0x3F) + 1; ++k) {
                                data[k] = (byte)this.readByte();
                            }
                            IOIOProtocol.this.handler_.handleSpiData(arg1 >> 6, arg2 & 0x3F, data, (arg1 & 0x3F) + 1);
                            continue;
                        }
                        case 18: {
                            arg1 = this.readByte();
                            final int arg2 = this.readByte();
                            IOIOProtocol.this.handler_.handleSpiReportTxStatus(arg1 & 0x3, arg1 >> 2 | arg2 << 6);
                            continue;
                        }
                        case 16: {
                            arg1 = this.readByte();
                            if ((arg1 & 0x80) != 0x0) {
                                IOIOProtocol.this.handler_.handleSpiOpen(arg1 & 0x3);
                                continue;
                            }
                            IOIOProtocol.this.handler_.handleSpiClose(arg1 & 0x3);
                            continue;
                        }
                        case 19: {
                            arg1 = this.readByte();
                            if ((arg1 & 0x80) != 0x0) {
                                IOIOProtocol.this.handler_.handleI2cOpen(arg1 & 0x3);
                                continue;
                            }
                            IOIOProtocol.this.handler_.handleI2cClose(arg1 & 0x3);
                            continue;
                        }
                        case 20: {
                            arg1 = this.readByte();
                            final int arg2 = this.readByte();
                            if (arg2 != 255) {
                                for (int k = 0; k < arg2; ++k) {
                                    data[k] = (byte)this.readByte();
                                }
                            }
                            IOIOProtocol.this.handler_.handleI2cResult(arg1 & 0x3, arg2, data);
                            continue;
                        }
                        case 21: {
                            arg1 = this.readByte();
                            final int arg2 = this.readByte();
                            IOIOProtocol.this.handler_.handleI2cReportTxStatus(arg1 & 0x3, arg1 >> 2 | arg2 << 6);
                            continue;
                        }
                        case 2: {
                            arg1 = this.readByte();
                            IOIOProtocol.this.handler_.handleCheckInterfaceResponse((arg1 & 0x1) == 0x1);
                            continue;
                        }
                        case 22: {
                            arg1 = this.readByte();
                            final int arg2 = this.readByte();
                            IOIOProtocol.this.handler_.handleIcspReportRxStatus(arg1 | arg2 << 8);
                            continue;
                        }
                        case 23: {
                            data[0] = (byte)this.readByte();
                            data[1] = (byte)this.readByte();
                            IOIOProtocol.this.handler_.handleIcspResult(2, data);
                            continue;
                        }
                        case 26: {
                            arg1 = this.readByte();
                            if ((arg1 & 0x1) == 0x1) {
                                IOIOProtocol.this.handler_.handleIcspOpen();
                                continue;
                            }
                            IOIOProtocol.this.handler_.handleIcspClose();
                            continue;
                        }
                        case 27: {
                            arg1 = this.readByte();
                            if ((arg1 & 0x80) != 0x0) {
                                IOIOProtocol.this.handler_.handleIncapOpen(arg1 & 0xF);
                                continue;
                            }
                            IOIOProtocol.this.handler_.handleIncapClose(arg1 & 0xF);
                            continue;
                        }
                        case 28: {
                            arg1 = this.readByte();
                            int size = arg1 >> 6;
                            if (size == 0) {
                                size = 4;
                            }
                            this.readBytes(size, data);
                            IOIOProtocol.this.handler_.handleIncapReport(arg1 & 0xF, size, data);
                            continue;
                        }
                        case 29: {
                            Log.d("IOIOProtocol", "Received soft close.");
                            throw new IOException("Soft close");
                        }
                        default: {
                            IOIOProtocol.this.in_.close();
                            final IOException e = new IOException("Received unexpected command: 0x" + Integer.toHexString(arg1));
                            Log.e("IOIOProtocol", "Protocol error", e);
                            throw e;
                        }
                    }
                }
            }
            catch (IOException e2) {
                IOIOProtocol.this.handler_.handleConnectionLost();
            }
        }
    }
    
    public interface IncomingHandler
    {
        void handleEstablishConnection(final byte[] p0, final byte[] p1, final byte[] p2);
        
        void handleConnectionLost();
        
        void handleSoftReset();
        
        void handleCheckInterfaceResponse(final boolean p0);
        
        void handleSetChangeNotify(final int p0, final boolean p1);
        
        void handleReportDigitalInStatus(final int p0, final boolean p1);
        
        void handleRegisterPeriodicDigitalSampling(final int p0, final int p1);
        
        void handleReportPeriodicDigitalInStatus(final int p0, final boolean[] p1);
        
        void handleAnalogPinStatus(final int p0, final boolean p1);
        
        void handleReportAnalogInStatus(final List<Integer> p0, final List<Integer> p1);
        
        void handleUartOpen(final int p0);
        
        void handleUartClose(final int p0);
        
        void handleUartData(final int p0, final int p1, final byte[] p2);
        
        void handleUartReportTxStatus(final int p0, final int p1);
        
        void handleSpiOpen(final int p0);
        
        void handleSpiClose(final int p0);
        
        void handleSpiData(final int p0, final int p1, final byte[] p2, final int p3);
        
        void handleSpiReportTxStatus(final int p0, final int p1);
        
        void handleI2cOpen(final int p0);
        
        void handleI2cClose(final int p0);
        
        void handleI2cResult(final int p0, final int p1, final byte[] p2);
        
        void handleI2cReportTxStatus(final int p0, final int p1);
        
        void handleIcspOpen();
        
        void handleIcspClose();
        
        void handleIcspReportRxStatus(final int p0);
        
        void handleIcspResult(final int p0, final byte[] p1);
        
        void handleIncapReport(final int p0, final int p1, final byte[] p2);
        
        void handleIncapClose(final int p0);
        
        void handleIncapOpen(final int p0);
    }
}
