package fr.aym.lib;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

public class LibRegistry {
    private static final List<BlockElement> blocks = new ArrayList<>();
    private static final List<ItemLibElement> items = new ArrayList<>();

    @SubscribeEvent
    public void registerBlocks(RegistryEvent.Register<Block> event) {
        blocks.forEach(b -> event.getRegistry().register(b));
    }

    @SubscribeEvent
    public void registerItems(RegistryEvent.Register<Item> event) {
        for (BlockElement block : blocks)
            items.add(new ItemBlockElement(block));
        items.forEach(b -> event.getRegistry().register((Item) b));
    }

    public static void registerBlock(BlockElement block) {
        blocks.add(block);
    }

    public static void registerItem(ItemLibElement item) {
        if (!(item instanceof Item))
            throw new IllegalArgumentException("ItemLibElement " + item + " is not a minecraft Item !");
        items.add(item);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void registerItemModels(ModelRegistryEvent event) {
        if (LibContext.CREATE_MISSING_JSONS)
            blocks.forEach(b -> ContentPackUtils.addMissingJSONs(b, Minecraft.getMinecraft().gameDir));
        items.forEach(items -> {
            for (int i = 0; i < items.getMaxMeta(); i++) {
                registerModel(items, i);
            }
        });
    }

    @SideOnly(Side.CLIENT)
    public static void registerModel(ItemLibElement item, int metadata) {
        if (LibContext.CREATE_MISSING_JSONS && item.createJson(metadata)) {
            ContentPackUtils.addMissingJSONs(item, Minecraft.getMinecraft().gameDir, metadata);
        }
        if (LibContext.CREATE_MISSING_TRANSLATIONS && item.createTranslation()) {
            ContentPackUtils.addMissingLangFile(Minecraft.getMinecraft().gameDir, item, metadata);
        }
        String resourceName = LibContext.ID + ":" + item.getJsonName(metadata);
        /*if (item.useObjModel(metadata))
            ClientProxy.objItemModelLoader.registerItemModel((Item) item, metadata, (new ResourceLocation(resourceName)));
        else*/
        ModelLoader.setCustomModelResourceLocation((Item) item, metadata, new ModelResourceLocation(resourceName, "inventory"));
    }
}
