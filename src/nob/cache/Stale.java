/* ======================================
 * File: Stale.java
 * Date: 2026-05-29
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.cache;

import java.util.List;
import java.util.ArrayList;

public class Stale {
    public static DiffResult check(BuildContext ctx) throws Exception {
        System.out.println("HI");
        MerkleNode curr = MerkleNode.build(ctx.src);
        if (ctx.merkleCache == null) {
            System.out.println("[nob] merkleCache is null");
            ctx.merkleCache = curr;
            return new DiffResult(curr.collectAllFiles(), List.of());
        }
        List<String> changed = new ArrayList<>();
        List<String> deleted = new ArrayList<>();
        MerkleNode.diff(ctx.merkleCache, curr, changed, deleted);
        ctx.merkleCache = curr;
        return new DiffResult(changed, deleted);
    }
}

