package fr.aym.gtwnpc.sqript;

import fr.aym.gtwnpc.entity.EntityGtwNpc;
import fr.nico.sqript.meta.Type;
import fr.nico.sqript.structures.ScriptElement;
import fr.nico.sqript.types.ScriptType;
import fr.nico.sqript.types.interfaces.ILocatable;
import net.minecraft.util.math.Vec3d;

@Type(name = "gnpc", parsableAs = {})
public class TypeGNpc extends ScriptType<EntityGtwNpc> implements ILocatable {
    public ScriptElement<?> parse(String typeName) {
        return null;
    }

    public String toString() {
        return this.getObject().getName();
    }

    public TypeGNpc(EntityGtwNpc entity) {
        super(entity);
    }

    public Vec3d getVector() {
        return new Vec3d(getObject().posX, getObject().posY, getObject().posZ);
    }
}
