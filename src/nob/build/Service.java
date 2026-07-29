/* ======================================
 * File: Service.java
 * Date: 2026-07-29
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import java.util.Map;
import java.util.HashMap;
import nob.NobException;

// Holds shareable states accross all active plugins and files
public class Service {
    // String is signature
    private Map<String, Object> states = new HashMap<>();
    private Map<String, Object> configs = new HashMap<>();
    private boolean frozen = false;
    
    public void setState(String sig, Object state) {
        states.put(sig, state);
    }

    public <T> T getState(String sig, Class<T> type) {
        return type.cast(states.get(sig));
    }

    public void setConfig(String sig, Object state) {
        if (frozen) {
            throw new NobException("Config is currently frozen, cannot be edited during execution");
        }
        configs.put(sig, state);
    }

    public <T> T getConfig(String sig, Class<T> type) {
        return type.cast(configs.get(sig));
    }

    public void freeze() {
        frozen = true;
    }
}

