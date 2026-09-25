package com.opatosan.opdropmanhunt.opdrop;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import com.opatosan.opdropmanhunt.item.Enchanting;

/**
 * Tabelas de loot dos OP drops, um bloco por tier.
 *
 * <p>Esta e a classe para editar quando quiser mexer no que cai. Cada tier e uma lista
 * de {@link Entry}; cada entrada tem um peso e produz um ou mais itens.</p>
 *
 * <p>Regras de encantamento:</p>
 * <ul>
 *   <li>{@link #upTo(ResourceKey, int)} -- tem {@code upToEnchantChance} (padrao 70%) de
 *       aparecer, com nivel aleatorio de 1 ate o maximo informado.</li>
 *   <li>{@link #fixed(ResourceKey, int)} -- sempre aparece, exatamente nesse nivel.</li>
 * </ul>
 *
 * <p>Niveis acima do maximo do vanilla (Protection V) e combinacoes incompativeis
 * (Density + Breach) sao aplicados mesmo assim.</p>
 */
public final class OpLootTables {
	private OpLootTables() {
	}

	// ------------------------------------------------------------------ pesos
	// Equipamento cai mais; comida e minerio cai menos.

	private static final int W_GEAR = 10;
	private static final int W_UTILITY = 8;
	private static final int W_APPLE = 6;
	private static final int W_FOOD = 4;
	private static final int W_ORE = 4;

	/** Peso para o item "assinatura" de um tier, que deve aparecer na maioria dos drops. */
	private static final int W_SIGNATURE = 25;

	// ------------------------------------------------------------------- sets

	private static final Item[] LEATHER_SET = {
			Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS
	};
	private static final Item[] COPPER_SET = {
			Items.COPPER_HELMET, Items.COPPER_CHESTPLATE, Items.COPPER_LEGGINGS, Items.COPPER_BOOTS
	};
	private static final Item[] IRON_SET = {
			Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS
	};
	private static final Item[] DIAMOND_SET = {
			Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS
	};
	private static final Item[] NETHERITE_SET = {
			Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS
	};

	/** Comida cozida sorteada nas entradas de comida. */
	private static final Item[] COOKED_FOOD = {
			Items.COOKED_BEEF, Items.COOKED_CHICKEN, Items.BAKED_POTATO, Items.COOKED_COD, Items.COOKED_SALMON
	};

	// =====================================================================
	//  TABELAS
	// =====================================================================

	private static final List<Entry> TIER_1 = List.of(
			armor(W_GEAR, LEATHER_SET, upTo(Enchantments.PROTECTION, 1)),
			gear(W_GEAR, Items.STONE_SPEAR, upTo(Enchantments.SHARPNESS, 1)),
			gear(W_GEAR, Items.STONE_SWORD, upTo(Enchantments.SHARPNESS, 1)),
			gear(W_GEAR, Items.STONE_PICKAXE, upTo(Enchantments.EFFICIENCY, 1)),
			stack(W_APPLE, Items.GOLDEN_APPLE, 1, 3),
			food(W_FOOD, 2, 5),
			stack(W_ORE, Items.COPPER_INGOT, 4, 10),
			stack(W_ORE, Items.OAK_LOG, 8, 16)
	);

	private static final List<Entry> TIER_2 = List.of(
			armor(W_GEAR, LEATHER_SET, upTo(Enchantments.PROTECTION, 1), upTo(Enchantments.UNBREAKING, 2)),
			gear(W_GEAR, Items.STONE_SWORD, upTo(Enchantments.SHARPNESS, 2), upTo(Enchantments.KNOCKBACK, 1)),
			stack(W_APPLE, Items.GOLDEN_APPLE, 1, 3),
			food(W_FOOD, 5, 10),
			stack(W_ORE, Items.COPPER_INGOT, 6, 12),
			stack(W_UTILITY, Items.SHIELD, 1, 1),
			stack(W_UTILITY, Items.WATER_BUCKET, 1, 1),
			// Uma entrada, dois itens.
			combo(W_GEAR, ctx -> List.of(
					new ItemStack(Items.BOW),
					new ItemStack(Items.ARROW, ctx.count(8, 16))))
	);

