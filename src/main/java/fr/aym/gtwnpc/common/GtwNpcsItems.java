package fr.aym.gtwnpc.common;

import fr.aym.gtwnpc.item.ItemNodes;
import fr.aym.lib.LibRegistry;
import net.minecraftforge.common.MinecraftForge;

public class GtwNpcsItems
{
    public static final ItemNodes itemNodes = new ItemNodes();

    public static void registerItems() {
        MinecraftForge.EVENT_BUS.register(new LibRegistry());
    }
}
