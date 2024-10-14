package net.vulkanmod.config.gui.option;

import com.google.common.collect.ImmutableList;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class OptionGroup {
    private final @NotNull Component name;
    private final ImmutableList<Option<?>> options;

    private OptionGroup(@NotNull Component name, ImmutableList<Option<?>> options) {
        this.name = name;
        this.options = options;
    }

    public static OptionGroup.@NotNull Builder createBuilder() {
        return new Builder();
    }

    public @NotNull Component name() {
        return name;
    }

    public @NotNull ImmutableList<Option<?>> options() {
        return options;
    }

    public OptionGroup filtered(Predicate<Option<?>> filter) {
        ImmutableList<Option<?>> filteredOptions = options().stream()
                .filter(filter)
                .collect(ImmutableList.toImmutableList());

        return new OptionGroup(name, filteredOptions);
    }

    public static class Builder {
        private final List<Option<?>> options = new ArrayList<>();
        private Component name = Component.empty();

        private Builder() {
        }

        public Builder name(@NotNull Component name) {
            Validate.notNull(name, "`name` must not be null");

            this.name = name;
            return this;
        }

        public Builder option(@NotNull Option<?> option) {
            Validate.notNull(option, "`option` must not be null");

            this.options.add(option);
            return this;
        }

        public Builder options(@NotNull Collection<Option<?>> options) {
            Validate.notEmpty(options, "`options` must not be empty");

            this.options.addAll(options);
            return this;
        }

        public OptionGroup build() {
            Validate.notEmpty(options, "`options` must not be empty to build `OptionGroup`");

            return new OptionGroup(name, ImmutableList.copyOf(options));
        }
    }
}
