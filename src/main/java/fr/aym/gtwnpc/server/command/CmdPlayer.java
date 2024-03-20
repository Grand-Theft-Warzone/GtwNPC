package fr.aym.gtwnpc.server.command;

import fr.aym.gtwnpc.player.PlayerInformation;
import fr.aym.gtwnpc.player.PlayerManager;
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
import java.util.List;

public class CmdPlayer implements ISubCommand
{
    @Override
    public String getName() {
        return "player";
    }

    @Override
    public String getUsage() {
        return "/gtwnpcmod player <get|set> <property> [value]";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 3)
            throw new WrongUsageException(getUsage());
        if (args[1].equals("get")) {
            switch (args[2]) {
                case "wanted_level":
                    PlayerInformation info = PlayerManager.getPlayerInformation((EntityPlayer) sender);
                    sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Your wanted level is: " + info.getWantedLevel()));
                    if(info.getWantedLevel() > 0) {
                        if (info.getHiddenTime() > 0) {
                            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "You are hidden since " + info.getHiddenTime() + " ticks"));
                        } else {
                            sender.sendMessage(new TextComponentString(TextFormatting.RED + "You have " + info.getSeeingPolicemenCount() + " policemen seeing you"));
                        }
                    }
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

    @Override
    public void getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos, List<String> r) {
        if (args.length == 2)
            r.addAll(CommandBase.getListOfStringsMatchingLastWord(args, "get", "set"));
        else if (args.length == 3) {
            r.addAll(CommandBase.getListOfStringsMatchingLastWord(args, "wanted_level"));
        }
    }
}
