package com.opatosan.opdropmanhunt.game;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/** Helpers de titulo, action bar, chat e som. Tudo enviado pelo servidor. */
public final class Broadcast {
	private Broadcast() {
	}

	public static void titleToAll(MinecraftServer server, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			title(player, title, subtitle, fadeIn, stay, fadeOut);
		}
	}

	public static void title(ServerPlayer player, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
		player.connection.send(new ClientboundClearTitlesPacket(false));
		player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
		player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
		player.connection.send(new ClientboundSetTitleTextPacket(title));
	}

	public static void actionBar(ServerPlayer player, Component message) {
		player.connection.send(new ClientboundSetActionBarTextPacket(message));
	}

	public static void chatToAll(MinecraftServer server, Component message) {
		server.getPlayerList().broadcastSystemMessage(message, false);
	}

	public static void sound(ServerPlayer player, Holder<SoundEvent> sound, float volume, float pitch) {
		player.connection.send(new ClientboundSoundPacket(
				sound, SoundSource.MASTER, player.getX(), player.getY(), player.getZ(), volume, pitch, 0L));
	}

	public static void sound(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
		sound(player, Holder.direct(sound), volume, pitch);
	}

	// Duas sobrecargas de proposito: no 26.2 algumas constantes de SoundEvents sao
	// Holder<SoundEvent> e outras sao SoundEvent puro.
	public static void soundToAll(MinecraftServer server, Holder<SoundEvent> sound, float volume, float pitch) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			sound(player, sound, volume, pitch);
		}
	}

	public static void soundToAll(MinecraftServer server, SoundEvent sound, float volume, float pitch) {
		soundToAll(server, Holder.direct(sound), volume, pitch);
	}
}
