/* ======================================
 * File: Plugin.java
 * Date: 2026-11-06
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.api.plugin;

public interface Plugin {
    String id();
    void apply(Registry r);
}

