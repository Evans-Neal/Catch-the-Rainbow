package dev.rainow.rainow;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RainowMod implements ModInitializer {
   public static final Logger LOGGER = LoggerFactory.getLogger("rainow");

   public void onInitialize() {
      LOGGER.info("[Catch the Rainbow] The forecast says: fish. Heavy fish.");
      RainowDebug.register();
   }
}
