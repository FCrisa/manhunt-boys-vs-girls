package com.opatosan.opdropmanhunt.team;

import net.minecraft.ChatFormatting;
import net.minecraft.world.scores.TeamColor;

/** Os dois times da partida. */
public enum ManhuntTeam {
	RUNNERS("runner", "Runners", ChatFormatting.GREEN, TeamColor.GREEN),
	HUNTERS("hunter", "Hunters", ChatFormatting.RED, TeamColor.RED);

	private final String id;
	private final String displayName;
	private final ChatFormatting chatColor;
	private final TeamColor teamColor;

	ManhuntTeam(String id, String displayName, ChatFormatting chatColor, TeamColor teamColor) {
		this.id = id;
		this.displayName = displayName;
		this.chatColor = chatColor;
		this.teamColor = teamColor;
	}

	/** Nome usado nos comandos: {@code /manhunt join runner|hunter}. */
	public String id() {
		return this.id;
	}

	public String displayName() {
		return this.displayName;
	}

	public ChatFormatting chatColor() {
		return this.chatColor;
	}

	public TeamColor teamColor() {
		return this.teamColor;
	}

	/** Nome do time no scoreboard do mundo. */
	public String scoreboardName() {
		return "manhunt_" + this.id + "s";
	}
}
