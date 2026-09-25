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
import com.opatosan.opdropmanhunt.game.GameState;

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

	/**
	 * Quantas entradas cada OP drop sorteia, por tier (indice 0 = tier 1).
	 *
	 * <p>Sobe junto com o tier: 6 no tier 1 ate 12 no tier 6. Se o valor passar do
	 * tamanho da pool do tier, a pool e reabastecida e algumas entradas repetem no
	 * mesmo drop -- veja {@code OpLootTables.roll}.</p>
	 */
	public int[] entriesPerDropByTier = {6, 7, 8, 10, 11, 12};

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
					if (loaded.sanitize()) {
						loaded.save();
					}

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

	/** Mantem os valores dentro de faixas utilizaveis. Retorna true se precisou corrigir. */
	private boolean sanitize() {
		this.upgradeIntervalSeconds = Math.max(1, this.upgradeIntervalSeconds);
		this.letterRevealIntervalSeconds = Math.max(1, this.letterRevealIntervalSeconds);
		this.upToEnchantChance = Math.clamp(this.upToEnchantChance, 0.0, 1.0);

		// Config antigo (de antes da escala por tier) nao tem a lista: usa o padrao e
		// regrava o arquivo, para o campo novo aparecer para edicao.
		if (this.entriesPerDropByTier == null || this.entriesPerDropByTier.length != GameState.MAX_DROP_TIER) {
			this.entriesPerDropByTier = new int[] {6, 7, 8, 10, 11, 12};
			return true;
		}

		for (int i = 0; i < this.entriesPerDropByTier.length; i++) {
			this.entriesPerDropByTier[i] = Math.max(1, this.entriesPerDropByTier[i]);
		}

		return false;
	}

	/** Entradas sorteadas no tier informado (1 a 6). */
	public int entriesForTier(int tier) {
		int index = Math.clamp(tier - 1, 0, this.entriesPerDropByTier.length - 1);
		return this.entriesPerDropByTier[index];
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
