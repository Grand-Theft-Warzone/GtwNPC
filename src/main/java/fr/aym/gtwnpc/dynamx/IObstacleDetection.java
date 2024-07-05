package fr.aym.gtwnpc.dynamx;

import fr.aym.gtwnpc.utils.AIRaycast;
import fr.aym.gtwnpc.utils.OOBB;
import fr.dynamx.common.entities.BaseVehicleEntity;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.List;

public interface IObstacleDetection {
    void detectObstacles();

    int getStuckTime();

    AxisAlignedBB getDetectionAABB(int rayDistance);

    List<AIRaycast> getLastVectors();

    OOBB getEntityOOBB(BaseVehicleEntity<?> entity);

    OOBB getMyOOBB();
}
