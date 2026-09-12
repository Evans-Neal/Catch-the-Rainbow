package dev.rainow.rainow;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

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
      dispatcher.register(
         Commands.literal("rainow")
            .then(spawner("dolphin_knight", (level, pos, random) -> RainNightDrowned.spawnDolphinKnight(level, pos, random)))
            .then(spawner("nautilus_knight", (level, pos, random) -> RainNightDrowned.spawnNautilusTridentKnight(level, pos, random)))
      );
   }

   /** Knight spawners: /rainow <name> spawns at the caller's position
    * (vanilla summon semantics), /rainow <name> <pos> spawns at pos --
    * ~ relative coordinates included (Vec3Argument handles them natively).
    * 0.2.6 (coordinate pass): the original integer x y z arguments were mandatory and rejected
    * relative coords; this matches vanilla summon behavior instead.
    * 0.2.6 hotfix note: the /rainow parent literal was accidentally dropped
    * in the 0.2.4.8 rename (commands lived at the dispatcher root). */
   private static LiteralArgumentBuilder<CommandSourceStack> spawner(String name, Spawner spawner) {
      return Commands.literal(name).requires(Commands.hasPermission(Commands.LEVEL_MODERATORS))
         .executes(ctx -> spawnAt(ctx, BlockPos.containing(ctx.getSource().getPosition()), name, spawner))
         .then(Commands.argument("pos", Vec3Argument.vec3())
            .executes(ctx -> spawnAt(ctx, BlockPos.containing(Vec3Argument.getVec3(ctx, "pos")), name, spawner)));
   }

   private static int spawnAt(CommandContext<CommandSourceStack> ctx, BlockPos pos, String name, Spawner spawner) {
      CommandSourceStack source = ctx.getSource();
      ServerLevel level = source.getLevel();
      spawner.spawn(level, pos, level.getRandom());
      source.sendSuccess(() -> Component.literal("[rainow] " + name + " spawned at " + pos.getX() + " " + pos.getY() + " " + pos.getZ()), true);
      return 1;
   }

   @FunctionalInterface
   private interface Spawner {
      void spawn(ServerLevel level, BlockPos pos, RandomSource random);
   }
}
