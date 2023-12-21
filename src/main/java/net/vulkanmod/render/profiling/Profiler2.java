package net.vulkanmod.render.profiling;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

//TODO needs a rework
public class Profiler2 {
    private static final boolean DEBUG = false;
    private static final boolean FORCE_ACTIVE = false;

    private static final float CONVERSION = 1000.0f;
    private static final float INV_CONVERSION = 1.0f / CONVERSION;
    private static final int SAMPLE_NUM = 200;

    private static final float TRIGGER_TIME = 10.0f * 1000;
    public static boolean ACTIVE = FORCE_ACTIVE;

    private static final Profiler2 MAIN_PROFILER = new Profiler2("Main");

    public static Profiler2 getMainProfiler() {
        return MAIN_PROFILER;
    }

    public static void setActive(boolean b) {
        if(!FORCE_ACTIVE)
            ACTIVE = b;

        if(!ACTIVE) {
            MAIN_PROFILER.entriesStack.clear();
            MAIN_PROFILER.slowEntries.clear();
            MAIN_PROFILER.hasStarted = false;
        }
    }

    private final String name;
    private Entries entries;
    private final LinkedList<Entries> entriesStack = new LinkedList<>();
    private boolean hasStarted = false;

    private final LinkedList<Entries> slowEntries = new LinkedList<>();

    public Profiler2(String s) {
        this.name = s;
    }

    public void push(String s) {
        if(ACTIVE && hasStarted)
            entries.push(s);
    }

    public void pop() {
        if(ACTIVE && hasStarted)
            entries.pop();
    }

    public void round() {
        if(!ACTIVE)
            return;

        if(!hasStarted) {
            entries = new Entries();
            hasStarted = true;
            return;
        }

        entries.round();
//        entries.calculateValues();

        //Slow entries
        if(entries.mainNode.value >= TRIGGER_TIME * 2) {
            if(slowEntries.size() > SAMPLE_NUM)
                slowEntries.pollLast();
            slowEntries.push(entries);
        }

        if(entriesStack.size() > SAMPLE_NUM)
            entriesStack.pollLast();
        entriesStack.push(entries);

        entries = new Entries();
    }

    public List<Result> getResults(int... indices) {
        if(!hasStarted || this.entriesStack.isEmpty())
            return null;

        Entries entries = this.entriesStack.getLast();
        Node startNode = entries.mainNode;

        //TODO select index on entries iteration
//        for (int i : indices) {
//            if(i < 0 || i >= startNode.children.size()) {
//                return null;
//            }
//
//            startNode = startNode.children.get(i);
//        }

        var nodes = startNode.children;

        List<Result> results = new ArrayList<>();

        //First is whole start node result
        results.add(new Result(startNode.name));

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
        return results;
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
        final Node mainNode;
        Node currentNode;

        byte level = 0;

        Entries() {
            mainNode = new Node(null, "Main");
            currentNode = mainNode;
        }

        void push(String s) {
            currentNode = new Node(currentNode, s);

            level++;
        }

        void pop() {
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
        final String name;
        final long start;
        float value;

        Node parent;
        List<Node> children = new ObjectArrayList<>();

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
