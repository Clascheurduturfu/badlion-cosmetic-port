package local.badlioncosmeticport.render;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.util.ResourceLocation;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import net.minecraft.client.renderer.texture.DynamicTexture;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import local.badlioncosmeticport.MeshCosmetics;
import local.badlioncosmeticport.model.Md5Model;
import local.badlioncosmeticport.model.PetEffects;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.InputStream;
import java.util.*;

/** Recovered aEd joint-9 emitter and MD normal-motion particles, adapted to the vanilla world pass. */
public final class CloakParticles {
    private static final List<Particle> LIVE = new ArrayList<>();
    private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();
    private static final Set<String> FAILED = new HashSet<>();
    private static final Random RANDOM = new Random();
    private static net.minecraft.client.multiplayer.WorldClient world;
    public static long spawned, drawn;
    public static long petSpawned, petDrawn;
    public static long fireSpawned, sonicSpawned;
    private static final class Particle {
        Vector3f position, previous, velocity;
        ResourceLocation sprite;
        int age, maxAge, red = 255, green = 255, blue = 255;
        boolean down, glow, leaf;
        float phase;
        int leafTick;
        int petId;
        float alpha = 1, diameter = .1f, rotation, previousRotation, spin, petGreen = 1;
        AbstractClientPlayer parent;
    }
    /** aEi supplier callbacks. Positions remain relative to the player's body anchor. */
    public static void emitPet(MeshCosmetics.Loaded cosmetic, Md5Model.Pose pose,
                               AbstractClientPlayer player, float delta) {
        Minecraft client = Minecraft.getMinecraft();
        if (client.currentScreen != null || client.isGamePaused() || client.gameSettings.thirdPersonView==0
                || pose == null || cosmetic.currentClip == null || cosmetic.lastParticleTick == player.ticksExisted) return;
        cosmetic.lastParticleTick = player.ticksExisted;
        int id = cosmetic.pack.id;
        local.badlioncosmeticport.model.Md5MotionClock clock = cosmetic.clocks.get(cosmetic.currentClip);
        if (clock == null) return;
        int count = PetEffects.batchCount(id, clock.phaseSeconds());
        if (count == 0) return;
        String petPose = cosmetic.petPose();
        Vector3f local = new Vector3f(petPose.equals("head") ? 0 : petPose.equals("air") ? -.6f : -.45f,
                (petPose.equals("head") ? .5f : petPose.equals("air") ? .1f : 0) - (player.isSneaking() ? .18f : 0), 0);
        if (id == 19) {
            Md5Model.Vec3 root = pose.joints().get(0).position();
            local.x *= .95f; local.add(root.x() * .1f, .25f + root.z() * .1f, .3f + root.y() * .1f);
        } else local.add(.025f, .45f, 0);
        for (int i = 0; i < count && LIVE.size() < 512; i++) {
            ResourceLocation texture = sprite(id == 19 ? "fire" : "sonic_boom_0");
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
    private static ResourceLocation sprite(String name) {
        if (TEXTURES.containsKey(name)) return TEXTURES.get(name);
        if (FAILED.contains(name) || !name.matches("[a-zA-Z0-9_-]{1,80}")) return null;
        try (InputStream input = java.nio.file.Files.newInputStream(MeshCosmetics.resolve("particles/" + name + ".png"))) {
            BufferedImage image = ImageIO.read(input);
            if (image.getWidth() > 4096 || image.getHeight() > 4096) { throw new java.io.IOException("Oversized sprite"); }
            ResourceLocation identifier = new ResourceLocation("badlioncosmeticport", "particle/" + name.toLowerCase(Locale.ROOT));
            Minecraft.getMinecraft().getTextureManager().loadTexture(identifier, new DynamicTexture(image));
            TEXTURES.put(name, identifier); return identifier;
        } catch (Exception failure) {
            FAILED.add(name);
            org.apache.logging.log4j.LogManager.getLogger("BadlionCosmeticPort").warn("Cannot load particle sprite {}",name,failure);
            return null;
        }
    }
    public static void emit(MeshCosmetics.Loaded cosmetic, Md5Model.Pose pose, AbstractClientPlayer player, float delta) {
        Minecraft client = Minecraft.getMinecraft();
        if (client.currentScreen != null || client.isGamePaused() || client.gameSettings.thirdPersonView==0) return;
        if (!cosmetic.proceduralCloak() && (pose == null || pose.joints().size() < 10)) return;
        JsonObject data = cosmetic.pack.catalog == null ? null : cosmetic.pack.catalog.getAsJsonObject("cloakParticleData");
        if (data == null || !data.has("particles") || !data.get("particles").isJsonArray()) return;
        JsonArray sprites = data.getAsJsonArray("particles"); if (sprites.size()==0) return;
        if (cosmetic.lastParticleTick == player.ticksExisted) return;
        cosmetic.lastParticleTick = player.ticksExisted;
        boolean crouch = player.isSneaking();
        float bodyYaw = lerpAngle(delta, player.prevRenderYawOffset, player.renderYawOffset);
        Matrix4f model = new Matrix4f().translate((float) lerp(delta, player.prevPosX, player.posX),
                (float) lerp(delta, player.prevPosY, player.posY), (float) lerp(delta, player.prevPosZ, player.posZ))
                .rotateY((float) Math.toRadians(180 - bodyYaw)).scale(-1, -1, 1).scale(.9375f)
                .translate(0, -1.5078125f, 0).scale(.055f).translate(0, .4f, cosmetic.proceduralCloak() ? 3 : 5);
        if (crouch) model.rotateX((float) Math.toRadians(30));
        model.translate(0, crouch ? (cosmetic.proceduralCloak() ? -.5f : 1.5f) : 0, .15f + (player.getCurrentArmor(2)!=null ? .05f : 0) + (crouch ? (cosmetic.proceduralCloak() ? -.5f : -.6f) : 0));
        if (cosmetic.proceduralCloak()) model.rotateZ((float) -Math.PI);
        model.rotateX((float) Math.PI / 2);
        Md5Model.Vec3 joint = cosmetic.proceduralCloak()
                ? new Md5Model.Vec3(0, cosmetic.cloakMotion.particleLift(), cosmetic.cloakMotion.particleLength()) : pose.joints().get(9).position();
        float probability = data.has("spawnRate") ? data.get("spawnRate").getAsFloat() : 1;
        for (int i = 0; i < 2 && LIVE.size() < 512; i++) {
            if (RANDOM.nextFloat() >= probability) continue;
            ResourceLocation identifier = sprite(sprites.get(RANDOM.nextInt(sprites.size())).getAsString());
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
            if (data.has("colors") && data.get("colors").isJsonArray() && data.getAsJsonArray("colors").size()>0) {
                JsonArray colors = data.getAsJsonArray("colors"); JsonObject color = colors.get(RANDOM.nextInt(colors.size())).getAsJsonObject();
                particle.red = component(color, "red"); particle.green = component(color, "green"); particle.blue = component(color, "blue");
            }
            LIVE.add(particle); spawned++;
        }
    }
    private static int component(JsonObject object, String field) { return object.has(field) ? clamp(object.get(field).getAsInt(), 0, 255) : 255; }
    private static boolean flag(JsonObject object, String field) { return object.has(field) && object.get(field).getAsBoolean(); }
    public static void tick(Minecraft client) {
        if (client.theWorld != world) { LIVE.clear(); world = client.theWorld; }
        if (client.theWorld == null || client.isGamePaused()) return;
        for (Particle particle : LIVE) {
            particle.previous.set(particle.position);
            particle.previousRotation = particle.rotation;
            if (particle.petId == 19) {
                particle.petGreen = Math.max(0, particle.petGreen - .04f); particle.green = (int) (particle.petGreen * 255);
                particle.alpha = Math.max(0, particle.alpha - .03f);
                particle.diameter += .006f; particle.velocity.mul(.95f); particle.rotation += particle.spin;
            } else if (particle.petId == 70) {
                ResourceLocation next = sprite("sonic_boom_" + Math.min(15, particle.age + 1));
                if (next != null) particle.sprite = next;
            } else if (particle.leaf) {
                if (particle.leafTick++ > 40) particle.leafTick = 0;
                // MD freezes Wz.N(0, YA()+90) in its constructor, where YA is still zero.
                particle.velocity.set(-(float) Math.sin((particle.leafTick + particle.phase) / 20.0 * Math.PI) * .01f, -.005f, 0);
            }
            else particle.velocity.mul(particle.down ? .8f : .9f, particle.down ? .7f : .95f, particle.down ? .8f : .9f);
            particle.position.add(particle.velocity); particle.age++;
        }
        LIVE.removeIf(p -> p.age >= p.maxAge || p.parent != null && p.parent.isDead);
    }
    private static float size(Particle particle, float age) {
        if (particle.petId != 0) return particle.diameter;
        float value = age <= 7.5f ? Math.min(.8f, age / 7.5f) : Math.min(.8f, particle.maxAge - age / 7.5f);
        return Math.max(0, value) * .2f;
    }
    public static void render(float delta) {
        if (LIVE.isEmpty()) return;
        Minecraft client=Minecraft.getMinecraft();
        net.minecraft.client.renderer.entity.RenderManager camera=client.getRenderManager();
        org.lwjgl.opengl.GL11.glPushAttrib(org.lwjgl.opengl.GL11.GL_ENABLE_BIT | org.lwjgl.opengl.GL11.GL_LIGHTING_BIT | org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT | org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT | org.lwjgl.opengl.GL11.GL_CURRENT_BIT | org.lwjgl.opengl.GL11.GL_POLYGON_BIT);
        float oldX=net.minecraft.client.renderer.OpenGlHelper.lastBrightnessX,oldY=net.minecraft.client.renderer.OpenGlHelper.lastBrightnessY;
        try {
            org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_BLEND);
            org.lwjgl.opengl.GL14.glBlendFuncSeparate(770,771,1,0);
            org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_CULL_FACE);
            org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_LIGHTING);
            org.lwjgl.opengl.GL11.glDepthMask(false);
            for(Particle particle:LIVE) {
                Vector3f p=new Vector3f(particle.previous).lerp(particle.position,delta);
                if(particle.parent!=null) {
                    AbstractClientPlayer parent=particle.parent;
                    p.rotateY((float)Math.toRadians(-lerpAngle(delta,parent.prevRenderYawOffset,parent.renderYawOffset)));
                    p.add((float)lerp(delta,parent.prevPosX,parent.posX),1.2f+(float)lerp(delta,parent.prevPosY,parent.posY),(float)lerp(delta,parent.prevPosZ,parent.posZ));
                }
                MatrixStack matrices=new MatrixStack();
                matrices.translate(p.x-camera.viewerPosX,p.y-camera.viewerPosY,p.z-camera.viewerPosZ);
                matrices.multiply(new org.joml.Quaternionf().rotationY((float)Math.toRadians(-camera.playerViewY)).rotateX((float)Math.toRadians(camera.playerViewX)));
                if(particle.petId==19) matrices.multiply(new org.joml.Quaternionf().rotationZ(lerp(delta,particle.previousRotation,particle.rotation)));
                float half=lerp(delta,size(particle,particle.age),size(particle,Math.min(particle.maxAge,particle.age+1)))/2;
                client.getTextureManager().bindTexture(particle.sprite);
                int light=particle.glow?15728880:client.theWorld.getCombinedLight(new net.minecraft.util.BlockPos(p.x,p.y,p.z),0);
                net.minecraft.client.renderer.OpenGlHelper.setLightmapTextureCoords(net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit,light&65535,light>>16);
                try(VertexConsumer out=new VertexConsumer()) {
                    vertex(out,matrices,particle,half,half,1,0);vertex(out,matrices,particle,half,-half,1,1);
                    vertex(out,matrices,particle,-half,-half,0,1);vertex(out,matrices,particle,-half,half,0,0);
                }
                drawn++;if(particle.petId!=0)petDrawn++;
            }
        } finally {
            net.minecraft.client.renderer.OpenGlHelper.setLightmapTextureCoords(net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit,oldX,oldY);
            org.lwjgl.opengl.GL11.glPopAttrib();
        }
    }
    private static void vertex(VertexConsumer out,MatrixStack matrix,Particle p,float x,float y,float u,float v) {
        out.vertex(matrix.peek(),x,y,0).color(p.red,p.green,p.blue,(int)(p.alpha*255)).texture(u,v).normal(matrix.peek(),0,0,1);
    }
    private static int clamp(int n,int low,int high) { return Math.max(low,Math.min(high,n)); }
    private static double lerp(float delta,double from,double to) { return from+(to-from)*delta; }
    private static float lerp(float delta,float from,float to) { return from+(to-from)*delta; }
    private static float lerpAngle(float delta,float from,float to) { return from+net.minecraft.util.MathHelper.wrapAngleTo180_float(to-from)*delta; }
    public static void close() {
        LIVE.clear(); for (ResourceLocation id : TEXTURES.values()) Minecraft.getMinecraft().getTextureManager().deleteTexture(id);
        TEXTURES.clear(); FAILED.clear(); world = null;
    }
}

