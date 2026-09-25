package com.opatosan.opdropmanhunt.team;

import java.util.Optional;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import com.opatosan.opdropmanhunt.game.GameState;

/** Cria e mantem os times de scoreboard "Runners" (verde) e "Hunters" (vermelho). */
public final class TeamManager {
	private TeamManager() {
	}

	/** Garante que os dois times existem no scoreboard, com cor e friendly fire desligado. */
	public static void ensureScoreboardTeams(MinecraftServer server) {
		Scoreboard scoreboard = server.getScoreboard();

		for (ManhuntTeam team : ManhuntTeam.values()) {
			PlayerTeam vanilla = scoreboard.getPlayerTeam(team.scoreboardName());

			if (vanilla == null) {
				vanilla = scoreboard.addPlayerTeam(team.scoreboardName());
			}

			vanilla.setDisplayName(Component.literal(team.displayName()));
			vanilla.setColor(Optional.of(team.teamColor()));
			vanilla.setAllowFriendlyFire(false);
		}
	}

	public static void join(MinecraftServer server, ServerPlayer player, ManhuntTeam team) {
		GameState state = GameState.get();
		state.rememberName(player.getUUID(), player.getName().getString());
		state.setTeam(player.getUUID(), team);
		applyScoreboard(server, player);
	}

	public static void leave(MinecraftServer server, ServerPlayer player) {
		GameState.get().clearTeam(player.getUUID());
		applyScoreboard(server, player);
	}

	/** Sincroniza o time de scoreboard do jogador com o time do mod. */
	public static void applyScoreboard(MinecraftServer server, ServerPlayer player) {
		ensureScoreboardTeams(server);

		Scoreboard scoreboard = server.getScoreboard();
		String name = player.getScoreboardName();
		ManhuntTeam team = GameState.get().teamOf(player.getUUID());

		for (ManhuntTeam candidate : ManhuntTeam.values()) {
			if (candidate != team) {
				PlayerTeam vanilla = scoreboard.getPlayerTeam(candidate.scoreboardName());

				if (vanilla != null && vanilla.getPlayers().contains(name)) {
					scoreboard.removePlayerFromTeam(name, vanilla);
				}
			}
		}

		if (team != null) {
			PlayerTeam vanilla = scoreboard.getPlayerTeam(team.scoreboardName());

			if (vanilla != null) {
				scoreboard.addPlayerToTeam(name, vanilla);
			}
		}
	}
}
