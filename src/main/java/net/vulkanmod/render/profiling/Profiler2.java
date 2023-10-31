package net.vulkanmod.render.profiling;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

//TODO
public class Profiler2 {
    private static final boolean DEBUG = false;

    private static final float CONVERSION = 1000.0f;
    private static final float INV_CONVERSION = 1.0f / CONVERSION;
    private static final long POLL_PERIOD = 100000000;
    private static final int SAMPLE_NUM = 200;

    private static final float TRIGGER_TIME = 10.0f * 1000;
    private static final boolean ACTIVE = true;

    private static final Profiler2 MAIN_PROFILER = new Profiler2("Main");

    public static Profiler2 getMainProfiler() {
        return MAIN_PROFILER;
    }

    private final String name;
    private Entries entries;
    private final LinkedList<Entries> entriesStack = new LinkedList<>();
    private long startTime;
    private long endTime;
    private boolean hasStarted = false;

    private final LinkedList<Entries> slowEntries = new LinkedList<>();

    private List<Result> lastResults;
    private long lastPollTime;

    public Profiler2(String s) {
        this.name = s;
        entries = new Entries(s);
    }

    public void start() {
        if(!ACTIVE)
            return;
        if(this.hasStarted)
            this.round();

//        this.startTime = System.nanoTime();
        this.hasStarted = true;
    }

    public void push(String s) {
        //long time = entries.get(entries.size() - 1).getB();
//        float time = convert(System.nanoTime() - startTime);
//        float time = System.nanoTime();
//        entries.values.add(new Entry(s, time));
        if(ACTIVE)
            entries.push(s);
    }

    public void pop() {
        if(ACTIVE)
            entries.pop();
    }

//    public void pushMilestone(String s) {
//        //long time = entries.get(entries.size() - 1).getB();
//        float time = convert(System.nanoTime() - startTime);
//        entries.milestones.add(new Entry(s, time));
//    }

//    public void end() {
////        Profiler.setCurrentProfiler(defaultProfiler);
//        this.hasStarted = false;
//        entries.values.clear();
////        entries.milestones.clear();
//    }

//    public static Profiler getProfiler(String name) {
//        return activeProfilers.computeIfAbsent(name, Profiler::new);
//    }

    public void round() {
        if(!ACTIVE)
            return;

        entries.round();
//        entries.calculateValues();

        if(entries.mainNode.value >= TRIGGER_TIME * 2) {
            if(slowEntries.size() > SAMPLE_NUM) slowEntries.pollLast();
            slowEntries.push(entries);
        }

        if(entriesStack.size() > SAMPLE_NUM) entriesStack.pollLast();
        entriesStack.push(entries);
        entries = new Entries(name);
        this.hasStarted = false;
    }

    public List<Result> getResults() {
        if((System.nanoTime() - lastPollTime) < POLL_PERIOD && lastResults != null)
            return lastResults;

        Entries entries = this.entriesStack.getLast();

        var nodes = entries.mainNode.children;

//        List<String> names = new ArrayList<>();
//        List<Float> values = new ArrayList<>();

        List<Result> results = new ArrayList<>();

        results.add(new Result(entries.mainNode.name));

        for (Node node : nodes) {
            results.add(new Result(node.name));
        }

        int resultSize = results.size();

        for(Entries entries1 : entriesStack) {
            results.get(0).addValue(entries1.mainNode.value);

            nodes = entries1.mainNode.children;
            Node node;
            for (int i = 0; i < nodes.size(); i++) {
                node = nodes.get(i);

                if(i+1 >= resultSize)
                    break;
                results.get(i+1).addValue(node.value);
            }
        }

        //Instantaneous
//        Entries entries1 = entriesStack.getLast();
//
//        results.get(0).addValue(entries1.mainNode.value);
//
//        nodes = entries1.mainNode.children;
//        Node node;
//        for (int i = 0; i < nodes.size(); i++) {
//            node = nodes.get(i);
//
//            if(i+1 >= resultSize)
//                break;
//            results.get(i+1).addValue(node.value);
//        }

        results.forEach(Result::computeAvg);

        lastPollTime = System.nanoTime();
        return lastResults = results;
    }

    public static class Result {
        public final String name;
        float value = 0;
        int count = 0;


        public Result(String name) {
            this.name = name;
        }

        public void addValue(float f) {
            value += f;
            count++;
        }

        public float computeAvg() {
            return value /= count * 1000.0f;
        }

        public float getValue() { return value; }

        public String toString() {
            return String.format("%s: %.3f", name, value);
        }
    }

    private static float convert(float v) {
        return v * INV_CONVERSION;
    }

    private static class Entries {
//        LinkedList<Node> stack = new LinkedList<>();
//        LinkedList<Entry> values = new LinkedList<>();
//        Object2FloatMap<String> valueMap;
        Node mainNode;
        Node currentNode;

        byte level = 0;

        Entries(String name) {
            mainNode = new Node(null, name);
            currentNode = mainNode;
        }

        void push(String s) {
            //            this.stack.add(node);
            currentNode = new Node(currentNode, s);

            level++;
        }

        void pop() {
//            Node entry = this.stack.pop();
//            this.values.add(new Node(entry.name, convert(endTime - entries.deltaTime)));
            Node parent = currentNode.parent;

            if (parent == null)
                return;

            currentNode.computeDelta();
            parent.addChild(currentNode);
            currentNode = parent;

            level--;
        }

        void round() {
            if(DEBUG && level != 0) {
                System.err.println("Profiler stack level is not 0");

                level = 0;
            }

            this.mainNode.computeDelta();
        }

        public Node getNodeFromPath(byte[] indices) {
            Node node = mainNode;

            for(byte i : indices) {
                Node next = node.children.get(i);
                if(next == null)
                    return null;
                node = next;
            }

            return node;
        }

        public String toString() {
            StringBuilder s = new StringBuilder();
            s.append("total time: ");
            s.append(this.mainNode.value);
            s.append(" | ");

            for(Node entry : this.mainNode.children) {
                s.append(" ").append(entry.name).append(": ").append(entry.value);
            }

            return s.toString();
        }
    }

    private static class Node {
//        byte level, index;
        String name;
        float value;
        long start;

        Node parent;
        LinkedList<Node> children = new LinkedList<>();

//        public Node(@Nullable Node parent, String name, float value) {
//            this.parent = parent;
//            this.name = name;
//            this.value = value;
//        }

        public Node(@Nullable Node parent, String name) {
            this.parent = parent;
            this.name = name;
            this.start = System.nanoTime();
        }

        void addChild(Node node) {
            children.add(node);
        }

        void computeDelta() {
            value = convert(System.nanoTime() - start);
        }

        public String toString() {
//            return this.name + ": " + this.value;

            StringBuilder s = new StringBuilder();
            s.append(this.name);
            s.append(": ");
            s.append(this.value);
            s.append(" | ");

            for(Node entry : this.children) {
                s.append(" ").append(entry.name).append(": ").append(entry.value);
            }

            return s.toString();
        }
    }
}
