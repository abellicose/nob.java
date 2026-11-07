/* ======================================
 * File: LocalRepository.java
 * Date: 2026-07-28
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build.dependencyresolver;

import java.nio.file.Path;
import java.nio.file.Files;
import java.net.URI;

public class LocalRepository implements Repository {

    public String id() { 
        return "local";
    }

    public URI resolveURI(Dependency dep, String ext) {
        return null;
    }

    // Todo: Cache local repository
    public void resolve(Dependency dep) {

    }
}

