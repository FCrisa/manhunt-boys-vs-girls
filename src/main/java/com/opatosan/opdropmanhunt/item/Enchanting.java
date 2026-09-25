package com.opatosan.opdropmanhunt.item;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

/**
 * Aplicacao de encantamentos ignorando os limites do vanilla.
 *
 * <p>Encantamentos sao data-driven, entao o {@link Enchantment} e buscado no registry
 * do servidor por {@link ResourceKey}. {@link ItemStack#enchant} escreve direto no
 * componente de encantamentos, sem validar nivel maximo nem incompatibilidade -- e por
 * isso que Protection V e Density V + Breach IV na mesma mace funcionam.</p>
 */
public final class Enchanting {
	private Enchanting() {
	}

	/** Busca o registry de encantamentos do mundo. */
	public static HolderLookup.RegistryLookup<Enchantment> lookup(Level level) {
		return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
	}

	public static void apply(ItemStack stack, HolderLookup.RegistryLookup<Enchantment> lookup,
			ResourceKey<Enchantment> key, int level) {
		if (level <= 0) {
			return;
		}

		stack.enchant(lookup.getOrThrow(key), level);
	}
}
