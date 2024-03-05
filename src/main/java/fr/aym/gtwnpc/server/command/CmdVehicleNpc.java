package fr.aym.gtwnpc.server.command;

import fr.aym.gtwnpc.dynamx.GtwNpcModule;
import fr.aym.gtwnpc.utils.GtwNpcsConfig;
import fr.dynamx.common.entities.BaseVehicleEntity;
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

public class CmdVehicleNpc implements ISubCommand {
    @Override
    public String getName() {
        return "vehicle_npc";
    }

    @Override
    public String getUsage() {
        return "/gtwnpcmod vehicle_npc <kill_all|get|set> <property> [value]";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2)
            throw new WrongUsageException(getUsage());
        if (args[1].equals("kill_all")) {
            List<Entity> npcs = sender.getEntityWorld().loadedEntityList.stream().filter(e -> e instanceof BaseVehicleEntity
                    && ((BaseVehicleEntity<?>) e).hasModuleOfType(GtwNpcModule.class)
                    && ((BaseVehicleEntity<?>) e).getModuleByType(GtwNpcModule.class).hasAutopilot()).collect(Collectors.toList());
            npcs.forEach(Entity::setDead);
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Killed " + npcs.size() + " vehicle npcs"));
            return;
        }
        if (args.length < 3)
            throw new WrongUsageException(getUsage());
        if (args[1].equals("get")) {
            switch (args[2]) {
                case "do_spawning":
                    sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Vehicle npc spawning: " +
                            GtwNpcsConfig.config.getVehiclesSpawningRules().isVehicleSpawningEnabled()));
                    return;
                default:
                    throw new WrongUsageException("/gtwnpcmod vehicle_npc get <do_spawning>");
            }
        } else if (args[1].equals("set")) {
            switch (args[2]) {
                case "do_spawning":
                    GtwNpcsConfig.config.getVehiclesSpawningRules().setVehicleSpawningEnabled(CommandBase.parseBoolean(args[3]));
                    GtwNpcsConfig.save();
                    sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Vehicle npc spawning set to: " +
                            GtwNpcsConfig.config.getVehiclesSpawningRules().isVehicleSpawningEnabled()));
                    return;
                default:
                    throw new WrongUsageException("/gtwnpcmod vehicle_npc set <do_spawning> <true/false>");
            }
        }
    }

    @Override
    public void getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos, List<String> r) {
        if (args.length == 2)
            r.addAll(CommandBase.getListOfStringsMatchingLastWord(args, "kill_all", "get", "set"));
        else if (args.length == 3)
            r.addAll(CommandBase.getListOfStringsMatchingLastWord(args, "do_spawning"));
    }
}
