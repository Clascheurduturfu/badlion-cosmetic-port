package local.badlioncosmeticport;

import java.util.*;
import java.io.InputStream;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

/** Screen-owned, bounded cache of original-model thumbnail renders. */
final class ThumbnailCache implements AutoCloseable {
    private final LinkedHashMap<String,ResourceLocation> images=new LinkedHashMap<>(72,.75f,true);
    private final Set<String> failed=new HashSet<>();
    private final String epoch=Long.toHexString(System.nanoTime());
    ResourceLocation get(MeshCosmetics.Pack pack) {
        String key=pack.type.name()+"/"+pack.id;
        if(images.containsKey(key))return images.get(key);
        if(failed.contains(key))return null;
        try(InputStream in=java.nio.file.Files.newInputStream(MeshCosmetics.resolve("thumbnails/"+key+".png"))) {
            java.awt.image.BufferedImage image=ImageIO.read(in);
            if(image==null||image.getWidth()!=128||image.getHeight()!=128)throw new java.io.IOException("Invalid thumbnail");
            ResourceLocation id=new ResourceLocation("badlioncosmeticport","thumbnail/"+epoch+"/"+key.toLowerCase(Locale.ROOT));
            Minecraft.getMinecraft().getTextureManager().loadTexture(id,new DynamicTexture(image));images.put(key,id);
            if(images.size()>72) {
                Iterator<Map.Entry<String,ResourceLocation>> it=images.entrySet().iterator();
                Minecraft.getMinecraft().getTextureManager().deleteTexture(it.next().getValue());it.remove();
            }
            return id;
        }catch(Exception error){failed.add(key);return null;}
    }
    public void close(){for(ResourceLocation id:images.values())Minecraft.getMinecraft().getTextureManager().deleteTexture(id);images.clear();failed.clear();}
}
