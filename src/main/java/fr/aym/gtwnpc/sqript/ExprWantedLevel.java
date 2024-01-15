package fr.aym.gtwnpc.sqript;

import fr.aym.gtwnpc.player.PlayerInformation;
import fr.aym.gtwnpc.player.PlayerManager;
import fr.nico.sqript.compiling.ScriptException;
import fr.nico.sqript.expressions.ScriptExpression;
import fr.nico.sqript.meta.Expression;
import fr.nico.sqript.meta.Feature;
import fr.nico.sqript.structures.ScriptContext;
import fr.nico.sqript.types.ScriptType;
import fr.nico.sqript.types.primitive.TypeNumber;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;

@Expression(name = "Player wanted level expression",
        features = {
                @Feature(
                        name = "Get/set player wanted level",
                        description = "Gets/sets the wanted level of a player",
                        examples = {"get wanted level of player", "set wanted level of player to 2"},
                        pattern = "wanted level of {player}")
        }
)
public class ExprWantedLevel extends ScriptExpression {
    @Override
    public ScriptType get(ScriptContext scriptContext, ScriptType<?>[] scriptTypes) {
        if (getMatchedIndex() == 0) {
            EntityPlayer player = (EntityPlayer) scriptTypes[0].getObject();
            if (player == null)
                throw new IllegalArgumentException("Player is null");
            PlayerInformation info = PlayerManager.getPlayerInformation(player.getPersistentID());
            return new TypeNumber(info == null ? 0 : info.getWantedLevel());
        }
        throw new IllegalStateException("Don't how we got there...");
    }

    @Override
    public boolean set(ScriptContext scriptContext, ScriptType scriptType, ScriptType<?>[] scriptTypes) throws ScriptException {
        if (getMatchedIndex() == 0) {
            EntityPlayer player = (EntityPlayer) scriptTypes[0].getObject();
            if (player == null)
                throw new IllegalArgumentException("Player is null");
            int value = ((Number) scriptType.getObject()).intValue();
            PlayerInformation info = PlayerManager.getPlayerInformation(player);
            info.setWantedLevel(value);
            //player.sendMessage(new TextComponentString("Your wanted level is now: " + info.getWantedLevel()));
            return true;
        }
        throw new IllegalStateException("Don't how we got there...");
    }
}
