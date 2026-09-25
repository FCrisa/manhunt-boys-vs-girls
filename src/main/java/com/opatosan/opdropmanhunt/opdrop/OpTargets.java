package com.opatosan.opdropmanhunt.opdrop;

import java.util.List;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * Pool de alvos OP, separada por dimensao e dificuldade.
 *
 * <p>Dificuldade: 1 facil, 2 medio, 3 dificil. Os alvos do End usam
 * {@link OpTarget#ANY_DIFFICULTY} porque valem em qualquer tier.</p>
 *
 * <p>Para adicionar um alvo novo, basta incluir uma linha aqui.</p>
 */
public final class OpTargets {
	private OpTargets() {
	}

	public static final List<OpTarget> ALL = List.of(
			// ------------------------------------------------------- OVERWORLD

			// Dificuldade 1
			OpTarget.tag("Log", 1, Level.OVERWORLD, BlockTags.LOGS),
			OpTarget.blocks("Dirt", 1, Level.OVERWORLD, Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT),
			OpTarget.blocks("Stone", 1, Level.OVERWORLD, Blocks.STONE),
			OpTarget.blocks("Sand", 1, Level.OVERWORLD, Blocks.SAND),
			OpTarget.blocks("Gravel", 1, Level.OVERWORLD, Blocks.GRAVEL),
			OpTarget.blocks("Clay", 1, Level.OVERWORLD, Blocks.CLAY),
			OpTarget.tag("Leaves", 1, Level.OVERWORLD, BlockTags.LEAVES),
			OpTarget.blocks("Sugar Cane", 1, Level.OVERWORLD, Blocks.SUGAR_CANE),
			OpTarget.blocks("Coal Ore", 1, Level.OVERWORLD, Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE),
			OpTarget.mob("Cow", 1, Level.OVERWORLD, EntityType.COW),
			OpTarget.mob("Pig", 1, Level.OVERWORLD, EntityType.PIG),
			OpTarget.mob("Sheep", 1, Level.OVERWORLD, EntityType.SHEEP),
			OpTarget.mob("Chicken", 1, Level.OVERWORLD, EntityType.CHICKEN),

			// Dificuldade 2
			OpTarget.blocks("Copper Ore", 2, Level.OVERWORLD, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE),
			OpTarget.blocks("Iron Ore", 2, Level.OVERWORLD, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE),
			OpTarget.blocks("Pumpkin", 2, Level.OVERWORLD, Blocks.PUMPKIN),
			OpTarget.blocks("Melon", 2, Level.OVERWORLD, Blocks.MELON),
			OpTarget.blocks("Deepslate", 2, Level.OVERWORLD, Blocks.DEEPSLATE),
			OpTarget.mob("Zombie", 2, Level.OVERWORLD, EntityType.ZOMBIE),
			OpTarget.mob("Skeleton", 2, Level.OVERWORLD, EntityType.SKELETON),
			OpTarget.mob("Spider", 2, Level.OVERWORLD, EntityType.SPIDER),
			OpTarget.mob("Creeper", 2, Level.OVERWORLD, EntityType.CREEPER),
			OpTarget.mob("Drowned", 2, Level.OVERWORLD, EntityType.DROWNED),

			// Dificuldade 3
			OpTarget.blocks("Gold Ore", 3, Level.OVERWORLD, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE),
			OpTarget.blocks("Redstone Ore", 3, Level.OVERWORLD, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE),
			OpTarget.blocks("Lapis Ore", 3, Level.OVERWORLD, Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE),
			OpTarget.blocks("Diamond Ore", 3, Level.OVERWORLD, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE),
			OpTarget.mob("Enderman", 3, Level.OVERWORLD, EntityType.ENDERMAN),
			OpTarget.mob("Witch", 3, Level.OVERWORLD, EntityType.WITCH),
			OpTarget.mob("Slime", 3, Level.OVERWORLD, EntityType.SLIME),

			// ---------------------------------------------------------- NETHER

			// Dificuldade 1
			OpTarget.blocks("Netherrack", 1, Level.NETHER, Blocks.NETHERRACK),
			OpTarget.blocks("Soul Sand", 1, Level.NETHER, Blocks.SOUL_SAND),
			OpTarget.blocks("Nether Quartz Ore", 1, Level.NETHER, Blocks.NETHER_QUARTZ_ORE),
			OpTarget.blocks("Nether Gold Ore", 1, Level.NETHER, Blocks.NETHER_GOLD_ORE),
			OpTarget.blocks("Crimson Stem", 1, Level.NETHER, Blocks.CRIMSON_STEM),
			OpTarget.blocks("Warped Stem", 1, Level.NETHER, Blocks.WARPED_STEM),
			OpTarget.mob("Zombified Piglin", 1, Level.NETHER, EntityType.ZOMBIFIED_PIGLIN),

			// Dificuldade 2
			OpTarget.blocks("Glowstone", 2, Level.NETHER, Blocks.GLOWSTONE),
			OpTarget.blocks("Magma Block", 2, Level.NETHER, Blocks.MAGMA_BLOCK),
			OpTarget.blocks("Basalt", 2, Level.NETHER, Blocks.BASALT),
			OpTarget.blocks("Blackstone", 2, Level.NETHER, Blocks.BLACKSTONE),
			OpTarget.mob("Piglin", 2, Level.NETHER, EntityType.PIGLIN),
			OpTarget.mob("Magma Cube", 2, Level.NETHER, EntityType.MAGMA_CUBE),
			OpTarget.mob("Strider", 2, Level.NETHER, EntityType.STRIDER),

			// Dificuldade 3
			OpTarget.blocks("Ancient Debris", 3, Level.NETHER, Blocks.ANCIENT_DEBRIS),
			OpTarget.mob("Blaze", 3, Level.NETHER, EntityType.BLAZE),
			OpTarget.mob("Wither Skeleton", 3, Level.NETHER, EntityType.WITHER_SKELETON),
			OpTarget.mob("Ghast", 3, Level.NETHER, EntityType.GHAST),
			OpTarget.mob("Hoglin", 3, Level.NETHER, EntityType.HOGLIN),

			// ------------------------------------------------------------- END

			OpTarget.blocks("End Stone", OpTarget.ANY_DIFFICULTY, Level.END, Blocks.END_STONE),
			OpTarget.blocks("Obsidian", OpTarget.ANY_DIFFICULTY, Level.END, Blocks.OBSIDIAN),
			OpTarget.mob("Enderman", OpTarget.ANY_DIFFICULTY, Level.END, EntityType.ENDERMAN)
	);

	/**
	 * Dificuldades liberadas por tier de drop:
	 * tiers 1-2 -> facil, tiers 3-4 -> facil/medio, tiers 5-6 -> medio/dificil.
	 */
	public static List<Integer> difficultiesForTier(int dropTier) {
		if (dropTier <= 2) {
			return List.of(1);
		}

		if (dropTier <= 4) {
			return List.of(1, 2);
		}

		return List.of(2, 3);
	}
}