	private static final List<Entry> TIER_3 = List.of(
			armor(W_GEAR, COPPER_SET, upTo(Enchantments.PROTECTION, 2), upTo(Enchantments.UNBREAKING, 3)),
			gear(W_GEAR, Items.COPPER_SPEAR, upTo(Enchantments.SHARPNESS, 2)),
			stack(W_APPLE, Items.GOLDEN_APPLE, 3, 5),
			food(W_FOOD, 10, 18),
			stack(W_ORE, Items.GOLD_INGOT, 10, 18),
			stack(W_UTILITY, Items.ENDER_PEARL, 2, 4),
			stack(W_UTILITY, Items.WATER_BUCKET, 1, 1),
			// Kit de portal.
			combo(W_UTILITY, ctx -> List.of(
					new ItemStack(Items.OBSIDIAN, 10),
					new ItemStack(Items.FLINT_AND_STEEL)))
	);

	private static final List<Entry> TIER_4 = List.of(
			armor(W_GEAR, IRON_SET, upTo(Enchantments.PROTECTION, 3), upTo(Enchantments.UNBREAKING, 2)),
			gear(W_GEAR, Items.IRON_SWORD,
					upTo(Enchantments.SHARPNESS, 3), upTo(Enchantments.FIRE_ASPECT, 2), upTo(Enchantments.KNOCKBACK, 1)),
			gear(W_GEAR, Items.SHIELD, upTo(Enchantments.UNBREAKING, 2)),
			gear(W_GEAR, Items.TRIDENT,
					fixed(Enchantments.RIPTIDE, 3), fixed(Enchantments.IMPALING, 4), fixed(Enchantments.UNBREAKING, 3)),
			gear(W_GEAR, Items.IRON_PICKAXE,
					fixed(Enchantments.EFFICIENCY, 3), fixed(Enchantments.UNBREAKING, 1), fixed(Enchantments.FORTUNE, 1)),
			gear(W_GEAR, Items.IRON_AXE,
					fixed(Enchantments.SHARPNESS, 3), fixed(Enchantments.EFFICIENCY, 3), fixed(Enchantments.UNBREAKING, 2)),
			stack(W_APPLE, Items.ENCHANTED_GOLDEN_APPLE, 3, 5),
			food(W_FOOD, 15, 23),
			stack(W_ORE, Items.IRON_INGOT, 10, 15),
			stack(W_UTILITY, Items.ENDER_PEARL, 4, 8),
			// Splash Potion of Healing II.
			combo(W_UTILITY, ctx -> List.of(
					potion(Items.SPLASH_POTION, Potions.STRONG_HEALING, ctx.count(2, 3))))
	);

	private static final List<Entry> TIER_5 = List.of(
			armor(W_GEAR, DIAMOND_SET, upTo(Enchantments.PROTECTION, 4), upTo(Enchantments.UNBREAKING, 3)),
			gear(W_GEAR, Items.DIAMOND_SWORD,
					fixed(Enchantments.SHARPNESS, 3), fixed(Enchantments.FIRE_ASPECT, 2), fixed(Enchantments.KNOCKBACK, 1)),
			gear(W_GEAR, Items.DIAMOND_PICKAXE,
					fixed(Enchantments.EFFICIENCY, 4), fixed(Enchantments.UNBREAKING, 2), fixed(Enchantments.FORTUNE, 2)),
			gear(W_GEAR, Items.DIAMOND_AXE,
					fixed(Enchantments.SHARPNESS, 4), fixed(Enchantments.EFFICIENCY, 4), fixed(Enchantments.UNBREAKING, 2)),
			// Mace + municao de wind charge. Density + Breach sao incompativeis no vanilla,
			// mas aqui os dois entram no item do mesmo jeito.
			combo(W_GEAR, ctx -> List.of(
					ctx.enchanted(Items.MACE,
							fixed(Enchantments.DENSITY, 5), fixed(Enchantments.BREACH, 4), fixed(Enchantments.WIND_BURST, 3)),
					new ItemStack(Items.WIND_CHARGE, ctx.count(26, 38)))),
			combo(W_GEAR, ctx -> List.of(
					ctx.enchanted(Items.BOW,
							fixed(Enchantments.INFINITY, 1), fixed(Enchantments.POWER, 4),
							fixed(Enchantments.FLAME, 2), fixed(Enchantments.UNBREAKING, 3)),
					new ItemStack(Items.ARROW, 16))),
			stack(W_APPLE, Items.ENCHANTED_GOLDEN_APPLE, 5, 10),
			food(W_FOOD, 15, 23),
			stack(W_ORE, Items.DIAMOND, 10, 15),
			stack(W_UTILITY, Items.BLAZE_ROD, 4, 8),
			stack(W_UTILITY, Items.ENDER_EYE, 6, 12),
			// Fire Resistance 8:00 (versao "long").
			combo(W_UTILITY, ctx -> List.of(
					potion(Items.POTION, Potions.LONG_FIRE_RESISTANCE, 2)))
	);

