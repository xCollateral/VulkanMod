package net.vulkanmod.render.vertex;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

public class CustomVertexFormat {

    public static final VertexFormatElement ELEMENT_POSITION = new VertexFormatElement(0,VertexFormatElement.Type.SHORT, VertexFormatElement.Usage.POSITION, 4);
    public static final VertexFormatElement ELEMENT_COLOR = new VertexFormatElement(0, VertexFormatElement.Type.UBYTE, VertexFormatElement.Usage.COLOR, 4);
    public static final VertexFormatElement ELEMENT_UV0 = new VertexFormatElement(0, VertexFormatElement.Type.USHORT, VertexFormatElement.Usage.UV, 2);
    public static final VertexFormatElement ELEMENT_UV2 = new VertexFormatElement(2, VertexFormatElement.Type.SHORT, VertexFormatElement.Usage.UV, 2);

    public static final VertexFormat COMPRESSED_TERRAIN = new VertexFormat(new ImmutableMap.Builder<String, VertexFormatElement>()
        .put("Position",ELEMENT_POSITION).put("Color",ELEMENT_COLOR).put("UV0",ELEMENT_UV0).put("UV2",ELEMENT_UV2).build());

    public static final VertexFormat NONE = new VertexFormat(ImmutableMap.of());
}
