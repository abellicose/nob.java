/* ======================================
 * File: MerkleNode.java
 * Date: 2026-05-28
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob;

import java.util.List;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.Serializable;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

public class MerkleNode implements Serializable {
    Path path;
    long hash = 0;
    List<MerkleNode> children = new ArrayList<>();
    boolean leaf;

    @Override
    public String toString() {
        return "Path: " + path.toString() + ", Hash: " + hash;
    }

    static MerkleNode build(Path path) throws Exception {
        return build(path, Files.getLastModifiedTime(path).toMillis());
    }

    static MerkleNode build(Path path, long mTime) throws Exception {
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

    static void diff(MerkleNode prev, MerkleNode curr, List<Path> changed, List<Path> deleted) throws Exception {
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
            if (cmp == 0) { diff(a, b, changed, deleted); }
            else if (cmp < 0) { collectLeaves(a.path, deleted); i++; }
            else { collectLeaves(b.path, changed); j++; }
        }

        while (i < prev.children.size()) { collectLeaves(prev.children.get(i++).path, deleted); }
        while (j < curr.children.size()) { collectLeaves(curr.children.get(j++).path, changed); }
    }

    static void collectLeaves(Path path, List<Path> collection) throws Exception {
        if (Files.isDirectory(path)) {
            for (Path p: Files.list(path).toList()) { collectLeaves(p, collection); }
        } else {
            collection.add(path);
        }
    }

    static MerkleNode readCache(String fileName) throws Exception {
        Path path = Path.of(fileName);
        if (!Files.exists(path)) return new MerkleNode();
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(path))) {
            return (MerkleNode) in.readObject();
        }
    }

    static void writeCache(MerkleNode node, String fileName) throws Exception {
        ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(Path.of(fileName)));
        out.writeObject(node);
        out.flush();
        out.close();
    }

}

