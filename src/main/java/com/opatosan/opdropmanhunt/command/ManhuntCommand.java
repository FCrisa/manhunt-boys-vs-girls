package com.opatosan.opdropmanhunt.command;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import com.opatosan.opdropmanhunt.config.ModConfig;
import com.opatosan.opdropmanhunt.game.GameManager;
import com.opatosan.opdropmanhunt.game.GameState;
import com.opatosan.opdropmanhunt.opdrop.OpDropManager;
import com.opatosan.opdropmanhunt.opdrop.OpTarget;
import com.opatosan.opdropmanhunt.team.ManhuntTeam;
import com.opatosan.opdropmanhunt.upgrade.ArmorLevel;
import com.opatosan.opdropmanhunt.upgrade.UpgradeManager;

/** Todos os comandos do mod, sob {@code /manhunt}. */
public final class ManhuntCommand {
	private ManhuntCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess, environment) -> dispatcher.register(build()));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> build() {
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("manhunt");

		// /manhunt join <runner|hunter>
		LiteralArgumentBuilder<CommandSourceStack> join = Commands.literal("join");

		for (ManhuntTeam team : ManhuntTeam.values()) {
			join.then(Commands.literal(team.id()).executes(ctx -> joinSelf(ctx.getSource(), team)));
		}

		root.then(join);

		// /manhunt leave
		root.then(Commands.literal("leave").executes(ctx -> leaveSelf(ctx.getSource())));

		// /manhunt team <jogador> <runner|hunter>   (op)
		LiteralArgumentBuilder<CommandSourceStack> teamCommand =
				Commands.literal("team").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));

		var playerArgument = Commands.argument("player", EntityArgument.player());

		for (ManhuntTeam team : ManhuntTeam.values()) {
			playerArgument.then(Commands.literal(team.id()).executes(
					ctx -> setTeam(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), team)));
		}

		teamCommand.then(playerArgument);
		root.then(teamCommand);

		// /manhunt teams
		root.then(Commands.literal("teams").executes(ctx -> listTeams(ctx.getSource())));

		// /manhunt start | stop   (op)
		root.then(Commands.literal("start")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(ctx -> start(ctx.getSource())));

		root.then(Commands.literal("stop")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(ctx -> stop(ctx.getSource())));

		// ---- comandos de teste (op) ----

		root.then(Commands.literal("skip")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(ctx -> skip(ctx.getSource())));

		root.then(Commands.literal("settier")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.argument("tier", IntegerArgumentType.integer(1, GameState.MAX_DROP_TIER))
						.executes(ctx -> setTier(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "tier")))));

		root.then(Commands.literal("reveal")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(ctx -> reveal(ctx.getSource())));

		root.then(Commands.literal("status")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(ctx -> status(ctx.getSource())));

		return root;
	}

	// ------------------------------------------------------------------ times

	private static int joinSelf(CommandSourceStack source, ManhuntTeam team) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();

		if (GameState.get().isRunning()) {
			source.sendFailure(Component.literal("Nao da para trocar de time com a partida em andamento."));
			return 0;
		}

		GameManager.onTeamChanged(source.getServer(), player, team);
		source.sendSuccess(() -> Component.literal("Voce entrou no time " + team.displayName() + ".")
				.withStyle(team.chatColor()), false);
		return 1;
	}

	private static int leaveSelf(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();

		if (GameState.get().teamOf(player.getUUID()) == null) {
			source.sendFailure(Component.literal("Voce nao esta em nenhum time."));
			return 0;
		}

		if (GameState.get().isRunning()) {
			source.sendFailure(Component.literal("Nao da para sair do time com a partida em andamento."));
			return 0;
		}

		GameManager.onTeamChanged(source.getServer(), player, null);
		source.sendSuccess(() -> Component.literal("Voce saiu do seu time.").withStyle(ChatFormatting.GRAY), false);
		return 1;
	}

	private static int setTeam(CommandSourceStack source, ServerPlayer target, ManhuntTeam team) {
		GameManager.onTeamChanged(source.getServer(), target, team);
		source.sendSuccess(() -> Component.literal(
						target.getName().getString() + " agora esta no time " + team.displayName() + ".")
				.withStyle(team.chatColor()), false);
		return 1;
	}

	private static int listTeams(CommandSourceStack source) {
		MinecraftServer server = source.getServer();

		for (ManhuntTeam team : ManhuntTeam.values()) {
			String names = String.join(", ", namesOf(server, team));

			source.sendSuccess(() -> Component.literal(team.displayName() + ": ")
					.withStyle(team.chatColor(), ChatFormatting.BOLD)
					.append(Component.literal(names.isEmpty() ? "(vazio)" : names).withStyle(ChatFormatting.WHITE)),
					false);
		}

		return 1;
	}

	private static List<String> namesOf(MinecraftServer server, ManhuntTeam team) {
		GameState state = GameState.get();
		List<String> names = new ArrayList<>();

		for (UUID id : state.membersOf(team)) {
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			String name = player != null ? player.getName().getString() : state.nameOf(id) + " (offline)";

			if (state.isEliminated(id)) {
				name = name + " [eliminado]";
			}

			names.add(name);
		}

		return names;
	}

	// ---------------------------------------------------------------- partida

	private static int start(CommandSourceStack source) {
		String error = GameManager.start(source.getServer());

		if (error != null) {
			source.sendFailure(Component.literal(error));
			return 0;
		}

		return 1;
	}

	private static int stop(CommandSourceStack source) {
		if (!GameManager.stop(source.getServer())) {
			source.sendFailure(Component.literal("Nenhuma partida em andamento."));
			return 0;
		}

		return 1;
	}

	// ------------------------------------------------------------------ teste

	private static int skip(CommandSourceStack source) {
		if (!GameState.get().isRunning()) {
			source.sendFailure(Component.literal("Nenhuma partida em andamento."));
			return 0;
		}

		MinecraftServer server = source.getServer();
		var dimension = source.getLevel().dimension();

		OpDropManager.pickNewTarget(server, dimension, source.getLevel().getRandom());
		OpDropManager.sendActionBars(server);

		OpTarget target = OpDropManager.current();
		String name = target == null ? "(nenhum)" : target.displayName();
		source.sendSuccess(() -> Component.literal("Novo alvo OP: " + name).withStyle(ChatFormatting.GREEN), false);
		return 1;
	}

	private static int setTier(CommandSourceStack source, int tier) {
		if (!GameState.get().isRunning()) {
			source.sendFailure(Component.literal("Nenhuma partida em andamento."));
			return 0;
		}

		GameState state = GameState.get();
		int armorLevel = Math.clamp(tier - 1, 0, ArmorLevel.MAX);

		// Alinha o relogio da partida com o nivel forcado, para o timer natural continuar
		// de onde parou em vez de voltar atras.
		state.setGameTicks(armorLevel * ModConfig.get().upgradeIntervalTicks());
		UpgradeManager.applyLevel(source.getServer(), armorLevel, true);

		source.sendSuccess(() -> Component.literal(
						"Tier de drop " + state.dropTier() + ", armadura dos Hunters: "
								+ ArmorLevel.byIndex(state.hunterArmorLevel()).displayName())
				.withStyle(ChatFormatting.YELLOW), false);
		return 1;
	}

	private static int reveal(CommandSourceStack source) {
		if (!GameState.get().isRunning() || OpDropManager.current() == null) {
			source.sendFailure(Component.literal("Nenhuma partida em andamento."));
			return 0;
		}

		if (!OpDropManager.revealOneLetter(source.getLevel().getRandom())) {
			source.sendFailure(Component.literal("Todas as letras ja foram reveladas."));
			return 0;
		}

		OpDropManager.sendActionBars(source.getServer());
		source.sendSuccess(() -> Component.literal("Revelado: " + OpDropManager.maskedName())
				.withStyle(ChatFormatting.GREEN), false);
		return 1;
	}

	private static int status(CommandSourceStack source) {
		GameState state = GameState.get();

		if (!state.isRunning()) {
			source.sendSuccess(() -> Component.literal("Nenhuma partida em andamento.")
					.withStyle(ChatFormatting.GRAY), false);
			return 1;
		}

		OpTarget target = OpDropManager.current();

		source.sendSuccess(() -> Component.literal("Tempo: ").withStyle(ChatFormatting.GRAY)
				.append(Component.literal(state.formattedTime()).withStyle(ChatFormatting.WHITE)), false);
		source.sendSuccess(() -> Component.literal("Tier de drop: ").withStyle(ChatFormatting.GRAY)
				.append(Component.literal(String.valueOf(state.dropTier())).withStyle(ChatFormatting.WHITE))
				.append(Component.literal(" | Armadura dos Hunters: ").withStyle(ChatFormatting.GRAY))
				.append(Component.literal(ArmorLevel.byIndex(state.hunterArmorLevel()).displayName())
						.withStyle(ChatFormatting.WHITE)), false);

		if (target == null) {
			source.sendSuccess(() -> Component.literal("Alvo OP: (nenhum)").withStyle(ChatFormatting.GRAY), false);
		} else {
			source.sendSuccess(() -> Component.literal("Alvo OP: ").withStyle(ChatFormatting.GRAY)
					.append(Component.literal(target.displayName()).withStyle(ChatFormatting.GREEN))
					.append(Component.literal(" (" + target.dimension().identifier() + ")")
							.withStyle(ChatFormatting.DARK_GRAY)), false);
			source.sendSuccess(() -> Component.literal("Runners veem: ").withStyle(ChatFormatting.GRAY)
					.append(Component.literal(OpDropManager.maskedName()).withStyle(ChatFormatting.GREEN))
					.append(Component.literal(" (" + OpDropManager.revealedSummary() + " letras)")
							.withStyle(ChatFormatting.DARK_GRAY)), false);
		}

		return 1;
	}
}
