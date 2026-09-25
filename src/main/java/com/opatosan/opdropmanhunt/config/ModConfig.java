package com.opatosan.opdropmanhunt.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

import com.opatosan.opdropmanhunt.OpDropManhunt;

/**
 * Configuracao do mod, em {@code config/opdropmanhunt.json}.
 *
 * <p>O arquivo e criado com os padroes na primeira execucao. Edite e reinicie o
 * servidor (ou o mundo) para aplicar.</p>
 */
public final class ModConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static ModConfig instance;

	/** Intervalo entre upgrades de armadura dos Hunters / tier de drop, em segundos. */
	public int upgradeIntervalSeconds = 600;

	/** Intervalo entre revelacoes de letra do alvo OP para os Runners, em segundos. */
	public int letterRevealIntervalSeconds = 60;

	/** Quantas entradas diferentes cada OP drop sorteia da pool do tier. */
	public int entriesPerDrop = 3;

	/** Chance (0.0 - 1.0) de um encantamento marcado "ate nivel X" aparecer no item. */
	public double upToEnchantChance = 0.7;

	public static ModConfig get() {
		if (instance == null) {
			instance = load();
		}

		return instance;
	}

	/** Recarrega do disco (usado no start da partida, para pegar edicoes recentes). */
	public static ModConfig reload() {
		instance = load();
		return instance;
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("opdropmanhunt.json");
	}

	private static ModConfig load() {
		Path file = path();

		if (Files.exists(file)) {
			try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				ModConfig loaded = GSON.fromJson(reader, ModConfig.class);

				if (loaded != null) {
					loaded.sanitize();
					return loaded;
				}
			} catch (IOException | RuntimeException e) {
				OpDropManhunt.LOGGER.error("Falha ao ler {}, usando os valores padrao.", file, e);
			}
		}

		ModConfig defaults = new ModConfig();
		defaults.save();
		return defaults;
	}

	/** Mantem os valores dentro de faixas utilizaveis. */
	private void sanitize() {
		this.upgradeIntervalSeconds = Math.max(1, this.upgradeIntervalSeconds);
		this.letterRevealIntervalSeconds = Math.max(1, this.letterRevealIntervalSeconds);
		this.entriesPerDrop = Math.max(1, this.entriesPerDrop);
		this.upToEnchantChance = Math.clamp(this.upToEnchantChance, 0.0, 1.0);
	}

	public void save() {
		Path file = path();

		try {
			Files.createDirectories(file.getParent());

			try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			OpDropManhunt.LOGGER.error("Falha ao gravar {}.", file, e);
		}
	}

	public long upgradeIntervalTicks() {
		return this.upgradeIntervalSeconds * 20L;
	}

	public long letterRevealIntervalTicks() {
		return this.letterRevealIntervalSeconds * 20L;
	}
}
