package fr.aym.gtwnpc.impl;

import fr.aym.acslib.api.services.mps.IMpsClassLoader;

public class ProtectionStarter
{
    public ProtectionStarter(IMpsClassLoader loader) {
        System.out.println("Protection starter started");
        System.out.println(loader);
    }
}
