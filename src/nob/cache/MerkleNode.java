/* ======================================
 * File: MerkleNode.java
 * Date: 2026-05-28
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.cache;

import java.util.List;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.Serializable;

public class MerkleNode implements Serializable {
    public Path path;
    public long hash = 0;
    public List<MerkleNode> children = new ArrayList<>();
    public boolean leaf;

    public static MerkleNode build(Path path) throws Exception {
        return build(path, Files.getLastModifiedTime(path).toMillis());
    }

    public static MerkleNode build(Path path, long mTime) throws Exception {
        MerkleNode node = new MerkleNode();
        node.path = path;
        long maxMTime = 0;

        if (Files.isDirectory(path)) {
            for (Path child: Files.list(path).sorted().toList()) {
                long childMTime = Files.getLastModifiedTime(child).toMillis();
                node.children.add(build(child, childMTime));
                if (childMTime > maxMTime) {
                    maxMTime = childMTime;
                }
            }
            node.hash = maxMTime;
        } else {
            node.leaf = true;
            node.hash = mTime;
        }
        return node;
    }

    public static void diff(MerkleNode prev, MerkleNode curr, List<String> changed, List<String> deleted) throws Exception {
        if (prev.hash == curr.hash) return;

        if (curr.leaf) {
            changed.add(curr.path.toString());
            return;
        }

        int i = 0, j = 0;

        while (i < prev.children.size() && j < curr.children.size()) {
            MerkleNode a = prev.children.get(i);
            MerkleNode b = curr.children.get(j);
            int cmp = a.path.compareTo(b.path);
            if (cmp == 0) { diff(a, b, changed, deleted); }
            else if (cmp < 0) { collectLeaves(a.path, deleted); i++; }
            else { collectLeaves(b.path, changed); j++; }
        }

        while (i < prev.children.size()) { collectLeaves(prev.children.get(i++).path.toString(), deleted); }
        while (j < curr.children.size()) { collectLeaves(curr.children.get(j++).path.toString(), changed); }
    }

    public static void collectLeaves(Path path, List<String> collection) throws Exception {
        if (Files.isDirectory(path)) {
            for (Path p: Files.list(path).toList()) { collectLeaves(p, collection); }
        } else {
            collection.add(path.toString());
        }
    }

    public List<String> collectAllFiles() {
        List<String> result = new ArrayList<>();
        getFilesFrom(path, result);
        return result;
    }

    private void getFilesFrom(Path path, List<String> children) {
        if (Files.isDirectory(path)) {
            try (var stream = Files.list(path)) {
                stream.forEach(childPath -> getFilesFrom(childPath, children));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }        
        } else {
            children.add(path.toString());
        }
    }
}

