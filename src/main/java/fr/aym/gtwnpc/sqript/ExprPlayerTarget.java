package fr.aym.gtwnpc.sqript;

import fr.nico.sqript.expressions.ScriptExpression;
import fr.nico.sqript.meta.Expression;
import fr.nico.sqript.meta.Feature;
import fr.nico.sqript.structures.ScriptContext;
import fr.nico.sqript.types.ScriptType;
import fr.nico.sqript.types.TypeBlock;
import fr.nico.sqript.types.TypeEntity;
import fr.nico.sqript.types.TypeNull;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@Expression(name = "PlayerTarget", features = {
        @Feature(name = "Player Block Target", description = "Returns the block the player is looking at", examples = "{player}'s block target", pattern = "{+player}['s] block target"),
        @Feature(name = "Player Entity Target", description = "Returns the entity the player is looking at", examples = "{player}'s entity target", pattern = "{+player}['s] entity target")
})
public class ExprPlayerTarget extends ScriptExpression {
    @Override
    public ScriptType get(ScriptContext scriptContext, ScriptType<?>[] parameters) {
        EntityPlayer player;
        player = (EntityPlayer) parameters[0].getObject();
        World world = player.world;
        Vec3d vec3d = player.getPositionEyes(0);
        Vec3d vec3d1 = player.getLook(0);
        int blockReachDistance = 5;
        Vec3d vec3d2 = vec3d.add(vec3d1.x * blockReachDistance, vec3d1.y * blockReachDistance, vec3d1.z * blockReachDistance);
        RayTraceResult result = world.rayTraceBlocks(vec3d, vec3d2, false, false, true);
        if(result == null)
            return new TypeNull();
        if (getMatchedIndex() == 0) {
            if (result.typeOfHit == RayTraceResult.Type.BLOCK) {
                return new TypeBlock(world.getBlockState(result.getBlockPos()), result.getBlockPos(), world);
            } else
                return new TypeNull();
        } else if (getMatchedIndex() == 1) {
            if (result.typeOfHit == RayTraceResult.Type.ENTITY) {
                return new TypeEntity(result.entityHit);
            } else
                return new TypeNull();
        }
        return null;
    }

    @Override
    public boolean set(ScriptContext scriptContext, ScriptType scriptType, ScriptType<?>[] scriptTypes) {
        return false;
    }
}
