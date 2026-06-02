/* ======================================
 * File: JarConfig.java
 * Date: 2026-06-02
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob;

import java.util.Map;
import java.util.HashMap;

public class JarConfig {
    public Map<String, String> mfAttribs = new HashMap<>();

    public void manifest(String... mf) {
        for (int i = 0; i < mf.length; i+=2) {
            mfAttribs.put(mf[i], mf[i+1]);
        }
    }
}

