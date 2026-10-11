package local.badlioncosmeticport.mixin;

import local.badlioncosmeticport.render.ShieldReplacement;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BuiltinModelItemRenderer.class)
public abstract class ShieldItemMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void cosmeticPort$heldShield(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices,
                                        VertexConsumerProvider consumers, int light, int overlay, CallbackInfo callback) {
        if (ShieldReplacement.render(stack, mode, matrices, consumers, light)) callback.cancel();
    }
}
