package local.badlioncosmeticport.render;

import local.badlioncosmeticport.MeshCosmetics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/** Replace only the local player's held shield appearance, retaining vanilla item poses. */
public final class ShieldReplacement {
    public static long draws;
    private ShieldReplacement() {}
    public static boolean holdingShield(LivingEntity player) {
        return player.getMainHandStack().isOf(Items.SHIELD) || player.getOffHandStack().isOf(Items.SHIELD);
    }
    public static boolean render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices,
                                 VertexConsumerProvider consumers, int light) {
        var player = MinecraftClient.getInstance().player;
        var cosmetic = MeshCosmetics.active(MeshCosmetics.Category.SHIELD);
        if (player == null || cosmetic == null || !MeshCosmetics.shieldOnArm || !stack.isOf(Items.SHIELD)
                || stack != player.getMainHandStack() && stack != player.getOffHandStack()
                || mode == ModelTransformationMode.GUI || mode == ModelTransformationMode.GROUND
                || mode == ModelTransformationMode.FIXED || mode == ModelTransformationMode.NONE) return false;
        // Third person uses the recovered player-arm attachment rather than vanilla's
        // held-item basis. Cancel the vanilla plate here; the player layer draws it.
        if (mode == ModelTransformationMode.THIRD_PERSON_LEFT_HAND
                || mode == ModelTransformationMode.THIRD_PERSON_RIGHT_HAND) {
            draws++;
            return true;
        }
        matrices.push();
        try {
            var vertices = consumers.getBuffer(CosmeticRenderLayers.layer(cosmetic.pack, cosmetic.texture()));
            // Vanilla's builtin shield flips Y/Z; native armScale is in exported model units.
            matrices.scale(1, -1, -1); matrices.translate(0, 0, -.09375);
            // OBJ models use upright Y; vanilla's plate coordinates otherwise turn their art upside down.
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(180));
            float scale = .009f;
            var metadata = cosmetic.pack.modelMetadata;
            if (metadata != null && metadata.has("shieldModelData")) {
                var data = metadata.getAsJsonObject("shieldModelData");
                if (data.has("armScale") && data.get("armScale").getAsFloat() > 0) scale = data.get("armScale").getAsFloat();
            }
            matrices.scale(scale, scale, scale);
            // Exported OBJ art faces the opposite direction to vanilla's builtin plate.
            // Native first/third-person rotation metadata takes precedence over the default.
            var data = metadata == null || !metadata.has("shieldModelData") ? null : metadata.getAsJsonObject("shieldModelData");
            String view = mode.isFirstPerson() ? "firstPerson" : "thirdPerson";
            var placement = data == null || !data.has(view) ? null : data.getAsJsonObject(view);
            var rotation = placement == null || !placement.has("rotate") ? null : placement.getAsJsonObject("rotate");
            if (rotation == null) matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(180));
            else {
                var axis = new org.joml.Vector3f(rotation.get("x").getAsFloat(), rotation.get("y").getAsFloat(), rotation.get("z").getAsFloat());
                if (axis.lengthSquared() > 0) matrices.multiply(new org.joml.Quaternionf().rotationAxis(
                        (float)Math.toRadians(rotation.get("w").getAsFloat()), axis.normalize()));
            }
            // The third-person item transform turns a vanilla shield's vertical axis into depth.
            // Restore an upright custom model, with mirrored compensation for the left hand.
            if (!mode.isFirstPerson())
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(
                        mode == ModelTransformationMode.THIRD_PERSON_LEFT_HAND ? -90 : 90));
            if (data != null && data.has("inverted") && data.get("inverted").getAsBoolean())
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(180));
            if (cosmetic.obj != null) {
                var bounds = cosmetic.obj.bounds();
                matrices.translate(-(bounds.minX() + bounds.maxX()) / 2, -(bounds.minY() + bounds.maxY()) / 2,
                        -(bounds.minZ() + bounds.maxZ()) / 2);
            }
            CosmeticFeatureRenderer.draw(matrices, vertices, light, cosmetic, cosmetic.model == null ? null : cosmetic.model.restPose());
            draws++; cosmetic.rendered++;
            return true;
        } catch (Exception failure) { MeshCosmetics.disable(MeshCosmetics.Category.SHIELD, failure); return false; }
        finally { matrices.pop(); }
    }
}
