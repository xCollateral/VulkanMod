package net.vulkanmod.render.profiling;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.vulkanmod.Initializer;

import java.util.List;

public class Profiler {
    private static final boolean DEBUG = false;
    private static final boolean FORCE_ACTIVE = false;

    private static final int NANOS_IN_MS = 1000000;
    private static final float CONVERSION = NANOS_IN_MS;
    private static final float INV_CONVERSION = 1.0f / CONVERSION;
    private static final int SAMPLE_COUNT = 200;

    public static boolean ACTIVE = FORCE_ACTIVE;

    private static final Profiler MAIN_PROFILER = new Profiler("Main");

    public static Profiler getMainProfiler() {
        return MAIN_PROFILER;
    }

    public static void setActive(boolean b) {
        if (!FORCE_ACTIVE)
            ACTIVE = b;

    }

    private final String name;

    LongArrayList startTimes = new LongArrayList();
    ObjectArrayList<Node> nodeStack = new ObjectArrayList<>();

    ObjectArrayList<Node> nodes = new ObjectArrayList<>();
    Object2ReferenceOpenHashMap<String, Node> nodeMap = new Object2ReferenceOpenHashMap<>();

    Node mainNode;
    Node selectedNode;
    Node currentNode;

    ProfilerResults profilerResults = new ProfilerResults();

    public Profiler(String s) {
        this.name = s;
        this.currentNode = this.selectedNode = this.mainNode = new Node(s);
    }

    public void push(String s) {
        if (!(ACTIVE))
            return;

        Node node = nodeMap.get(s);

        if (node == null) {
            node = new Node(s);
            nodeMap.put(s, node);

            currentNode.addChild(node);
        }

        node.setParent(currentNode);
        node.children.clear();

        if (node.parent == selectedNode)
            nodes.add(node);

        currentNode = node;

        pushNodeStack(node);
    }

    private void pushNodeStack(Node node) {
        long startTime = System.nanoTime();
        startTimes.push(startTime);
        nodeStack.push(node);
    }

    public void pop() {
        if (!(ACTIVE))
            return;

        if (nodeStack.isEmpty()) {
            if (DEBUG)
                Initializer.LOGGER.error("Profiler %s: Pop called with no more nodes on the stack".formatted(name));

            return;
        }

        int i = nodeStack.size() - 1;
        Node node = nodeStack.remove(i);
        long startTime = startTimes.removeLong(i);
        long deltaMs = (System.nanoTime() - startTime);

        node.push(deltaMs);

        currentNode = currentNode.parent;
    }

    public void start() {
        if (!(ACTIVE))
            return;

        if (!nodeStack.isEmpty()) {
            if (DEBUG)
                Initializer.LOGGER.error("Profiler %s: Node stack is not empty".formatted(name));

            nodeStack.clear();
            startTimes.clear();
        }

        currentNode = mainNode;
        mainNode.children.clear();

        pushNodeStack(mainNode);

        nodes.clear();
    }

    public void end() {
        if (!(ACTIVE))
            return;

        if (DEBUG && currentNode != mainNode) {
            Initializer.LOGGER.error("Profiler %s: current node is not the main node".formatted(name));
        }

        this.pop();
    }

    public void round() {
        this.end();
        this.start();
    }

    public ProfilerResults getProfilerResults() {
        profilerResults.update(selectedNode, nodes);
        return this.profilerResults;
    }

    public static class ProfilerResults {
        Result result;
        ObjectArrayList<Result> partialResults = new ObjectArrayList<>();

        public void update(Node mainNode, List<Node> nodes) {
            mainNode.updateResult();
            result = mainNode.result;

            partialResults.clear();

            for (Node node : nodes) {
                node.updateResult();
                partialResults.push(node.result);
            }
        }

        public Result getResult() {
            return result;
        }

        public ObjectArrayList<Result> getPartialResults() {
            return partialResults;
        }
    }

    public static class Result {
        public final String name;
        public float value;

        public Result(String name) {
            this.name = name;
        }

        void setValue(float value) {
            this.value = value;
        }
    }

    public static class Node {
        final String name;
        Node parent;
        List<Node> children = new ObjectArrayList<>();

        long maxDuration;
        long minDuration;

        LongArrayFIFOQueue values = new LongArrayFIFOQueue(SAMPLE_COUNT);

        long accumulatedDuration;

        Result result;

        Node(String name) {
            this.name = name;

            this.result = new Result(name);
            this.reset();
        }

        void setParent(Node node) {
            this.parent = node;
            node.addChild(this);
        }

        void addChild(Node node) {
            children.add(node);
        }

        void push(long duration) {
            if (duration < minDuration) {
                minDuration = duration;
            }
            if (duration > maxDuration) {
                maxDuration = duration;
            }

            if (values.size() >= SAMPLE_COUNT) {
                accumulatedDuration -= values.dequeueLong();
            }

            values.enqueue(duration);
            accumulatedDuration += duration;
        }

        public void updateResult() {
            this.result.setValue((float) this.accumulatedDuration / this.values.size() * INV_CONVERSION);
        }

        void reset() {
            minDuration = Long.MAX_VALUE;
            maxDuration = Long.MIN_VALUE;

            accumulatedDuration = 0;
        }

        public String toString() {
            return this.name;
        }

    }
}

