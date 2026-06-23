/* ======================================
 * File: Build.java
 * Date: 2026-05-14
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

import nob.Nob;
 
public class Build {
    public static void main(String[] args) {
        Nob nob = new Nob() {{
            packageName = "nob";
            jarName = "Nob.jar";
        }};
        nob.use(new nob.build.ResolveDeps());
        nob.compile();
        nob.jar();
    }
}
