package fr.aym.gtwnpc.impl;

import fr.aym.acslib.api.services.mps.IMpsClassLoader;
import fr.aym.gtwnpc.GtwNpcMod;
import fr.aym.gtwnpc.dynamx.AutopilotModule;
import fr.aym.gtwnpc.entity.EntityGtwNpc;

public class ProtectionStarter {
    public ProtectionStarter(IMpsClassLoader loader) {
        GtwNpcMod.log.info("Protection loader started");
        //TODO FILTER DIRECTORIES
        GtwNpcMod.isValidConfig = true;
        EntityGtwNpc.moveToNodesAiFactory = GEntityAIMoveToNodes::new;
        AutopilotModule.obstacleDetectionFactory = ObstacleDetection::new;
    }
}
