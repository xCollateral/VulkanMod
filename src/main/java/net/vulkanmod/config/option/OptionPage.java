package net.vulkanmod.config.option;

import net.vulkanmod.config.gui.OptionBlock;
import net.vulkanmod.config.gui.VOptionList;

public class OptionPage {
    public final String name;
    OptionBlock[] optionBlocks;
    private VOptionList optionList;

    public OptionPage(String name, OptionBlock[] optionBlocks) {
        this.name = name;
        this.optionBlocks = optionBlocks;
    }

    public void createList(int x, int y, int width, int height, int itemHeight) {
        this.optionList = new VOptionList(x, y, width, height, itemHeight);
        this.optionList.addAll(optionBlocks);
    }

    public VOptionList getOptionList() {
        return this.optionList;
    }

    public boolean optionChanged() {
        boolean changed = false;
        for (var block : this.optionBlocks) {
            for (var option : block.options()) {
                if (option.isChanged())
                    changed = true;
            }
        }
        return changed;
    }

    public void applyOptionChanges() {
        for (var block : this.optionBlocks) {
            for (var option : block.options()) {
                if (option.isChanged())
                    option.apply();
            }
        }
    }
}
