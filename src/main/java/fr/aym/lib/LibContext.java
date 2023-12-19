package fr.aym.lib;

import fr.aym.gtwnpc.utils.GtwNpcConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LibContext {
    public static final String ID = GtwNpcConstants.ID;
    public static final String NAME = "AymLib for " + GtwNpcConstants.NAME;
    public static final String CACHE_DIR_NAME = "GtwLibCache";
    public static final boolean CREATE_MISSING_JSONS = true; //FOR DEV ENV
    public static final boolean CREATE_MISSING_TRANSLATIONS = true;

    public static Logger log = LogManager.getLogger(NAME);
}
