/* ======================================
 * File: Bucket.java
 * Date: 2026-08-07
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import java.util.List;
import java.util.ArrayList;

public class Bucket<T> {
    List<T> lists = new ArrayList<>();
    
    public void feed(T t) {
        lists.add(t);
    }

    public List<T> get() {
        List<T> combined = new ArrayList<>();
        for (T list: lists) {
            combined.addAll(list);
        }
        return combined;
    }
}

