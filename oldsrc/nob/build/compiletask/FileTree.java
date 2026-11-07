/* ======================================
 * File: FileTree.java
 * Date: 2026-07-01
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import nob.NobException;
import java.io.Serializable;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.function.Consumer;
import java.lang.StringBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class FileTree implements Serializable {
    public FileEntry root;
    private Map<String, FileEntry> entries = new HashMap<>();

    public FileTree(FileEntry root, Map<String, FileEntry> entries) {
        this.root = root;
        this.entries = entries;
    }

    public boolean exists(String entry) {
        return entries.containsKey(entry);
    }

    public FileEntry get(String entry) {
        return entries.get(entry);
    }

    public FileEntry remove(String entry) {
        return entries.remove(entry);
    }

    public void collectLeaves(String path, List<String> result) {
        FileEntry entry = entries.get(path);
        if (entry.isFile()) {
            result.add(path);
            return;
        }

        for (String childPath: entry.children) {
            collectLeaves(childPath, result);
        }
    }

    public void forEachLeaf(String path, Consumer<String> action) {
        FileEntry entry = get(path);
        if (entry.isFile()) {
            action.accept(path);
            return;
        }
        for (String child : entry.children) {
            forEachLeaf(child, action);
        }
    }

    // every source file is saved as its internal name.
    // every manipulation is done with the internal name
    // we just add .java at the end just during compilation
    public static FileTree build(String root) {
        Map<String, FileEntry> entries = new HashMap<>();
        Deque<FileEntry> stack = new ArrayDeque<>();
        int offset = root.length() + 1;

        try {
            Files.walkFileTree(Path.of(root), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    String path = dir.toString();
                    if (!path.equals(root)) {
                        path = path.substring(offset, path.length());
                    }
                    FileEntry d = FileEntry.dir(path, attrs.lastModifiedTime().toMillis());
                    stack.push(d);
                    entries.put(path, d);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String path = file.toString();
                    if (!path.endsWith(".java")) return FileVisitResult.CONTINUE;

                    path = path.substring(offset, path.length() - 5);
                    long mTime = attrs.lastModifiedTime().toMillis();
                    FileEntry parent = stack.peek();
                    if (mTime > parent.mTime) {
                        parent.mTime = mTime;
                    }
                    FileEntry f = FileEntry.file(path, mTime);
                    entries.put(path, f);
                    parent.addChild(path);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException ex) throws IOException {
                    if (stack.size() > 1) {
                        String path = dir.toString();
                        if (!path.equals(root)) {
                            path = path.substring(offset, path.length());
                        }
                        FileEntry child = stack.pop();
                        FileEntry parent = stack.peek();
                        if (child.mTime > parent.mTime) {
                            parent.mTime = child.mTime; 
                        }
                        parent.addChild(path);
                    }  
                    return FileVisitResult.CONTINUE;
                }
            });

        } catch (IOException e) {
            throw new NobException("File IO error while building node.", e);
        }

        return new FileTree(stack.pop(), entries);
    }

    static void diff(FileTree prev, FileTree curr, DiffResult diff) {
        if (prev.root.mTime == curr.root.mTime) return;

        Deque<String> deque = new ArrayDeque<>();
        deque.addAll(prev.root.children);

        while (!deque.isEmpty()) {
            String path = deque.pop();
            FileEntry oldChild = prev.get(path);
            FileEntry newChild = curr.get(path);

            if (newChild == null) { // removed
                diff.deleted().add(path);
            } else if (oldChild.mTime != newChild.mTime) { // changed
                if (newChild.isFile()) {
                    diff.changed().add(path);
                } else {
                    deque.addAll(oldChild.children);
                }
            }
        }

        for (Map.Entry<String, FileEntry> entry: curr.entries.entrySet()) {
            if (!prev.entries.containsKey(entry.getKey()) && entry.getValue().isFile()) {
                diff.changed().add(entry.getKey());
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FileTree[root=").append(root).append("]\n");
        buildString(sb, root.pathStr, 0);
        return sb.toString();
    }

    private void buildString(StringBuilder sb, String path, int depth) {
        sb.append("  ".repeat(depth))
            .append(path)
            .append("\n");
        Set<String> children = entries.get(path).children;
        if (children != null) {
            for (String child : children) {
                buildString(sb, child, depth + 1);
            }
        }
    }
}

// FileSystem cache, exists so that we dont need to call os to walk again and again and again
// Also a MerkleTree
class FileEntry implements Serializable {
    public String pathStr;
    public long mTime;
    public Set<String> children;

    public FileEntry(String pathStr, long mTime, Set<String> children) {
        this.pathStr = pathStr;
        this.mTime =  mTime;
        this.children = children;
    }
    
    public void addChild(String path) {
        children.add(path);
    }

    public boolean isFile() { 
        return children == null; 
    }

    public static FileEntry file(String pathStr, long mTime) {
        return new FileEntry(pathStr, mTime, null);
    }

    public static FileEntry dir(String pathStr, long mTime) {
        return new FileEntry(pathStr, mTime, new HashSet<>());
    }

    @Override
    public String toString() {
        return (isFile() ? "File" : "Dir") + "[" + pathStr + ", mTime=" + mTime + ", children=" + children + "]";
    }
}
