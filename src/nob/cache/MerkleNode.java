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
import nob.util.NobException;

public class MerkleNode implements Serializable {
    private static final long serialVersionUID = 1L;

    public String path;
    public long hash = 0;
    public List<MerkleNode> children = new ArrayList<>();
    public boolean leaf;

    public static MerkleNode build(Path path) throws Exception {
        return build(path, Files.getLastModifiedTime(path).toMillis());
    }

    @Override
    public String toString() {
        return "MerkleNode{path='" + path + "', hash=" + hash + ", leaf=" + leaf + ", children=" + children.size() + "}";
    }

    public static MerkleNode build(Path path, long mTime) throws Exception {
        MerkleNode node = new MerkleNode();
        node.path = path.toString();
        long maxMTime = 0;

        if (Files.isDirectory(path)) {
            for (Path child: Files.list(path).sorted().toList()) {
                MerkleNode childNode = build(child, Files.getLastModifiedTime(child).toMillis());
                node.children.add(childNode);
                if (childNode.hash > maxMTime) {
                    maxMTime = childNode.hash;
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
            changed.add(curr.path);
            return;
        }

        int i = 0, j = 0;

        while (i < prev.children.size() && j < curr.children.size()) {
            MerkleNode a = prev.children.get(i);
            MerkleNode b = curr.children.get(j);
            int cmp = a.path.compareTo(b.path);
            if (cmp == 0) { diff(a, b, changed, deleted); i++; j++; }
            else if (cmp < 0) { collectLeaves(a.path, deleted); i++; }
            else { collectLeaves(b.path, changed); j++; }
        }

        while (i < prev.children.size()) { collectLeaves(prev.children.get(i++).path, deleted); }
        while (j < curr.children.size()) { collectLeaves(curr.children.get(j++).path, changed); }
    }

    static void collectLeaves(String pathDir, List<String> collection) throws Exception {
        Path path = Path.of(pathDir);
        if (Files.isDirectory(path)) {
            for (Path p: Files.list(path).toList()) { collectLeaves(p.toString(), collection); }
        } else {
            collection.add(pathDir);
        }
    }

    List<String> collectAllFiles() throws Exception {
        List<String> result = new ArrayList<>();
        getFilesFrom(path, result);
        return result;
    }

    void getFilesFrom(String pathDir, List<String> children) throws Exception {
        Path path = Path.of(pathDir);
        try (var stream = Files.walk(path)) {
            stream.filter(p -> !Files.isDirectory(p))
                .forEach(p -> children.add(p.toString()));
        }
    }
}
