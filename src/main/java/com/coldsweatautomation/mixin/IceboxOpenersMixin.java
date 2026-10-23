package com.coldsweatautomation.mixin;

import com.coldsweatautomation.container.BigIceboxContainer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps the icebox lid animation and open/close sounds working.
 *
 * <p>Cold Sweat's icebox decides whether an open container is "its own" by checking
 * {@code player.containerMenu instanceof IceboxContainer}. The expanded icebox uses its own menu
 * type (NeoForge refuses duplicate screen registrations), so without this patch the lid would
 * never animate.</p>
 */
@Mixin(targets = "com.momosoftworks.coldsweat.common.blockentity.IceboxBlockEntity$1")
public abstract class IceboxOpenersMixin
{
    @Inject(method = "isOwnContainer", at = @At("HEAD"), cancellable = true)
    private void csa$isOwnContainer(Player player, CallbackInfoReturnable<Boolean> cir)
    {
        if (player.containerMenu instanceof BigIceboxContainer)
        {   cir.setReturnValue(true);
        }
    }
}
