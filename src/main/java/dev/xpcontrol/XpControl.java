package dev.xpcontrol;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XpControl implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("xpcontrol");

    @Override
    public void onInitialize() {
        XpConfig.load();
    }
}
