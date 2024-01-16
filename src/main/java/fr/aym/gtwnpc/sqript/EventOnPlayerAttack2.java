package fr.aym.gtwnpc.sqript;

import fr.nico.sqript.events.ScriptEvent;
import fr.nico.sqript.meta.Event;
import fr.nico.sqript.meta.Feature;
import fr.nico.sqript.structures.ScriptTypeAccessor;
import fr.nico.sqript.types.ScriptType;
import fr.nico.sqript.types.TypeEntity;
import fr.nico.sqript.types.TypePlayer;
import fr.nico.sqript.types.primitive.TypeResource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

@Cancelable
@Event(
        feature = @Feature(
                name = "Player attack",
                description = "Called when a player attacks another entity.",
                examples = {"on player2 attacking minecraft:pig :\n    cancel event #Removes pvp"},
                pattern = "player2 attack[ing] [{entity|resource}]"
        ),
        accessors = {@Feature(
                name = "Attacker",
                description = "The player that attacked.",
                pattern = "attacker",
                type = "player"
        ), @Feature(
                name = "Victim",
                description = "The entity that was attacked.",
                pattern = "victim",
                type = "entity"
        )}
)
public class EventOnPlayerAttack2 extends ScriptEvent {
    protected final Entity victim;

    public EventOnPlayerAttack2(Entity victim, EntityPlayer attacker) {
        super(new ScriptTypeAccessor(new TypeEntity(victim), "victim"), new ScriptTypeAccessor(new TypePlayer(attacker), "attacker"));
        this.victim = victim;
    }

    @Override
    public boolean check(ScriptType[] parameters, int marks) {
        if (parameters.length != 0 && parameters[0] != null) {
            if (parameters[0] instanceof TypeEntity) {
                return this.victim.getClass().isAssignableFrom(((TypeEntity) parameters[0]).getObject().getClass());
            } else if (parameters[0] instanceof TypeResource) {
                return ForgeRegistries.ENTITIES.getValue((ResourceLocation) parameters[0].getObject()).getEntityClass() == this.victim.getClass();
            } else {
                return false;
            }
        } else {
            return true;
        }
    }
}