package com.opatosan.opdropmanhunt.game;

import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

import com.opatosan.opdropmanhunt.OpDropManhunt;
import com.opatosan.opdropmanhunt.config.ModConfig;
import com.opatosan.opdropmanhunt.opdrop.OpDropManager;
import com.opatosan.opdropmanhunt.team.ManhuntTeam;
import com.opatosan.opdropmanhunt.team.TeamManager;
import com.opatosan.opdropmanhunt.tracker.TrackerCompass;
import com.opatosan.opdropmanhunt.upgrade.UpgradeManager;

/** Inicio, fim, mortes e vitoria da partida. */
public final class GameManager {
	/** Horario do mundo no inicio da partida (manha). */
	private static final long MORNING = 1000L;

	private static boolean matchOver;

	private GameManager() {
	}

	// ----------------------------------------------------------------- start

	/** Motivo pelo qual o start falhou, ou null se deu certo. */
	public static String start(MinecraftServer server) {
		GameState state = GameState.get();

		if (state.isRunning()) {
			return "Uma partida ja esta em andamento.";
		}

		int runners = state.membersOf(ManhuntTeam.RUNNERS).size();
		int hunters = state.membersOf(ManhuntTeam.HUNTERS).size();

		if (runners == 0 && hunters == 0) {
			return "Precisa de pelo menos 1 runner e 1 hunter para comecar. Use /manhunt join runner ou /manhunt join hunter.";
		}

		if (runners == 0) {
			return "Precisa de pelo menos 1 runner para comecar. Use /manhunt join runner.";
		}

		if (hunters == 0) {
			return "Precisa de pelo menos 1 hunter para comecar. Use /manhunt join hunter.";
		}

		ModConfig.reload();

		state.clearEliminations();
		state.resetTicks();
		state.setHunterArmorLevel(0);
		state.setRunning(true);
		matchOver = false;

		TrackerCompass.reset();
		OpDropManager.reset();
		UpgradeManager.onGameStart();
		TeamManager.ensureScoreboardTeams(server);

		// Dia, para comecar sem mobs em cima de todo mundo. No 26.2 o horario do mundo
		// e controlado por "clocks" por tipo de dimensao, nao mais por setDayTime.
		ServerClockManager clocks = server.clockManager();

		for (ServerLevel level : server.getAllLevels()) {
			level.dimensionTypeRegistration().value().defaultClock()
					.ifPresent(clock -> clocks.setTotalTicks(clock, MORNING));
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			TeamManager.applyScoreboard(server, player);

			if (state.teamOf(player.getUUID()) != null) {
				resetPlayer(player);
			}
		}

		// Hunters comecam com a bussola e sem armadura.
		for (ServerPlayer hunter : state.onlineMembers(server, ManhuntTeam.HUNTERS)) {
			TrackerCompass.ensureCompass(hunter);
			UpgradeManager.equip(hunter);
		}

		OpDropManager.pickNewTarget(server, Level.OVERWORLD, server.overworld().getRandom());
		OpDropManager.sendActionBars(server);

		Broadcast.titleToAll(server,
				Component.literal("Game Start").withStyle(ChatFormatting.YELLOW),
				Component.empty(), 10, 50, 20);
		Broadcast.soundToAll(server, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);

		OpDropManhunt.LOGGER.info("Partida iniciada com {} runner(s) e {} hunter(s)", runners, hunters);
		return null;
	}

	/** Deixa o jogador pronto para a partida: sobrevivencia, vida/fome cheias, sem itens. */
	private static void resetPlayer(ServerPlayer player) {
		player.getInventory().clearContent();
		player.removeAllEffects();
		player.setHealth(player.getMaxHealth());
		player.getFoodData().setFoodLevel(20);
		player.getFoodData().setSaturation(5.0F);

		// Zera XP; o ServerPlayer reenvia o valor para o cliente no proximo tick.
		player.experienceLevel = 0;
		player.experienceProgress = 0.0F;
		player.totalExperience = 0;

		player.setGameMode(GameType.SURVIVAL);
	}

	// ------------------------------------------------------------------ stop

	public static boolean stop(MinecraftServer server) {
		if (!GameState.get().isRunning()) {
			return false;
		}

		shutdown(server);
		Broadcast.chatToAll(server, Component.literal("Manhunt stopped").withStyle(ChatFormatting.RED));
		OpDropManhunt.LOGGER.info("Partida encerrada por comando");
		return true;
	}

