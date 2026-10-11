package local.badlioncosmeticport.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import local.badlioncosmeticport.MeshCosmetics;
import local.badlioncosmeticport.model.Md5Model;
import local.badlioncosmeticport.model.PetEffects;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.InputStream;
import java.util.*;

/** Recovered aEd joint-9 emitter and MD normal-motion particles, adapted to the vanilla world pass. */
public final class CloakParticles {
    private static final List<Particle> LIVE = new ArrayList<>();
    private static final Map<String, Identifier> TEXTURES = new HashMap<>();
    private static final Set<String> FAILED = new HashSet<>();
    private static final Random RANDOM = new Random();
    private static net.minecraft.client.world.ClientWorld world;
    public static long spawned, drawn;
    public static long petSpawned, petDrawn;
    public static long fireSpawned, sonicSpawned;
    private static final class Particle {
        Vector3f position, previous, velocity;
        Identifier sprite;
        int age, maxAge, red = 255, green = 255, blue = 255;
        boolean down, glow, leaf;
        float phase;
        int leafTick;
        int petId;
        float alpha = 1, diameter = .1f, rotation, previousRotation, spin, petGreen = 1;
        AbstractClientPlayerEntity parent;
    }
    /** aEi supplier callbacks. Positions remain relative to the player's body anchor. */
    public static void emitPet(MeshCosmetics.Loaded cosmetic, Md5Model.Pose pose,
                               AbstractClientPlayerEntity player, float delta) {
        var client = MinecraftClient.getInstance();
        if (client.currentScreen != null || client.isPaused() || client.options.getPerspective().isFirstPerson()
                || pose == null || cosmetic.currentClip == null || cosmetic.lastParticleTick == player.age) return;
        cosmetic.lastParticleTick = player.age;
        int id = cosmetic.pack.id;
        var clock = cosmetic.clocks.get(cosmetic.currentClip);
        if (clock == null) return;
        int count = PetEffects.batchCount(id, clock.phaseSeconds());
        if (count == 0) return;
        String petPose = cosmetic.petPose();
        Vector3f local = new Vector3f(petPose.equals("head") ? 0 : petPose.equals("air") ? -.6f : -.45f,
                (petPose.equals("head") ? .5f : petPose.equals("air") ? .1f : 0) - (player.isInSneakingPose() ? .18f : 0), 0);
        if (id == 19) {
            var root = pose.joints().get(0).position();
            local.x *= .95f; local.add(root.x() * .1f, .25f + root.z() * .1f, .3f + root.y() * .1f);
        } else local.add(.025f, .45f, 0);
        for (int i = 0; i < count && LIVE.size() < 512; i++) {
            Identifier texture = sprite(id == 19 ? "fire" : "sonic_boom_0");
            if (texture == null) continue;
            if (id == 70) local.z += .1f;
            Particle particle = new Particle(); particle.sprite = texture; particle.petId = id; particle.parent = player;
            particle.position = new Vector3f(local); particle.previous = new Vector3f(local);
            particle.maxAge = id == 19 ? 40 : 16;
            particle.velocity = id == 19 ? new Vector3f(RANDOM.nextFloat() * .02f - .01f,
                    RANDOM.nextFloat() * .02f - .01f, RANDOM.nextFloat() * .02f + .05f) : new Vector3f();
            if (id == 19) {
                particle.petGreen = RANDOM.nextFloat() * .3f + .7f;
                particle.green = (int) (particle.petGreen * 255); particle.blue = 0; particle.alpha = .8f;
                particle.spin = RANDOM.nextFloat() * .3f; particle.rotation = RANDOM.nextFloat() * (float) (Math.PI * 2);
            } else particle.diameter = .15f;
            particle.previousRotation = particle.rotation;
            LIVE.add(particle); petSpawned++;
            if (id == 19) fireSpawned++; else sonicSpawned++;
        }
    }
    private static Identifier sprite(String name) {
        if (TEXTURES.containsKey(name)) return TEXTURES.get(name);
        if (FAILED.contains(name) || !name.matches("[a-zA-Z0-9_-]{1,80}")) return null;
        try (InputStream input = java.nio.file.Files.newInputStream(MeshCosmetics.resolve("particles/" + name + ".png"))) {
            NativeImage image = NativeImage.read(input);
            if (image.getWidth() > 4096 || image.getHeight() > 4096) { image.close(); throw new java.io.IOException("Oversized sprite"); }
            Identifier identifier = Identifier.of("badlioncosmeticport", "particle/" + name.toLowerCase(Locale.ROOT));
            MinecraftClient.getInstance().getTextureManager().registerTexture(identifier, new NativeImageBackedTexture(image));
            TEXTURES.put(name, identifier); return identifier;
        } catch (Exception failure) { FAILED.add(name); return null; }
    }
    public static void emit(MeshCosmetics.Loaded cosmetic, Md5Model.Pose pose, AbstractClientPlayerEntity player, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != null || client.isPaused() || client.options.getPerspective().isFirstPerson()) return;
        if (!cosmetic.proceduralCloak() && (pose == null || pose.joints().size() < 10)) return;
        JsonObject data = cosmetic.pack.catalog == null ? null : cosmetic.pack.catalog.getAsJsonObject("cloakParticleData");
        if (data == null || !data.has("particles") || !data.get("particles").isJsonArray()) return;
        JsonArray sprites = data.getAsJsonArray("particles"); if (sprites.isEmpty()) return;
        if (cosmetic.lastParticleTick == player.age) return;
        cosmetic.lastParticleTick = player.age;
        boolean crouch = player.isInSneakingPose();
        float bodyYaw = MathHelper.lerpAngleDegrees(delta, player.prevBodyYaw, player.bodyYaw);
        Matrix4f model = new Matrix4f().translate((float) MathHelper.lerp(delta, player.prevX, player.getX()),
                (float) MathHelper.lerp(delta, player.prevY, player.getY()), (float) MathHelper.lerp(delta, player.prevZ, player.getZ()))
                .rotateY((float) Math.toRadians(180 - bodyYaw)).scale(-1, -1, 1).scale(.9375f)
                .translate(0, -1.5078125f, 0).scale(.055f).translate(0, .4f, cosmetic.proceduralCloak() ? 3 : 5);
        if (crouch) model.rotateX((float) Math.toRadians(30));
        model.translate(0, crouch ? (cosmetic.proceduralCloak() ? -.5f : 1.5f) : 0, .15f + (!player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST).isEmpty() ? .05f : 0) + (crouch ? (cosmetic.proceduralCloak() ? -.5f : -.6f) : 0));
        if (cosmetic.proceduralCloak()) model.rotateZ((float) -Math.PI);
        model.rotateX((float) Math.PI / 2);
        Md5Model.Vec3 joint = cosmetic.proceduralCloak()
                ? new Md5Model.Vec3(0, cosmetic.cloakMotion.particleLift(), cosmetic.cloakMotion.particleLength()) : pose.joints().get(9).position();
        float probability = data.has("spawnRate") ? data.get("spawnRate").getAsFloat() : 1;
        for (int i = 0; i < 2 && LIVE.size() < 512; i++) {
            if (RANDOM.nextFloat() >= probability) continue;
            Identifier identifier = sprite(sprites.get(RANDOM.nextInt(sprites.size())).getAsString());
            if (identifier == null) continue;
            Particle particle = new Particle();
            particle.sprite = identifier;
            particle.position = model.transformPosition(new Vector3f(joint.x() - 6 + RANDOM.nextFloat() * 12, joint.y(), joint.z()));
            particle.previous = new Vector3f(particle.position);
            particle.down = flag(data, "moveDown"); particle.glow = flag(data, "hasGlow");
            particle.leaf = data.has("moveType") && data.get("moveType").getAsString().equals("LEAF");
            particle.phase = RANDOM.nextInt(40);
            particle.maxAge = data.has("maxAge") ? Math.max(1, Math.min(200, data.get("maxAge").getAsInt())) : 40;
            particle.velocity = new Vector3f(particle.down ? 0 : RANDOM.nextFloat() * .02f - .01f, particle.down ? -.1f : -.01f, particle.down ? 0 : RANDOM.nextFloat() * .02f - .01f);
            if (data.has("colors") && data.get("colors").isJsonArray() && !data.getAsJsonArray("colors").isEmpty()) {
                JsonArray colors = data.getAsJsonArray("colors"); JsonObject color = colors.get(RANDOM.nextInt(colors.size())).getAsJsonObject();
                particle.red = component(color, "red"); particle.green = component(color, "green"); particle.blue = component(color, "blue");
            }
            LIVE.add(particle); spawned++;
        }
    }
    private static int component(JsonObject object, String field) { return object.has(field) ? MathHelper.clamp(object.get(field).getAsInt(), 0, 255) : 255; }
    private static boolean flag(JsonObject object, String field) { return object.has(field) && object.get(field).getAsBoolean(); }
    public static void tick(MinecraftClient client) {
        if (client.world != world) { LIVE.clear(); world = client.world; }
        if (client.world == null || client.isPaused()) return;
        for (Particle particle : LIVE) {
            particle.previous.set(particle.position);
            particle.previousRotation = particle.rotation;
            if (particle.petId == 19) {
                particle.petGreen = Math.max(0, particle.petGreen - .04f); particle.green = (int) (particle.petGreen * 255);
                particle.alpha = Math.max(0, particle.alpha - .03f);
                particle.diameter += .006f; particle.velocity.mul(.95f); particle.rotation += particle.spin;
            } else if (particle.petId == 70) {
                Identifier next = sprite("sonic_boom_" + Math.min(15, particle.age + 1));
                if (next != null) particle.sprite = next;
            } else if (particle.leaf) {
                if (particle.leafTick++ > 40) particle.leafTick = 0;
                // MD freezes Wz.N(0, YA()+90) in its constructor, where YA is still zero.
                particle.velocity.set(-(float) Math.sin((particle.leafTick + particle.phase) / 20.0 * Math.PI) * .01f, -.005f, 0);
            }
            else particle.velocity.mul(particle.down ? .8f : .9f, particle.down ? .7f : .95f, particle.down ? .8f : .9f);
            particle.position.add(particle.velocity); particle.age++;
        }
        LIVE.removeIf(p -> p.age >= p.maxAge || p.parent != null && p.parent.isRemoved());
    }
    private static float size(Particle particle, float age) {
        if (particle.petId != 0) return particle.diameter;
        float value = age <= 7.5f ? Math.min(.8f, age / 7.5f) : Math.min(.8f, particle.maxAge - age / 7.5f);
        return Math.max(0, value) * .2f;
    }
    public static void render(WorldRenderContext context) {
        if (LIVE.isEmpty() || context.matrixStack() == null || context.consumers() == null) return;
        var camera = context.camera(); var position = camera.getPos();
        float delta = context.tickDelta();
        MatrixStack matrices = context.matrixStack();
        for (Particle particle : LIVE) {
            matrices.push();
            Vector3f p = new Vector3f(particle.previous).lerp(particle.position, delta);
            if (particle.parent != null) {
                var parent = particle.parent;
                p.rotateY((float) Math.toRadians(-MathHelper.lerpAngleDegrees(delta, parent.prevBodyYaw, parent.bodyYaw)));
                p.add((float) MathHelper.lerp(delta, parent.prevX, parent.getX()),
                        1.2f + (float) MathHelper.lerp(delta, parent.prevY, parent.getY()),
                        (float) MathHelper.lerp(delta, parent.prevZ, parent.getZ()));
            }
            matrices.translate(p.x - position.x, p.y - position.y, p.z - position.z); matrices.multiply(camera.getRotation());
            if (particle.petId == 19) matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotation(
                    MathHelper.lerp(delta, particle.previousRotation, particle.rotation)));
            float half = MathHelper.lerp(delta, size(particle, particle.age), size(particle, Math.min(particle.maxAge, particle.age + 1))) / 2;
            VertexConsumer out = context.consumers().getBuffer(RenderLayer.getEntityTranslucent(particle.sprite));
            int light = particle.glow ? LightmapTextureManager.MAX_LIGHT_COORDINATE : WorldRenderer.getLightmapCoordinates(context.world(), net.minecraft.util.math.BlockPos.ofFloored(p.x, p.y, p.z));
            vertex(out, matrices, particle, half, half, 1, 0, light); vertex(out, matrices, particle, half, -half, 1, 1, light);
            vertex(out, matrices, particle, -half, -half, 0, 1, light); vertex(out, matrices, particle, -half, half, 0, 0, light);
            matrices.pop(); drawn++;
            if (particle.petId != 0) petDrawn++;
        }
    }
    private static void vertex(VertexConsumer out, MatrixStack matrix, Particle p, float x, float y, float u, float v, int light) {
        out.vertex(matrix.peek().getPositionMatrix(), x, y, 0).color(p.red, p.green, p.blue, (int) (p.alpha * 255)).texture(u, v).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(matrix.peek().getNormalMatrix(), 0, 0, 1).next();
    }
    public static void close() {
        LIVE.clear(); for (Identifier id : TEXTURES.values()) MinecraftClient.getInstance().getTextureManager().destroyTexture(id);
        TEXTURES.clear(); FAILED.clear(); world = null;
    }
}
