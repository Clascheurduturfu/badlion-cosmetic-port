package local.badlioncosmeticport.mixin;

import local.badlioncosmeticport.MeshCosmetics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class PlayerSkinMixin {
    @Inject(method = "getCapeTexture", at = @At("RETURN"), cancellable = true)
    private void cosmeticPort$localCape(CallbackInfoReturnable<Identifier> callback) {
        if ((Object) this != MinecraftClient.getInstance().player) return;
        if (MeshCosmetics.active(MeshCosmetics.Category.CAPE) == null) return;
        callback.setReturnValue(null);
    }
}
