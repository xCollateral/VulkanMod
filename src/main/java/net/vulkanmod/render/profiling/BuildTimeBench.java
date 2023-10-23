package net.vulkanmod.render.profiling;

import net.minecraft.client.Minecraft;
import net.vulkanmod.render.chunk.build.TaskDispatcher;

public abstract class BuildTimeBench {

    private static boolean bench = false;
    private static long startTime;
    private static float benchTime;

    public static void runBench(boolean building, TaskDispatcher taskDispatcher) {
        if(bench) {
            if (startTime == 0) {
                startTime = System.nanoTime();
            }

            if(!building && taskDispatcher.isIdle()) {
                benchTime = (System.nanoTime() - startTime) * 0.000001f;
                bench = false;
                startTime = 0;
            }
        }
    }

    public static void startBench() {
        bench = true;
        Minecraft.getInstance().levelRenderer.allChanged();
    }

    public static float getBenchTime() {
        return benchTime;
    }
}
