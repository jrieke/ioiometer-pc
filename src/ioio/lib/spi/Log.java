// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.spi;

public class Log
{
    public static final int ASSERT = 7;
    public static final int DEBUG = 3;
    public static final int ERROR = 6;
    public static final int INFO = 4;
    public static final int VERBOSE = 2;
    public static final int WARN = 5;
    private static char[] LEVELS;
    
    static {
        Log.LEVELS = new char[] { '0', '1', 'V', 'D', 'I', 'W', 'E', 'F' };
    }
    
    public static int println(final int priority, final String tag, final String msg) {
        System.err.println("[" + Log.LEVELS[priority] + "/" + tag + "] " + msg);
        return 0;
    }
    
    public static void e(final String tag, final String message) {
        println(6, tag, message);
    }
    
    public static void e(final String tag, final String message, final Throwable tr) {
        println(6, tag, message);
        tr.printStackTrace();
    }
    
    public static void w(final String tag, final String message) {
        println(5, tag, message);
    }
    
    public static void w(final String tag, final String message, final Throwable tr) {
        println(5, tag, message);
        tr.printStackTrace();
    }
    
    public static void i(final String tag, final String message) {
        println(4, tag, message);
    }
    
    public static void d(final String tag, final String message) {
        println(3, tag, message);
    }
    
    public static void v(final String tag, final String message) {
        println(2, tag, message);
    }
}
