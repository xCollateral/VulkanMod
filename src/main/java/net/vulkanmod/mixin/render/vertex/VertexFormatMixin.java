package net.vulkanmod.mixin.render.vertex;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.vulkanmod.interfaces.VertexFormatMixed;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(VertexFormat.class)
public class VertexFormatMixin implements VertexFormatMixed {

    private int[] offsets;

    private ObjectArrayList<VertexFormatElement> fastList;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectList(List<VertexFormatElement> list, List<String> list2, IntList intList, int i, CallbackInfo ci) {
        ObjectArrayList<VertexFormatElement> fList = new ObjectArrayList<>();
        fList.addAll(list);

        this.fastList = fList;

        this.offsets = intList.toIntArray();
    }

    public int getOffset(int i) {
        return this.offsets[i];
    }

    public VertexFormatElement getElement(int i) {
        return this.fastList.get(i);
    }

    public List<VertexFormatElement> getFastList() {
        return this.fastList;
    }

}
