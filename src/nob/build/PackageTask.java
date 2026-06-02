/* ======================================
 * File: PackageTask.java
 * Date: 2026-06-02
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import nob.NobException;

public class PackageTask implements Task{
    public String id() {
        return "package";
    }

    public List<String> dependsOn() {
        return List.of("compile");
    }

    public void execute(Context ctx) {
        
    }
}