	private static final List<Entry> TIER_6 = List.of(
			armor(W_GEAR, NETHERITE_SET, upTo(Enchantments.PROTECTION, 5), upTo(Enchantments.UNBREAKING, 3)),
			// Item assinatura do tier final: peso alto de proposito.
			gear(W_SIGNATURE, Items.NETHERITE_SWORD,
					fixed(Enchantments.SHARPNESS, 4), fixed(Enchantments.FIRE_ASPECT, 2), fixed(Enchantments.KNOCKBACK, 2)),
			gear(W_GEAR, Items.NETHERITE_PICKAXE,
					fixed(Enchantments.EFFICIENCY, 5), fixed(Enchantments.UNBREAKING, 3), fixed(Enchantments.FORTUNE, 3)),
			gear(W_GEAR, Items.NETHERITE_AXE,
					fixed(Enchantments.SHARPNESS, 5), fixed(Enchantments.EFFICIENCY, 5), fixed(Enchantments.UNBREAKING, 3)),
			// Elytra + foguetes de duracao 3.
			combo(W_GEAR, ctx -> List.of(
					ctx.enchanted(Items.ELYTRA, fixed(Enchantments.UNBREAKING, 3), fixed(Enchantments.MENDING, 1)),
					firework(ctx.count(32, 46), 3))),
			stack(W_APPLE, Items.ENCHANTED_GOLDEN_APPLE, 8, 12),
			food(W_FOOD, 24, 32),
			stack(W_UTILITY, Items.TOTEM_OF_UNDYING, 1, 1),
			stack(W_UTILITY, Items.ENDER_EYE, 12, 12),
			stack(W_UTILITY, Items.ENDER_PEARL, 8, 16)
	);

	private static final List<List<Entry>> TIERS = List.of(TIER_1, TIER_2, TIER_3, TIER_4, TIER_5, TIER_6);

	/** Pool do tier (1 a 6). */
	public static List<Entry> forTier(int tier) {
		return TIERS.get(Math.clamp(tier, 1, TIERS.size()) - 1);
	}

	// =====================================================================
	//  ESTRUTURA
	// =====================================================================

	/** Uma entrada da pool: peso + o que ela produz. */
	public record Entry(int weight, Factory factory) {
	}

	@FunctionalInterface
	public interface Factory {
		List<ItemStack> create(RollContext ctx);
	}

	/** Um encantamento pedido por uma entrada. */
	public record EnchantSpec(ResourceKey<Enchantment> key, int level, boolean upTo) {
	}

	/** Nivel aleatorio de 1 ate {@code maxLevel}, com chance de nao vir. */
	private static EnchantSpec upTo(ResourceKey<Enchantment> key, int maxLevel) {
		return new EnchantSpec(key, maxLevel, true);
	}

	/** Sempre vem, exatamente neste nivel. */
	private static EnchantSpec fixed(ResourceKey<Enchantment> key, int level) {
		return new EnchantSpec(key, level, false);
	}

