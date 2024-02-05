package fr.aym.gtwnpc.sqript;

import fr.aym.gtwnpc.entity.EntityGtwNpc;
import fr.nico.sqript.actions.ScriptAction;
import fr.nico.sqript.compiling.ScriptException;
import fr.nico.sqript.expressions.ScriptExpression;
import fr.nico.sqript.meta.Action;
import fr.nico.sqript.meta.Feature;
import fr.nico.sqript.structures.ScriptContext;
import fr.nico.sqript.types.TypeItem;
import fr.nico.sqript.types.TypeItemStack;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;

@Action(name = "Set npc equipment",
        features = {
                @Feature(
                        name = "Set npc main hand equipment",
                        description = "Sets the main-hand item of an npc",
                        examples = "set held item of npc entity to \"minecraft:diamond_sword\"",
                        pattern = "set held item of npc {gnpc} to {string}"),
                @Feature(
                        name = "Set npc second hand equipment",
                        description = "Sets the second-hand item of an npc",
                        examples = "set second held item of npc entity to \"minecraft:diamond_sword\"",
                        pattern = "set second held item of npc {gnpc} to {string}"),
                @Feature(
                        name = "Set npc head equipment",
                        description = "Sets the head item of an npc",
                        examples = "set head item of npc entity to \"minecraft:diamond_helmet\"",
                        pattern = "set head item of npc {gnpc} to {string}"),
                @Feature(
                        name = "Set npc chest equipment",
                        description = "Sets the chest item of an npc",
                        examples = "set chest item of npc entity to \"minecraft:diamond_chestplate\"",
                        pattern = "set chest item of npc {gnpc} to {string}"),
                @Feature(
                        name = "Set npc legs equipment",
                        description = "Sets the legs item of an npc",
                        examples = "set legs item of npc entity to \"minecraft:diamond_leggings\"",
                        pattern = "set legs item of npc {gnpc} to {string}"),
                @Feature(
                        name = "Set npc feet equipment",
                        description = "Sets the feet item of an npc",
                        examples = "set feet item of npc entity to \"minecraft:diamond_boots\"",
                        pattern = "set feet item of npc {gnpc} to {string}"),
        })
public class ActionSetNpcEquipment extends ScriptAction {
    @Override
    public void execute(ScriptContext context) throws ScriptException {
        EntityGtwNpc npc = (EntityGtwNpc) getParameter(1, context);
        String itemStr = (String) getParameter(2, context);
        System.out.println("Received item string: " + itemStr);
        Item iitem = Item.getByNameOrId(itemStr);
        if (iitem == null)
            throw new IllegalArgumentException("Unknown item: " + itemStr);
        ItemStack item = new ItemStack(iitem);
        switch (getMatchedIndex()) {
            case 0:
                npc.setHeldItem(EnumHand.MAIN_HAND, item);
                break;
            case 1:
                npc.setHeldItem(EnumHand.OFF_HAND, item);
                break;
            case 2:
                npc.setItemStackToSlot(EntityEquipmentSlot.HEAD, item);
                break;
            case 3:
                npc.setItemStackToSlot(EntityEquipmentSlot.CHEST, item);
                break;
            case 4:
                npc.setItemStackToSlot(EntityEquipmentSlot.LEGS, item);
                break;
            case 5:
                npc.setItemStackToSlot(EntityEquipmentSlot.FEET, item);
                break;
        }
    }
}
