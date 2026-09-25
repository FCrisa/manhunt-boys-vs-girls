package com.opatosan.opdropmanhunt.upgrade;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

import com.opatosan.opdropmanhunt.item.Enchanting;
import com.opatosan.opdropmanhunt.item.ModItems;

/**
 * Progressao de armadura dos Hunters, um nivel a cada 10 minutos.
 *
 * <p>0 min nada, 10 couro, 20 cobre, 30 ferro, 40 diamante, 50 netherite,
 * 60 netherite encantada. Depois disso nao ha mais upgrades.</p>
 */
public enum ArmorLevel {
	NONE("Nenhuma", Items.AIR, Items.AIR, Items.AIR, Items.AIR, false),
	LEATHER("Couro", Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS, false),
	COPPER("Cobre", Items.COPPER_HELMET, Items.COPPER_CHESTPLATE, Items.COPPER_LEGGINGS, Items.COPPER_BOOTS, false),
	IRON("Ferro", Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS, false),
	DIAMOND("Diamante", Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS, false),
	NETHERITE("Netherite", Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS, false),
	NETHERITE_ENCHANTED("Netherite encantada", Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE,
			Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS, true);

	public static final ArmorLevel[] ORDER = values();
	public static final int MAX = ORDER.length - 1;

	public static final EquipmentSlot[] ARMOR_SLOTS = {
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};

	private final String displayName;
	private final Item helmet;
	private final Item chestplate;
	private final Item leggings;
	private final Item boots;
	private final boolean enchanted;

	ArmorLevel(String displayName, Item helmet, Item chestplate, Item leggings, Item boots, boolean enchanted) {
		this.displayName = displayName;
		this.helmet = helmet;
		this.chestplate = chestplate;
		this.leggings = leggings;
		this.boots = boots;
		this.enchanted = enchanted;
	}

	public static ArmorLevel byIndex(int index) {
		return ORDER[Math.clamp(index, 0, MAX)];
	}

	public String displayName() {
		return this.displayName;
	}

	public Item itemFor(EquipmentSlot slot) {
		return switch (slot) {
			case HEAD -> this.helmet;
			case CHEST -> this.chestplate;
			case LEGS -> this.leggings;
			case FEET -> this.boots;
			default -> Items.AIR;
		};
	}

	/** Monta a peca ja marcada como armadura de Hunter (nao dropa). */
	public ItemStack build(Level level, EquipmentSlot slot) {
		Item item = itemFor(slot);

		if (item == Items.AIR) {
			return ItemStack.EMPTY;
		}

		ItemStack stack = new ItemStack(item);

		if (this.enchanted) {
			HolderLookup.RegistryLookup<Enchantment> lookup = Enchanting.lookup(level);
			Enchanting.apply(stack, lookup, Enchantments.PROTECTION, 4);
			Enchanting.apply(stack, lookup, Enchantments.UNBREAKING, 3);

			if (slot == EquipmentSlot.FEET) {
				Enchanting.apply(stack, lookup, Enchantments.FEATHER_FALLING, 4);
			}
		}

		ModItems.tag(stack, ModItems.HUNTER_ARMOR_TAG);
		return stack;
	}
}
