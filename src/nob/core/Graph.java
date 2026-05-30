/* ======================================
 * File: Graph.java
 * Date: 2026-05-28
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.core;

import java.util.List;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
import nob.cache.MerkleNode;

public class Graph {

    public static List<Path> getFilesToCompile(Path src) throws Exception {
/*
        MerkleNode curr = MerkleNode.build(src);
        prev.path = Path.of("./");
        System.out.println(prev.toString());
        System.out.println(curr.toString());
        List<Path> changed = new ArrayList<>();
        List<Path> deleted = new ArrayList<>();
        MerkleNode.diff(prev, curr, changed, deleted);
        return changed;
*/
        return new ArrayList<>();
    }

    public static void buildCache(Path out) {
        // getFielsToCompile()
        // load cache (should include both file hash and method hash), hashmap (or sumn else, might learn new data structure if required)
        // find all .java files in src dir
        // create new hashmap with file hash
        // compare the two file hashes to find what changed (might learn some new data strcuture to speed it up, just cuz)
        // find files that changed, return those
        //
        // buildCache()
        // update the file and method cache based on the files changed
        // open changed class files, if method structure changed, find dependencies of those
        //  method structures and recompile those classes if they havent been recompiled - return those for second compilation
        //      in an ideal scenario they'll already be recompiled since the user will have to edit things.
        //      but just in case the user forgets, we dont wnat them to figure that out after relaunching
        //
        // writeCache - could be synchronized
        // update method caches as well
        // delete files that were deleted (in old cache, not in new cache)
    }
}

