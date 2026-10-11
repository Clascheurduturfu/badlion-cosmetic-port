package local.badlioncosmeticport;

import com.google.gson.*;
import local.badlioncosmeticport.model.*;
import local.badlioncosmeticport.render.ObjGeometry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Local, original-resolution asset catalog. All mutable state belongs to the client thread. */
public final class MeshCosmetics {
    public static final String VERSION = "0.5.0-fabric";
    private static final Logger LOG = LoggerFactory.getLogger("BadlionCosmeticPort");
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Path ROOT = FabricLoader.getInstance().getConfigDir().resolve("badlion-cosmetic-port/mesh-assets");
    public enum Category { CAPE, HAT, BANDANA, SHIELD, PET, WINGS, SWORD, BOW }
    public enum CapeMotion { DYNAMIC, CURVED, RECT }
    public static CapeMotion capeMotion = CapeMotion.DYNAMIC;
    public static String petPose = "shoulder";
    public static boolean shieldOnArm = true;
    public static boolean shieldWithoutItem = true;
    public static int hatHeight;
    private static final EnumMap<Category, List<Pack>> PACKS = new EnumMap<>(Category.class);
    private static final EnumMap<Category, Loaded> ACTIVE = new EnumMap<>(Category.class);
    private static Loaded preview;
    private static final Path SELECTION = ROOT.getParent().resolve("mesh-selection.json");
    public static boolean initialized;

    public static final class Pack {
        public Category type;
        public int id;
        public String name;
        public String model;
        public String[] frames;
        public int[] frameDurationsMs;
        public Map<String, TextureSet> textures;
        public Map<String, String> animations;
        public JsonObject modelMetadata;
        public JsonObject catalog;
    }
    public static final class TextureSet { public String[] frames; public int[] frameDurationsMs; }
    private static final class Catalog { int formatVersion; List<Pack> packs; }
    private static final class Selection {
        Map<Category, Integer> selected = new EnumMap<>(Category.class);
        CapeMotion capeMotion = CapeMotion.DYNAMIC;
        String petPose = "shoulder";
        boolean shieldOnArm = true;
        boolean shieldWithoutItem = true;
        int hatHeight;
    }
    public static final class Loaded {
        public final Pack pack;
        public Md5Model model;
        public final ObjGeometry.Mesh obj;
        public final Map<String, Md5Animation> animations = new HashMap<>();
        private final Map<String, Md5Model> animationModels = new HashMap<>();
        public final Map<String, Md5MotionClock> clocks = new HashMap<>();
        private final Map<String, ObjGeometry.Mesh> objFrames = new LinkedHashMap<>();
        public double lastTime = Double.NaN;
        public double previousEnergy;
        public String currentClip;
        public LegacyCloak.Motion cloakMotion;
        public float cloakPhase;
        public int lastParticleTick = Integer.MIN_VALUE;
        public float wingFallBlend;
        public double wingElapsed;
        public long rendered;
        public long textureChanges;
        private final Map<String, Identifier> variantTextures = new HashMap<>();
        private final Map<String, Integer> variantFrames = new HashMap<>();
        private int currentFrame = -1;
        private Identifier texture;
        private final long epoch = System.nanoTime();
        private final long frameCycleMs;

