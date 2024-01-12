package fr.aym.gtwnpc.sqript;

import fr.aym.gtwnpc.entity.EntityGtwNpc;
import fr.nico.sqript.events.ScriptEvent;
import fr.nico.sqript.meta.Event;
import fr.nico.sqript.meta.Feature;
import fr.nico.sqript.structures.ScriptTypeAccessor;
import net.minecraftforge.fml.common.eventhandler.Cancelable;

@Event(
        feature = @Feature(
                name = "Gtw Npc init",
                description = "Called on GtwNpc entity init.",
                examples = {"on gtw entity init:"},
                pattern = "gtw entity init"
        ),
        accessors = {@Feature(
                name = "Gtw Npc",
                description = "The npc entity.",
                pattern = "entity",
                type = "gnpc"
        )}
)
@Cancelable
public class EventGNpcInit extends ScriptEvent {
    public EventGNpcInit(EntityGtwNpc entity) {
        super(new ScriptTypeAccessor[]{new ScriptTypeAccessor(new TypeGNpc(entity), "entity")});
    }
}
