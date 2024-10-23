package net.vulkanmod.config.gui.option;

import com.google.common.collect.ImmutableList;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class OptionPage {
    private final Component name;
    private final ImmutableList<OptionGroup> groups;

    private OptionPage(Component name, ImmutableList<OptionGroup> groups) {
        this.name = name;
        this.groups = groups;
    }

    public static OptionPage.@NotNull Builder createBuilder() {
        return new Builder();
    }

    public static @NotNull OptionPage getDummy() {
        return new OptionPage(Component.empty(), ImmutableList.of());
    }

    public @NotNull Component name() {
        return name;
    }

    public @NotNull ImmutableList<OptionGroup> groups() {
        return groups;
    }

    public @NotNull ImmutableList<Option<?>> options() {
        return groups.stream()
                .flatMap(group -> group.options().stream())
                .collect(ImmutableList.toImmutableList());
    }

    public OptionPage filtered(@NotNull Predicate<Option<?>> filter) {
        Validate.notNull(filter, "`filter` must not be null");

        ImmutableList<OptionGroup> filteredGroups = groups.stream()
                .map(group -> group.filtered(filter))
                .collect(ImmutableList.toImmutableList());

        return new OptionPage(name, filteredGroups);
    }

    public static class Builder {
        private final List<OptionGroup> groups = new ArrayList<>();
        private Component name;

        private Builder() {
        }

        public Builder name(@NotNull Component name) {
            Validate.notNull(name, "`name` cannot be null");

            this.name = name;
            return this;
        }

        public Builder group(@NotNull OptionGroup group) {
            Validate.notNull(group, "`group` must not be null");

            this.groups.add(group);
            return this;
        }

        public Builder groups(@NotNull Collection<OptionGroup> groups) {
            Validate.notEmpty(groups, "`groups` must not be empty");

            this.groups.addAll(groups);
            return this;
        }

        public OptionPage build() {
            Validate.notNull(name, "`name` must not be null to build `OptionPage`");
            Validate.notEmpty(groups, "at least one group must be added to build `OptionPage`");

            return new OptionPage(name, ImmutableList.copyOf(groups));
        }
    }
}
