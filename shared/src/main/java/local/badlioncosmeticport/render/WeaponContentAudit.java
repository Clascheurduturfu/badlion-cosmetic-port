package local.badlioncosmeticport.render;
import local.badlioncosmeticport.MeshCosmetics;
/** Opt-in audit of every cached weapon component, pull stage and geometry frame. */
public final class WeaponContentAudit {
    private WeaponContentAudit() {}
    public static String run() {
        int packs=0,parts=0,frames=0;
        java.util.Set<java.nio.file.Path> parsed=new java.util.HashSet<>();
        try {
            for(MeshCosmetics.Category kind:new MeshCosmetics.Category[]{MeshCosmetics.Category.SWORD,MeshCosmetics.Category.BOW})
                for(MeshCosmetics.Pack pack:MeshCosmetics.packs(kind)) {
                    try {
                        MeshCosmetics.Loaded loaded=MeshCosmetics.loadPreview(pack);
                        try {
                            for(int ticks:new int[]{0,1,14,18})parts+=WeaponGeometry.parts(loaded,kind==MeshCosmetics.Category.BOW?"bow":"diamond",ticks).size();
                            for(MeshCosmetics.TextureSet set:pack.textures.values())for(String file:set.frames) {MeshCosmetics.resolve(file);frames++;}
                            // Parse all sibling animation components, not only the current elapsed-time frame.
                            try(java.nio.file.DirectoryStream<java.nio.file.Path> stream=java.nio.file.Files.newDirectoryStream(MeshCosmetics.resolve(pack.model).getParent(),"*.obj")) {
                                for(java.nio.file.Path path:stream)if(parsed.add(path))loaded.objAt(MeshCosmetics.ROOT.relativize(path).toString());
                            }
                            packs++;
                        } finally {loaded.close();}
                    } catch(Exception failure) {throw new IllegalStateException("Weapon asset audit failed: "+kind+" "+pack.id,failure);}
                }
            return "BCP_WEAPON_ASSETS PASS packs="+packs+" pullComponents="+parts+" textureFrames="+frames;
        } catch(Exception failure) {throw new IllegalStateException(failure);}
    }
}
