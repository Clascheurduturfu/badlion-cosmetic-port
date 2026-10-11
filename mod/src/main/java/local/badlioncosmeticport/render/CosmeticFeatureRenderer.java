package local.badlioncosmeticport.render;

import com.google.gson.JsonObject;
import local.badlioncosmeticport.MeshCosmetics;
import local.badlioncosmeticport.MeshCosmetics.*;
import local.badlioncosmeticport.model.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

/** Player attachment adapter for the recovered MD5/OBJ renderer. */
public final class CosmeticFeatureRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    private static long waveTime;
    private static long waveFrames;
    public CosmeticFeatureRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) { super(context); }

    @Override public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light,
            AbstractClientPlayerEntity player, float limbAngle, float limbDistance, float tickDelta,
            float animationProgress, float headYaw, float headPitch) {
        if (player != MinecraftClient.getInstance().player || player.isInvisible()) return;
        for (Category category : Category.values()) {
            if (category == Category.SWORD || category == Category.BOW) continue;
            Loaded cosmetic = MeshCosmetics.active(category);
            if (cosmetic == null) continue;
            if (category == Category.SHIELD && !MeshCosmetics.shieldWithoutItem && !ShieldReplacement.holdingShield(player)) continue;
            matrices.push();
            try {
                if (cosmetic.proceduralCloak()) updateProcedural(cosmetic, player);
                attach(matrices, category, cosmetic, player);
                int lighting = bool(cosmetic.pack.catalog, "glow") ? LightmapTextureManager.MAX_LIGHT_COORDINATE : light;
                VertexConsumer vertices = consumers.getBuffer(CosmeticRenderLayers.layer(cosmetic.pack, cosmetic.texture()));
                Md5Model.Pose pose = cosmetic.proceduralCloak() ? null : pose(cosmetic, player, tickDelta);
                if (category == Category.CAPE && !cosmetic.proceduralCloak() && player.isInSneakingPose()) {
                    Md5MotionClock clock = cosmetic.clocks.get(MeshCosmetics.capeMotion.name().toLowerCase(java.util.Locale.ROOT));
                    if (clock != null) rotateX(matrices, clock.crouchAngle(30));
                }
                if (category == Category.WINGS && cosmetic.model == null) {
                    JsonObject wing = object(cosmetic.pack.catalog, "wingData");
                    float flap = number(wing, "flapBase", -40) - (float) Math.cos(cosmetic.seconds() / 2 * Math.PI * 2) * 30;
                    for (int side = 0; side < 2; side++) {
                        matrices.push();
                        matrices.translate(number(wing, "xOffset", 0), number(wing, "yOffset", 0), number(wing, "xOffset", 0));
                        rotateY(matrices, flap); draw(matrices, vertices, lighting, cosmetic, pose); matrices.pop();
                        if (wing == null || !wing.has("wingTranslate") || bool(wing, "wingTranslate")) matrices.translate(2, 0, 0);
                        matrices.scale(-1, 1, 1);
                    }
                } else if (cosmetic.proceduralCloak()) drawProcedural(matrices, vertices, lighting, cosmetic);
                else draw(matrices, vertices, lighting, cosmetic, pose);
                if (category == Category.CAPE) CloakParticles.emit(cosmetic, pose, player, tickDelta);
                if (category == Category.PET) CloakParticles.emitPet(cosmetic, pose, player, tickDelta);
                if (category == Category.WINGS && cosmetic.model != null) {
                    matrices.scale(-1, 1, 1); matrices.translate(-2.5, 0, 0);
                    draw(matrices, vertices, lighting, cosmetic, pose);
                }
                cosmetic.rendered++;
            } catch (Exception failure) { MeshCosmetics.disable(category, failure); }
            finally { matrices.pop(); }
        }
    }

    private Md5Model.Pose pose(Loaded cosmetic, AbstractClientPlayerEntity player, float tickDelta) {
        if (cosmetic.model == null) return null;
        String clip = switch (cosmetic.pack.type) {
            case CAPE -> MeshCosmetics.capeMotion.name().toLowerCase(java.util.Locale.ROOT);
            case PET -> cosmetic.petPose();
            case WINGS -> "flapping";
            default -> player.getVelocity().horizontalLengthSquared() > .01 && cosmetic.animations.containsKey("animation_1") ? "animation_1" : "animation";
        };
        if (cosmetic.pack.type == Category.PET) {
            JsonObject pet = object(cosmetic.pack.catalog, "petData");
            JsonObject cycle = object(pet, "petAnimationData");
            if (cycle != null && cycle.has("animationType") && cycle.get("animationType").getAsString().equals("FRAME_CYCLE")) {
                int perClip = (int) Math.max(1, number(cycle, "framesPerAnimation", 8));
                // aBQ: four 10-second pose groups, each with eight 100ms texture frames.
                int textureFrame = cosmetic.frame();
                clip = "frame-" + textureFrame / perClip;
            }
        }
        Md5Animation animation = cosmetic.animations.get(clip);
        if (animation == null && !cosmetic.animations.isEmpty()) {
            clip = cosmetic.animations.keySet().stream().sorted().findFirst().orElseThrow();
            animation = cosmetic.animations.get(clip);
        }
        if (animation == null) return cosmetic.model.restPose();
        cosmetic.useAnimationModel(clip);
        if (!clip.equals(cosmetic.currentClip)) {
            cosmetic.currentClip = clip; cosmetic.lastTime = Double.NaN;
            cosmetic.restartClock(clip);
        }
        if (cosmetic.pack.type == Category.CAPE) {
            Md5MotionClock clock = cosmetic.clocks.get(clip);
            float elapsed = cosmetic.elapsed(MinecraftClient.getInstance().isPaused());
            // Captured X.a(aBS,gdy,float) supplies interpolated cape-position lag for X/Z
            // and abs(currentY-lastRenderY) for aeE. Yarn's official field mapping confirms these inputs.
            float partial = MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(false);
            double lagX = player.prevCapeX + (player.capeX - player.prevCapeX) * partial - (player.prevX + (player.getX() - player.prevX) * partial);
            double lagZ = player.prevCapeZ + (player.capeZ - player.prevCapeZ) * partial - (player.prevZ + (player.getZ() - player.prevZ) * partial);
            double vertical = Math.abs(player.getY() - player.lastRenderY);
            double energy = lagX * lagX + lagZ * lagZ + vertical * vertical * 2.5;
            boolean moving = energy >= cosmetic.previousEnergy / 2 && energy > (player.isInSneakingPose() ? .03 : .1);
            if (!MinecraftClient.getInstance().isPaused()) clock.advance((float) ((moving ? 1.2 : -1.6) * elapsed / (player.isInSneakingPose() ? 2 : 1)));
            cosmetic.previousEnergy = energy;
            return clock.pose();
        }
        if (cosmetic.pack.type == Category.WINGS && animation.frameCount() >= 80) {
            Md5MotionClock clock = cosmetic.clocks.get(clip);
            float elapsed = cosmetic.elapsed(MinecraftClient.getInstance().isPaused());
            if (!MinecraftClient.getInstance().isPaused()) clock.advance(elapsed);
            // The captured skeletal path has no fall controller. Keep the original clip's
            // open range while grounded; expose its deep fold only while descending.
            if (cosmetic.pack.model.contains("dragon_wings_v3")) {
                cosmetic.wingElapsed += elapsed;
                boolean falling = !player.isOnGround() && player.getVelocity().y < -.1;
                cosmetic.wingFallBlend = Math.max(0, Math.min(1, cosmetic.wingFallBlend + (falling ? 1 : -1) * elapsed * 4));
                float frame = 9 + 9 * (float) Math.cos(cosmetic.wingElapsed * Math.PI * 2 / animation.durationSeconds());
                var idle = WingMotion.withAnimatedChildren(animation, animation.sampleFrame(frame, false), clock.pose());
                return Md5Model.Pose.interpolate(idle, clock.pose(), cosmetic.wingFallBlend);
            }
            return clock.pose();
        }
        Md5MotionClock clock = cosmetic.clocks.get(clip);
        clock.advance(cosmetic.elapsed(MinecraftClient.getInstance().isPaused()));
        return clock.pose();
    }

    private void updateProcedural(Loaded cosmetic, AbstractClientPlayerEntity player) {
        var client = MinecraftClient.getInstance();
        long now = System.nanoTime();
        if (client.isPaused()) waveTime = now;
        else if (now - waveTime > 16_666_666L) { waveTime = now; waveFrames++; }
        float partial = client.getRenderTickCounter().getTickDelta(false);
        double x = player.prevCapeX + (player.capeX - player.prevCapeX) * partial - (player.prevX + (player.getX() - player.prevX) * partial);
        double y = player.prevCapeY + (player.capeY - player.prevCapeY) * partial - (player.prevY + (player.getY() - player.prevY) * partial);
        double z = player.prevCapeZ + (player.capeZ - player.prevCapeZ) * partial - (player.prevZ + (player.getZ() - player.prevZ) * partial);
        float stride = net.minecraft.util.math.MathHelper.lerp(partial, player.prevStrideDistance, player.strideDistance);
        double distance = net.minecraft.util.math.MathHelper.lerp(partial, player.prevHorizontalSpeed, player.horizontalSpeed);
        float yaw = net.minecraft.util.math.MathHelper.lerp(partial, player.prevBodyYaw, player.bodyYaw);
        cosmetic.cloakPhase = waveFrames / 10f;
        cosmetic.cloakMotion = LegacyCloak.motion(x, y, z, yaw, stride, distance,
                LegacyCloak.Shape.valueOf(MeshCosmetics.capeMotion.name()), cosmetic.cloakPhase, player.isInSneakingPose());
    }

    private void drawProcedural(MatrixStack matrix, VertexConsumer out, int light, Loaded cosmetic) {
        var strip = LegacyCloak.strip(LegacyCloak.Shape.valueOf(MeshCosmetics.capeMotion.name()), cosmetic.cloakMotion, cosmetic.cloakPhase);
        for (int i = 2; i < strip.size(); i++) {
            var a = strip.get(i - 2); var b = strip.get(i - 1); var c = strip.get(i);
            if ((i & 1) != 0) { var swap = a; a = b; b = swap; }
            var normal = new org.joml.Vector3f(b.x() - a.x(), b.y() - a.y(), b.z() - a.z())
                    .cross(c.x() - a.x(), c.y() - a.y(), c.z() - a.z());
            if (normal.lengthSquared() < 1e-10f) continue;
            normal.normalize();
            for (var v : new LegacyCloak.Vertex[]{a, b, c, c})
                vertex(out, matrix, light, v.x(), v.y(), v.z(), v.u(), v.v(), normal.x, normal.y, normal.z);
        }
    }

    private void attach(MatrixStack matrix, Category category, Loaded cosmetic, AbstractClientPlayerEntity player) {
        boolean crouch = player.isInSneakingPose(), armored = !player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST).isEmpty();
        JsonObject metadata = cosmetic.pack.modelMetadata;
        JsonObject sizing = object(cosmetic.pack.catalog, "sizingData");
        switch (category) {
            case CAPE -> {
                if (cosmetic.proceduralCloak()) {
                    var motion = cosmetic.cloakMotion;
                    if (crouch) matrix.translate(0, .15, -.0178);
                    scale(matrix, .0625f); matrix.translate(0, 0, 2); sizing(matrix, sizing, false);
                    rotateX(matrix, 6 + Math.min(motion.forward() / 2 + motion.lift(), 90));
                    rotateZ(matrix, motion.sideways() / 2); rotateY(matrix, -motion.sideways() / 2);
                    rotateY(matrix, -90); rotateX(matrix, 180);
                    break;
                }
                // These are Badlion aCA's original model-unit transforms, in their original order.
                matrix.scale(.055f, .055f, .055f); matrix.translate(0, .4, 3);
                if (crouch) rotateX(matrix, 30);
                matrix.translate(0, crouch ? 1.5 : 0, .15 + (armored ? .9 : 0) + (crouch ? -.6 : 0));
                rotateX(matrix, 90); sizing(matrix, sizing, false);
            }
            case HAT, BANDANA -> {
                getContextModel().head.rotate(matrix);
                if (category == Category.HAT) matrix.translate(0, -MeshCosmetics.hatHeight / 16f, 0);
                if (cosmetic.model == null) {
                    rotateZ(matrix, 180); rotateY(matrix, 180);
                    JsonObject rotation = object(metadata, "rotation");
                    if (rotation != null) {
                        org.joml.Vector3f axis = new org.joml.Vector3f(number(rotation, "y", 0), number(rotation, "z", 0), number(rotation, "w", 0));
                        if (axis.lengthSquared() > 0) matrix.multiply(new org.joml.Quaternionf().rotationAxis((float) Math.toRadians(number(rotation, "x", 0)), axis.normalize()));
                    }
                    translation(matrix, object(metadata, "translation"), false);
                } else {
                    translation(matrix, object(metadata, "translation"), true); rotateX(matrix, 90);
                }
                scale(matrix, number(metadata, "scale", .0625f)); sizing(matrix, sizing, false);
                if (category == Category.BANDANA) scale(matrix, 1.01f);
            }
            case SHIELD -> {
                if (MeshCosmetics.shieldOnArm) {
                    getContextModel().leftArm.rotate(matrix);
                    matrix.translate(armored ? .1 : .015, 0, armored ? -.05 : 0);
                    if (armored) matrix.multiply(new org.joml.Quaternionf().rotationAxis((float) -Math.PI / 2, new org.joml.Vector3f(-.15f, 1, 0).normalize()));
                    else rotateY(matrix, -90);
                    matrix.translate(crouch ? -.1 : 0, crouch ? 1.05 : .85, player.getSkinTextures().model() == net.minecraft.client.util.SkinTextures.Model.SLIM && !armored ? -.14 : -.225); rotateX(matrix, 180);
                } else {
                    getContextModel().body.rotate(matrix); rotateZ(matrix, 180);
                    if (crouch) rotateX(matrix, -3);
                    matrix.translate(0, crouch ? -1.1 : -.9, crouch ? .03 : .17);
                    if (armored) matrix.translate(0, 0, .06);
                }
                JsonObject shieldSizing = object(metadata, "sizingData");
                translation(matrix, shieldSizing, false);
                if (bool(object(metadata, "shieldModelData"), "inverted")) rotateY(matrix, 180);
                scale(matrix, number(shieldSizing, "scale", 1) * .1f);
            }
            case PET -> {
                String petPose = cosmetic.petPose();
                if (petPose.equals("head")) {
                    getContextModel().head.rotate(matrix); matrix.translate(0, -.5, 0);
                } else matrix.translate(petPose.equals("air") ? -.6 : -.45,
                        (crouch ? .18 : 0) + (petPose.equals("air") ? -.1 : 0), 0);
                sizing(matrix, sizing, true); rotateZ(matrix, 180); rotateY(matrix, 180); rotateX(matrix, -90); scale(matrix, .1f);
            }
            case WINGS -> {
                if (cosmetic.model == null) {
                    JsonObject wing = object(cosmetic.pack.catalog, "wingData");
                    float size = number(sizing, "scale", 1); scale(matrix, size == 1 ? .1f : size);
                    matrix.translate(-1 + number(sizing, "offsetX", 0), 2 + number(sizing, "offsetY", 0) + (crouch ? number(wing, "sneakOffset", 1.5f) : 0),
                            2 + number(sizing, "offsetZ", 0) + (armored ? number(wing, "armorOffset", .5f) : 0) + (crouch ? number(wing, "sneakZOffset", -1) : 0));
                    if (crouch) rotateX(matrix, 30); rotateX(matrix, 180);
                    break;
                }
                rotateX(matrix, 90); matrix.translate(0, .165, -.15); scale(matrix, .1f);
                if (crouch) rotateX(matrix, 30);
                matrix.translate(0, armored ? .5 : 0, crouch ? -1.4 : 0); sizing(matrix, sizing, false);
                matrix.translate(-1.25, 0, 0);
            }
        }
    }

    static void draw(MatrixStack matrix, VertexConsumer out, int light, Loaded cosmetic, Md5Model.Pose pose) throws java.io.IOException {
        boolean flip = bool(cosmetic.pack.modelMetadata, "flipUVs");
        if (cosmetic.model != null) {
            for (Md5Model.SkinnedMesh part : cosmetic.model.skin(pose)) {
                for (Md5Model.Triangle triangle : part.triangles()) {
                    Md5Model.SkinnedVertex a = part.vertices().get(triangle.a()), b = part.vertices().get(triangle.b()), c = part.vertices().get(triangle.c());
                    vertex(out, matrix, light, a, flip); vertex(out, matrix, light, b, flip);
                    vertex(out, matrix, light, c, flip); vertex(out, matrix, light, c, flip);
                }
            }
        } else {
            ObjGeometry.Mesh geometry = cosmetic.obj;
            // Geometry animation uses numbered OBJ frames independently of texture animation.
            String base = cosmetic.pack.model.substring(0, cosmetic.pack.model.length() - 4);
            int geometryFrame = cosmetic.frame();
            if (cosmetic.pack.modelMetadata != null && object(cosmetic.pack.modelMetadata, "animationData") != null) {
                JsonObject animation = object(cosmetic.pack.modelMetadata, "animationData");
                if (base.endsWith("_0")) base = base.substring(0, base.length() - 2);
                if (base.endsWith("fire_shield")) {
                    geometryFrame = (int) (cosmetic.seconds() * 1000 / 166) % 7;
                    if (geometryFrame >= 4) geometryFrame = 6 - geometryFrame;
                } else geometryFrame = (int) (cosmetic.seconds() * 1000 / Math.max(20, number(animation, "frameDelay", 100))) % (int) Math.max(1, number(animation, "frames", 1));
            }
            String candidate = base + "_" + geometryFrame + ".obj";
            if (java.nio.file.Files.isRegularFile(MeshCosmetics.ROOT.resolve(candidate))) geometry = cosmetic.objAt(candidate);
            for (ObjGeometry.Triangle triangle : geometry.triangles()) {
                vertex(out, matrix, light, triangle.a(), flip); vertex(out, matrix, light, triangle.b(), flip);
                vertex(out, matrix, light, triangle.c(), flip); vertex(out, matrix, light, triangle.c(), flip);
            }
        }
    }
    private static void vertex(VertexConsumer out, MatrixStack matrix, int light, Md5Model.SkinnedVertex v, boolean flip) {
        vertex(out, matrix, light, v.x(), v.y(), v.z(), v.u(), flip ? 1 - v.v() : v.v(), v.nx(), v.ny(), v.nz());
    }
    private static void vertex(VertexConsumer out, MatrixStack matrix, int light, ObjGeometry.Vertex v, boolean flip) {
        vertex(out, matrix, light, v.x(), v.y(), v.z(), v.u(), flip ? 1 - v.v() : v.v(), v.nx(), v.ny(), v.nz());
    }
    private static void vertex(VertexConsumer out, MatrixStack matrix, int light, float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        out.vertex(matrix.peek(), x, y, z).color(255, 255, 255, 255).texture(u, v).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(matrix.peek(), nx, ny, nz);
    }
    private static boolean bool(JsonObject object, String key) { return object != null && object.has(key) && !object.get(key).isJsonNull() && object.get(key).getAsBoolean(); }
    private static JsonObject object(JsonObject parent, String key) { return parent != null && parent.has(key) && parent.get(key).isJsonObject() ? parent.getAsJsonObject(key) : null; }
    private static float number(JsonObject object, String key, float fallback) { return object != null && object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsFloat() : fallback; }
    private static void translation(MatrixStack matrix, JsonObject data, boolean invertY) {
        if (data != null) matrix.translate(number(data, "x", number(data, "offsetX", 0)), number(data, "y", number(data, "offsetY", 0)) * (invertY ? -1 : 1), number(data, "z", number(data, "offsetZ", 0)));
    }
    private static void sizing(MatrixStack matrix, JsonObject data, boolean invertY) { translation(matrix, data, invertY); if (data != null) scale(matrix, number(data, "scale", 1)); }
    private static void scale(MatrixStack matrix, float scale) { if (scale > 0 && Float.isFinite(scale)) matrix.scale(scale, scale, scale); }
    private static void rotateX(MatrixStack matrix, float degrees) { matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(degrees)); }
    private static void rotateY(MatrixStack matrix, float degrees) { matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(degrees)); }
    private static void rotateZ(MatrixStack matrix, float degrees) { matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(degrees)); }
}
