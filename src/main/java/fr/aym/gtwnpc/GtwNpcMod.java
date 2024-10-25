package fr.aym.gtwnpc;

import fr.aym.acsguis.api.ACsGuiApi;
import fr.aym.gtwnpc.block.BlockTrafficLight;
import fr.aym.gtwnpc.block.TETrafficLight;
import fr.aym.gtwnpc.client.skin.SkinRepository;
import fr.aym.gtwnpc.common.CommonProxy;
import fr.aym.gtwnpc.common.GtwNpcsItems;
import fr.aym.gtwnpc.dynamx.VehicleType;
import fr.aym.gtwnpc.entity.EntityGtwNpc;
import fr.aym.gtwnpc.entity.EntityGtwPoliceNpc;
import fr.aym.gtwnpc.impl.ProtectionStarter;
import fr.aym.gtwnpc.network.BBMessagePathNodes;
import fr.aym.gtwnpc.network.CSMessageSetNodeMode;
import fr.aym.gtwnpc.network.SCMessagePlayerInformation;
import fr.aym.gtwnpc.network.SCMessagePlayerMoney;
import fr.aym.gtwnpc.server.command.CommandGtwNpcMod;
import fr.aym.gtwnpc.utils.GtwNpcsConfig;
import fr.dynamx.api.contentpack.DynamXAddon;
import fr.dynamx.api.network.sync.EntityVariableSerializer;
import fr.dynamx.api.network.sync.EntityVariableTypes;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.File;
import java.lang.reflect.Type;

import static fr.aym.gtwnpc.utils.GtwNpcConstants.*;

@DynamXAddon(modid = ID, name = NAME, version = VERSION)
@Mod(modid = ID, name = NAME, version = VERSION, dependencies = "required-before:sqript@1.0; required-before: dynamxmod;")
public class GtwNpcMod {
    @Mod.Instance(value = ID)
    public static GtwNpcMod instance;

    public static SimpleNetworkWrapper network;

    @SidedProxy(serverSide = "fr.aym.gtwnpc.server.ServerProxy", clientSide = "fr.aym.gtwnpc.client.ClientProxy")
    public static CommonProxy proxy;

    public static final Logger log = LogManager.getLogger("GtwNpcMod");

    public static Block trafficLight;
    public static Block pedTrafficLight;

    public static boolean isValidConfig;

    public GtwNpcMod() {
        GtwNpcsItems.registerItems();
    }

    @DynamXAddon.AddonEventSubscriber
    public static void initAddon() {
        trafficLight = new BlockTrafficLight(false, "trafficlight");
        pedTrafficLight = new BlockTrafficLight(true, "ped_trafficlight");

        if (FMLCommonHandler.instance().getSide().isClient()) {
            ACsGuiApi.registerStyleSheetToPreload(new ResourceLocation(ID, "fonts/hud.css"));
        }
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        log.info("Loading protection class");
        //ModProtectionConfig config = new BasicMpsConfig(VERSION, MPS_ACCESS_KEY, MPS_SERVER_VERSION, new DynamXMpsConfig.DynamXMpsUrlFactory(MPS_URL, new String[]{MPS_AUX_URL}), new String[0], "fr.aym.gtwnpc.impl.ProtectionStarter");
        //ModProtectionContainer container = ACsLib.getPlatform().provideService(ModProtectionService.class).createNewMpsContainer(ID, config, false);
        //container.setup(NAME);
        new ProtectionStarter(null);

        log.info("Loading GtwNpcMod");
        EntityRegistry.registerModEntity(new ResourceLocation(ID, "entity_gtw_npc"), EntityGtwNpc.class, "entity_gtw_npc", 1, this, 80, 3, false, new Color(0, 255, 0).getRGB(), new Color(255, 0, 0).getRGB());
        EntityRegistry.registerModEntity(new ResourceLocation(ID, "entity_police_gtw_npc"), EntityGtwPoliceNpc.class, "entity_police_gtw_npc", 1, this, 80, 3, false, new Color(0, 255, 0).getRGB(), new Color(0, 0, 255).getRGB());
        proxy.preInit(event);
        GtwNpcsConfig.load(new File(event.getModConfigurationDirectory(), "GtwNpcConfig.json"));

        GameRegistry.registerTileEntity(TETrafficLight.class, new ResourceLocation(ID, "traffic_light"));
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!isValidConfig) {
            throw new IllegalStateException("Invalid minecraft configuration. Unsupported launcher. Stopping.");
        }

