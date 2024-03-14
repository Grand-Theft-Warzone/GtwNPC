package fr.aym.gtwnpc.client.render;

import com.jme3.math.Vector3f;
import fr.aym.gtwnpc.dynamx.GtwNpcModule;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.node.SimpleNode;
import fr.dynamx.common.entities.BaseVehicleEntity;
import net.minecraftforge.client.MinecraftForgeClient;

import javax.annotation.Nonnull;
import java.util.List;

public class NpcSeatsPadre extends SimpleNode<BaseRenderContext.EntityRenderContext, IModelPackObject> {
    public NpcSeatsPadre(@Nonnull Vector3f scale, List<NpcSeatNode> linkedChildren) {
        super(null, null, Vector3f.UNIT_XYZ, (List) linkedChildren);
    }

    @Override
    public void render(BaseRenderContext.EntityRenderContext context, IModelPackObject packInfo) {
        BaseVehicleEntity<?> entity = (BaseVehicleEntity<?>) context.getEntity();
        if (entity == null || entity.getControllingPassenger() != null) {
            return;
        }
        GtwNpcModule autopilot = entity.getModuleByType(GtwNpcModule.class);
        if (autopilot == null || !autopilot.hasAutopilot() || autopilot.getStolenTime() > 0) {
            return;
        }
        if (MinecraftForgeClient.getRenderPass() == 0 && entity instanceof IModuleContainer.ISeatsContainer) {
            this.transform.set(this.parent.getTransform());
            renderChildren(context, packInfo);
        }
    }

    @Override
    public void renderDebug(BaseRenderContext.EntityRenderContext context, IModelPackObject packInfo) {
        BaseVehicleEntity<?> entity = (BaseVehicleEntity<?>) context.getEntity();
        if (entity == null) {
            return;
        }
        GtwNpcModule autopilot = entity.getModuleByType(GtwNpcModule.class);
        if (autopilot == null || !autopilot.hasAutopilot()) {
            return;
        }
        super.renderDebug(context, packInfo);
    }
}
