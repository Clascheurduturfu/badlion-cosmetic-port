package local.badlioncosmeticport.render;

import com.google.gson.*;
import local.badlioncosmeticport.MeshCosmetics;
import local.badlioncosmeticport.MeshCosmetics.Loaded;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Recovered weapon component selection; shared by the legacy and modern item adapters. */
public final class WeaponGeometry {
    public static final class Part {
        public final ObjGeometry.Mesh mesh;
        public final String texture;
        public final boolean flip, glow;
        public final float z;
        Part(ObjGeometry.Mesh mesh, String texture, boolean flip, float z, boolean glow) {
            this.mesh=mesh; this.texture=texture; this.flip=flip; this.z=z; this.glow=glow;
        }
    }
    private WeaponGeometry() {}
    public static int pullStage(int ticks) { return ticks >= 18 ? 3 : ticks > 13 ? 2 : ticks > 0 ? 1 : 0; }
    public static int frame(double seconds, int count, int delay) {
        if(count <= 1) return 0;
        int phase=(int)((long)(Math.max(0,seconds)*1000)/Math.max(10,delay)%(2L*count-2));
        return phase<count ? phase : 2*count-2-phase;
    }
    public static List<Part> parts(Loaded loaded, String material, int ticks) throws IOException {
        List<Part> result=new ArrayList<>();
        JsonObject info=loaded.pack.modelMetadata;
        boolean bow=loaded.pack.type==MeshCosmetics.Category.BOW;
        String texture=bow ? "bow" : material;
        Path folder=MeshCosmetics.resolve(loaded.pack.model).getParent();
        String main=info.get("modelName").getAsString();
        add(result,loaded,folder,info,main,texture,0,false,-1);
        if(info.has("subModels")) for(JsonElement element:info.getAsJsonArray("subModels")) {
            JsonObject sub=element.getAsJsonObject();
            String key=bow && sub.has("animationData") ? "bow-anim" : texture;
            add(result,loaded,folder,sub,sub.get("modelName").getAsString(),key,0,false,-1);
        }
        if(bow && info.has("extraModels")) {
            JsonObject extras=info.getAsJsonObject("extraModels");
            int stage=pullStage(ticks);
            if(extras.has("string")) add(result,loaded,folder,extras.getAsJsonObject("string"),"string",texture,0,false,stage);
            if(stage>0 && extras.has("arrow")) {
                float offset=stage==1 ? .5f : stage==2 ? .3125f : .125f;
                JsonObject item=loaded.pack.catalog.has("itemData") ? loaded.pack.catalog.getAsJsonObject("itemData") : null;
                if(item!=null && item.has("arrowFireOffset")) offset+=item.get("arrowFireOffset").getAsFloat();
                add(result,loaded,folder,extras.getAsJsonObject("arrow"),"arrow",texture,-offset,false,-1);
                if(extras.has("arrow_tail")) add(result,loaded,folder,extras.getAsJsonObject("arrow_tail"),"arrow_tail","bow-anim",-offset,false,-1);
            }
        }
        if(!bow && loaded.pack.textures!=null && loaded.pack.textures.containsKey("glow"))
            add(result,loaded,folder,info,main,"glow",0,true,-1);
        return result;
    }
    private static void add(List<Part> out,Loaded loaded,Path folder,JsonObject info,String name,
                            String texture,float z,boolean glow,int stage) throws IOException {
        if(info.has("animationData")) {
            JsonObject animation=info.getAsJsonObject("animationData");
            int index=stage>=0 ? stage : frame(loaded.seconds(),animation.get("frames").getAsInt(),
                    animation.has("frameDelay") ? animation.get("frameDelay").getAsInt() : 100);
            name+="_"+index;
        }
        Path file=folder.resolve(name+".obj");
        if(!Files.isRegularFile(file)) throw new IOException("Missing weapon component: "+file.getFileName());
        ObjGeometry.Mesh mesh=loaded.objAt(MeshCosmetics.ROOT.relativize(file).toString());
        out.add(new Part(mesh,texture,info.has("flipUVs")&&info.get("flipUVs").getAsBoolean(),z,glow));
    }
}
