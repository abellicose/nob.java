/* ======================================
 * File: Task.java
 * Date: 2026-05-31
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob;

import nob.build.*;
import java.util.List;
import java.util.ArrayList;

public interface Task {
    String id();

    default List<BucketId> bucketIdsFrom(String... signatures) {
        List<BucketId> result = new ArrayList<>();

        for (int i = 0; i < signatures.length; i++) {
            result.add(new BucketId(signatures[i]));
        }

        return result;
    }

    default List<BucketId> consumes() {
        return List.of();
    }

    default List<BucketId> produces() {
        return List.of();
    }

    // TODO: change to execute before and execute after
    default List<String> dependsOn() { return List.of(); }
    void execute(Context ctx);
}

