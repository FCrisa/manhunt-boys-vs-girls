package com.opatosan.opdropmanhunt.upgrade;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import com.opatosan.opdropmanhunt.OpDropManhunt;
import com.opatosan.opdropmanhunt.config.ModConfig;
import com.opatosan.opdropmanhunt.game.Broadcast;
import com.opatosan.opdropmanhunt.game.GameState;
import com.opatosan.opdropmanhunt.item.ModItems;
import com.opatosan.opdropmanhunt.team.ManhuntTeam;

/** Upgrade de armadura dos Hunters (e, junto, do tier dos OP drops). */
public final class UpgradeManager {
	private UpgradeManager() {
	}

	public static void onGameStart() {
		GameState.get().setHunterArmorLevel(0);
	}

	/** Sobe o nivel quando o tempo de partida cruza mais um intervalo de upgrade. */
	public static void tick(MinecraftServer server) {
		GameState state = GameState.get();

		if (!state.isRunning()) {
			return;
		}

		long interval = ModConfig.get().upgradeIntervalTicks();
		int targetLevel = (int) Math.min(ArmorLevel.MAX, state.gameTicks() / interval);

		if (targetLevel > state.hunterArmorLevel()) {
			applyLevel(server, targetLevel, true);
		}
	}

	/**
	 * Aplica um nivel de armadura.
	 *
	 * @param announce se deve mandar as mensagens de upgrade no chat
	 */
	public static void applyLevel(MinecraftServer server, int level, boolean announce) {
		GameState state = GameState.get();
		int previousDropTier = state.dropTier();

		state.setHunterArmorLevel(Math.clamp(level, 0, ArmorLevel.MAX));

		for (ServerPlayer hunter : state.onlineMembers(server, ManhuntTeam.HUNTERS)) {
			equip(hunter);
		}

		if (announce) {
			Broadcast.chatToAll(server, Component.literal("Hunters have been UPGRADED")
					.withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
			Broadcast.soundToAll(server, SoundEvents.ANVIL_USE, 1.0F, 1.0F);

			// O tier de drop trava em 6, entao o upgrade de 60 min nao manda esta mensagem.
			if (state.dropTier() != previousDropTier) {
				Broadcast.chatToAll(server, Component.literal("OP drops have been UPGRADED")
						.withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
				Broadcast.soundToAll(server, SoundEvents.PLAYER_LEVELUP, 1.0F, 1.0F);
			}
		}

		OpDropManhunt.LOGGER.info("Hunters no nivel {} ({}), tier de drop {}",
				state.hunterArmorLevel(), ArmorLevel.byIndex(state.hunterArmorLevel()).displayName(), state.dropTier());
	}

	/** Coloca o set completo do nivel atual direto nos slots de armadura. */
	public static void equip(ServerPlayer hunter) {
		ArmorLevel level = ArmorLevel.byIndex(GameState.get().hunterArmorLevel());

		for (EquipmentSlot slot : ArmorLevel.ARMOR_SLOTS) {
			hunter.setItemSlot(slot, level.build(hunter.level(), slot));
		}
	}

	/** Remove apenas as pecas entregues pelo mod. */
	public static void unequip(ServerPlayer player) {
		for (EquipmentSlot slot : ArmorLevel.ARMOR_SLOTS) {
			if (ModItems.isHunterArmor(player.getItemBySlot(slot))) {
				player.setItemSlot(slot, ItemStack.EMPTY);
			}
		}
	}
}
