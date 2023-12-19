package fr.aym.lib;

import net.minecraft.item.ItemBlock;

public class ItemBlockElement extends ItemBlock implements ItemLibElement
{
    public ItemBlockElement(BlockElement block) {
        super(block);
        setRegistryName(block.getRegistryName());
    }

    @Override
    public String getName() {
        return ((BlockElement)block).getName();
    }

    @Override
    public String getTranslationKey(int meta) {
        return "tile."+getName();
    }

    @Override
    public String getTranslatedName(int meta) {
        return ((BlockElement)block).getTranslatedName();
    }
}
