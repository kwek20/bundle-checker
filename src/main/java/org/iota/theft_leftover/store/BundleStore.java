package org.iota.theft_leftover.store;

import org.iota.jota.model.Bundle;

import java.util.List;

public interface BundleStore {
    
    void load();
    
    boolean contains(String address);
    
    List<Bundle> get(String address);
    
    void put(String address, List<Bundle> bundles);
}
