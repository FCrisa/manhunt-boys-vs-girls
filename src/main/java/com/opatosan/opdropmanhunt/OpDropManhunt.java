package com.opatosan.opdropmanhunt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

import com.opatosan.opdropmanhunt.command.ManhuntCommand;
import com.opatosan.opdropmanhunt.config.ModConfig;
import com.opatosan.opdropmanhunt.game.GameManager;
import com.opatosan.opdropmanhunt.game.GameState;
import com.opatosan.opdropmanhunt.item.ModItems;
import com.opatosan.opdropmanhunt.opdrop.OpDropManager;
import com.opatosan.opdropmanhunt.opdrop.OpTarget;
import com.opatosan.opdropmanhunt.tracker.TrackerCompass;

/**
 * Entrypoint do mod. Toda a logica roda no servidor (inclusive o servidor integrado
 * de um mundo singleplayer aberto para amigos).
 */
public class OpDropManhunt implements ModInitializer {
	public static final String MOD_ID = "opdropmanhunt";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModConfig.get();
		ManhuntCommand.register();

		ServerTickEvents.END_SERVER_TICK.register(GameManager::tick);

		ServerPlayConnectionEvents.JOIN.register(
				(handler, sender, server) -> GameManager.onPlayerJoin(handler.player));

		ServerPlayerEvents.AFTER_RESPAWN.register(
				(oldPlayer, newPlayer, alive) -> GameManager.onRespawn(newPlayer));

		// Runner quebrou um bloco: so conta se for o alvo OP e o jogador estiver jogando
		// de verdade (nem criativo, nem espectador).
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
				return;
			}

			OpTarget target = OpDropManager.current();

			if (target == null || target.isMob() || !target.matchesBlock(state)) {
				return;
			}

			if (!isEligibleRunner(serverPlayer)) {
				return;
			}

			OpDropManager.award(serverPlayer, serverLevel,
					new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			if (entity.level().getServer() == null) {
				return;
			}

			if (entity instanceof EnderDragon dragon) {
				GameManager.onDragonKilled(dragon.level().getServer());
				return;
			}

			if (entity instanceof ServerPlayer player) {
				if (GameState.get().isRunner(player.getUUID())) {
					GameManager.onRunnerDeath(player);
				}

				return;
			}

			handleMobKill(entity);
		});

		// Rede de seguranca: nenhuma bussola/armadura do mod sobrevive no chao.
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof ItemEntity item && ModItems.isProtected(item.getItem())) {
				item.discard();
			}
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			GameState.get().clearAll();
			TrackerCompass.reset();
			OpDropManager.reset();
		});

		LOGGER.info("OP Drop Manhunt carregado. Use /manhunt join e /manhunt start.");
	}

	/**
	 * Mob morreu: o credito de kill cobre flecha, tridente e dano indireto, entao vale
	 * para qualquer coisa que o Runner tenha causado.
	 */
	private static void handleMobKill(LivingEntity entity) {
		OpTarget target = OpDropManager.current();

		if (target == null || !target.isMob() || !target.matchesEntity(entity)) {
			return;
		}

		if (!(entity.getKillCredit() instanceof ServerPlayer killer) || !isEligibleRunner(killer)) {
			return;
		}

		if (!(entity.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		OpDropManager.award(killer, serverLevel, entity.position());
	}

	/** Runner vivo, em modo de jogo normal e com a partida rolando. */
	private static boolean isEligibleRunner(ServerPlayer player) {
		GameState state = GameState.get();

		return state.isRunning()
				&& state.isRunner(player.getUUID())
				&& !state.isEliminated(player.getUUID())
				&& !player.isCreative()
				&& !player.isSpectator();
	}
}
