/* ======================================
 * File: ProgressDisplay.java
 * Date: 2026-06-11
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

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

class ProgressBar {
    public String name;
    public long totalSize;
    public AtomicLong received;

    public ProgressBar(String name, long totalSize) {
        this.name = name;
        this.totalSize = totalSize;
        this.received = new AtomicLong(0);
    }

    public String render(int width) {
        String prefix = name;
        int progress = (int) (received.get() * 100L / totalSize);
        String suffix = String.format("%4d%%", progress);

        int textWidth = prefix.length() + suffix.length();
        int barWidth = width / 2 - 2;
        int padding = barWidth - textWidth;
        int completedWidth = progress * barWidth / 100;

       return prefix + " ".repeat(padding) + "[" + "#".repeat(completedWidth) + "-".repeat(barWidth - completedWidth) + "]" + suffix;
    }
}

