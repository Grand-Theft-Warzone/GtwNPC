package fr.aym.gtwnpc.sqript;

import fr.nico.sqript.actions.ScriptAction;
import fr.nico.sqript.compiling.ScriptException;
import fr.nico.sqript.meta.Action;
import fr.nico.sqript.meta.Feature;
import fr.nico.sqript.structures.ScriptContext;

@Action(name = "Set npc equipment",
        features = @Feature(
                name = "Follow entity",
                description = "Follows an entity",
                examples = "follow entity \"my_npc\" with speed 0.5",
                pattern = "set {gnpc} to follow {entity}"))
public class ActionSetNpcEquipment extends ScriptAction
{
    @Override
    public void execute(ScriptContext context) throws ScriptException {
        super.execute(context);
    }
}
