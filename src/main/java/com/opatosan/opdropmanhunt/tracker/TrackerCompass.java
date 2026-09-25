package com.opatosan.opdropmanhunt.tracker;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import com.opatosan.opdropmanhunt.game.GameState;
import com.opatosan.opdropmanhunt.item.ModItems;
import com.opatosan.opdropmanhunt.team.ManhuntTeam;

/**
 * Bussola "Runner Tracker" dos Hunters.
 *
 * <p>Aponta para o Runner vivo mais proximo na mesma dimensao. Se nenhum Runner estiver
 * na dimensao do Hunter, aponta para a ultima posicao conhecida de um Runner ali (tipica
 * mente o portal por onde ele saiu).</p>
 */
public final class TrackerCompass {
	private static final int UPDATE_INTERVAL_TICKS = 10;
	private static final int SWEEP_INTERVAL_TICKS = 20;

	/** Raio, em chunks, da varredura de bau em volta de cada jogador online. */
	private static final int SWEEP_CHUNK_RADIUS = 2;

	/** Ultima posicao vista de cada Runner, por dimensao. */
	private static final Map<UUID, Map<ResourceKey<Level>, LastSeen>> LAST_KNOWN = new HashMap<>();

	private TrackerCompass() {
	}

	private record LastSeen(BlockPos pos, long tick) {
	}

	public static void reset() {
		LAST_KNOWN.clear();
	}

	/** Cria a bussola marcada com custom data. */
	public static ItemStack create() {
		ItemStack stack = new ItemStack(Items.COMPASS);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal("Runner Tracker").withStyle(ChatFormatting.RED));
		// Lodestone tracker sem posicao ainda; "tracked = false" evita que o item suma
		// quando nao existe lodestone de verdade na posicao apontada.
		stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.empty(), false));
		ModItems.tag(stack, ModItems.TRACKER_TAG);
		return stack;
	}

	public static void tick(MinecraftServer server) {
		GameState state = GameState.get();

		if (!state.isRunning()) {
			return;
		}

		recordRunnerPositions(server);

		if (state.gameTicks() % UPDATE_INTERVAL_TICKS == 0L) {
			for (ServerPlayer hunter : state.onlineMembers(server, ManhuntTeam.HUNTERS)) {
				updateCompass(server, hunter);
			}
		}

		if (state.gameTicks() % SWEEP_INTERVAL_TICKS == 0L) {
			sweep(server);
		}
	}

	private static void recordRunnerPositions(MinecraftServer server) {
		GameState state = GameState.get();

		for (ServerPlayer runner : state.onlineMembers(server, ManhuntTeam.RUNNERS)) {
			if (state.isEliminated(runner.getUUID())) {
				continue;
			}

			LAST_KNOWN.computeIfAbsent(runner.getUUID(), key -> new HashMap<>())
					.put(runner.level().dimension(), new LastSeen(runner.blockPosition(), state.gameTicks()));
		}
	}

	/** Garante exatamente uma bussola no inventario do Hunter. */
	public static void ensureCompass(ServerPlayer hunter) {
		Inventory inventory = hunter.getInventory();
		boolean found = false;

		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			if (ModItems.isTracker(inventory.getItem(slot))) {
				if (found) {
					inventory.setItem(slot, ItemStack.EMPTY);
				} else {
					found = true;
				}
			}
		}

		if (!found) {
			hunter.addItem(create());
		}
	}

	public static void removeCompass(ServerPlayer player) {
		Inventory inventory = player.getInventory();

		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			if (ModItems.isTracker(inventory.getItem(slot))) {
				inventory.setItem(slot, ItemStack.EMPTY);
			}
		}
	}

	private static void updateCompass(MinecraftServer server, ServerPlayer hunter) {
		ResourceKey<Level> dimension = hunter.level().dimension();
		BlockPos target = findTarget(server, hunter, dimension);

		if (target == null) {
			return;
		}

		LodestoneTracker tracker = new LodestoneTracker(Optional.of(GlobalPos.of(dimension, target)), false);
		Inventory inventory = hunter.getInventory();

		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);

			if (ModItems.isTracker(stack)) {
				stack.set(DataComponents.LODESTONE_TRACKER, tracker);
			}
		}
	}

	/** Runner vivo mais proximo na dimensao; senao, a ultima posicao conhecida ali. */
	private static BlockPos findTarget(MinecraftServer server, ServerPlayer hunter, ResourceKey<Level> dimension) {
		GameState state = GameState.get();
		BlockPos nearest = null;
		double nearestDistance = Double.MAX_VALUE;

		for (UUID runnerId : state.aliveRunners()) {
			ServerPlayer runner = server.getPlayerList().getPlayer(runnerId);

			if (runner != null && runner.level().dimension().equals(dimension)) {
				double distance = hunter.distanceToSqr(runner);

				if (distance < nearestDistance) {
					nearestDistance = distance;
					nearest = runner.blockPosition();
				}
			}
		}

		if (nearest != null) {
			return nearest;
		}

		// Ninguem nesta dimensao: usa o registro mais recente de qualquer Runner.
		LastSeen best = null;

		for (UUID runnerId : state.aliveRunners()) {
			Map<ResourceKey<Level>, LastSeen> known = LAST_KNOWN.get(runnerId);

			if (known == null) {
				continue;
			}

			LastSeen seen = known.get(dimension);

			if (seen != null && (best == null || seen.tick() > best.tick())) {
				best = seen;
			}
		}

		return best == null ? null : best.pos();
	}

	/**
	 * Garantia de 1 em 1 segundo: todo Hunter tem a bussola, e ninguem mais tem.
	 * Tambem limpa bussolas esquecidas em baus perto dos jogadores.
	 */
	private static void sweep(MinecraftServer server) {
		GameState state = GameState.get();

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (state.isHunter(player.getUUID())) {
				ensureCompass(player);
			} else {
				removeCompass(player);
			}
		}

		sweepContainers(server);
	}

	private static void sweepContainers(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!(player.level() instanceof ServerLevel level)) {
				continue;
			}

			int centerX = player.blockPosition().getX() >> 4;
			int centerZ = player.blockPosition().getZ() >> 4;

			for (int x = centerX - SWEEP_CHUNK_RADIUS; x <= centerX + SWEEP_CHUNK_RADIUS; x++) {
				for (int z = centerZ - SWEEP_CHUNK_RADIUS; z <= centerZ + SWEEP_CHUNK_RADIUS; z++) {
					LevelChunk chunk = level.getChunk(x, z);

					for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
						if (blockEntity instanceof Container container) {
							clearTrackers(container);
						}
					}
				}
			}
		}
	}

	private static void clearTrackers(Container container) {
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			if (ModItems.isTracker(container.getItem(slot))) {
				container.setItem(slot, ItemStack.EMPTY);
			}
		}
	}
}