	/** Contexto de um sorteio: random, registry de encantamentos e as chances configuradas. */
	public record RollContext(RandomSource random, HolderLookup.RegistryLookup<Enchantment> enchantments,
			double upToChance, double extraArmorPieceChance) {

		/**
		 * Peças de armadura de um set: uma garantida, e cada peça a mais entra com
		 * {@code extraArmorPieceChance}. Os slots (capacete, peitoral, calça, bota) são
		 * sorteados sem repetir, então no máximo sai o set completo.
		 */
		public List<ItemStack> armorPieces(Item[] set, EnchantSpec... specs) {
			List<Item> slots = new ArrayList<>(List.of(set));
			List<ItemStack> pieces = new ArrayList<>();

			while (true) {
				pieces.add(enchanted(slots.remove(this.random.nextInt(slots.size())), specs));

				if (slots.isEmpty() || this.random.nextDouble() >= this.extraArmorPieceChance) {
					return pieces;
				}
			}
		}

		/** Quantidade aleatoria entre min e max (inclusivo). */
		public int count(int min, int max) {
			return max <= min ? min : min + this.random.nextInt(max - min + 1);
		}

		public <T> T pick(T[] options) {
			return options[this.random.nextInt(options.length)];
		}

		/** Cria o item ja com os encantamentos pedidos. */
		public ItemStack enchanted(Item item, EnchantSpec... specs) {
			ItemStack stack = new ItemStack(item);
			applyEnchants(stack, specs);
			return stack;
		}

		public void applyEnchants(ItemStack stack, EnchantSpec... specs) {
			for (EnchantSpec spec : specs) {
				if (spec.upTo()) {
					if (this.random.nextDouble() >= this.upToChance) {
						continue;
					}

					Enchanting.apply(stack, this.enchantments, spec.key(), 1 + this.random.nextInt(spec.level()));
				} else {
					Enchanting.apply(stack, this.enchantments, spec.key(), spec.level());
				}
			}
		}
	}

	// =====================================================================
	//  ATALHOS PARA MONTAR ENTRADAS
	// =====================================================================

	/** Um item, quantidade fixa ou em faixa, sem encantamento. */
	private static Entry stack(int weight, Item item, int min, int max) {
		return new Entry(weight, ctx -> List.of(new ItemStack(item, ctx.count(min, max))));
	}

	/** Uma peca de equipamento com encantamentos. */
	private static Entry gear(int weight, Item item, EnchantSpec... specs) {
		return new Entry(weight, ctx -> List.of(ctx.enchanted(item, specs)));
	}

	/**
	 * Pecas do set de armadura: uma garantida, com chance de vir mais (ate o set
	 * completo). Veja {@link RollContext#armorPieces}.
	 */
	private static Entry armor(int weight, Item[] set, EnchantSpec... specs) {
		return new Entry(weight, ctx -> ctx.armorPieces(set, specs));
	}

	/** Um tipo aleatorio de comida cozida, na quantidade do tier. */
	private static Entry food(int weight, int min, int max) {
		return new Entry(weight, ctx -> List.of(new ItemStack(ctx.pick(COOKED_FOOD), ctx.count(min, max))));
	}

	/** Entrada livre, para quando uma entrada dropa mais de um item. */
	private static Entry combo(int weight, Factory factory) {
		return new Entry(weight, factory);
	}

	// ------------------------------------------------------------- utilitarios

	private static ItemStack potion(Item item, Holder<Potion> potion, int count) {
		ItemStack stack = new ItemStack(item, count);
		stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
		return stack;
	}

	private static ItemStack firework(int count, int flightDuration) {
		ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET, count);
		stack.set(DataComponents.FIREWORKS, new Fireworks(flightDuration, List.of()));
		return stack;
	}

	// =====================================================================
	//  SORTEIO
	// =====================================================================

	/**
	 * Sorteia {@code entryCount} entradas diferentes da pool do tier, por peso,
	 * e devolve todos os itens gerados.
	 */
	public static List<ItemStack> roll(int tier, int entryCount, RollContext ctx) {
		List<Entry> tierPool = forTier(tier);
		List<Entry> remaining = new ArrayList<>(tierPool);
		List<ItemStack> result = new ArrayList<>();

		for (int i = 0; i < entryCount; i++) {
			// Nos tiers altos o numero de entradas pedido pode passar do tamanho da
			// pool. Em vez de cortar o drop, reabastece: as entradas so comecam a
			// repetir depois que a pool inteira ja saiu uma vez.
			if (remaining.isEmpty()) {
				remaining.addAll(tierPool);
			}

			Entry entry = pickWeighted(remaining, ctx.random());

			if (entry == null) {
				break;
			}

			remaining.remove(entry);
			result.addAll(entry.factory().create(ctx));
		}

		return result;
	}

	private static Entry pickWeighted(List<Entry> pool, RandomSource random) {
		int total = 0;

		for (Entry entry : pool) {
			total += entry.weight();
		}

		if (total <= 0) {
			return null;
		}

		int roll = random.nextInt(total);

		for (Entry entry : pool) {
			roll -= entry.weight();

			if (roll < 0) {
				return entry;
			}
		}

		return pool.get(pool.size() - 1);
	}
}
