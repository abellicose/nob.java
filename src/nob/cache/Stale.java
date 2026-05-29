/* ======================================
 * File: Stale.java
 * Date: 2026-05-29
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.cache;

public class Stale {
    public static DiffResult check(BuildContext ctx) throws Exception {
        MerkleNode curr = MerkleNode.build(ctx.src);
        if (ctx.merkleCache == null) {
            ctx.merkleCache = curr;
            return new DiffResult(curr.collectAllFiles(), List.emptyList());
        }
        List<String> changed = new ArrayList<>();
        List<String> deleted = new ArrayList<>();
        MarkleNode.diff(ctx.merkleCache, curr, changed, deleted);
        ctx.merkleCache = curr;
        return new DiffResult(changed, deleted);
    }
}

