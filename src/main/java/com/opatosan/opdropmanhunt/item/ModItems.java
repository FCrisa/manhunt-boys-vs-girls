package com.opatosan.opdropmanhunt.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Marcacao de itens do mod via componente custom_data.
 *
 * <p>Itens marcados ("protegidos") nao podem ser dropados com Q, arrastados para fora
 * do inventario nem dropados na morte -- veja {@code ServerPlayerDropMixin}.</p>
 */
public final class ModItems {
	/** Bussola rastreadora dos Hunters. */
	public static final String TRACKER_TAG = "opdropmanhunt:tracker";

	/** Pecas de armadura entregues pelo upgrade dos Hunters. */
	public static final String HUNTER_ARMOR_TAG = "opdropmanhunt:hunter_armor";

	private ModItems() {
	}

	public static void tag(ItemStack stack, String tag) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> nbt.putBoolean(tag, true));
	}

	public static boolean hasTag(ItemStack stack, String tag) {
		if (stack.isEmpty()) {
			return false;
		}

		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data != null && data.copyTag().getBooleanOr(tag, false);
	}

	public static boolean isTracker(ItemStack stack) {
		return hasTag(stack, TRACKER_TAG);
	}

	public static boolean isHunterArmor(ItemStack stack) {
		return hasTag(stack, HUNTER_ARMOR_TAG);
	}

	/** Itens que nunca podem sair do inventario do Hunter. */
	public static boolean isProtected(ItemStack stack) {
		return isTracker(stack) || isHunterArmor(stack);
	}
}
