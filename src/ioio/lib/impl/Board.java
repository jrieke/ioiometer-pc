// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.impl;

enum Board
{
    SPRK0015("SPRK0015", 0, Hardware.IOIO0002), 
    SPRK0016("SPRK0016", 1, Hardware.IOIO0003), 
    MINT0010("MINT0010", 2, Hardware.IOIO0003), 
    SPRK0020("SPRK0020", 3, Hardware.IOIO0004);
    
    public final Hardware hardware;
    
    private Board(final String s, final int n, final Hardware hw) {
        this.hardware = hw;
    }
    
    static class Hardware
    {
        private static final boolean[][] MAP_IOIO0002_IOIO0003;
        private static final boolean[][] MAP_IOIO0004;
        static final Hardware IOIO0002;
        static final Hardware IOIO0003;
        static final Hardware IOIO0004;
        private final boolean[][] map_;
        private final int numPwmModules_;
        private final int numUartModules_;
        private final int numSpiModules_;
        private final int[] incapSingleModules_;
        private final int[] incapDoubleModules_;
        private final int[][] twiPins_;
        private final int[] icspPins_;
        
        static {
            MAP_IOIO0002_IOIO0003 = new boolean[][] { { true, true, false }, new boolean[3], new boolean[3], { true, true, false }, { true, true, false }, { true, true, false }, { true, true, false }, { true, true, false }, new boolean[3], { false, true, false }, { true, true, false }, { true, true, false }, { true, true, false }, { true, true, false }, { true, true, false }, new boolean[3], new boolean[3], new boolean[3], new boolean[3], new boolean[3], new boolean[3], new boolean[3], new boolean[3], new boolean[3], new boolean[3], new boolean[3], new boolean[3], { true, true, false }, { true, true, false }, { true, true, false }, { true, true, false }, { true, true, true }, { true, true, true }, { false, false, true }, { true, true, true }, { true, true, true }, { true, true, true }, { true, true, true }, { true, true, true }, { true, true, true }, { true, true, true }, { false, false, true }, { false, false, true }, { false, false, true }, { false, false, true }, { true, true, true }, { true, true, true }, { true, true, false }, { true, true, false } };
            MAP_IOIO0004 = new boolean[][] { new boolean[3], { true, true, false }, { true, true, false }, { true, true, false }, { true, true, false }, { true, true, false }, { true, true, false }, { true, true, false }, new boolean[3], { false, true, false }, { true, true, false }, { true, true, false }, { true, true, false }, { true, true, false }, { true, true, false }, new boolean[3], new boolean[3], new boolean[3], new boolean[3], new boolean[3], new boolean[3], new boolean[3], new boolean[3], new boolean[3], new boolean[3], new boolean[3], new boolean[3], { true, true, false }, { true, true, false }, { true, true, false }, { true, true, false }, { true, true, true }, { true, true, true }, { false, false, true }, { true, true, true }, { true, true, true }, { true, true, true }, { true, true, true }, { true, true, true }, { true, true, true }, { true, true, true }, { false, false, true }, { false, false, true }, { false, false, true }, { false, false, true }, { true, true, true }, { true, true, true } };
            IOIO0002 = new Hardware(Hardware.MAP_IOIO0002_IOIO0003, 9, 4, 3, new int[] { 0, 2, 4 }, new int[] { 6, 7, 8 }, new int[][] { { 4, 5 }, { 47, 48 }, { 26, 25 } }, new int[] { 36, 37, 38 });
            IOIO0003 = Hardware.IOIO0002;
            IOIO0004 = new Hardware(Hardware.MAP_IOIO0004, 9, 4, 3, new int[] { 0, 2, 4 }, new int[] { 6, 7, 8 }, new int[][] { { 4, 5 }, { 1, 2 }, { 26, 25 } }, new int[] { 36, 37, 38 });
        }
        
        private Hardware(final boolean[][] map, final int numPwmModules, final int numUartModules, final int numSpiModules, final int[] incapDoubleModules, final int[] incapSingleModules, final int[][] twiPins, final int[] icspPins) {
            if (map == null) {
                throw new IllegalArgumentException("WTF");
            }
            this.map_ = map;
            this.numPwmModules_ = numPwmModules;
            this.numUartModules_ = numUartModules;
            this.numSpiModules_ = numSpiModules;
            this.incapSingleModules_ = incapSingleModules;
            this.incapDoubleModules_ = incapDoubleModules;
            this.twiPins_ = twiPins;
            this.icspPins_ = icspPins;
        }
        
        int numPins() {
            return this.map_.length;
        }
        
        int numAnalogPins() {
            int result = 0;
            boolean[][] map_;
            for (int length = (map_ = this.map_).length, i = 0; i < length; ++i) {
                final boolean[] b = map_[i];
                if (b[Function.ANALOG_IN.ordinal()]) {
                    ++result;
                }
            }
            return result;
        }
        
        int numPwmModules() {
            return this.numPwmModules_;
        }
        
        int numUartModules() {
            return this.numUartModules_;
        }
        
        int numSpiModules() {
            return this.numSpiModules_;
        }
        
        int numTwiModules() {
            return this.twiPins().length;
        }
        
        int[] incapSingleModules() {
            return this.incapSingleModules_;
        }
        
        int[] incapDoubleModules() {
            return this.incapDoubleModules_;
        }
        
        int[][] twiPins() {
            return this.twiPins_;
        }
        
        int[] icspPins() {
            return this.icspPins_;
        }
        
        void checkSupportsAnalogInput(final int pin) {
            this.checkValidPin(pin);
            if (!this.map_[pin][Function.ANALOG_IN.ordinal()]) {
                throw new IllegalArgumentException("Pin " + pin + " does not support analog input");
            }
        }
        
        void checkSupportsPeripheralInput(final int pin) {
            this.checkValidPin(pin);
            if (!this.map_[pin][Function.PERIPHERAL_IN.ordinal()]) {
                throw new IllegalArgumentException("Pin " + pin + " does not support peripheral input");
            }
        }
        
        void checkSupportsPeripheralOutput(final int pin) {
            this.checkValidPin(pin);
            if (!this.map_[pin][Function.PERIPHERAL_OUT.ordinal()]) {
                throw new IllegalArgumentException("Pin " + pin + " does not support peripheral output");
            }
        }
        
        void checkValidPin(final int pin) {
            if (pin < 0 || pin >= this.map_.length) {
                throw new IllegalArgumentException("Illegal pin: " + pin);
            }
        }
        
        private enum Function
        {
            PERIPHERAL_OUT("PERIPHERAL_OUT", 0), 
            PERIPHERAL_IN("PERIPHERAL_IN", 1), 
            ANALOG_IN("ANALOG_IN", 2);
            
            private Function(final String s, final int n) {
            }
        }
    }
}
