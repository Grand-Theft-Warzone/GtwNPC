package fr.aym.lib;

import net.minecraft.item.Item;

public class ItemElement extends Item implements ItemLibElement {
    private final String name, translatedName;

    public ItemElement(String name) {
        this.name = name;
        this.translatedName = name;
        this.setRegistryName(LibContext.ID, name);
        this.setTranslationKey(name);
        LibRegistry.registerItem(this);
    }

    @Override
    public String getName() {
        return getTranslationKey().substring(5);
    }
}
