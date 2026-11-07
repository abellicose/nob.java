/* ======================================
 * File: CompilePlugin.java
 * Date: 2026-07-29
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import java.util.Set;
import nob.Plugin;
import nob.Task;

class CompilePlugin implements Plugin {
    public String id() {
        return "comp";
    }

    public void apply(Registry reg) {
        Task tsk = new CompileTask();
    }
}
