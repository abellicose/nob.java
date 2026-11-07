/* ======================================
 * File: ConfigEdit.java
 * Date: 2026-11-06
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.api.plugin;

public interface ConfigEdit<T> {
    public void replace(T value);
    public void append(T value);
    public void add(T value);
    public void remove(T value);
}
