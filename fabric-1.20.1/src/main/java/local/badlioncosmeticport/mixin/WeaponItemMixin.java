package local.badlioncosmeticport.mixin;
import local.badlioncosmeticport.render.WeaponReplacement;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemRenderer.class)
public abstract class WeaponItemMixin {
    @Inject(method="renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",at=@At("HEAD"),cancellable=true)
    private void cosmeticPort$weapon(ItemStack stack,ModelTransformationMode mode,boolean left,MatrixStack matrices,
            VertexConsumerProvider consumers,int light,int overlay,BakedModel model,CallbackInfo callback) {
        if(WeaponReplacement.render(stack,mode,left,matrices,consumers,light,overlay,model)) callback.cancel();
    }
}
