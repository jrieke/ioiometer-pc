// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.api;

import ioio.lib.api.exception.IncompatibilityException;
import ioio.lib.api.exception.ConnectionLostException;

public interface IOIO
{
    public static final int INVALID_PIN = -1;
    public static final int LED_PIN = 0;
    
    void waitForConnect() throws ConnectionLostException, IncompatibilityException;
    
    void disconnect();
    
    void waitForDisconnect() throws InterruptedException;
    
    State getState();
    
    void softReset() throws ConnectionLostException;
    
    void hardReset() throws ConnectionLostException;
    
    String getImplVersion(final VersionType p0) throws ConnectionLostException;
    
    DigitalInput openDigitalInput(final DigitalInput.Spec p0) throws ConnectionLostException;
    
    DigitalInput openDigitalInput(final int p0) throws ConnectionLostException;
    
    DigitalInput openDigitalInput(final int p0, final DigitalInput.Spec.Mode p1) throws ConnectionLostException;
    
    DigitalOutput openDigitalOutput(final DigitalOutput.Spec p0, final boolean p1) throws ConnectionLostException;
    
    DigitalOutput openDigitalOutput(final int p0, final DigitalOutput.Spec.Mode p1, final boolean p2) throws ConnectionLostException;
    
    DigitalOutput openDigitalOutput(final int p0, final boolean p1) throws ConnectionLostException;
    
    DigitalOutput openDigitalOutput(final int p0) throws ConnectionLostException;
    
    AnalogInput openAnalogInput(final int p0) throws ConnectionLostException;
    
    PwmOutput openPwmOutput(final DigitalOutput.Spec p0, final int p1) throws ConnectionLostException;
    
    PwmOutput openPwmOutput(final int p0, final int p1) throws ConnectionLostException;
    
    PulseInput openPulseInput(final DigitalInput.Spec p0, final PulseInput.ClockRate p1, final PulseInput.PulseMode p2, final boolean p3) throws ConnectionLostException;
    
    PulseInput openPulseInput(final int p0, final PulseInput.PulseMode p1) throws ConnectionLostException;
    
    Uart openUart(final DigitalInput.Spec p0, final DigitalOutput.Spec p1, final int p2, final Uart.Parity p3, final Uart.StopBits p4) throws ConnectionLostException;
    
    Uart openUart(final int p0, final int p1, final int p2, final Uart.Parity p3, final Uart.StopBits p4) throws ConnectionLostException;
    
    SpiMaster openSpiMaster(final DigitalInput.Spec p0, final DigitalOutput.Spec p1, final DigitalOutput.Spec p2, final DigitalOutput.Spec[] p3, final SpiMaster.Config p4) throws ConnectionLostException;
    
    SpiMaster openSpiMaster(final int p0, final int p1, final int p2, final int[] p3, final SpiMaster.Rate p4) throws ConnectionLostException;
    
    SpiMaster openSpiMaster(final int p0, final int p1, final int p2, final int p3, final SpiMaster.Rate p4) throws ConnectionLostException;
    
    TwiMaster openTwiMaster(final int p0, final TwiMaster.Rate p1, final boolean p2) throws ConnectionLostException;
    
    IcspMaster openIcspMaster() throws ConnectionLostException;
    
    void beginBatch() throws ConnectionLostException;
    
    void endBatch() throws ConnectionLostException;
    
    public enum State
    {
        INIT("INIT", 0), 
        CONNECTED("CONNECTED", 1), 
        INCOMPATIBLE("INCOMPATIBLE", 2), 
        DEAD("DEAD", 3);
        
        private State(final String s, final int n) {
        }
    }
    
    public enum VersionType
    {
        HARDWARE_VER("HARDWARE_VER", 0), 
        BOOTLOADER_VER("BOOTLOADER_VER", 1), 
        APP_FIRMWARE_VER("APP_FIRMWARE_VER", 2), 
        IOIOLIB_VER("IOIOLIB_VER", 3);
        
        private VersionType(final String s, final int n) {
        }
    }
}
