package fr.aym.gtwnpc.sqript;


import fr.aym.gtwnpc.entity.EntityGtwNpc;
import fr.nico.sqript.compiling.ScriptException;
import fr.nico.sqript.expressions.ScriptExpression;
import fr.nico.sqript.meta.Expression;
import fr.nico.sqript.meta.Feature;
import fr.nico.sqript.structures.ScriptContext;
import fr.nico.sqript.types.ScriptType;
import fr.nico.sqript.types.primitive.TypeString;

@Expression(name = "Npc state expression",
        features = {
                @Feature(
                        name = "Get/set npc attribute",
                        description = "Gets/sets an attribute of an npc",
                        examples = {"get \"state\" of npc \"my_npc\"", "set \"state\" of npc \"my_npc\" to \"my_state\""},
                        pattern = "{string} of npc {gnpc}")
        }
)
public class ExprNpcAttribute extends ScriptExpression {
    @Override
    public ScriptType get(ScriptContext scriptContext, ScriptType<?>[] scriptTypes) {
        if (getMatchedIndex() == 0) {
            return new TypeString(((EntityGtwNpc) scriptTypes[1].getObject()).getAttribute((String) scriptTypes[0].getObject()));
        }
        throw new IllegalStateException("Don't how we got there...");
    }

    @Override
    public boolean set(ScriptContext scriptContext, ScriptType scriptType, ScriptType<?>[] scriptTypes) throws ScriptException {
        if (getMatchedIndex() == 0) {
            ((EntityGtwNpc) scriptTypes[1].getObject()).setAttribute((String) scriptTypes[0].getObject(), scriptType.getObject().toString());
            return true;
        }
        throw new IllegalStateException("Don't how we got there...");
    }
}
