package local.badlioncosmeticport.render;
import local.badlioncosmeticport.MeshCosmetics;
import local.badlioncosmeticport.MeshCosmetics.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.*;
import org.lwjgl.opengl.GL11;
import org.joml.Matrix4f;
/** Renders only local held weapons and GUI artwork; item behavior stays vanilla. */
public final class WeaponReplacement {
    public static long swords,bows,gui,pulls,firstPerson,thirdPerson;
    private static String mode="GUI";
    public static void mode(Object value) { mode=String.valueOf(value); }
    public static boolean render(Object value) {
        if(!(value instanceof ItemStack) || !MeshCosmetics.initialized) return false;
        ItemStack stack=(ItemStack)value;Minecraft mc=Minecraft.getMinecraft();
        boolean bow=stack.getItem() instanceof ItemBow;
        if(!bow && !(stack.getItem() instanceof ItemSword)) return false;
        Category kind=bow?Category.BOW:Category.SWORD;Loaded loaded=MeshCosmetics.active(kind);
        if(loaded==null || mc.player==null) return false;
        boolean inventory=mode.equals("GUI");
        if(!inventory && stack!=mc.player.getHeldItemMainhand() && stack!=mc.player.getHeldItemOffhand()) return false;
        String material=bow?"bow":material(stack);
        if(!material.equals("bow") && !java.util.Arrays.asList("wood","stone","iron","gold","diamond","netherite").contains(material)) return false;
        int ticks=mc.player.getActiveItemStack()==stack?mc.player.getItemInUseMaxCount():0;
        GlStateManager.pushMatrix();
        int bound=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D),shade=GL11.glGetInteger(GL11.GL_SHADE_MODEL);
        boolean cull=GL11.glIsEnabled(GL11.GL_CULL_FACE),blend=GL11.glIsEnabled(GL11.GL_BLEND);
        try {
            GlStateManager.disableCull();GlStateManager.enableBlend();GlStateManager.tryBlendFuncSeparate(770,771,1,0);GlStateManager.color(1,1,1,1);
            MatrixStack matrix=new MatrixStack();
            if(inventory && loaded.pack.textures.containsKey(material+"-gui")) {
                String key=material+"-gui";
                mc.getTextureManager().bindTexture(loaded.weaponTexture(key));
                try(VertexConsumer out=new VertexConsumer()) {
                    quad(out,matrix.peek());
                }
                gui++;
            } else {
                // The surrounding vanilla item transform already supplies swing, blocking and hand mirroring.
                matrix.scale(.35f,.35f,.35f);
                for(WeaponGeometry.Part part:WeaponGeometry.parts(loaded,material,ticks)) {
                    mc.getTextureManager().bindTexture(loaded.weaponTexture(part.texture));
                    matrix.push();matrix.translate(0,0,part.z);
                    try(VertexConsumer out=new VertexConsumer()) {
                        for(ObjGeometry.Triangle t:part.mesh.triangles()) {
                            vertex(out,matrix.peek(),t.a(),part.flip);vertex(out,matrix.peek(),t.b(),part.flip);
                            vertex(out,matrix.peek(),t.c(),part.flip);vertex(out,matrix.peek(),t.c(),part.flip);
                        }
                    } finally {matrix.pop();}
                }
                if(ticks>0) pulls++;
                if(mode.contains("FIRST_PERSON"))firstPerson++;if(mode.contains("THIRD_PERSON"))thirdPerson++;
            }
            loaded.rendered++;if(bow)bows++;else swords++;
            return true;
        } catch(Exception failure) {MeshCosmetics.disable(kind,failure);return false;}
        finally {
            GlStateManager.popMatrix();GlStateManager.bindTexture(bound);GL11.glShadeModel(shade);
            if(cull)GlStateManager.enableCull();else GlStateManager.disableCull();
            if(blend)GlStateManager.enableBlend();else GlStateManager.disableBlend();GlStateManager.color(1,1,1,1);
        }
    }
    private static String material(ItemStack stack) {
        if(!stack.getItem().getRegistryName().getResourceDomain().equals("minecraft")) return "unsupported";
        String name=stack.getItem().getRegistryName().getResourcePath();
        return name.startsWith("wooden_")?"wood":name.startsWith("golden_")?"gold":name.contains("_")?name.substring(0,name.indexOf('_')):name;
    }
    private static void vertex(VertexConsumer out,Matrix4f m,ObjGeometry.Vertex v,boolean flip) {
        out.vertex(m,v.x(),v.y(),v.z()).texture(v.u(),flip?1-v.v():v.v()).normal(m,v.nx(),v.ny(),v.nz());
    }
    private static void quad(VertexConsumer out,Matrix4f m) {
        out.vertex(m,-.5f,-.5f,0).texture(0,1).normal(m,0,0,1);
        out.vertex(m,.5f,-.5f,0).texture(1,1).normal(m,0,0,1);
        out.vertex(m,.5f,.5f,0).texture(1,0).normal(m,0,0,1);
        out.vertex(m,-.5f,.5f,0).texture(0,0).normal(m,0,0,1);
    }
}
