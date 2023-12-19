package fr.aym.lib;

public interface ItemLibElement extends LibElement
{
    default int getMaxMeta() {return 1;}

    default boolean createJson(int metadata) {return true;}//!useObjModel(metadata);}
    default boolean createTranslation() {return true;}
    //default boolean useObjModel(int metadata) {return false;}

    default String getTranslationKey(int meta) {return "item."+getName();}
    default String getTranslatedName(int meta) {return getName();}

    default String getJsonName(int metadata) { return getName().toLowerCase(); }
}
