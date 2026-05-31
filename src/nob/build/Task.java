/* ======================================
 * File: Task.java
 * Date: 2026-05-31
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import java.util.List;

public interface Task {
    String id();
    default List<String> dependsOn() { List.of() };
    void execute(Context ctx);
}

