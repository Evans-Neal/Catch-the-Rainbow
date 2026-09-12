package dev.rainow.rainow;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

public final class RainowDebug {
   private RainowDebug() {
   }

   public static void register() {
      if (!FabricLoader.getInstance().isModLoaded("fabric-api")) {
         RainowMod.LOGGER.info("[rainow] fabric-api absent, /rainow debug commands disabled");
      } else {
         CommandRegistrationCallback.EVENT.register((CommandRegistrationCallback)(dispatcher, registryAccess, environment) -> registerCommands(dispatcher));
      }
   }

   private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
      registerSpawner(dispatcher, "dolphin_knight", (level, pos, random) -> RainNightDrowned.spawnDolphinKnight(level, pos, random));
      registerSpawner(dispatcher, "nautilus_knight", (level, pos, random) -> RainNightDrowned.spawnNautilusTridentKnight(level, pos, random));
   }

   /** Positioned knight spawners: /rainow <name> <x> <y> <z> spawns exactly
    * at the given block coords (replaces the old scatter "knight" command). */
   private static void registerSpawner(CommandDispatcher<CommandSourceStack> dispatcher, String name, Spawner spawner) {
      dispatcher.register(
         Commands.literal(name).requires(Commands.hasPermission(Commands.LEVEL_MODERATORS))
            .then(Commands.argument("x", IntegerArgumentType.integer())
               .then(Commands.argument("y", IntegerArgumentType.integer())
                  .then(Commands.argument("z", IntegerArgumentType.integer())
                     .executes(ctx -> {
                        CommandSourceStack source = ctx.getSource();
                        ServerLevel level = source.getLevel();
                        int x = IntegerArgumentType.getInteger(ctx, "x");
                        int y = IntegerArgumentType.getInteger(ctx, "y");
                        int z = IntegerArgumentType.getInteger(ctx, "z");
                        spawner.spawn(level, new BlockPos(x, y, z), level.getRandom());
                        source.sendSuccess(() -> Component.literal("[rainow] " + name + " spawned at " + x + " " + y + " " + z), true);
                        return 1;
                     }))))
      );
   }

   @FunctionalInterface
   private interface Spawner {
      void spawn(ServerLevel level, BlockPos pos, RandomSource random);
   }
}
