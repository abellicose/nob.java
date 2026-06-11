/* ======================================
 * File: ProgressDisplay.java
 * Date: 2026-06-11
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.deps;

import java.util.List;
import java.util.ArrayList;

public class ProgressDisplay {
    public static List<ProgressBar> bars = new ArrayList<>();
    public static List<ProgressBar> completed = new ArrayList<>();

    public static void add(ProgressBar bar) {
        bars.add(bar);
    }

    public static void remove(ProgressBar bar) {
        bars.remove(bar);
        completed.add(bar);
    }
}

