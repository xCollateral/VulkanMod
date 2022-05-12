package net.vulkanmod.vulkan;

import oshi.util.tuples.Pair;

import java.util.ArrayList;
import java.util.List;

public class Profiler {
    private static Profiler defaultProfiler = new Profiler("default");
    private static Profiler currentProfiler;
    private final String name;
    private List<Pair<String, Long>> entries =  new ArrayList<>();
    private final int conversion = 1000;
    private long startTime;

    static {
        currentProfiler = defaultProfiler;
    }

    public Profiler(String s) {
        this.name = s;
    }

    public void start() {
        startTime = System.nanoTime()/conversion;
    }

    public void push(String s) {
        //long time = entries.get(entries.size() - 1).getB();
        entries.add(new Pair<>(s, System.nanoTime()/conversion - startTime));
    }

    public void end() {
        currentProfiler = defaultProfiler;
        entries.clear();
    }

    public static void Start() {
        currentProfiler.start();
    }

    public static void Push(String s) {
        currentProfiler.push(s);
    }

    public static void End() {
        currentProfiler.end();
    }

    public static void setCurrentProfiler(Profiler p) {
        currentProfiler = p;
    }

}
