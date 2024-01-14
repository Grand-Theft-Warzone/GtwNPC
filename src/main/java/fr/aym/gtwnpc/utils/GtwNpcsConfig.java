package fr.aym.gtwnpc.utils;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.WrongUsageException;
import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class GtwNpcsConfig
{
    private static Configuration config;
    public static int maxNpcs = 1000; //integrate
    public static int npcsLimitRadius = 40; //integrate
    public static int npcsLimit = 20; //integrate
    public static int spawnClusterSize = 10;
    public static int policeSpawnClusterSize = 20; //integrate
    public static int spawnChance = 25;
    public static float minNpcMoveSpeed = 0.45f;
    public static float maxNpcMoveSpeed = 0.60f;
    public static float panicMoveSpeed = 0.8f;
    public static int attackBackChance = 50;
    public static float attackingMoveSpeed = 0.65f;
    public static float attackDamage = 2;
    public static float attackSpeed = 4;
    public static float npcHealth = 20;

    public static void load(File configFile) {
        config = new Configuration(configFile);
        config.load();
        spawnClusterSize = 20;//config.getInt("SpawnClusterSize", "Spawning", 10, 1, 100, "The maximum number of npcs around you");
        spawnChance = config.getInt("SpawnChance", "Spawning", 25, 0, 100, "The npcs spawn chance");
        minNpcMoveSpeed = config.getFloat("MinNpcMoveSpeed", "Npc", 0.45f, 0.1f, 1f, "The min npc move speed");
        maxNpcMoveSpeed = config.getFloat("MaxNpcMoveSpeed", "Npc", 0.65f, 0.1f, 1f, "The max npc move speed");
        panicMoveSpeed = config.getFloat("PanicMoveSpeed", "Npc", 0.8f, 0.1f, 1f, "The npc move speed when panicking");
        attackBackChance = config.getInt("AttackBackChance", "Npc", 50, 0, 100, "The probability of attacking back when attacked");
        attackingMoveSpeed = config.getFloat("AttackingMoveSpeed", "Npc", 0.65f, 0.1f, 1f, "The npc move speed when attacking a player");
        attackDamage = config.getFloat("AttackDamage", "Npc", 2, 0, 100, "The npc attack damage");
        attackSpeed = config.getFloat("AttackSpeed", "Npc", 4, 0, 100, "The npc attack speed");
        npcHealth = config.getFloat("NpcHealth", "Npc", 20, 1, 100, "The npc health");
        config.save();
    }

    //TODO COMMAND
    public static void setConfigOption(String option, String value) throws CommandException {
        switch (option) {
            case "SpawnClusterSize":
                config.get("Spawning", "SpawnClusterSize", 10).set(CommandBase.parseInt(value));
                break;
            case "SpawnChance":
                config.get("Spawning", "SpawnChance", 25).set(CommandBase.parseInt(value));
                break;
            case "MinNpcMoveSpeed":
                config.get("Npc", "MinNpcMoveSpeed", 0.45f).set((float) CommandBase.parseDouble(value));
                break;
            case "MaxNpcMoveSpeed":
                config.get("Npc", "MaxNpcMoveSpeed", 0.65f).set((float) CommandBase.parseDouble(value));
                break;
            case "PanicMoveSpeed":
                config.get("Npc", "PanicMoveSpeed", 0.8f).set((float) CommandBase.parseDouble(value));
                break;
            case "PanicChance":
                config.get("Npc", "PanicChance", 50).set(CommandBase.parseInt(value));
                break;
            case "AttackingMoveSpeed":
                config.get("Npc", "AttackingMoveSpeed", 0.65f).set((float) CommandBase.parseDouble(value));
                break;
            case "AttackDamage":
                config.get("Npc", "AttackDamage", 2f).set((float) CommandBase.parseDouble(value));
                break;
            case "AttackSpeed":
                config.get("Npc", "AttackSpeed", 4f).set((float) CommandBase.parseDouble(value));
                break;
            case "NpcHealth":
                config.get("Npc", "NpcHealth", 20f).set((float) CommandBase.parseDouble(value));
                break;
            default:
                throw new WrongUsageException("Unknown parameter " + option);
        }
        config.save();
    }
}
