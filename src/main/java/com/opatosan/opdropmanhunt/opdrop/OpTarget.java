package com.opatosan.opdropmanhunt.opdrop;

import java.util.List;
import java.util.Set;

import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Um alvo OP: o bloco ou mob secreto que, quando quebrado/morto por um Runner,
 * libera o OP drop.
 *
 * @param displayName nome em ingles mostrado na action bar (ex.: "Iron Ore")
 * @param difficulty  1 facil, 2 medio, 3 dificil, ou {@link #ANY_DIFFICULTY}
 * @param dimension   dimensao onde o alvo vale
 * @param matcher     o que conta como "conseguir" o alvo
 */
public record OpTarget(String displayName, int difficulty, ResourceKey<Level> dimension, Matcher matcher) {
	/** Alvos do End valem para qualquer tier. */
	public static final int ANY_DIFFICULTY = 0;

	/** O que identifica o alvo no mundo. */
	public sealed interface Matcher {
		/** Um ou mais blocos especificos (ex.: iron_ore + deepslate_iron_ore). */
		record Blocks(Set<Block> blocks) implements Matcher {
		}

		/** Qualquer bloco de uma tag (ex.: qualquer tronco, qualquer folha). */
		record BlockTag(TagKey<Block> tag) implements Matcher {
		}

		/** Um tipo de mob. */
		record Mob(EntityType<?> type) implements Matcher {
		}
	}

	// ------------------------------------------------------------- construtores

	public static OpTarget blocks(String displayName, int difficulty, ResourceKey<Level> dimension, Block... blocks) {
		return new OpTarget(displayName, difficulty, dimension, new Matcher.Blocks(Set.of(blocks)));
	}

	public static OpTarget tag(String displayName, int difficulty, ResourceKey<Level> dimension, TagKey<Block> tag) {
		return new OpTarget(displayName, difficulty, dimension, new Matcher.BlockTag(tag));
	}

	public static OpTarget mob(String displayName, int difficulty, ResourceKey<Level> dimension, EntityType<?> type) {
		return new OpTarget(displayName, difficulty, dimension, new Matcher.Mob(type));
	}

	// ----------------------------------------------------------------- matching

	public boolean isMob() {
		return this.matcher instanceof Matcher.Mob;
	}

	public boolean matchesBlock(BlockState state) {
		return switch (this.matcher) {
			case Matcher.Blocks blocks -> blocks.blocks().stream().anyMatch(state::is);
			case Matcher.BlockTag blockTag -> state.is(blockTag.tag());
			case Matcher.Mob ignored -> false;
		};
	}

	public boolean matchesEntity(Entity entity) {
		return this.matcher instanceof Matcher.Mob mob && entity.getType() == mob.type();
	}

	/** Passa no filtro de dificuldade do tier atual. */
	public boolean allowedBy(List<Integer> difficulties) {
		return this.difficulty == ANY_DIFFICULTY || difficulties.contains(this.difficulty);
	}
}
