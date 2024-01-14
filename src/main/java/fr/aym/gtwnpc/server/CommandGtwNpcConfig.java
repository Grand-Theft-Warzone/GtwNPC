package fr.aym.gtwnpc.server;

import fr.aym.gtwnpc.player.PlayerInformation;
import fr.aym.gtwnpc.player.PlayerManager;
import fr.aym.gtwnpc.utils.GtwNpcsConfig;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class CommandGtwNpcConfig extends CommandBase {
    @Override
    public String getName() {
        return "gtwnpcmod";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/gtwnpcmod <npc|player> <get|set> <property> [value]";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if(args.length < 3)
            throw new WrongUsageException(getUsage(sender));
        if(args[0].equals("player")) {
            if(args[1].equals("get")) {
                switch (args[2]) {
                    case "wanted_level":
                        PlayerInformation info = PlayerManager.getPlayerInformation((EntityPlayer) sender);
                        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Your wanted level is: " + info.getWantedLevel()));
                        return;
                    default:
                        throw new WrongUsageException("/gtwnpcmod player get <wanted_level>");
                }
            } else if(args[1].equals("set")) {
                switch (args[2]) {
                    case "wanted_level":
                        PlayerInformation info = PlayerManager.getPlayerInformation((EntityPlayer) sender);
                        info.setWantedLevel(CommandBase.parseInt(args[3]));
                        sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Your wanted level is now: " + info.getWantedLevel()));
                        return;
                    default:
                        throw new WrongUsageException("/gtwnpcmod player set <wanted_level> <value>");
                }
            }
        }
        if(args[0].equals("npc")) {
            if(args[1].equals("get")) {
                switch (args[2]) {
                    case "spawnClusterSize":
                        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Spawn cluster size : " + GtwNpcsConfig.spawnClusterSize));
                        return;
                    case "spawnChance":
                        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Spawn chance : " + GtwNpcsConfig.spawnChance));
                        return;
                    case "minNpcMoveSpeed":
                        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Min npc move speed : " + GtwNpcsConfig.minNpcMoveSpeed));
                        return;
                    case "maxNpcMoveSpeed":
                        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Max npc move speed : " + GtwNpcsConfig.maxNpcMoveSpeed));
                        return;
                    case "panicMoveSpeed":
                        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Panic move speed : " + GtwNpcsConfig.panicMoveSpeed));
                        return;
                    case "panicChance":
                        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Panic chance : " + GtwNpcsConfig.attackBackChance));
                        return;
                    case "attackingMoveSpeed":
                        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Attacking move speed : " + GtwNpcsConfig.attackingMoveSpeed));
                        return;
                    case "attackDamage":
                        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Attack damage : " + GtwNpcsConfig.attackDamage));
                        return;
                    case "attackSpeed":
                        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Attack speed : " + GtwNpcsConfig.attackSpeed));
                        return;
                    case "npcHealth":
                        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Npc health : " + GtwNpcsConfig.npcHealth));
                        return;
                    default:
                        throw new WrongUsageException("/gtwnpcmod npc get <spawnClusterSize|spawnChance|minNpcMoveSpeed|maxNpcMoveSpeed|panicMoveSpeed|panicChance|attackingMoveSpeed|attackDamage|attackSpeed|npcHealth>");
                }
            } else if(args[1].equals("set")) {
                switch (args[2]) {
                    case "spawnClusterSize":
                        GtwNpcsConfig.spawnClusterSize = CommandBase.parseInt(args[3]);
                        sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Spawn cluster size set to " + GtwNpcsConfig.spawnClusterSize));
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "You need to restart the game for this to take effect"));
                        return;
                    case "spawnChance":
                        GtwNpcsConfig.spawnChance = CommandBase.parseInt(args[3]);
                        sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Spawn chance set to " + GtwNpcsConfig.spawnChance));
                        return;
                    case "minNpcMoveSpeed":
                        GtwNpcsConfig.minNpcMoveSpeed = (float) CommandBase.parseDouble(args[3]);
                        sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Min npc move speed set to " + GtwNpcsConfig.minNpcMoveSpeed));
                        return;
                    case "maxNpcMoveSpeed":
                        GtwNpcsConfig.maxNpcMoveSpeed = (float) CommandBase.parseDouble(args[3]);
                        sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Max npc move speed set to " + GtwNpcsConfig.maxNpcMoveSpeed));
                        return;
                    case "panicMoveSpeed":
                        GtwNpcsConfig.panicMoveSpeed = (float) CommandBase.parseDouble(args[3]);
                        sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Panic move speed set to " + GtwNpcsConfig.panicMoveSpeed));
                        return;
                    case "panicChance":
                        GtwNpcsConfig.attackBackChance = CommandBase.parseInt(args[3]);
                        sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Panic chance set to " + GtwNpcsConfig.attackBackChance));
                        return;

                    case "attackingMoveSpeed":
                        GtwNpcsConfig.attackingMoveSpeed = (float) CommandBase.parseDouble(args[3]);
                        sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Attacking move speed set to " + GtwNpcsConfig.attackingMoveSpeed));
                        return;
                    case "attackDamage":
                        GtwNpcsConfig.attackDamage = (float) CommandBase.parseDouble(args[3]);
                        sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Attack damage set to " + GtwNpcsConfig.attackDamage));
                        return;
                    case "attackSpeed":
                        GtwNpcsConfig.attackSpeed = (float) CommandBase.parseDouble(args[3]);
                        sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Attack speed set to " + GtwNpcsConfig.attackSpeed));
                        return;
                    case "npcHealth":
                        GtwNpcsConfig.npcHealth = (float) CommandBase.parseDouble(args[3]);
                        sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Npc health set to " + GtwNpcsConfig.npcHealth));
                        return;
                    default:
                        throw new WrongUsageException("/gtwnpcmod npc set <spawnClusterSize|spawnChance|minNpcMoveSpeed|maxNpcMoveSpeed|panicMoveSpeed|panicChance|attackingMoveSpeed|attackDamage|attackSpeed|npcHealth> <value>");
                }
            }
        }
        throw new WrongUsageException(getUsage(sender));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if(args.length == 1)
            return getListOfStringsMatchingLastWord(args, "npc", "player");
        if(args.length == 2)
            return getListOfStringsMatchingLastWord(args, "get", "set");
        if(args.length == 3 && args[0].equals("player")) {
            return getListOfStringsMatchingLastWord(args, "wanted_level");
        }
        if(args.length == 3 && args[0].equals("npc")) {
            return getListOfStringsMatchingLastWord(args, "spawnClusterSize", "spawnChance", "minNpcMoveSpeed", "maxNpcMoveSpeed", "panicMoveSpeed", "panicChance", "attackingMoveSpeed", "attackDamage", "attackSpeed", "npcHealth");
        }
        return Collections.EMPTY_LIST;
    }
}
