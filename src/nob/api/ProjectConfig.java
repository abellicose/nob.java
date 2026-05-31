/* ======================================
 * File: ProjectConfig.java
 * Date: 2026-05-31
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.api;

import nob.util.NobException;

// Carries Data
public class ProjectConfig {
    public String packageName   = null;
    public String mainClass     = null;
    public String src           = "src/";
    public String dest          = "build/";
    public String classes       = "classes/";
    public String libs          = "build/libs";
    public String jarName       = "out.jar";

    public void validate() throws NobException {
        if (packageName == null) throw new NobException("Must set Nob.packageName");
    }
}

