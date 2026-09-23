package net.vulkanmod.config.api.page;

import net.vulkanmod.config.api.VkModSettingsEntryBuilder;
import net.vulkanmod.config.gui.OptionBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class VkPageOptionsBuilder {
    private final VkModSettingsEntryBuilder parent;
    private final List<Supplier<OptionBlock>> optionBlockSuppliers = new ArrayList<>();

    public VkPageOptionsBuilder(VkModSettingsEntryBuilder parent) {
        this.parent = parent;
    }

    public VkOptionsBuilder withOptionBlock(String name) {
        VkOptionsBuilder builder = new VkOptionsBuilder(this);
        optionBlockSuppliers.add(() -> new OptionBlock(name, builder.build()));
        return builder;
    }

    public VkModSettingsEntryBuilder finish() {
        return parent;
    }

    public OptionBlock[] build() {
        final OptionBlock[] blocks = new OptionBlock[optionBlockSuppliers.size()];
        int i = 0;
        for (Supplier<OptionBlock> optionBlockSupplier : optionBlockSuppliers) {
            blocks[i] = optionBlockSupplier.get();
            i++;
        }
        return blocks;
    }
}
