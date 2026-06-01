/* ======================================
 * File: Changes.java
 * Date: 2026-05-31
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Map;
import java.util.HashMap;

public class Changes {
    public static DiffResult diff(Context ctx) {
        List<String> changed = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        MerkleNode curr = MerkleNode.build(ctx.source.toString());
        DiffResult diff = new DiffResult(changed, removed);
        if (ctx.merkleCache == null) {
            MerkleNode.collectLeaves(curr, changed);
        } else {
            MerkleNode.diff(ctx.merkleCache, curr, diff);
        }
        ctx.merkleCache = curr;
        return diff;
    }
}

class MerkleNode {
    String path;
    long hash = 0;
    Map<String, MerkleNode> children = new HashMap<>();
    boolean leaf = false;

    public MerkleNode(String path, long hash, Map<String, MerkleNode> children, boolean leaf) {
        this.path = path;
        this.hash = hash;
        this.children = children;
        this.leaf = leaf;
    }

    public MerkleNode(String path, boolean leaf) {
        this.path = path;
        this.leaf = leaf;
    }

    public MerkleNode() {
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MerkleNode)) return false;
        return path.equals(((MerkleNode) o).path);
    }

    // hash of a node is the max mtime of its children
    // if child is leaf, hash is its mtime
    static MerkleNode build(String nodeDir) {
        Path path = Path.of(nodeDir);
        MerkleNode curr = new MerkleNode(nodeDir, Files.isRegularFile(path));
        if (curr.leaf) {
            curr.hash = Files.getLastModifiedTime(path).toMillis();
            return curr;
        }

        long maxHash = 0;
        for (Path childPath: Files.list(path).toList()) {
            String childDir = childPath.toString();
            MerkleNode child = build(childDir);
            curr.children.put(childDir, child);
            if (child.hash > maxHash) 
                maxHash = child.hash;
        }
        curr.hash = maxHash;
        return curr;
    }

    static void diff(MerkleNode prev, MerkleNode curr, DiffResult diff) {
        if (prev.hash == curr.hash) return;

        for (MerkleNode child: curr.children.values()) {
            MerkleNode twin = prev.children.remove(child.path);
            if (twin == null) { // newly added
                collectLeaves(child, diff.changed());
                continue;
            } 

            if (twin.hash != child.hash) { // changed
                diff(twin, child, diff);
            }
        }

        for (MerkleNode removed: prev.children.values()) { // doesnt exist now
            collectLeaves(removed, diff.deleted());
        }
    }

    static void collectLeaves(MerkleNode node, List<String> leaves) {
        if (node.leaf) {
            leaves.add(node.path);
            return;
        }
        for (MerkleNode child: node.children.values())
            collectLeaves(child, leaves);
    }
}

