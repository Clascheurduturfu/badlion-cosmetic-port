package local.badlioncosmeticport;

import java.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import local.badlioncosmeticport.MeshCosmetics.Pack;

/** Screen-owned, bounded GPU cache of offline rendered cosmetic thumbnails. */
final class ThumbnailCache implements AutoCloseable {
    private final LinkedHashMap<String, Identifier> images = new LinkedHashMap<>(72, .75f, true);
    private final Set<String> failed = new HashSet<>();
    private final String epoch = Long.toHexString(System.nanoTime());
    Identifier get(Pack pack) {
        String key = pack.type.name().toLowerCase(Locale.ROOT) + "/" + pack.id;
        if (images.containsKey(key)) return images.get(key);
        if (failed.contains(key)) return null;
        try (var stream = java.nio.file.Files.newInputStream(MeshCosmetics.resolve("thumbnails/" + pack.type.name() + "/" + pack.id + ".png"))) {
            NativeImage pixels = NativeImage.read(stream);
            if (pixels.getWidth() != 128 || pixels.getHeight() != 128) { pixels.close(); throw new IllegalArgumentException("Invalid thumbnail dimensions"); }
            Identifier id = Identifier.of("badlioncosmeticport", "thumbnail/" + epoch + "/" + key);
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, new NativeImageBackedTexture(pixels));
            images.put(key, id);
            if (images.size() > 72) {
                var oldest = images.entrySet().iterator();
                MinecraftClient.getInstance().getTextureManager().destroyTexture(oldest.next().getValue());
                oldest.remove();
            }
            return id;
        } catch (Exception exception) { failed.add(key); return null; }
    }
    @Override public void close() {
        for (Identifier id : images.values()) MinecraftClient.getInstance().getTextureManager().destroyTexture(id);
        images.clear(); failed.clear();
    }
}
