package local.badlioncosmeticport;

import net.minecraft.client.Minecraft;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import local.badlioncosmeticport.MeshCosmetics.Category;
import local.badlioncosmeticport.render.CloakParticles;
import local.badlioncosmeticport.render.LegacyShader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Explicit opt-in, offline production audit. Normal launcher runs never create worlds. */
final class ForgeAudit {
    private static final Logger LOG=LogManager.getLogger("BadlionCosmeticPort");
    private static final boolean AUDIT="1".equals(System.getenv("BCP_FORGE_AUDIT"));
    private static final boolean PLAY="1".equals(System.getenv("BCP_FORGE_PLAY"));
    private static int menuTicks,worldTicks;
    private static boolean launched;
    private static long variants;
    private static long nextStatus;
    private ForgeAudit() {}
    static void tick(Minecraft client) {
        if(!AUDIT&&!PLAY)return;
        if(!launched&&++menuTicks==25) {
            launched=true;
            WorldSettings settings=new WorldSettings(189L,WorldSettings.GameType.CREATIVE,true,false,WorldType.FLAT);
            settings.enableCommands();client.launchIntegratedServer("Cosmetic Port Forge Verification","Cosmetic Port Forge Verification",settings);
        }
        if(client.thePlayer==null)return;
        if(AUDIT)client.gameSettings.thirdPersonView=worldTicks>=69&&worldTicks<85?0:1;
        worldTicks++;
        if(AUDIT && System.currentTimeMillis()>nextStatus) {
            nextStatus=System.currentTimeMillis()+5000;
            LOG.info("BCP_FORGE_PROGRESS tick={} camera={} screen={} paused={} focus={} visible={}",worldTicks,client.gameSettings.thirdPersonView,client.currentScreen,client.isGamePaused(),client.inGameHasFocus,org.lwjgl.opengl.Display.isVisible());
            LOG.info("BCP_FORGE_PLAYER invisible={} spectator={} cameraPlayer={} managerPlayer={} position={},{},{}",client.thePlayer.isInvisible(),client.thePlayer.isSpectator(),client.getRenderViewEntity()==client.thePlayer,client.getRenderManager().livingPlayer==client.thePlayer,client.thePlayer.posX,client.thePlayer.posY,client.thePlayer.posZ);
        }
        if(worldTicks==1) {
            if(AUDIT)LOG.info(local.badlioncosmeticport.render.WeaponContentAudit.run());
            client.gameSettings.thirdPersonView=1;client.gameSettings.pauseOnLostFocus=false;
            client.thePlayer.rotationYaw=0;client.thePlayer.rotationPitch=10;
            if(client.getIntegratedServer()!=null)client.getIntegratedServer().worldServerForDimension(0).setWorldTime(6000);
        }
        if(PLAY){if(worldTicks==35)client.displayGuiScreen(new MeshSelectionScreen(null));return;}
        if(worldTicks==3) {
            MeshCosmetics.selectId(Category.CAPE,732,false);MeshCosmetics.selectId(Category.HAT,9,false);
            MeshCosmetics.selectId(Category.BANDANA,315,false);MeshCosmetics.selectId(Category.SHIELD,12,false);
            MeshCosmetics.selectId(Category.PET,8,false);MeshCosmetics.selectId(Category.WINGS,93,false);
            MeshCosmetics.shieldOnArm=true;
            MeshCosmetics.selectId(Category.SWORD,8,false);MeshCosmetics.selectId(Category.BOW,1,false);
            client.thePlayer.inventory.mainInventory[0]=new net.minecraft.item.ItemStack(net.minecraft.init.Items.diamond_sword);
            client.thePlayer.inventory.mainInventory[1]=new net.minecraft.item.ItemStack(net.minecraft.init.Items.bow);
            client.thePlayer.inventory.currentItem=0;
            client.playerController.sendSlotPacket(client.thePlayer.inventory.mainInventory[0],36);
            client.playerController.sendSlotPacket(client.thePlayer.inventory.mainInventory[1],37);
        }
        if(worldTicks==35)client.displayGuiScreen(new MeshSelectionScreen(null));
        if(worldTicks==55)client.displayGuiScreen(null);
        if(worldTicks>55 && client.currentScreen!=null)client.displayGuiScreen(null);
        // An occluded/minimized OpenGL window can keep ticking without rendering.
        // Wait for the actual world draw instead of treating that as a model failure.
        if(worldTicks==105 && MeshCosmetics.active(Category.CAPE).rendered==0) {
            worldTicks=104;
            return;
        }
        if(worldTicks==80)net.minecraft.util.ScreenShotHelper.saveScreenshot(client.mcDataDir,client.displayWidth,client.displayHeight,client.getFramebuffer());
        if(worldTicks==105) {
            LOG.info("BCP_FORGE_STATE perspective={} screen={} paused={}",client.gameSettings.thirdPersonView,client.currentScreen,client.isGamePaused());
            for(Category type:Category.values()) {
                if(type==Category.SWORD||type==Category.BOW)continue;
                MeshCosmetics.Loaded loaded=MeshCosmetics.active(type);
                if(loaded==null||loaded.rendered==0)throw new IllegalStateException("No draw for "+type);
                LOG.info("BCP_FORGE_DRAW {} draws={} textures={}",type,loaded.rendered,loaded.textureChanges);
            }
            MeshCosmetics.selectId(Category.SHIELD,250,false);MeshCosmetics.selectId(Category.BANDANA,345,false);
            MeshCosmetics.selectId(Category.PET,19,false);
        }
        if(worldTicks>=120 && worldTicks<160)net.minecraft.client.settings.KeyBinding.setKeyBindState(client.gameSettings.keyBindUseItem.getKeyCode(),true);
        if(worldTicks==120) { client.thePlayer.inventory.currentItem=1; client.playerController.updateController(); client.playerController.sendUseItem(client.thePlayer,client.theWorld,client.thePlayer.getHeldItem()); }
        if(worldTicks==160)net.minecraft.client.settings.KeyBinding.setKeyBindState(client.gameSettings.keyBindUseItem.getKeyCode(),false);
        if(worldTicks==160) client.thePlayer.clearItemInUse();
        if(worldTicks==265) {
            MeshCosmetics.selectId(Category.PET,16,false);variants=MeshCosmetics.active(Category.PET).rendered;
        }
        if(worldTicks==290) {
            MeshCosmetics.Loaded pet=MeshCosmetics.active(Category.PET);
            if(pet.rendered<=variants||!pet.currentClip.startsWith("frame-"))throw new IllegalStateException("Pet frame cycle not rendered");
            LOG.info("BCP_FORGE_FRAME_CYCLE {}",pet.currentClip);
            MeshCosmetics.selectId(Category.PET,70,false);
        }
        if(worldTicks==435 && CloakParticles.sonicSpawned==0) { worldTicks=434; return; }
        if(worldTicks==435) {
            MeshCosmetics.selectId(Category.PET,12,false);MeshCosmetics.petPose="air";
        }
        if(worldTicks==460) {
            MeshCosmetics.Loaded pet=MeshCosmetics.active(Category.PET);
            LOG.info("BCP_FORGE_PET_AIR joints={} draws={}",pet.model.joints().size(),pet.rendered);
            if(pet.model.joints().size()!=32||pet.rendered==0)throw new IllegalStateException("Pet air variant failed");
            LOG.info("BCP_FORGE_EFFECTS shaders={} cloakSpawned={} cloakDrawn={} fire={} sonic={}",LegacyShader.draws,CloakParticles.spawned,CloakParticles.drawn,CloakParticles.fireSpawned,CloakParticles.sonicSpawned);
            if(LegacyShader.draws==0||CloakParticles.spawned==0||CloakParticles.drawn==0||CloakParticles.fireSpawned==0||CloakParticles.sonicSpawned==0)throw new IllegalStateException("Effect audit failed");
            net.minecraft.util.ScreenShotHelper.saveScreenshot(client.mcDataDir,client.displayWidth,client.displayHeight,client.getFramebuffer());
            LOG.info("BCP_WEAPON_DRAW swords={} bows={} gui={} pulls={}",local.badlioncosmeticport.render.WeaponReplacement.swords,local.badlioncosmeticport.render.WeaponReplacement.bows,local.badlioncosmeticport.render.WeaponReplacement.gui,local.badlioncosmeticport.render.WeaponReplacement.pulls);
            if(local.badlioncosmeticport.render.WeaponReplacement.swords==0||local.badlioncosmeticport.render.WeaponReplacement.bows==0||local.badlioncosmeticport.render.WeaponReplacement.gui==0||local.badlioncosmeticport.render.WeaponReplacement.pulls==0)throw new IllegalStateException("Weapon audit failed");
            LOG.info("BCP_WEAPON_VIEWS first={} third={}",local.badlioncosmeticport.render.WeaponReplacement.firstPerson,local.badlioncosmeticport.render.WeaponReplacement.thirdPerson);
            if(local.badlioncosmeticport.render.WeaponReplacement.firstPerson==0||local.badlioncosmeticport.render.WeaponReplacement.thirdPerson==0)throw new IllegalStateException("Weapon view audit failed");
            LOG.info("BCP_FORGE_AUDIT PASS");client.shutdown();
        }
    }
}
