/* ======================================
 * File: ExecutionContext.java
 * Date: 2026-08-15
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import nob.NobException;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Type;

public class ExecutionContext {
    private final PluginId pluginId;
    private final Map<BucketId, BucketInfo> bucketRegistry;

    public ExecutionContext(PluginId pluginId, Map<BucketId, BucketInfo> bucketRegistry) {
        this.pluginId = pluginId;
        this.bucketRegistry = bucketRegistry;
    }

    public <T> Bucket<T> getBucket(String rawId, Type expectedType) {
        if (expectedType == null) {
            throw new NobException(
                "Cannot get bucket '" + rawId + "': expectedType must not be null.");
        }
        BucketId bucketId = getBucketId(rawId);
        BucketInfo bucketInfo = bucketRegistry.get(bucketId);
        if (bucketInfo == null) {
            throw new NobException(
                "Cannot get bucket '" + rawId + "' (resolved id: '" + bucketId + "'): no bucket registered " +
                "under this id. Check that it was registered via registerBucket() before this point.");
        }
        return castBucket(bucketId, bucketInfo, expectedType);
    }

    @SuppressWarnings("unchecked")
    private <T> Bucket<T> castBucket(BucketId bucketId, BucketInfo bucketInfo, Type expectedType) {
        Bucket<T> bucket = (Bucket<T>) bucketInfo.bucket();
        if (!expectedType.equals(bucket.getType())) {
            throw new NobException(
                "Bucket '" + bucketId + "' was registered with type '" + bucket.getType().getTypeName() +
                "' but requested as '" + expectedType.getTypeName() + "'.");
        }
        return bucket;
    }

    public <T> void addToBucket(String rawId, T value) {
        BucketId bucketId = getBucketId(rawId);
        BucketInfo bucketInfo = bucketRegistry.get(bucketId);
        if (bucketInfo == null) {
            throw new NobException(
                "Cannot add to bucket '" + rawId + "' (resolved id: '" + bucketId + "'): no bucket registered " +
                "under this id. Check that it was registered via registerBucket() before this point.");
        }
        addValue(bucketId, bucketInfo, value);
    }

    @SuppressWarnings("unchecked")
    private <T> void addValue(BucketId bucketId, BucketInfo bucketInfo, T value) {
        ((Bucket<T>) bucketInfo.bucket()).add(value);
    }

    private BucketId getBucketId(String rawId) {
        if (rawId == null || rawId.isEmpty()) {
            throw new NobException(
                "Cannot resolve bucket: rawId must not be null or empty.");
        }
        BucketId bucketId;
        if (Signatures.isForeign(rawId)) {
            bucketId = new BucketId(rawId);
            if (bucketId.pluginId.equals(pluginId)) {
                throw new NobException(
                    "Cannot resolve bucket '" + rawId + "': plugin '" + pluginId.rawId + "' foreign-referenced " +
                    "its own bucket. Use the unprefixed local name '" + bucketId + "' instead.");
            }
        } else {
            bucketId = new BucketId(pluginId, rawId);
        }
        return bucketId;
    }
}
