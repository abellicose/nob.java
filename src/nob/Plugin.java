/* ======================================
 * File: Plugin.java
 * Date: 2026-06-23
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob;

import nob.build.Registrar;

public interface Plugin {
    String id();
    void apply(Registrar registrar);
}

