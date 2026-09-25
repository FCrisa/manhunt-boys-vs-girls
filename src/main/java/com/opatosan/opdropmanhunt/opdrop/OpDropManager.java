package com.opatosan.opdropmanhunt.opdrop;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.opatosan.opdropmanhunt.OpDropManhunt;
import com.opatosan.opdropmanhunt.config.ModConfig;
import com.opatosan.opdropmanhunt.game.Broadcast;
import com.opatosan.opdropmanhunt.game.GameState;
import com.opatosan.opdropmanhunt.item.Enchanting;

/**
 * O alvo OP atual, a revelacao das letras para os Runners, a action bar e o drop em si.
 */
public final class OpDropManager {
	private static final int ACTION_BAR_INTERVAL_TICKS = 20;

	/** Quantos alvos recentes nao podem se repetir. */
	private static final int RECENT_MEMORY = 5;

	private static final Deque<String> RECENT = new ArrayDeque<>();

	private static OpTarget current;

	/** Indices ja revelados do nome do alvo atual. */
	private static final Set<Integer> REVEALED = new HashSet<>();

	private static long nextRevealTick;

	private OpDropManager() {
	}

	// ----------------------------------------------------------------- estado

	public static OpTarget current() {
		return current;
	}

	public static void reset() {
		current = null;
		RECENT.clear();
		REVEALED.clear();
		nextRevealTick = 0L;
	}

	/** Nome do alvo com as letras ainda escondidas como "_". */
	public static String maskedName() {
		if (current == null) {
			return "";
		}

		String name = current.displayName();
		StringBuilder masked = new StringBuilder(name.length());

		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);