        network = NetworkRegistry.INSTANCE.newSimpleChannel(ID + ".ch");
        network.registerMessage(BBMessagePathNodes.HandlerClient.class, BBMessagePathNodes.class, 1, Side.CLIENT);
        network.registerMessage(BBMessagePathNodes.HandlerServer.class, BBMessagePathNodes.class, 2, Side.SERVER);
        network.registerMessage(CSMessageSetNodeMode.Handler.class, CSMessageSetNodeMode.class, 3, Side.SERVER);
        network.registerMessage(SCMessagePlayerInformation.Handler.class, SCMessagePlayerInformation.class, 4, Side.CLIENT);
        network.registerMessage(SCMessagePlayerMoney.Handler.class, SCMessagePlayerMoney.class, 5, Side.CLIENT);

        SkinRepository.loadSkins(new File("GtwNpc", "skins"));

        EntityVariableTypes.registerSerializer(String[].class, new EntityVariableSerializer<String[]>() {
            @Override
            public void writeObject(ByteBuf byteBuf, String[] strings) {
                byteBuf.writeInt(strings.length);
                for (String s : strings) {
                    ByteBufUtils.writeUTF8String(byteBuf, s);
                }
            }

            @Override
            public String[] readObject(ByteBuf byteBuf) {
                int size = byteBuf.readInt();
                String[] strings = new String[size];
                for (int i = 0; i < size; i++) {
                    strings[i] = ByteBufUtils.readUTF8String(byteBuf);
                }
                return strings;
            }
        });
        EntityVariableTypes.registerSerializer(VehicleType.class, new EntityVariableSerializer<VehicleType>() {
            @Override
            public void writeObject(ByteBuf byteBuf, VehicleType vehicleType) {
                byteBuf.writeInt(vehicleType.ordinal());
            }

            @Override
            public VehicleType readObject(ByteBuf byteBuf) {
                return VehicleType.values()[byteBuf.readInt()];
            }
        });
        EntityVariableTypes.registerSerializer(new EntityVariableTypes.CustomType(new Type[]{ItemStack.class}, NonNullList.class),
                new EntityVariableSerializer<NonNullList<ItemStack>>() {
                    @Override
                    public void writeObject(ByteBuf byteBuf, NonNullList<ItemStack> itemStacks) {
                        byteBuf.writeInt(itemStacks == null ? -1 : itemStacks.size());
                        if (itemStacks == null)
                            return;
                        for (ItemStack itemStack : itemStacks) {
                            ByteBufUtils.writeItemStack(new PacketBuffer(byteBuf), itemStack);
                        }
                    }

                    @Override
                    public NonNullList<ItemStack> readObject(ByteBuf byteBuf) {
                        int size = byteBuf.readInt();
                        if (size == -1)
                            return null;
                        NonNullList<ItemStack> itemStacks = NonNullList.withSize(size, ItemStack.EMPTY);
                        for (int i = 0; i < size; i++) {
                            itemStacks.set(i, ByteBufUtils.readItemStack(new PacketBuffer(byteBuf)));
                        }
                        return itemStacks;
                    }
                });
    }

    @Mod.EventHandler
    public void postInit(FMLInitializationEvent event) {
        //EntityNpcTypes.init();

    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandGtwNpcMod());
    }
}
