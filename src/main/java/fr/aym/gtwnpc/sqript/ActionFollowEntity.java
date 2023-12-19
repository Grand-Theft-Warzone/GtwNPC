package fr.aym.gtwnpc.sqript;


import fr.nico.sqript.actions.ScriptAction;
import fr.nico.sqript.meta.Action;
import fr.nico.sqript.meta.Feature;

@Action(name = "Follow entity",
        features = @Feature(
                name = "Follow entity",
                description = "Follows an entity",
                examples = "follow entity \"my_npc\" with speed 0.5",
                pattern = "set {gnpc} to follow {entity}"))
public class ActionFollowEntity extends ScriptAction {

}
