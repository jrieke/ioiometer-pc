// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.util.pc;

import java.awt.event.WindowEvent;
import java.awt.Window;
import javax.swing.SwingUtilities;
import java.awt.event.WindowListener;
import ioio.lib.util.IOIOLooperProvider;
import java.awt.event.WindowAdapter;

public abstract class IOIOSwingApp extends WindowAdapter implements IOIOLooperProvider
{
    public IOIOPcApplicationHelper helper_;
    
    public IOIOSwingApp() {
        this.helper_ = new IOIOPcApplicationHelper(this);
    }
    
    public void go(final String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                IOIOSwingApp.this.createMainWindow(args).addWindowListener(IOIOSwingApp.this);
            }
        });
    }
    
    protected abstract Window createMainWindow(final String[] p0);
    
    @Override
    public void windowClosing(final WindowEvent event) {
        this.helper_.stop();
    }
    
    @Override
    public void windowOpened(final WindowEvent event) {
        this.helper_.start();
    }
}
