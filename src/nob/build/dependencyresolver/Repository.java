/* ======================================
 * File: Repository.java
 * Date: 2026-07-28
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build.dependencyresolver;

import java.nio.file.Path;
import java.net.URI;

// Strategy Specifier
public interface Repository {
    String id();
    void resolve(Dependency dep);
    URI resolveURI(Dependency dep, String ext);
}

