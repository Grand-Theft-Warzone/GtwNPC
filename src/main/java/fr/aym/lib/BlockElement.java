package fr.aym.lib;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;

public class BlockElement extends Block implements LibElement
{
    public BlockElement(Material blockMaterialIn, MapColor blockMapColorIn) {
        super(blockMaterialIn, blockMapColorIn);
        LibRegistry.registerBlock(this);
    }

    public BlockElement(Material materialIn) {
        super(materialIn);
        LibRegistry.registerBlock(this);
    }

    @Override
    public String getName() {
        return getTranslationKey().substring(5);
    }

    /**
     * For default name in lang files, used in dev env
     * @see LibContext
     */
    public String getTranslatedName() {
        return getName();
    }
}
