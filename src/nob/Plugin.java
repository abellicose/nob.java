/* ======================================
 * File: Plugin.java
 * Date: 2026-06-23
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob;

import java.util.Set;
import nob.build.Registry;

public interface Plugin {
    String id();
    void apply(Registry registry);
}

