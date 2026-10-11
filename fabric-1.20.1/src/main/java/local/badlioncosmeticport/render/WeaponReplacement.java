package local.badlioncosmeticport.render;
import local.badlioncosmeticport.MeshCosmetics;
import local.badlioncosmeticport.MeshCosmetics.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
/** Cosmetic appearance of local vanilla weapons. Vanilla use/damage packets are untouched. */
public final class WeaponReplacement {
    public static long swords,bows,gui,pulls,firstPerson,thirdPerson;
    private WeaponReplacement() {}
    public static boolean render(ItemStack stack,ModelTransformationMode mode,boolean left,MatrixStack matrices,
                                 VertexConsumerProvider consumers,int light,int overlay,BakedModel model) {
        var player=MinecraftClient.getInstance().player;
        boolean bow=stack.getItem()==Items.BOW;
        if(!bow && !(stack.getItem() instanceof SwordItem)) return false;
        Category kind=bow?Category.BOW:Category.SWORD;Loaded loaded=MeshCosmetics.active(kind);
        if(loaded==null || player==null) return false;
        boolean inventory=mode==ModelTransformationMode.GUI;
        if(!inventory && (stack!=player.getMainHandStack() && stack!=player.getOffHandStack()
                || mode==ModelTransformationMode.GROUND || mode==ModelTransformationMode.FIXED || mode==ModelTransformationMode.NONE)) return false;
        String material=bow?"bow":material(stack);
        if(!material.equals("bow") && !java.util.Arrays.asList("wood","stone","iron","gold","diamond","netherite").contains(material)) return false;
        int ticks=player.getActiveItem()==stack?player.getItemUseTime():0;
        matrices.push();
        try {
            model.getTransformation().getTransformation(mode).apply(left,matrices);
            if(inventory && loaded.pack.textures.containsKey(material+"-gui")) {
                var out=consumers.getBuffer(RenderLayer.getEntityTranslucent(loaded.weaponTexture(material+"-gui")));
                vertex(out,matrices,light,overlay,-.5f,-.5f,0,0,1,0,0,1);
                vertex(out,matrices,light,overlay,.5f,-.5f,0,1,1,0,0,1);
                vertex(out,matrices,light,overlay,.5f,.5f,0,1,0,0,0,1);
                vertex(out,matrices,light,overlay,-.5f,.5f,0,0,0,0,0,1);
                gui++;
            } else {
                matrices.scale(.35f,.35f,.35f);
                for(WeaponGeometry.Part part:WeaponGeometry.parts(loaded,material,ticks)) {
                    var layer=part.glow?RenderLayer.getEyes(loaded.weaponTexture(part.texture)):
                            RenderLayer.getEntityTranslucent(loaded.weaponTexture(part.texture));
                    var out=ItemRenderer.getItemGlintConsumer(consumers,layer,true,stack.hasGlint());
                    matrices.push();matrices.translate(0,0,part.z);
                    try {
                        int lighting=part.glow?LightmapTextureManager.MAX_LIGHT_COORDINATE:light;
                        for(ObjGeometry.Triangle t:part.mesh.triangles()) {
                            vertex(out,matrices,lighting,overlay,t.a(),part.flip);vertex(out,matrices,lighting,overlay,t.b(),part.flip);
                            vertex(out,matrices,lighting,overlay,t.c(),part.flip);vertex(out,matrices,lighting,overlay,t.c(),part.flip);
                        }
                    } finally {matrices.pop();}
                }
                if(ticks>0) pulls++;
                if(mode.isFirstPerson())firstPerson++;else if(mode==ModelTransformationMode.THIRD_PERSON_LEFT_HAND||mode==ModelTransformationMode.THIRD_PERSON_RIGHT_HAND)thirdPerson++;
            }
            loaded.rendered++;if(bow)bows++;else swords++;
            return true;
        } catch(Exception failure) {MeshCosmetics.disable(kind,failure);return false;}
        finally {matrices.pop();}
    }
    private static String material(ItemStack stack) {
        if(!Registries.ITEM.getId(stack.getItem()).getNamespace().equals("minecraft")) return "unsupported";
        String name=Registries.ITEM.getId(stack.getItem()).getPath();
        return name.startsWith("wooden_")?"wood":name.startsWith("golden_")?"gold":name.contains("_")?name.substring(0,name.indexOf('_')):name;
    }
    private static void vertex(VertexConsumer out,MatrixStack matrices,int light,int overlay,ObjGeometry.Vertex v,boolean flip) {
        vertex(out,matrices,light,overlay,v.x(),v.y(),v.z(),v.u(),flip?1-v.v():v.v(),v.nx(),v.ny(),v.nz());
    }
    private static void vertex(VertexConsumer out,MatrixStack matrices,int light,int overlay,float x,float y,float z,float u,float v,float nx,float ny,float nz) {
        out.vertex(matrices.peek().getPositionMatrix(),x,y,z).color(255,255,255,255).texture(u,v).overlay(overlay).light(light).normal(matrices.peek().getNormalMatrix(),nx,ny,nz).next();
    }
}