	/** Para os timers, a action bar e devolve a liberdade dos comandos de time. */
	private static void shutdown(MinecraftServer server) {
		GameState.get().setRunning(false);

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			TrackerCompass.removeCompass(player);
			UpgradeManager.unequip(player);
		}
	}

	// ------------------------------------------------------------------ tick

	public static void tick(MinecraftServer server) {
		GameState state = GameState.get();

		if (!state.isRunning()) {
			return;
		}

		state.tickGame();
		UpgradeManager.tick(server);
		TrackerCompass.tick(server);
		OpDropManager.tick(server);
	}

	// --------------------------------------------------------------- eventos

	public static void onPlayerJoin(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();

		if (server == null) {
			return;
		}

		GameState state = GameState.get();
		TeamManager.applyScoreboard(server, player);

		if (!state.isRunning()) {
			return;
		}

		if (state.isHunter(player.getUUID())) {
			TrackerCompass.ensureCompass(player);
			UpgradeManager.equip(player);
		} else if (state.isEliminated(player.getUUID())) {
			player.setGameMode(GameType.SPECTATOR);
		}
	}

	/** Hunter volta com bussola e armadura; Runner eliminado volta como espectador. */
	public static void onRespawn(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();

		if (server == null) {
			return;
		}

		GameState state = GameState.get();
		TeamManager.applyScoreboard(server, player);

		if (!state.isRunning()) {
			return;
		}

		if (state.isHunter(player.getUUID())) {
			TrackerCompass.ensureCompass(player);
			UpgradeManager.equip(player);
		} else if (state.isEliminated(player.getUUID())) {
			player.setGameMode(GameType.SPECTATOR);
		}
	}

	public static void onTeamChanged(MinecraftServer server, ServerPlayer player, ManhuntTeam team) {
		if (team == null) {
			TeamManager.leave(server, player);
		} else {
			TeamManager.join(server, player, team);
		}

		TrackerCompass.removeCompass(player);
		UpgradeManager.unequip(player);

		if (GameState.get().isRunning() && team == ManhuntTeam.HUNTERS) {
			TrackerCompass.ensureCompass(player);
			UpgradeManager.equip(player);
		}
	}

	public static void onRunnerDeath(ServerPlayer runner) {
		MinecraftServer server = runner.level().getServer();
		GameState state = GameState.get();

		if (server == null || !state.isRunning()) {
			return;
		}

		state.eliminate(runner.getUUID());
		OpDropManhunt.LOGGER.info("Runner {} foi eliminado", runner.getName().getString());

		if (state.allRunnersEliminated()) {
			huntersWin(server);
		}
	}

	public static void onDragonKilled(MinecraftServer server) {
		GameState state = GameState.get();

		if (state.isRunning() && !matchOver && !state.allRunnersEliminated()) {
			runnersWin(server);
		}
	}

	// --------------------------------------------------------------- vitoria

	private static void runnersWin(MinecraftServer server) {
		matchOver = true;
		celebrateNearRunners(server);
		shutdown(server);

		Broadcast.titleToAll(server,
				Component.literal("Runners Wins!!").withStyle(ChatFormatting.GREEN),
				Component.empty(), 10, 80, 20);
		Broadcast.soundToAll(server, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
		Broadcast.soundToAll(server, SoundEvents.PLAYER_LEVELUP, 1.0F, 1.0F);
		OpDropManhunt.LOGGER.info("Runners venceram: o Ender Dragon caiu");
	}

	private static void huntersWin(MinecraftServer server) {
		matchOver = true;
		shutdown(server);

		Broadcast.titleToAll(server,
				Component.literal("Hunters Wins!!").withStyle(ChatFormatting.RED),
				Component.empty(), 10, 80, 20);
		Broadcast.soundToAll(server, SoundEvents.ENDER_DRAGON_DEATH, 1.0F, 1.0F);
		OpDropManhunt.LOGGER.info("Hunters venceram: todos os runners caíram");
	}

	/** Solta alguns fogos perto de cada Runner vivo. */
	private static void celebrateNearRunners(MinecraftServer server) {
		GameState state = GameState.get();

		for (UUID runnerId : state.aliveRunners()) {
			ServerPlayer runner = server.getPlayerList().getPlayer(runnerId);

			if (runner == null || !(runner.level() instanceof ServerLevel level)) {
				continue;
			}

			RandomSource random = level.getRandom();

			for (int i = 0; i < 5; i++) {
				double x = runner.getX() + (random.nextDouble() - 0.5) * 6.0;
				double z = runner.getZ() + (random.nextDouble() - 0.5) * 6.0;
				level.addFreshEntity(new FireworkRocketEntity(
						level, x, runner.getY() + 0.5, z, new ItemStack(Items.FIREWORK_ROCKET)));
			}
		}
	}
}
