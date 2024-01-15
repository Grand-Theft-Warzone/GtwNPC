package fr.aym.gtwnpc.server;

import fr.aym.gtwnpc.entity.EntityGtwNpc;
import fr.aym.gtwnpc.player.PlayerInformation;
import fr.aym.gtwnpc.player.PlayerManager;
import fr.aym.gtwnpc.utils.GtwNpcsConfig;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandGtwNpcConfig extends CommandBase {
    @Override
    public String getName() {
        return "gtwnpcmod";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/gtwnpcmod <npc|player> <reload_config|kill_all|get|set> <property> [value]";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1)
            throw new WrongUsageException(getUsage(sender));
        if (args[0].equals("player")) {
            if (args.length < 3)
                throw new WrongUsageException(getUsage(sender));
            if (args[1].equals("get")) {
                switch (args[2]) {
                    case "wanted_level":
                        PlayerInformation info = PlayerManager.getPlayerInformation((EntityPlayer) sender);
                        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Your wanted level is: " + info.getWantedLevel()));
                        return;
                    default:
                        throw new WrongUsageException("/gtwnpcmod player get <wanted_level>");
                }
            } else if (args[1].equals("set")) {
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
        if (args[0].equals("npc")) {
            if (args.length < 2)
                throw new WrongUsageException(getUsage(sender));
            if (args[1].equals("reload_config")) {
                GtwNpcsConfig.reload();
                sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Config reloaded"));
                return;
            }
            if (args[1].equals("kill_all")) {
                List<Entity> npcs = sender.getEntityWorld().loadedEntityList.stream().filter(e -> e instanceof EntityGtwNpc).collect(Collectors.toList());
                npcs.forEach(Entity::setDead);
                sender.sendMessage(new TextComponentString(TextFormatting.RED + "Killed " + npcs.size() + " npcs"));
                return;
            }
            if (args.length < 3)
                throw new WrongUsageException(getUsage(sender));
            if (args[1].equals("get")) {
                switch (args[2]) {
                    case "do_spawning":
                        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Npc spawning: " + GtwNpcsConfig.config.isEnableSpawning()));
                        return;
                    default:
                        throw new WrongUsageException("/gtwnpcmod npc get <do_spawning>");
                }
            } else if (args[1].equals("set")) {
                switch (args[2]) {
                    case "do_spawning":
                        GtwNpcsConfig.config.setEnableSpawning(CommandBase.parseBoolean(args[3]));
                        GtwNpcsConfig.save();
                        sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Npc spawning set to: " + GtwNpcsConfig.config.isEnableSpawning()));
                        return;
                    default:
                        throw new WrongUsageException("/gtwnpcmod npc set <do_spawning> <true/false>");
                }
            }
        }
        throw new WrongUsageException(getUsage(sender));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1)
            return getListOfStringsMatchingLastWord(args, "npc", "player");
        if (args.length == 2 && args[0].equals("npc"))
            return getListOfStringsMatchingLastWord(args, "reload_config", "kill_all", "get", "set");
        if (args.length == 2 && args[0].equals("player"))
            return getListOfStringsMatchingLastWord(args, "get", "set");
        if (args.length == 3 && args[0].equals("npc")) {
            return getListOfStringsMatchingLastWord(args, "do_spawning");
        }
        if (args.length == 3 && args[0].equals("player")) {
            return getListOfStringsMatchingLastWord(args, "wanted_level");
        }
        return Collections.EMPTY_LIST;
    }
}
