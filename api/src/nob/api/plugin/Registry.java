/* ======================================
 * File: Registry.java
 * Date: 2026-11-06
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.api.plugin;

import java.lang.reflect.Type;

import nob.api.task.Task;

public interface Registry {
    public void task(Class<? extends Task> taskType);
    public void config(String configName, Type type, Object defaultValue);
    public void bucket(String bucketName, Type bucketType);
    public <T> ConfigEdit<T> edit(String configId);
}
