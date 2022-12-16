package net.vulkanmod.interfaces;

import net.vulkanmod.render.VBO;

public interface VBOPxy {
    VBO getCurrentVBO();

    void translateChunk(int a);
}
