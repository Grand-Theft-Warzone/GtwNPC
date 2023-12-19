package fr.aym.lib;

import com.google.common.collect.Maps;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.statemap.IStateMapper;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.*;

@SideOnly(Side.CLIENT)
public class ContentPackUtils
{
//TODO CLEAN THIS
    public static void addMissingJSONs(BlockElement objectInfo, File dynxDir) {
        if (dynxDir.isDirectory()) {
            File modelDir = new File(dynxDir, LibContext.CACHE_DIR_NAME + "/assets/" + LibContext.ID +"/blockstates");
            if (!modelDir.exists())
                modelDir.mkdirs();
            createBlockstateJsonFile(modelDir, objectInfo.getName().toLowerCase(), objectInfo.getName().toLowerCase());
            modelDir = new File(dynxDir, LibContext.CACHE_DIR_NAME + "/assets/" + LibContext.ID +"/models/block");
            if (!modelDir.exists())
                modelDir.mkdirs();
            createBlockJsonFile(modelDir, objectInfo.getName().toLowerCase(), objectInfo.getName().toLowerCase());
        }
    }

    public static void addMissingJSONs(ItemLibElement objectInfo, File dynxDir, int metadata) {
        if (dynxDir.isDirectory()) {
            File modelDir = new File(dynxDir, LibContext.CACHE_DIR_NAME + "/assets/" + LibContext.ID +"/models/item");
            if (!modelDir.exists())
                modelDir.mkdirs();
            createItemJsonFile(modelDir, objectInfo.getName().toLowerCase(), objectInfo.getName().toLowerCase());
        }
    }

    private static void createItemJsonFile(File dir, String fileName, String iconName) {
        String folder = "blocks"; //TODO TYPE OF ITEM
        try {
            /*createJSONFile(new File(dir, fileName + ".json"),
                    "{ \"parent\": \"builtin/generated\", \"textures\": { \"layer0\": \"" + LibContext.ID + ":"+folder+"/" + iconName + "\" }, \"display\": { "
                            + "\"thirdperson_lefthand\": { \"rotation\": [ 0, 90, -35 ], \"translation\": [ 0, 1.25, -2.5 ], \"scale\": [ 0.85, 0.85, 0.85 ] }, "
                            + "\"thirdperson_righthand\": { \"rotation\": [ 0, 90, -35 ], \"translation\": [ 0, 1.25, -2.5 ], \"scale\": [ 0.85, 0.85, 0.85 ] }, "
                            + "\"firstperson_lefthand\": { \"rotation\": [ 0, -45, 25 ], \"translation\": [ 0, 4, 2 ], \"scale\": [ 0.85, 0.85, 0.85 ] }, "
                            + "\"firstperson_righthand\": { \"rotation\": [ 0, -45, 25 ], \"translation\": [ 0, 4, 2 ], \"scale\": [ 0.85, 0.85, 0.85 ] }"
                            + " } }");*/
            createJSONFile(new File(dir, fileName + ".json"),"{\n" +
                    "    \"parent\": \"" + LibContext.ID + ":block/" + iconName + "\"\n" +
                    "}\n");
        } catch (IOException e) {
            LibContext.log.error("Failed to create item json file " + fileName, e);
        }

    }
    private static void createBlockJsonFile(File dir, String fileName, String iconName) {
        try {
            String folder = "blocks";
            createJSONFile(new File(dir, fileName + ".json"),
                    "{\n" +
                            "    \"parent\": \"block/cube_all\",\n" +
                            "    \"textures\": {\n" +
                            "        \"all\": \"" + LibContext.ID + ":"+folder+"/" + iconName + "\"\n" +
                            "    }\n" +
                            "}\n");
        } catch (IOException e) {
            LibContext.log.error("Failed to create blockstate json file " + fileName, e);
        }
    }
    private static void createBlockstateJsonFile(File dir, String fileName, String iconName) {
        try {
            createJSONFile(new File(dir, fileName + ".json"),
                    "{ \"variants\": { \"normal\": { \"model\": \""+LibContext.ID+":"+iconName+"\" } } }");
        } catch (IOException e) {
            LibContext.log.error("Failed to create blockstate json file " + fileName, e);
        }
    }

    public static void createJSONFile(File file, String contents) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
            BufferedWriter out = new BufferedWriter(new FileWriter(file));
            out.write(contents);
            out.close();
            LibContext.log.info(file.getPath() + " not found so we created one");
        }
    }

    @SuppressWarnings("unchecked")
    public static void addMissingLangFile(File dynxDir, ItemLibElement item, int metadata) {
        File langPath = new File(dynxDir, LibContext.CACHE_DIR_NAME + "/assets/" + LibContext.ID + "/lang/");
        if (!langPath.exists()) {
            langPath.mkdirs();
        }
        File langFile = new File(langPath, "en_us.lang");
        try {
            if (!langFile.exists())
                langFile.createNewFile();
            writeLangFile(langFile, item, metadata);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void writeLangFile(File langFile, ItemLibElement item, int metadata) throws IOException {
        BufferedReader inputStream = new BufferedReader(new InputStreamReader(new FileInputStream(langFile)));
        String translation;
        translation = item.getTranslationKey(metadata) + ".name=" + item.getTranslatedName(metadata);

        if (inputStream.lines().noneMatch(s -> s.contains(translation.substring(0, translation.lastIndexOf("="))))) {
            BufferedWriter out = new BufferedWriter(new FileWriter(langFile, true));
            out.write(translation + "\n");
            out.close();
            //log.info("Translation not found so we added one");
        }
        inputStream.close();
    }

    /**
     * Registers an block with a {@link IStateMapper}, it permits to ignore some blockstate properties for the render <br>
     * See {@link net.minecraft.client.renderer.BlockModelShapes} to see how Minecraft uses this
     *
     * @param block The block
     * @param stateMapper The {@link IStateMapper}
     */
    public static void registerBlockWithStateMapper(Block block, IStateMapper stateMapper)
    {
        ModelLoader.setCustomStateMapper(block, stateMapper);
    }
    public static void registerBlockWithNoModel(Block block)
    {
        registerBlockWithStateMapper(block, NO_MODEL);
    }
    private static final IStateMapper NO_MODEL = blockIn -> Maps.newHashMap();
}
