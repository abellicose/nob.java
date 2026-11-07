/* ======================================
 * File: Task.java
 * Date: 2026-05-31
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob;

import nob.build.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.lang.reflect.Type;

public interface Task {
    String id();

    default Map<String, Type> writes() {
        return Map.of();
    }

    default List<String> reads() {
        return List.of();
    }

    default List<String> consumesConfig() {
        return List.of();
    }

    void execute(ExecutionContext ctx);
}

