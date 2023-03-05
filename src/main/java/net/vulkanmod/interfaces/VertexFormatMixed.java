package net.vulkanmod.interfaces;

import com.mojang.blaze3d.vertex.VertexFormatElement;

import java.util.List;

public interface VertexFormatMixed {

    int getOffset(int i);

    List<VertexFormatElement> getFastList();
}
