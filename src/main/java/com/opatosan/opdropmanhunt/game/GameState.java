package com.opatosan.opdropmanhunt.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import com.opatosan.opdropmanhunt.team.ManhuntTeam;

/** Estado da partida em memoria. Vive apenas enquanto o servidor esta ligado. */
public final class GameState {
	/** Tier maximo de OP drop. */
	public static final int MAX_DROP_TIER = 6;

	private static final GameState INSTANCE = new GameState();

	private final Map<UUID, ManhuntTeam> teams = new HashMap<>();
	private final Map<UUID, String> names = new HashMap<>();
	private final Set<UUID> eliminated = new HashSet<>();

	private boolean running;
	private long gameTicks;

	/** Indice do nivel de armadura dos Hunters (0 = sem armadura, 6 = netherite encantada). */
	private int hunterArmorLevel;

	private GameState() {
	}

	public static GameState get() {
		return INSTANCE;
	}

	// ---------------------------------------------------------------- partida

	public boolean isRunning() {
		return this.running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	public long gameTicks() {
		return this.gameTicks;
	}

	public void tickGame() {
		this.gameTicks++;
	}

	public void setGameTicks(long ticks) {
		this.gameTicks = Math.max(0L, ticks);
	}

	public void resetTicks() {
		this.gameTicks = 0L;
	}

	/** Tempo de partida formatado como {@code mm:ss}. */
	public String formattedTime() {
		long totalSeconds = this.gameTicks / 20L;
		return String.format("%02d:%02d", totalSeconds / 60L, totalSeconds % 60L);
	}

	// ------------------------------------------------------------------ tiers

	public int hunterArmorLevel() {
		return this.hunterArmorLevel;
	}

	public void setHunterArmorLevel(int level) {
		this.hunterArmorLevel = level;
	}

	/**
	 * Tier do OP drop (1 a 6).
	 *
	 * <p>Comeca em 1 e sobe junto com a armadura dos Hunters, travando em 6. Por isso
	 * o upgrade de 60 min (armadura 6) nao muda o tier de drop.</p>
	 */
	public int dropTier() {
		return Math.min(MAX_DROP_TIER, this.hunterArmorLevel + 1);
	}

	// ------------------------------------------------------------------ times

	public void setTeam(UUID id, ManhuntTeam team) {
		this.teams.put(id, team);
	}

	public void clearTeam(UUID id) {
		this.teams.remove(id);
		this.eliminated.remove(id);
	}

	public ManhuntTeam teamOf(UUID id) {
		return this.teams.get(id);
	}

	public boolean isRunner(UUID id) {
		return this.teams.get(id) == ManhuntTeam.RUNNERS;
	}

	public boolean isHunter(UUID id) {
		return this.teams.get(id) == ManhuntTeam.HUNTERS;
	}

	/** Todos os jogadores registrados no time, online ou nao. */
	public Set<UUID> membersOf(ManhuntTeam team) {
		Set<UUID> result = new LinkedHashSet<>();

		for (Map.Entry<UUID, ManhuntTeam> entry : this.teams.entrySet()) {
			if (entry.getValue() == team) {
				result.add(entry.getKey());
			}
		}

		return result;
	}

	/** Jogadores do time que estao online agora. */
	public List<ServerPlayer> onlineMembers(MinecraftServer server, ManhuntTeam team) {
		List<ServerPlayer> result = new ArrayList<>();

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (this.teams.get(player.getUUID()) == team) {
				result.add(player);
			}
		}

		return result;
	}

	public void rememberName(UUID id, String name) {
		this.names.put(id, name);
	}

	public String nameOf(UUID id) {
		return this.names.getOrDefault(id, "?");
	}

	// ------------------------------------------------------------ eliminacoes

	public void eliminate(UUID id) {
		this.eliminated.add(id);
	}

	public boolean isEliminated(UUID id) {
		return this.eliminated.contains(id);
	}

	/** True quando existe pelo menos um Runner e todos estao eliminados. */
	public boolean allRunnersEliminated() {
		Set<UUID> runners = membersOf(ManhuntTeam.RUNNERS);

		if (runners.isEmpty()) {
			return false;
		}

		return this.eliminated.containsAll(runners);
	}

	public List<UUID> aliveRunners() {
		List<UUID> result = new ArrayList<>();

		for (UUID id : membersOf(ManhuntTeam.RUNNERS)) {
			if (!this.eliminated.contains(id)) {
				result.add(id);
			}
		}

		return result;
	}

	public void clearEliminations() {
		this.eliminated.clear();
	}

	public void clearAll() {
		this.teams.clear();
		this.names.clear();
		this.eliminated.clear();
		this.running = false;
		this.gameTicks = 0L;
		this.hunterArmorLevel = 0;
	}
}
