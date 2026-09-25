package com.opatosan.opdropmanhunt.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import com.opatosan.opdropmanhunt.item.ModItems;

/**
 * Impede que a bussola rastreadora e a armadura dos Hunters saiam do inventario.
 *
 * <p>Cobre os tres casos de uma vez, porque todos passam por
 * {@code ServerPlayer.drop(...)}: a tecla Q, arrastar o item para fora do inventario
 * e o drop de morte (via {@code Inventory.dropAll()}).</p>
 */
@Mixin(ServerPlayer.class)
public class ServerPlayerDropMixin {
	@Inject(
			method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
			at = @At("HEAD"),
			cancellable = true
	)
	private void opdropmanhunt$blockProtectedDrops(ItemStack stack, boolean dropAround, boolean includeThrower,
			CallbackInfoReturnable<ItemEntity> cir) {
		if (!ModItems.isProtected(stack)) {
			return;
		}

		ServerPlayer player = (ServerPlayer) (Object) this;
		cir.setReturnValue(null);

		// Vivo: devolve para o inventario. Morto: o item simplesmente some e volta no respawn.
		if (!player.isDeadOrDying()) {
			player.getInventory().add(stack);
		}
	}
}
