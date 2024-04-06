package fr.aym.gtwnpc.server.command;

import fr.aym.gtwnpc.entity.EntityGtwNpc;
import fr.aym.gtwnpc.utils.GtwNpcsConfig;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

public class CmdNpc implements ISubCommand {
    @Override
    public String getName() {
        return "npc";
    }

    @Override
    public String getUsage() {
        return "/gtwnpcmod npc <reload_config|kill_all|get|set> <property> [value]";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2)
            throw new WrongUsageException(getUsage());
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
            throw new WrongUsageException(getUsage());
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

    @Override
    public void getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos, List<String> r) {
        if (args.length == 2)
            r.addAll(CommandBase.getListOfStringsMatchingLastWord(args, "reload_config", "kill_all", "get", "set"));
        else if (args.length == 3)
            r.addAll(CommandBase.getListOfStringsMatchingLastWord(args, "do_spawning"));
        else if (args.length == 4 && args[2].equals("do_spawning"))
            r.addAll(CommandBase.getListOfStringsMatchingLastWord(args, "true", "false"));
    }
}
