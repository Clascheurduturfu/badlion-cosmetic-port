package local.badlioncosmeticport;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import local.badlioncosmeticport.render.CosmeticFeatureRenderer;
import local.badlioncosmeticport.render.CloakParticles;
import org.lwjgl.input.Keyboard;

@Mod(modid="badlioncosmeticport",name="Cosmetic Port",version="0.5.0-forge-1.12.2",clientSideOnly=true,acceptedMinecraftVersions="[1.12.2]")
public final class ForgeCosmeticPort {
    private final KeyBinding selector=new KeyBinding("Cosmetic browser",Keyboard.KEY_K,"Cosmetic Port");
    @Mod.EventHandler public void init(FMLInitializationEvent event) {
        MeshCosmetics.load();
        for(net.minecraft.client.renderer.entity.RenderPlayer renderer:Minecraft.getMinecraft().getRenderManager().getSkinMap().values())
            renderer.addLayer(new CosmeticFeatureRenderer(renderer));
        ClientRegistry.registerKeyBinding(selector);
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END) return;
        Minecraft client=Minecraft.getMinecraft();
        if(selector.isPressed()&&client.player!=null) client.displayGuiScreen(new MeshSelectionScreen(client.currentScreen));
        CloakParticles.tick(client);
        ForgeAudit.tick(client);
    }
    @SubscribeEvent public void world(RenderWorldLastEvent event) { CloakParticles.render(event.getPartialTicks()); }
}