			if (c == ' ') {
				masked.append(' ');
			} else if (REVEALED.contains(i)) {
				masked.append(c);
			} else {
				masked.append('_');
			}
		}

		return masked.toString();
	}

	public static String revealedSummary() {
		return REVEALED.size() + "/" + countLetters();
	}

	private static int countLetters() {
		if (current == null) {
			return 0;
		}

		int letters = 0;

		for (int i = 0; i < current.displayName().length(); i++) {
			if (current.displayName().charAt(i) != ' ') {
				letters++;
			}
		}

		return letters;
	}

	// -------------------------------------------------------------- revelacao

	/** Revela uma letra aleatoria ainda escondida. Retorna false se ja estava tudo revelado. */
	public static boolean revealOneLetter(RandomSource random) {
		if (current == null) {
			return false;
		}

		List<Integer> hidden = new ArrayList<>();
		String name = current.displayName();

		for (int i = 0; i < name.length(); i++) {
			if (name.charAt(i) != ' ' && !REVEALED.contains(i)) {
				hidden.add(i);
			}
		}

		if (hidden.isEmpty()) {
			return false;
		}

		REVEALED.add(hidden.get(random.nextInt(hidden.size())));
		return true;
	}

	// ------------------------------------------------------------ novo alvo

	/**
	 * Sorteia um novo alvo na dimensao informada, respeitando a dificuldade do tier
	 * e sem repetir os ultimos {@value #RECENT_MEMORY}.
	 */
	public static void pickNewTarget(MinecraftServer server, ResourceKey<Level> dimension, RandomSource random) {
		int tier = GameState.get().dropTier();
		List<Integer> difficulties = OpTargets.difficultiesForTier(tier);

		// 1) dimensao + dificuldade do tier + sem repetir os recentes
		List<OpTarget> candidates = filter(dimension, difficulties, true);

		// 2) relaxa a dificuldade
		if (candidates.isEmpty()) {
			candidates = filter(dimension, null, true);
		}

		// 3) ultimo recurso: permite repetir um recente
		if (candidates.isEmpty()) {
			candidates = filter(dimension, null, false);
		}

		if (candidates.isEmpty()) {
			OpDropManhunt.LOGGER.warn("Nenhum alvo OP disponivel para a dimensao {}", dimension.identifier());
			return;
		}

		setTarget(candidates.get(random.nextInt(candidates.size())));
	}

	private static List<OpTarget> filter(ResourceKey<Level> dimension, List<Integer> difficulties, boolean avoidRecent) {
		List<OpTarget> result = new ArrayList<>();

		for (OpTarget target : OpTargets.ALL) {
			if (!target.dimension().equals(dimension)) {
				continue;
			}

			if (difficulties != null && !target.allowedBy(difficulties)) {
				continue;
			}

			if (avoidRecent && RECENT.contains(key(target))) {
				continue;
			}

			result.add(target);
		}

		return result;
	}

	private static String key(OpTarget target) {
		return target.dimension().identifier() + "/" + target.displayName();
	}

	private static void setTarget(OpTarget target) {
		current = target;
		REVEALED.clear();
		nextRevealTick = GameState.get().gameTicks() + ModConfig.get().letterRevealIntervalTicks();

		RECENT.addLast(key(target));

		while (RECENT.size() > RECENT_MEMORY) {
			RECENT.removeFirst();
		}

		OpDropManhunt.LOGGER.info("Novo alvo OP: {} ({}), tier {}",
				target.displayName(), target.dimension().identifier(), GameState.get().dropTier());
	}

	// ------------------------------------------------------------------ tick

	public static void tick(MinecraftServer server) {
		GameState state = GameState.get();

		if (!state.isRunning() || current == null) {
			return;
		}

		if (state.gameTicks() >= nextRevealTick) {
			revealOneLetter(server.overworld().getRandom());
			nextRevealTick = state.gameTicks() + ModConfig.get().letterRevealIntervalTicks();
		}

		if (state.gameTicks() % ACTION_BAR_INTERVAL_TICKS == 0L) {
			sendActionBars(server);
		}
	}

	/** Action bar verde: Runner ve underscores, Hunter e espectador veem o nome. */
	public static void sendActionBars(MinecraftServer server) {
		if (current == null) {
			return;
		}

		GameState state = GameState.get();
		Component hidden = line(maskedName());
		Component full = line(current.displayName());

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			boolean isActiveRunner = state.isRunner(player.getUUID()) && !state.isEliminated(player.getUUID());
			Broadcast.actionBar(player, isActiveRunner ? hidden : full);
		}
	}

	private static Component line(String name) {
		return Component.literal("Current OP Drop: " + name).withStyle(ChatFormatting.GREEN);
	}

	// ------------------------------------------------------------------ drop

	/**
	 * Um Runner conseguiu o alvo: dropa os itens do tier, avisa todo mundo e
	 * sorteia o proximo alvo.
	 */
	public static void award(ServerPlayer runner, ServerLevel level, Vec3 position) {
		GameState state = GameState.get();
		MinecraftServer server = level.getServer();
		OpTarget achieved = current;

		if (achieved == null || server == null) {
			return;
		}

		ModConfig config = ModConfig.get();
		RandomSource random = level.getRandom();

		OpLootTables.RollContext ctx = new OpLootTables.RollContext(
				random, Enchanting.lookup(level), config.upToEnchantChance);

		List<ItemStack> loot = OpLootTables.roll(state.dropTier(), config.entriesPerDrop, ctx);

		for (ItemStack stack : loot) {
			spawnItem(level, position, stack, random);
		}

		level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
				position.x(), position.y() + 0.5, position.z(), 40, 0.6, 0.6, 0.6, 0.1);
		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
				position.x(), position.y() + 0.5, position.z(), 25, 0.5, 0.5, 0.5, 0.2);

		Broadcast.soundToAll(server, SoundEvents.PLAYER_LEVELUP, 1.0F, 1.0F);
		Broadcast.chatToAll(server, Component.literal(
						runner.getName().getString() + " found the OP drop: " + achieved.displayName() + "!")
				.withStyle(ChatFormatting.GREEN));

		OpDropManhunt.LOGGER.info("{} conseguiu o alvo OP {} (tier {}), {} item(ns) dropado(s)",
				runner.getName().getString(), achieved.displayName(), state.dropTier(), loot.size());

		// Proximo alvo na dimensao onde o Runner esta, escondido de novo.
		pickNewTarget(server, level.dimension(), random);
		sendActionBars(server);
	}

	private static void spawnItem(ServerLevel level, Vec3 position, ItemStack stack, RandomSource random) {
		if (stack.isEmpty()) {
			return;
		}

		ItemEntity entity = new ItemEntity(level, position.x(), position.y() + 0.5, position.z(), stack);
		// Leve estouro para cima, para os itens nao ficarem empilhados no mesmo ponto.
		entity.setDeltaMovement(
				(random.nextDouble() - 0.5) * 0.15,
				0.25 + random.nextDouble() * 0.1,
				(random.nextDouble() - 0.5) * 0.15);
		level.addFreshEntity(entity);
	}
}
