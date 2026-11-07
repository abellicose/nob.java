/* ======================================
 * File: ConfigEdit.java
 * Date: 2026-11-06
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.api.plugin;

public interface ConfigEdit {
    public void replace(Object value);
    public void append(Object value);
    public void add(Object value);
    public void remove(Object value);
}
