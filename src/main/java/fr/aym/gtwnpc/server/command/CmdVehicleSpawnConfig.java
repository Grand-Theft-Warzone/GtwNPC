package fr.aym.gtwnpc.server.command;

import fr.aym.gtwnpc.dynamx.VehicleType;
import fr.aym.gtwnpc.dynamx.spawning.VehicleSpawnConfig;
import fr.aym.gtwnpc.dynamx.spawning.VehicleSpawnConfigs;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.List;

public class CmdVehicleSpawnConfig implements ISubCommand
{
    @Override
    public String getName() {
        return "vehicle_config";
    }

    @Override
    public String getUsage() {
        return "/gtwnpcmod vehicle_config <list|add|remove> [vehicleName] [weight] [type] [metadata]";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2)
            throw new WrongUsageException(getUsage());
        VehicleSpawnConfigs configs = VehicleSpawnConfigs.getInstance();
        if (args[1].equals("list")) {
            sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "==== Vehicle spawn configs ===="));
            for (VehicleSpawnConfig config : configs.getVehicleSpawnConfigs()) {
                sender.sendMessage(new TextComponentString(config.getVehicleName() + " : " + config.getVehicleType() + " : weight=" + config.itemWeight + " : meta=" + config.getVehicleMeta()));
            }
        } else if(args[1].equals("add")) {
            if(args.length < 4)
                throw new WrongUsageException("/gtwnpcmod vehicle_config add <vehicleName> <weight> [CIVILIAN|POLICE|SWAT|MILITARY] [metadata]");
            String vehicleName = args[2];
            VehicleType type = VehicleType.CIVILIAN;
            int weight = CommandBase.parseInt(args[3]);
            if(args.length > 4) {
                try {
                    type = VehicleType.valueOf(args[4].toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new CommandException("Unknown vehicle type: " + args[4]);
                }
            }
            byte metadata = 0;
            if(args.length > 5) {
                metadata = (byte) CommandBase.parseInt(args[5]);
            }
            configs.getVehicleSpawnConfigs().add(new VehicleSpawnConfig(vehicleName, metadata, type, weight));
            configs.markDirty();
            sender.sendMessage(new TextComponentString("Added vehicle spawn config for " + vehicleName));
        } else if(args[1].equals("remove")) {
            if(args.length < 3)
                throw new WrongUsageException("/gtwnpcmod vehicle_config remove <vehicleName>");
            String vehicleName = args[2];
            configs.getVehicleSpawnConfigs().removeIf(config -> config.getVehicleName().equals(vehicleName));
            configs.markDirty();
            sender.sendMessage(new TextComponentString("Removed vehicle spawn config for " + vehicleName));
        } else {
            throw new WrongUsageException(getUsage());
        }
    }

    @Override
    public void getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos, List<String> r) {
        if (args.length == 2) {
            r.addAll(CommandBase.getListOfStringsMatchingLastWord(args, "list", "add", "remove"));
        } else if(args.length == 3 && args[1].equals("add")) {
            r.addAll(DynamXObjectLoaders.WHEELED_VEHICLES.getInfos().keySet());
        } else if (args.length == 5 && args[1].equals("add")) {
            r.addAll(CommandBase.getListOfStringsMatchingLastWord(args, "CIVILIAN", "POLICE", "SWAT", "MILITARY"));
        } else if (args.length == 3 && args[1].equals("remove")) {
            VehicleSpawnConfigs.getInstance().getVehicleSpawnConfigs().forEach(config -> r.add(config.getVehicleName()));
        }
    }
}
