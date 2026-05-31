/* ======================================
 * File: Context.java
 * Date: 2026-05-31
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import nob.Nob;
import nob.NobException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Context {
    Path source     = Path.of("src/");
    Path build      = Path.of("build/");
    Path out        = Path.of("build/classes");
    Path libs       = Path.of("build/libs");
    Path jarOut     = Path.of("build/jars");
    Path cacheFile  = Path.of("build/nob.cache");

    String packageName  = null;
    String mainClass    = null;
    String jarName      = "out.jar";
    
    // merkle cache and shit here

    public static Context load(Nob nob) {
        if (nob.packageName == null) {
            throw new NobException("Missing value for Nob.packageName");
        }

        Context ctx = new Context();
        ctx.source =  Path.of(nob.sourceDir);
        ctx.build = Path.of(nob.buildDir);
        ctx.out =  build.resolve(classesDir);
        ctx.libs = Path.of(libsDir);
        ctx.jarOut = build.resolve("jars/");
        ctx.cacheFile = build.resolve("nob.cache");
        ctx.packageName = nob.packageName;
        ctx.mainClass = nob.mainClass;
        ctx.jarName = nob.jarName;

        return ctx;
    }
}

