package local.badlioncosmeticport.mixin;

import local.badlioncosmeticport.MeshCosmetics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class PlayerSkinMixin {
    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void cosmeticPort$localCape(CallbackInfoReturnable<SkinTextures> callback) {
        if ((Object) this != MinecraftClient.getInstance().player) return;
        if (MeshCosmetics.active(MeshCosmetics.Category.CAPE) == null) return;
        SkinTextures original = callback.getReturnValue();
        callback.setReturnValue(new SkinTextures(original.texture(), original.textureUrl(), null,
                original.elytraTexture(), original.model(), original.secure()));
    }
}
