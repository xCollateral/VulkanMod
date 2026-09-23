package net.vulkanmod.config.api.page;

import net.vulkanmod.config.api.VkModSettingsEntryBuilder;
import net.vulkanmod.config.option.OptionPage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class VkPagesBuilder {
    private final List<Supplier<OptionPage>> pageSuppliers = new ArrayList<>();

    public VkPageOptionsBuilder withPage(String name, VkModSettingsEntryBuilder parent) {
        VkPageOptionsBuilder builder = new VkPageOptionsBuilder(parent);
        pageSuppliers.add(() -> new OptionPage(name, builder.build()));
        return builder;
    }

    public List<OptionPage> build() {
        final List<OptionPage> pages = new ArrayList<>(pageSuppliers.size());
        for (Supplier<OptionPage> pageSupplier : pageSuppliers) {
            pages.add(pageSupplier.get());
        }
        return pages;
    }
}
