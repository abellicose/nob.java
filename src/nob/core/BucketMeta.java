/* ======================================
 * File: BucketMeta.java
 * Date: 2026-11-07
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.core;

import java.lang.reflect.Type;

record BucketMeta(String bucketName, Type bucketType) {
    @Override
    public int hashCode() {
        return bucketName.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BucketMeta meta)) {
            return false;
        }
        return bucketName.equals(meta.bucketName());
    }
}

