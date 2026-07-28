/* ======================================
 * File: Plugin.java
 * Date: 2026-06-23
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob;

import java.util.List;
import nob.build.Registrar;

public interface Plugin {
    default List<String> dependsOn() {
        return List.of();
    }

    String id();
    void apply(Registrar registrar);
}