        private Loaded(Pack pack) throws IOException {
            this.pack = pack;
            Path file = resolve(pack.model);
            model = pack.model.endsWith(".md5mesh") ? Md5Model.read(file) : null;
            obj = model == null ? ObjGeometry.read(file) : null;
            List<Md5Model> variants = new ArrayList<>();
            if (model != null) {
                variants.add(model);
                if (pack.type == Category.PET && pack.modelMetadata != null && pack.modelMetadata.has("subModels")) {
                    for (JsonElement entry : pack.modelMetadata.getAsJsonArray("subModels")) {
                        String name = entry.getAsJsonObject().get("modelName").getAsString();
                        Path variant = file.resolveSibling(name + ".md5mesh");
                        variants.add(Md5Model.read(resolve(ROOT.relativize(variant).toString())));
                    }
                }
            }
            if (model != null && pack.animations != null) {
                for (var clip : pack.animations.entrySet()) {
                    if (pack.type == Category.WINGS && !clip.getKey().equals("flapping")) continue;
                    try {
                        Md5Animation animation = Md5Animation.read(resolve(clip.getValue()));
                        // aCH selects air=0, shoulder=1, head=2; aBY clamps to the
                        // loaded metadata model list, which can contain pose variants.
                        int variantIndex = pack.type == Category.PET ? switch (clip.getKey()) {
                            case "shoulder" -> 1;
                            case "head" -> 2;
                            default -> 0;
                        } : 0;
                        Md5Model animationModel = variants.get(Math.min(variantIndex, variants.size() - 1));
                        animation.validateFor(animationModel);
                        animations.put(clip.getKey(), animation);
                        animationModels.put(clip.getKey(), animationModel);
                        Md5MotionClock clock = new Md5MotionClock(animation);
                        if (pack.type == Category.CAPE && clip.getKey().equals("dynamic") && animation.frameCount() > 46)
                            clock.configureDynamic(30, 45, 36);
                        else if (pack.type == Category.CAPE && animation.frameCount() >= 30) clock.configureOffset(30, true);
                        else if (pack.type == Category.WINGS && animation.frameCount() >= 80) clock.configureOffset(80, false);
                        else clock.configureOffset(animation.frameCount(), false);
                        clocks.put(clip.getKey(), clock);
                    } catch (IOException | IllegalArgumentException failure) {
                        LOG.warn("Rejected incompatible clip {}/{}: {}", pack.name, clip.getKey(), failure.toString());
                    }
                }
            }
            long duration = 0;
            for (int i = 0; i < pack.frames.length; i++) duration += delay(i);
            frameCycleMs = duration;
        }
        private int delay(int index) {
            return pack.frameDurationsMs != null && index < pack.frameDurationsMs.length
                    ? Math.max(10, Math.min(60_000, pack.frameDurationsMs[index])) : 100;
        }
        public double seconds() { return (System.nanoTime() - epoch) / 1e9; }
        public void useAnimationModel(String clip) {
            Md5Model selected = animationModels.get(clip);
            if (selected != null) model = selected;
        }
        public void restartClock(String clip) {
            Md5Animation animation = animations.get(clip);
            Md5MotionClock clock = new Md5MotionClock(animation);
            if (pack.type == Category.CAPE && clip.equals("dynamic") && animation.frameCount() > 46)
                clock.configureDynamic(30, 45, 36);
            else if (pack.type == Category.CAPE && animation.frameCount() >= 30) clock.configureOffset(30, true);
            else if (pack.type == Category.WINGS && animation.frameCount() >= 80) clock.configureOffset(80, false);
            else clock.configureOffset(animation.frameCount(), false);
            clocks.put(clip, clock);
        }
        public String petPose() {
            if (animations.containsKey(MeshCosmetics.petPose)) return MeshCosmetics.petPose;
            if (animations.containsKey("shoulder")) return "shoulder";
            if (animations.containsKey("air")) return "air";
            return MeshCosmetics.petPose;
        }
        public boolean proceduralCloak() {
            return pack.type == Category.CAPE && pack.catalog != null && (!pack.catalog.has("modelName")
                    || pack.catalog.get("modelName").isJsonNull() || pack.catalog.get("modelName").getAsString().isBlank());
        }
        public float elapsed(boolean paused) {
            double now = seconds();
            float elapsed = Double.isNaN(lastTime) || paused ? 0 : (float) Math.max(0, Math.min(.1, now - lastTime));
            lastTime = now;
            return elapsed;
        }
        public int frame() {
            if (pack.type == Category.PET && pack.id == 16) {
                long now = System.currentTimeMillis();
                return PetEffects.thirtyVirusFrame(now);
            }
            long phase = (long) (seconds() * 1000) % frameCycleMs;
            for (int i = 0; i < pack.frames.length; i++) { phase -= delay(i); if (phase < 0) return i; }
            return 0;
        }
        public Identifier texture() throws IOException {
            int index = frame();
            if (index == currentFrame) return texture;
            NativeImage image;
            try (InputStream in = Files.newInputStream(resolve(pack.frames[index]))) { image = NativeImage.read(in); }
            if (image.getWidth() > 4096 || image.getHeight() > 4096) { image.close(); throw new IOException("Texture exceeds 4096 pixels"); }
            Identifier next = Identifier.of("badlioncosmeticport", "mesh/" + pack.type.name().toLowerCase(Locale.ROOT) + "/" + pack.id + "/" + Long.toHexString(epoch) + "/" + index);
            var manager = MinecraftClient.getInstance().getTextureManager();
            try { manager.registerTexture(next, new NativeImageBackedTexture(image)); }
            catch (RuntimeException failure) { image.close(); throw failure; }
            // Only one HD frame per equipped cosmetic is resident, never the entire animation.
            if (texture != null && !texture.equals(next)) manager.destroyTexture(texture);
            currentFrame = index; texture = next;
            textureChanges++;
            return next;
        }
        public ObjGeometry.Mesh objAt(String relative) throws IOException {
            ObjGeometry.Mesh mesh = objFrames.get(relative);
            if (mesh == null) {
                mesh = ObjGeometry.read(resolve(relative));
                if (objFrames.size() >= 8) objFrames.remove(objFrames.keySet().iterator().next());
                objFrames.put(relative, mesh);
            }
            return mesh;
        }
        public Identifier weaponTexture(String key) throws IOException {
            TextureSet set = pack.textures == null ? null : pack.textures.get(key);
            if (set == null || set.frames == null || set.frames.length == 0) return texture();
            long cycle = 0;
            for (int i=0;i<set.frames.length;i++) cycle += variantDelay(set,i);
            long phase = (long)(seconds()*1000) % cycle;
            int index = 0;
            for (; index<set.frames.length-1;index++) { phase -= variantDelay(set,index); if(phase<0) break; }
            if (variantFrames.containsKey(key) && variantFrames.get(key) == index) return variantTextures.get(key);
            Identifier next = Identifier.of("badlioncosmeticport", "weapon/" + pack.type.name().toLowerCase(Locale.ROOT) + "/" + pack.id + "/" + Long.toHexString(epoch) + "/" + key + "/" + index);
            NativeImage image;
            try(InputStream in=Files.newInputStream(resolve(set.frames[index]))) { image=NativeImage.read(in); }
            if(image.getWidth()>4096 || image.getHeight()>4096) { image.close(); throw new IOException("Invalid weapon image"); }
            try { MinecraftClient.getInstance().getTextureManager().registerTexture(next,new NativeImageBackedTexture(image)); }
            catch(RuntimeException failure) { image.close(); throw failure; }
            Identifier old = variantTextures.put(key,next);
            if (old != null) MinecraftClient.getInstance().getTextureManager().destroyTexture(old);
            variantFrames.put(key,index); textureChanges++;
            return next;
        }
        private int variantDelay(TextureSet set,int index) {
            return set.frameDurationsMs != null && index < set.frameDurationsMs.length ? Math.max(10,Math.min(60000,set.frameDurationsMs[index])) : 100;
        }
        public void close() {
            for(Identifier id:variantTextures.values()) MinecraftClient.getInstance().getTextureManager().destroyTexture(id);
            variantTextures.clear(); variantFrames.clear();
            if (texture != null) MinecraftClient.getInstance().getTextureManager().destroyTexture(texture);
        }
    }
    public static Path resolve(String relative) throws IOException {
        if (relative == null) throw new IOException("Missing asset path");
        Path root = ROOT.toAbsolutePath().normalize();
        Path file = root.resolve(relative).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) throw new IOException("Missing/invalid asset: " + relative);
        return file;
    }
    public static void load() {
        for (Category type : Category.values()) PACKS.put(type, new ArrayList<>());
        try (Reader reader = Files.newBufferedReader(ROOT.resolve("catalog.json"))) {
            Catalog catalog = JSON.fromJson(reader, Catalog.class);
            if (catalog.formatVersion != 1 || catalog.packs == null) throw new IOException("Unsupported asset catalog");
            for (Pack pack : catalog.packs) {
                if (pack.type == null || pack.name == null || pack.frames == null || pack.frames.length == 0 || pack.frames.length > 256) continue;
                PACKS.get(pack.type).add(pack);
            }
            for (List<Pack> packs : PACKS.values()) packs.sort(Comparator.comparing(p -> p.name, String.CASE_INSENSITIVE_ORDER));
            Selection selection = new Selection();
            if (Files.isRegularFile(SELECTION)) {
                try (Reader saved = Files.newBufferedReader(SELECTION)) { selection = JSON.fromJson(saved, Selection.class); }
            } else {
                selection.selected.put(Category.CAPE, 1438);
                selection.selected.put(Category.HAT, 13);
                selection.selected.put(Category.BANDANA, 26);
                selection.selected.put(Category.SHIELD, 1);
                selection.selected.put(Category.PET, 1);
            }
            if (selection != null) {
                capeMotion = selection.capeMotion == null ? CapeMotion.DYNAMIC : selection.capeMotion;
                petPose = Set.of("shoulder", "head", "air").contains(selection.petPose == null ? "" : selection.petPose) ? selection.petPose : "shoulder";
                shieldOnArm = selection.shieldOnArm;
                shieldWithoutItem = selection.shieldWithoutItem;
                hatHeight = Math.max(-8, Math.min(8, selection.hatHeight));
                if (selection.selected != null) for (var selected : selection.selected.entrySet()) selectId(selected.getKey(), selected.getValue(), false);
            }
            initialized = true;
            LOG.info("Cosmetic Port {}: {} HD packs; MD5/OBJ renderer ready", VERSION, count());
        } catch (Exception failure) { LOG.error("Mesh catalog initialization failed", failure); }
    }
    public static int count() { return PACKS.values().stream().mapToInt(List::size).sum(); }
    public static List<Pack> packs(Category type) { return Collections.unmodifiableList(PACKS.getOrDefault(type, List.of())); }
    public static Loaded active(Category type) { return preview != null && preview.pack.type == type ? preview : ACTIVE.get(type); }
    public static Loaded loadPreview(Pack pack) throws IOException { return new Loaded(pack); }
    public static void withPreview(Loaded candidate, Runnable render) {
        Loaded previous = preview;
        preview = candidate;
        try { render.run(); } finally { preview = previous; }
    }
    public static boolean selected(Pack pack) { return active(pack.type) != null && active(pack.type).pack.id == pack.id; }
    public static boolean selectId(Category type, int id, boolean save) {
        if (type == null) return false;
        if (id < 0) { Loaded old = ACTIVE.remove(type); if (old != null) old.close(); if (save) save(); return true; }
        for (Pack pack : packs(type)) if (pack.id == id) {
            try {
                Loaded replacement = new Loaded(pack);
                replacement.texture();
                Loaded old = ACTIVE.put(type, replacement);
                if (old != null) old.close();
                LOG.info("Equipped {} {}: {} ({})", type, id, pack.name, replacement.model == null ? "OBJ" : "MD5");
                if (save) save();
                return true;
            } catch (Exception failure) { LOG.error("Cannot equip {} {}", type, id, failure); return false; }
        }
        return false;
    }
    public static void disable(Category type, Throwable failure) {
        LOG.error("Mesh rendering failed for {}", type, failure);
        if (preview != null && preview.pack.type == type) return;
        selectId(type, -1, false);
    }
    public static void save() {
        Selection selection = new Selection();
        for (Category type : Category.values()) selection.selected.put(type, ACTIVE.containsKey(type) ? ACTIVE.get(type).pack.id : -1);
        selection.capeMotion = capeMotion; selection.petPose = petPose; selection.shieldOnArm = shieldOnArm;
        selection.shieldWithoutItem = shieldWithoutItem;
        selection.hatHeight = hatHeight;
        try {
            Files.createDirectories(SELECTION.getParent());
            Path pending = SELECTION.resolveSibling(SELECTION.getFileName() + ".tmp");
            try (Writer out = Files.newBufferedWriter(pending)) { JSON.toJson(selection, out); }
            Files.move(pending, SELECTION, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException failure) { LOG.warn("Cannot save cosmetic selection", failure); }
    }
    public static void close() { for (Loaded loaded : ACTIVE.values()) loaded.close(); ACTIVE.clear(); }
}
