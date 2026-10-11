package local.badlioncosmeticport;

import local.badlioncosmeticport.render.CosmeticFeatureRenderer;
import local.badlioncosmeticport.render.CloakParticles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CosmeticPortClient implements ClientModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("BadlionCosmeticPort");
    private int smokeTicks;
    private boolean worldStarted;
    private int worldTicks;
    private boolean corePass;
    private long shieldVisibilityDraws;
    @Override public void onInitializeClient() {
        local.badlioncosmeticport.render.CosmeticRenderLayers.register();
        var key = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.badlioncosmeticport.cycle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.badlioncosmeticport"));
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (renderer instanceof PlayerEntityRenderer playerRenderer) helper.register(new CosmeticFeatureRenderer(playerRenderer));
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> { CloakParticles.close(); MeshCosmetics.close(); });
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AFTER_ENTITIES.register(CloakParticles::render);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            CloakParticles.tick(client);
            while (key.wasPressed()) client.setScreen(new MeshSelectionScreen(client.currentScreen));
            if ("1".equals(System.getenv("BCP_SMOKE_WORLD")) && MeshCosmetics.initialized && client.getOverlay() == null) {
                if (!worldStarted) {
                    worldStarted = true;
                    LOG.info(local.badlioncosmeticport.render.WeaponContentAudit.run());
                    for (var category : MeshCosmetics.Category.values()) {
                        if(category==MeshCosmetics.Category.SWORD||category==MeshCosmetics.Category.BOW)continue;
                        if (MeshCosmetics.active(category) == null) {
                            var pack = MeshCosmetics.packs(category).stream().filter(p -> category != MeshCosmetics.Category.WINGS || p.model.endsWith(".md5mesh")).findFirst().orElse(null);
                            if (pack != null) MeshCosmetics.selectId(category, pack.id, false);
                        }
                    }
                    MeshCosmetics.selectId(MeshCosmetics.Category.CAPE,732,false);
                    MeshCosmetics.selectId(MeshCosmetics.Category.HAT,9,false);
                    MeshCosmetics.selectId(MeshCosmetics.Category.BANDANA,315,false);
                    MeshCosmetics.selectId(MeshCosmetics.Category.SHIELD,12,false);
                    MeshCosmetics.selectId(MeshCosmetics.Category.PET,8,false);
                    MeshCosmetics.selectId(MeshCosmetics.Category.WINGS,93,false);
                    MeshCosmetics.selectId(MeshCosmetics.Category.SWORD,8,false);
                    MeshCosmetics.selectId(MeshCosmetics.Category.BOW,1,false);
                    client.createIntegratedServerLoader().start(client.currentScreen, "Mesh Audit 0.3.3 Verification");
                }
                if (client.player != null && ++worldTicks == 30) {
                    client.player.setStackInHand(net.minecraft.util.Hand.OFF_HAND, new net.minecraft.item.ItemStack(net.minecraft.item.Items.SHIELD));
                    MeshCosmetics.shieldOnArm = true;
                    MeshCosmetics.shieldWithoutItem = true;
                    client.player.getInventory().setStack(0,new net.minecraft.item.ItemStack(net.minecraft.item.Items.DIAMOND_SWORD));
                    client.player.getInventory().setStack(1,new net.minecraft.item.ItemStack(net.minecraft.item.Items.BOW));
                    client.interactionManager.clickCreativeStack(client.player.getInventory().getStack(0),36);
                    client.interactionManager.clickCreativeStack(client.player.getInventory().getStack(1),37);
                    client.player.getInventory().selectedSlot=0;
                    client.options.setPerspective(net.minecraft.client.option.Perspective.THIRD_PERSON_BACK);
                    client.setScreen(new MeshSelectionScreen(null));
                }
                if (client.player != null && worldTicks == 90) {
                    try (var image = net.minecraft.client.util.ScreenshotRecorder.takeScreenshot(client.getFramebuffer())) {
                        image.writeTo(client.runDirectory.toPath().resolve("mesh-preview.png"));
                    } catch (java.io.IOException failure) { LOG.error("Audit screenshot failed", failure); }
                    client.setScreen(null);
                    client.options.forwardKey.setPressed(true);
                    client.options.sprintKey.setPressed(true);
                }
                if(client.player!=null && worldTicks==130)client.options.setPerspective(net.minecraft.client.option.Perspective.FIRST_PERSON);
                if(client.player!=null && worldTicks==145)client.options.setPerspective(net.minecraft.client.option.Perspective.THIRD_PERSON_BACK);
                if (client.player != null && worldTicks == 135) {
                    try (var image = net.minecraft.client.util.ScreenshotRecorder.takeScreenshot(client.getFramebuffer())) {
                        image.writeTo(client.runDirectory.toPath().resolve("mesh-world.png"));
                    } catch (java.io.IOException failure) { LOG.error("Audit screenshot failed", failure); }
                }
                if (client.player != null && worldTicks == 150) {
                    boolean pass = true;
                    for (var category : MeshCosmetics.Category.values()) {
                        if(category==MeshCosmetics.Category.SWORD||category==MeshCosmetics.Category.BOW)continue;
                        var active = MeshCosmetics.active(category);
                        LOG.info("BCP_RENDER_AUDIT {} draws={} textureChanges={}", category, active == null ? 0 : active.rendered, active == null ? 0 : active.textureChanges);
                        pass &= active != null && active.rendered > 0;
                    }
                    corePass = pass;
                    LOG.info("BCP_SHIELD_REPLACEMENT draws={}", local.badlioncosmeticport.render.ShieldReplacement.draws);
                    corePass &= local.badlioncosmeticport.render.ShieldReplacement.draws > 0;
                    var cape = MeshCosmetics.active(MeshCosmetics.Category.CAPE);
                    LOG.info("BCP_MOTION_AUDIT phaseFrames={}", cape == null ? -1 : cape.clocks.get("dynamic").phaseFrames());
                    LOG.info("BCP_CLOAK_PATH procedural={} wavePhase={}", cape != null && cape.proceduralCloak(), cape == null ? 0 : cape.cloakPhase);
                    corePass &= cape != null && cape.proceduralCloak() && cape.cloakPhase > 0;
                    client.options.forwardKey.setPressed(false); client.options.sprintKey.setPressed(false);
                    MeshCosmetics.selectId(MeshCosmetics.Category.CAPE, 313, false);
                }
                if (client.player != null && worldTicks == 160) {
                    client.player.setStackInHand(net.minecraft.util.Hand.OFF_HAND, net.minecraft.item.ItemStack.EMPTY);
                    MeshCosmetics.shieldWithoutItem = false;
                    shieldVisibilityDraws = MeshCosmetics.active(MeshCosmetics.Category.SHIELD).rendered;
                }
                if (client.player != null && worldTicks == 175) {
                    boolean hidden = MeshCosmetics.active(MeshCosmetics.Category.SHIELD).rendered == shieldVisibilityDraws;
                    LOG.info("BCP_SHIELD_EMPTY_HIDDEN pass={}", hidden); corePass &= hidden;
                    MeshCosmetics.shieldWithoutItem = true;
                }
                if (client.player != null && worldTicks == 190) {
                    boolean shown = MeshCosmetics.active(MeshCosmetics.Category.SHIELD).rendered > shieldVisibilityDraws;
                    LOG.info("BCP_SHIELD_EMPTY_SHOWN pass={}", shown); corePass &= shown;
                    client.player.setStackInHand(net.minecraft.util.Hand.OFF_HAND, new net.minecraft.item.ItemStack(net.minecraft.item.Items.SHIELD));
                }
                if(client.player!=null && worldTicks>=195 && worldTicks<230)client.options.useKey.setPressed(true);
                if(client.player!=null && worldTicks==230)client.options.useKey.setPressed(false);
                if (client.player != null && worldTicks == 195) {
                    client.player.getInventory().selectedSlot=1;
                    client.interactionManager.interactItem(client.player,net.minecraft.util.Hand.MAIN_HAND);
                }
                if (client.player != null && worldTicks == 230) client.interactionManager.stopUsingItem(client.player);
                if (client.player != null && worldTicks == 200) {
                    try (var image = net.minecraft.client.util.ScreenshotRecorder.takeScreenshot(client.getFramebuffer())) {
                        image.writeTo(client.runDirectory.toPath().resolve("mesh-particles.png"));
                    } catch (java.io.IOException failure) { LOG.error("Audit screenshot failed", failure); }
                    LOG.info("BCP_PARTICLE_AUDIT spawned={} drawn={}", CloakParticles.spawned, CloakParticles.drawn);
                    corePass &= CloakParticles.spawned > 0 && CloakParticles.drawn > 0;
                    MeshCosmetics.selectId(MeshCosmetics.Category.CAPE, 799, false);
                    MeshCosmetics.selectId(MeshCosmetics.Category.SHIELD, 250, false);
                    MeshCosmetics.selectId(MeshCosmetics.Category.BANDANA, 345, false);
                    MeshCosmetics.selectId(MeshCosmetics.Category.WINGS, 6, false);
                    client.setScreen(new MeshSelectionScreen(null));
                }
                if (client.player != null && worldTicks == 245) {
                    try (var image = net.minecraft.client.util.ScreenshotRecorder.takeScreenshot(client.getFramebuffer())) {
                        image.writeTo(client.runDirectory.toPath().resolve("mesh-effects.png"));
                    } catch (java.io.IOException failure) { LOG.error("Audit screenshot failed", failure); }
                    corePass &= local.badlioncosmeticport.render.CosmeticRenderLayers.effectDraws > 0;
                    LOG.info("BCP_EFFECT_AUDIT draws={}", local.badlioncosmeticport.render.CosmeticRenderLayers.effectDraws);
                    MeshCosmetics.selectId(MeshCosmetics.Category.PET, 12, false);
                    MeshCosmetics.petPose = "shoulder";
                }
                if (client.player != null && worldTicks == 285) {
                    var pet = MeshCosmetics.active(MeshCosmetics.Category.PET);
                    int joints = pet == null ? 0 : pet.model.joints().size();
                    LOG.info("BCP_PET_AUDIT shoulderJoints={} draws={}", joints, pet == null ? 0 : pet.rendered);
                    corePass &= joints == 23 && pet.rendered > 0;
                    MeshCosmetics.petPose = "air";
                }
                if (client.player != null && worldTicks == 325) {
                    var pet = MeshCosmetics.active(MeshCosmetics.Category.PET);
                    int joints = pet == null ? 0 : pet.model.joints().size();
                    LOG.info("BCP_PET_AUDIT airJoints={}", joints);
                    corePass &= joints == 32;
                    client.setScreen(null);
                    MeshCosmetics.selectId(MeshCosmetics.Category.PET, 19, false);
                    MeshCosmetics.petPose = "shoulder";
                }
                if (client.player != null && worldTicks == 345) {
                    var pet = MeshCosmetics.active(MeshCosmetics.Category.PET);
                    var clock = pet.clocks.get(pet.currentClip);
                    clock.reset(); clock.advance(5.9f);
                }
                if (client.player != null && worldTicks == 380) {
                    LOG.info("BCP_PET_EFFECT_FIRE spawned={}", CloakParticles.fireSpawned);
                    corePass &= CloakParticles.fireSpawned > 0;
                    MeshCosmetics.selectId(MeshCosmetics.Category.PET, 70, false);
                }
                if (client.player != null && worldTicks == 400) {
                    var pet = MeshCosmetics.active(MeshCosmetics.Category.PET);
                    var clock = pet.clocks.get(pet.currentClip);
                    clock.reset(); clock.advance(6.32f);
                }
                if (client.player != null && worldTicks == 430) {
                    LOG.info("BCP_PET_EFFECT_SONIC spawned={} totalDraws={}", CloakParticles.sonicSpawned, CloakParticles.petDrawn);
                    corePass &= CloakParticles.sonicSpawned > 0 && CloakParticles.petDrawn > 0;
                    MeshCosmetics.selectId(MeshCosmetics.Category.PET, 16, false);
                }
                if (client.player != null && worldTicks == 465) {
                    var pet = MeshCosmetics.active(MeshCosmetics.Category.PET);
                    LOG.info("BCP_PET_CYCLE clip={} draws={}", pet.currentClip, pet.rendered);
                    corePass &= pet.currentClip.startsWith("frame-") && pet.rendered > 0;

                    LOG.info("BCP_WEAPON_DRAW swords={} bows={} gui={} pulls={}",local.badlioncosmeticport.render.WeaponReplacement.swords,local.badlioncosmeticport.render.WeaponReplacement.bows,local.badlioncosmeticport.render.WeaponReplacement.gui,local.badlioncosmeticport.render.WeaponReplacement.pulls);
                    corePass &= local.badlioncosmeticport.render.WeaponReplacement.swords>0 && local.badlioncosmeticport.render.WeaponReplacement.bows>0 && local.badlioncosmeticport.render.WeaponReplacement.gui>0 && local.badlioncosmeticport.render.WeaponReplacement.pulls>0;
                    LOG.info("BCP_WEAPON_VIEWS first={} third={}",local.badlioncosmeticport.render.WeaponReplacement.firstPerson,local.badlioncosmeticport.render.WeaponReplacement.thirdPerson);
                    corePass &= local.badlioncosmeticport.render.WeaponReplacement.firstPerson>0 && local.badlioncosmeticport.render.WeaponReplacement.thirdPerson>0;
                    LOG.info("BCP_WORLD_AUDIT {}", corePass ? "PASS" : "FAIL");
                    client.scheduleStop();
                }
            }
            if ("1".equals(System.getenv("BCP_SMOKE_TEST")) && MeshCosmetics.initialized && client.getOverlay() == null) {
                if (++smokeTicks == 20) client.setScreen(new MeshSelectionScreen(client.currentScreen));
                if (smokeTicks == 40) {
                    for (var category : MeshCosmetics.Category.values()) {
                        if(category==MeshCosmetics.Category.SWORD||category==MeshCosmetics.Category.BOW)continue;
                        var packs = MeshCosmetics.packs(category);
                        if (!packs.isEmpty() && MeshCosmetics.active(category) == null) MeshCosmetics.selectId(category, packs.get(0).id, false);
                    }
                    LOG.info("BCP_SMOKE_EQUIPPED categories=6");
                }
                if (smokeTicks == 120) {
                    LOG.info("BCP_SMOKE_PASS meshPacks={} featureRenderer={}", MeshCosmetics.count(), CosmeticFeatureRenderer.class.getName());
                    client.scheduleStop();
                }
            }
        });
        MinecraftClient.getInstance().execute(MeshCosmetics::load);
        LOG.info("Badlion Cosmetic Port {} initialized", MeshCosmetics.VERSION);
    }
}
