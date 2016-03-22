// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.util;

import ioio.lib.spi.IOIOConnectionFactory;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Collection;

public class IOIOConnectionManager
{
    private final IOIOConnectionThreadProvider provider_;
    private Collection<Thread> threads_;
    
    public IOIOConnectionManager(final IOIOConnectionThreadProvider provider) {
        this.threads_ = new LinkedList<Thread>();
        this.provider_ = provider;
    }
    
    public void start() {
        this.createAllThreads();
        this.startAllThreads();
    }
    
    public void stop() {
        this.abortAllThreads();
        try {
            this.joinAllThreads();
        }
        catch (InterruptedException ex) {}
    }
    
    private void abortAllThreads() {
        for (final Thread thread : this.threads_) {
            thread.abort();
        }
    }
    
    private void joinAllThreads() throws InterruptedException {
        for (final Thread thread : this.threads_) {
            thread.join();
        }
    }
    
    private void createAllThreads() {
        this.threads_.clear();
        final Collection<IOIOConnectionFactory> factories = IOIOConnectionRegistry.getConnectionFactories();
        for (final IOIOConnectionFactory factory : factories) {
            final Thread thread = this.provider_.createThreadFromFactory(factory);
            if (thread != null) {
                this.threads_.add(thread);
            }
        }
    }
    
    private void startAllThreads() {
        for (final Thread thread : this.threads_) {
            thread.start();
        }
    }
    
    public abstract static class Thread extends java.lang.Thread
    {
        public abstract void abort();
    }
    
    public interface IOIOConnectionThreadProvider
    {
        Thread createThreadFromFactory(final IOIOConnectionFactory p0);
    }
}
