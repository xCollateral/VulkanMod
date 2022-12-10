package net.vulkanmod.render;

import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;

import java.util.*;

public class Profiler {
    private static Profiler defaultProfiler = new Profiler("default");
    private static Profiler currentProfiler;
    private static Map<String, Profiler> activeProfilers = new HashMap<>();

    private final String name;
    private Entries entries =  new Entries();
    private LinkedList<Entries> entriesStack = new LinkedList<>();
    private static final float conversion = 1000.0f;
    private static final float invConversion = 1 / conversion;
    private long startTime;
    private long endTime;
    private boolean hasStarted = false;

    static {
        currentProfiler = defaultProfiler;
    }

    public Profiler(String s) {
        this.name = s;
    }

    public void start() {
        if(this.hasStarted) this.round();
        this.startTime = System.nanoTime();
        this.hasStarted = true;
    }

    public void push(String s) {
        //long time = entries.get(entries.size() - 1).getB();
        float time = convert(System.nanoTime() - startTime);
        entries.values.add(new Entry(s, time));
    }

    public void pushMilestone(String s) {
        //long time = entries.get(entries.size() - 1).getB();
        float time = convert(System.nanoTime() - startTime);
        entries.milestones.add(new Entry(s, time));
    }

    public void end() {
        Profiler.setCurrentProfiler(defaultProfiler);
        this.hasStarted = false;
        entries.values.clear();
        entries.milestones.clear();
    }

    public static Profiler getProfiler(String name) {
        return activeProfilers.computeIfAbsent(name, Profiler::new);
    }

    public void round() {
        entries.setDelta();
        entries.calculateValues();

        if(entriesStack.size() > 100) entriesStack.pollLast();
        entriesStack.push(entries);
        entries = new Entries();
        this.hasStarted = false;
    }

    private static float convert(float v) {
        return v * invConversion;
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

    public static void testOwnProfilerTime() {}

    private class Entries {
        List<Entry> values = new ArrayList<>();
        List<Entry> milestones = new ArrayList<>();
        Object2FloatMap<String> valueMap;
        float deltaTime;

        public void calculateValues() {
            if(this.values.size() == 0) return;

            this.valueMap = new Object2FloatOpenHashMap<>();

            this.valueMap.put(this.values.get(0).name, this.values.get(0).value);

            for(int i = 1; i < this.values.size(); ++i) {
                Entry entry = this.values.get(i);

                if(valueMap.containsKey(entry.name)) {
                    Entry prevEntry = this.values.get(i - 1);

                    float delta = entry.value - prevEntry.value;
                    this.valueMap.put(entry.name, delta + this.valueMap.getOrDefault(entry.name, 0.0f));
                } else {
                    this.valueMap.put(entry.name, entry.value);
                }
            }
        }

        public void setDelta() {
            long endTime = System.nanoTime();
            this.deltaTime = convert(endTime - startTime);
        }

        public String toString() {
            String s = "total time: " + this.deltaTime + " | ";

            for(Entry milestone : this.milestones) {
                s += " " +  milestone.name + ": " + milestone.value;
            }

            return s;
        }
    }

    private static class Entry {
        String name;
        float value;

        public Entry(String name, float value) {
            this.name = name;
            this.value = value;
        }

        public String toString() {
            return this.name + ": " + this.value;
        }
    }
}