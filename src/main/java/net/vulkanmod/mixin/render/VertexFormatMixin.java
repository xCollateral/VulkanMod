package net.vulkanmod.mixin.render;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.vulkanmod.interfaces.VertexFormatMixed;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(VertexFormat.class)
public class VertexFormatMixin implements VertexFormatMixed {
    @Shadow private IntList offsets;

    private ObjectArrayList<VertexFormatElement> fastList;

    public int getOffset(int i) {
        return offsets.getInt(i);
    }

    public VertexFormatElement getElement(int i) {
        return this.fastList.get(i);
    }

    public List<VertexFormatElement> getFastList() {
        return this.fastList;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectList(ImmutableMap<String, VertexFormatElement> immutableMap, CallbackInfo ci) {
        ObjectArrayList<VertexFormatElement> list = new ObjectArrayList<>();
        list.addAll(immutableMap.values());

        this.fastList = list;
    }

}
