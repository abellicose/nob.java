/* ======================================
 * File: ConfigMeta.java
 * Date: 2026-11-07
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.core;

import java.lang.reflect.Type;

record ConfigMeta(String configName, Type type, Object defaultValue) {
    @Override
    public int hashCode() {
        return configName.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ConfigMeta meta)) {
            return false;
        }
        return configName.equals(meta.configName());
    }
}

