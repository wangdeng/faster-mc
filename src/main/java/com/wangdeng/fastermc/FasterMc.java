package com.wangdeng.fastermc;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FasterMc implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("faster-mc");
    @Override
    public void onInitialize() {
        LOGGER.info("faster-mc started");
    }
}
