/* ======================================
 * File: ResolveDeps.java
 * Date: 2026-06-23
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import nob.Plugin;
import nob.build.Registrar;

public class ResolveDeps implements Plugin {
    private static final String ID = "maven";

    public String id() {
        return ID;
    }

    public void apply(Registry registrar) {
    }

}

