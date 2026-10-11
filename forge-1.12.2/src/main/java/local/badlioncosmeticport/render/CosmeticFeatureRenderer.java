package local.badlioncosmeticport.render;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;

import com.google.gson.JsonObject;
import local.badlioncosmeticport.MeshCosmetics;
import local.badlioncosmeticport.MeshCosmetics.*;
import local.badlioncosmeticport.model.*;

/** Player attachment adapter for the recovered MD5/OBJ renderer. */
public final class CosmeticFeatureRenderer implements net.minecraft.client.renderer.entity.layers.LayerRenderer<AbstractClientPlayer> {
    private final net.minecraft.client.renderer.entity.RenderPlayer renderer;
    private float partial;
    private net.minecraft.client.model.ModelPlayer getContextModel() { return renderer.getMainModel(); }
    @Override public boolean shouldCombineTextures() { return false; }
    private static long waveTime;
    private static long waveFrames;
    public CosmeticFeatureRenderer(net.minecraft.client.renderer.entity.RenderPlayer renderer) { this.renderer=renderer; }

    @Override public void doRenderLayer(AbstractClientPlayer player, float limbAngle, float limbDistance,
            float tickDelta, float animationProgress, float headYaw, float headPitch, float renderScale) {
        partial=tickDelta;
        MatrixStack matrices=new MatrixStack();
        int light=player.getBrightnessForRender();
        if (player != Minecraft.getMinecraft().player || player.isInvisible()) return;
        for (Category category : Category.values()) {
            if (category == Category.SWORD || category == Category.BOW) continue;
            Loaded cosmetic = MeshCosmetics.active(category);
            if (cosmetic == null) continue;
            matrices.push();
            VertexConsumer vertices=null;
            int previousProgram=0;
            org.lwjgl.opengl.GL11.glPushAttrib(org.lwjgl.opengl.GL11.GL_ENABLE_BIT | org.lwjgl.opengl.GL11.GL_LIGHTING_BIT | org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT | org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT | org.lwjgl.opengl.GL11.GL_CURRENT_BIT | org.lwjgl.opengl.GL11.GL_POLYGON_BIT);
            float oldLightX=net.minecraft.client.renderer.OpenGlHelper.lastBrightnessX, oldLightY=net.minecraft.client.renderer.OpenGlHelper.lastBrightnessY;
            try {
                if (cosmetic.proceduralCloak()) updateProcedural(cosmetic, player);
                attach(matrices, category, cosmetic, player);
                int lighting = bool(cosmetic.pack.catalog, "glow") ? 15728880 : light;
                net.minecraft.client.renderer.OpenGlHelper.setLightmapTextureCoords(net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit,lighting & 65535,lighting >> 16);
                org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_BLEND);
                org.lwjgl.opengl.GL14.glBlendFuncSeparate(770,771,1,0);
                org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_CULL_FACE);
                if (category == Category.PET) {
                    JsonObject petData = object(cosmetic.pack.catalog, "petData");
                    if (bool(petData,"lighting")) org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_LIGHTING);
                    else org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_LIGHTING);
                    if (bool(petData,"culled")) org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_CULL_FACE);
                    org.lwjgl.opengl.GL11.glShadeModel(bool(petData,"smooth") ? org.lwjgl.opengl.GL11.GL_SMOOTH : org.lwjgl.opengl.GL11.GL_FLAT);
                }
                Minecraft.getMinecraft().getTextureManager().bindTexture(cosmetic.texture());
                previousProgram=LegacyShader.begin(cosmetic.pack);
                vertices = new VertexConsumer();
                Md5Model.Pose pose = cosmetic.proceduralCloak() ? null : pose(cosmetic, player, tickDelta);
                if (category == Category.CAPE && !cosmetic.proceduralCloak() && player.isSneaking()) {
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
                if (category == Category.WINGS && cosmetic.model != null) {
                    matrices.scale(-1, 1, 1); matrices.translate(-2.5, 0, 0);
                    draw(matrices, vertices, lighting, cosmetic, pose);
                }
                // Sprite registration can change the bound texture. Flush the model first.
                vertices.close(); vertices=null;
                if (category == Category.CAPE) CloakParticles.emit(cosmetic, pose, player, tickDelta);
                if (category == Category.PET) CloakParticles.emitPet(cosmetic, pose, player, tickDelta);
                cosmetic.rendered++;
            } catch (Exception failure) { MeshCosmetics.disable(category, failure); }
            finally { if (vertices!=null) vertices.close(); LegacyShader.end(previousProgram); net.minecraft.client.renderer.OpenGlHelper.setLightmapTextureCoords(net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit,oldLightX,oldLightY); org.lwjgl.opengl.GL11.glPopAttrib(); matrices.pop(); }
        }
    }

    private Md5Model.Pose pose(Loaded cosmetic, AbstractClientPlayer player, float tickDelta) {
        if (cosmetic.model == null) return null;
        String clip;
        switch(cosmetic.pack.type) {
            case CAPE: clip=MeshCosmetics.capeMotion.name().toLowerCase(java.util.Locale.ROOT); break;
            case PET: clip=cosmetic.petPose(); break;
            case WINGS: clip="flapping"; break;
            default: clip=player.motionX*player.motionX+player.motionZ*player.motionZ > .01 && cosmetic.animations.containsKey("animation_1") ? "animation_1" : "animation";
        }
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
            clip = cosmetic.animations.keySet().stream().sorted().findFirst().get();
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
            float elapsed = cosmetic.elapsed(Minecraft.getMinecraft().isGamePaused());
            // Captured X.a(aBS,gdy,float) supplies interpolated cape-position lag for X/Z
            // and abs(currentY-lastRenderY) for aeE. Yarn's official field mapping confirms these inputs.
            float partial = this.partial;
            double lagX = player.prevChasingPosX + (player.chasingPosX - player.prevChasingPosX) * partial - (player.prevPosX + (player.posX - player.prevPosX) * partial);
            double lagZ = player.prevChasingPosZ + (player.chasingPosZ - player.prevChasingPosZ) * partial - (player.prevPosZ + (player.posZ - player.prevPosZ) * partial);
            double vertical = Math.abs(player.posY - player.lastTickPosY);
            double energy = lagX * lagX + lagZ * lagZ + vertical * vertical * 2.5;
            boolean moving = energy >= cosmetic.previousEnergy / 2 && energy > (player.isSneaking() ? .03 : .1);
            if (!Minecraft.getMinecraft().isGamePaused()) clock.advance((float) ((moving ? 1.2 : -1.6) * elapsed / (player.isSneaking() ? 2 : 1)));
            cosmetic.previousEnergy = energy;
            return clock.pose();
        }
        if (cosmetic.pack.type == Category.WINGS && animation.frameCount() >= 80) {
            Md5MotionClock clock = cosmetic.clocks.get(clip);
            float elapsed = cosmetic.elapsed(Minecraft.getMinecraft().isGamePaused());
            if (!Minecraft.getMinecraft().isGamePaused()) clock.advance(elapsed);
            // The captured skeletal path has no fall controller. Keep the original clip's
            // open range while grounded; expose its deep fold only while descending.
            if (cosmetic.pack.model.contains("dragon_wings_v3")) {
                cosmetic.wingElapsed += elapsed;
                boolean falling = !player.onGround && player.motionY < -.1;
                cosmetic.wingFallBlend = Math.max(0, Math.min(1, cosmetic.wingFallBlend + (falling ? 1 : -1) * elapsed * 4));
                float frame = 9 + 9 * (float) Math.cos(cosmetic.wingElapsed * Math.PI * 2 / animation.durationSeconds());
                Md5Model.Pose idle = WingMotion.withAnimatedChildren(animation, animation.sampleFrame(frame, false), clock.pose());
                return Md5Model.Pose.interpolate(idle, clock.pose(), cosmetic.wingFallBlend);
            }
            return clock.pose();
        }
        Md5MotionClock clock = cosmetic.clocks.get(clip);
        clock.advance(cosmetic.elapsed(Minecraft.getMinecraft().isGamePaused()));
        return clock.pose();
    }

    private void updateProcedural(Loaded cosmetic, AbstractClientPlayer player) {
        Minecraft client = Minecraft.getMinecraft();
        long now = System.nanoTime();
        if (client.isGamePaused()) waveTime = now;
        else if (now - waveTime > 16_666_666L) { waveTime = now; waveFrames++; }
        float partial = this.partial;
        double x = player.prevChasingPosX + (player.chasingPosX - player.prevChasingPosX) * partial - (player.prevPosX + (player.posX - player.prevPosX) * partial);
        double y = player.prevChasingPosY + (player.chasingPosY - player.prevChasingPosY) * partial - (player.prevPosY + (player.posY - player.prevPosY) * partial);
        double z = player.prevChasingPosZ + (player.chasingPosZ - player.prevChasingPosZ) * partial - (player.prevPosZ + (player.posZ - player.prevPosZ) * partial);
        float stride = lerp(partial, player.prevCameraYaw, player.cameraYaw);
        double distance = lerp(partial, player.prevDistanceWalkedModified, player.distanceWalkedModified);
        float yaw = lerp(partial, player.prevRenderYawOffset, player.renderYawOffset);
        cosmetic.cloakPhase = waveFrames / 10f;
        cosmetic.cloakMotion = LegacyCloak.motion(x, y, z, yaw, stride, distance,
                LegacyCloak.Shape.valueOf(MeshCosmetics.capeMotion.name()), cosmetic.cloakPhase, player.isSneaking());
    }

    private void drawProcedural(MatrixStack matrix, VertexConsumer out, int light, Loaded cosmetic) {
        java.util.List<LegacyCloak.Vertex> strip = LegacyCloak.strip(LegacyCloak.Shape.valueOf(MeshCosmetics.capeMotion.name()), cosmetic.cloakMotion, cosmetic.cloakPhase);
        for (int i = 2; i < strip.size(); i++) {
            LegacyCloak.Vertex a = strip.get(i - 2); LegacyCloak.Vertex b = strip.get(i - 1); LegacyCloak.Vertex c = strip.get(i);
            if ((i & 1) != 0) { LegacyCloak.Vertex swap = a; a = b; b = swap; }
            org.joml.Vector3f normal = new org.joml.Vector3f(b.x() - a.x(), b.y() - a.y(), b.z() - a.z())
                    .cross(c.x() - a.x(), c.y() - a.y(), c.z() - a.z());
            if (normal.lengthSquared() < 1e-10f) continue;
            normal.normalize();
            for (LegacyCloak.Vertex v : new LegacyCloak.Vertex[]{a, b, c, c})
                vertex(out, matrix, light, v.x(), v.y(), v.z(), v.u(), v.v(), normal.x, normal.y, normal.z);
        }
    }

    private void attach(MatrixStack matrix, Category category, Loaded cosmetic, AbstractClientPlayer player) {
        boolean crouch = player.isSneaking(), armored = !player.getItemStackFromSlot(net.minecraft.inventory.EntityEquipmentSlot.CHEST).isEmpty();
        JsonObject metadata = cosmetic.pack.modelMetadata;
        JsonObject sizing = object(cosmetic.pack.catalog, "sizingData");
        switch (category) {
            case CAPE: {
                if (cosmetic.proceduralCloak()) {
                    LegacyCloak.Motion motion = cosmetic.cloakMotion;
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
                break;
            }
            case HAT: case BANDANA: {
                matrix.part(getContextModel().bipedHead);
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
                break;
            }
            case SHIELD: {
                if (MeshCosmetics.shieldOnArm) {
                    matrix.part(getContextModel().bipedLeftArm);
                    matrix.translate(armored ? .1 : .015, 0, armored ? -.05 : 0);
                    if (armored) matrix.multiply(new org.joml.Quaternionf().rotationAxis((float) -Math.PI / 2, new org.joml.Vector3f(-.15f, 1, 0).normalize()));
                    else rotateY(matrix, -90);
                    matrix.translate(crouch ? -.1 : 0, crouch ? 1.05 : .85, player.getSkinType().equals("slim") && !armored ? -.14 : -.225); rotateX(matrix, 180);
                } else {
                    matrix.part(getContextModel().bipedBody); rotateZ(matrix, 180);
                    if (crouch) rotateX(matrix, -3);
                    matrix.translate(0, crouch ? -1.1 : -.9, crouch ? .03 : .17);
                    if (armored) matrix.translate(0, 0, .06);
                }
                JsonObject shieldSizing = object(metadata, "sizingData");
                translation(matrix, shieldSizing, false);
                if (bool(object(metadata, "shieldModelData"), "inverted")) rotateY(matrix, 180);
                scale(matrix, number(shieldSizing, "scale", 1) * .1f);
                break;
            }
            case PET: {
                String petPose = cosmetic.petPose();
                if (petPose.equals("head")) {
                    matrix.part(getContextModel().bipedHead); matrix.translate(0, -.5, 0);
                } else matrix.translate(petPose.equals("air") ? -.6 : -.45,
                        (crouch ? .18 : 0) + (petPose.equals("air") ? -.1 : 0), 0);
                sizing(matrix, sizing, true); rotateZ(matrix, 180); rotateY(matrix, 180); rotateX(matrix, -90); scale(matrix, .1f);
                break;
            }
            case WINGS: {
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
                break;
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
        out.vertex(matrix.peek(), x, y, z).color(255, 255, 255, 255).texture(u, v).overlay(0).light(light).normal(matrix.peek(), nx, ny, nz);
    }
    private static float lerp(float delta,float from,float to) { return from+(to-from)*delta; }
    private static boolean bool(JsonObject object, String key) { return object != null && object.has(key) && !object.get(key).isJsonNull() && object.get(key).getAsBoolean(); }
    private static JsonObject object(JsonObject parent, String key) { return parent != null && parent.has(key) && parent.get(key).isJsonObject() ? parent.getAsJsonObject(key) : null; }
    private static float number(JsonObject object, String key, float fallback) { return object != null && object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsFloat() : fallback; }
    private static void translation(MatrixStack matrix, JsonObject data, boolean invertY) {
        if (data != null) matrix.translate(number(data, "x", number(data, "offsetX", 0)), number(data, "y", number(data, "offsetY", 0)) * (invertY ? -1 : 1), number(data, "z", number(data, "offsetZ", 0)));
    }
    private static void sizing(MatrixStack matrix, JsonObject data, boolean invertY) { translation(matrix, data, invertY); if (data != null) scale(matrix, number(data, "scale", 1)); }
    private static void scale(MatrixStack matrix, float scale) { if (scale > 0 && Float.isFinite(scale)) matrix.scale(scale, scale, scale); }
    private static void rotateX(MatrixStack matrix, float degrees) { matrix.multiply(new org.joml.Quaternionf().rotationX((float)Math.toRadians(degrees))); }
    private static void rotateY(MatrixStack matrix, float degrees) { matrix.multiply(new org.joml.Quaternionf().rotationY((float)Math.toRadians(degrees))); }
    private static void rotateZ(MatrixStack matrix, float degrees) { matrix.multiply(new org.joml.Quaternionf().rotationZ((float)Math.toRadians(degrees))); }
}



