// 
// Decompiled by Procyon v0.5.30
// 

package ioio.lib.impl;

import ioio.lib.api.exception.OutOfResourceException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Collection;
import java.util.Set;

class ModuleAllocator
{
    private final Set<Integer> availableModuleIds_;
    private final Set<Integer> allocatedModuleIds_;
    private final String name_;
    
    public ModuleAllocator(final Collection<Integer> availableModuleIds, final String name) {
        this.availableModuleIds_ = new TreeSet<Integer>(availableModuleIds);
        this.allocatedModuleIds_ = new HashSet<Integer>();
        this.name_ = name;
    }
    
    public ModuleAllocator(final int[] availableModuleIds, final String name) {
        this(getList(availableModuleIds), name);
    }
    
    public ModuleAllocator(final int maxModules, final String name) {
        this(getList(maxModules), name);
    }
    
    private static Collection<Integer> getList(final int maxModules) {
        final List<Integer> availableModuleIds = new ArrayList<Integer>();
        for (int i = 0; i < maxModules; ++i) {
            availableModuleIds.add(i);
        }
        return availableModuleIds;
    }
    
    private static Collection<Integer> getList(final int[] array) {
        final List<Integer> availableModuleIds = new ArrayList<Integer>(array.length);
        for (final int i : array) {
            availableModuleIds.add(i);
        }
        return availableModuleIds;
    }
    
    public synchronized Integer allocateModule() {
        if (this.availableModuleIds_.isEmpty()) {
            throw new OutOfResourceException("No more resources of the requested type: " + this.name_);
        }
        final Integer moduleId = this.availableModuleIds_.iterator().next();
        this.availableModuleIds_.remove(moduleId);
        this.allocatedModuleIds_.add(moduleId);
        return moduleId;
    }
    
    public synchronized void releaseModule(final int moduleId) {
        if (!this.allocatedModuleIds_.contains(moduleId)) {
            throw new IllegalArgumentException("moduleId: " + moduleId + "; not yet allocated");
        }
        this.availableModuleIds_.add(moduleId);
        this.allocatedModuleIds_.remove(moduleId);
    }
}
