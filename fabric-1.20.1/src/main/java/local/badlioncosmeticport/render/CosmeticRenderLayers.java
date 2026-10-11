package local.badlioncosmeticport.render;

import local.badlioncosmeticport.MeshCosmetics;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import java.util.*;
import org.slf4j.LoggerFactory;

public final class CosmeticRenderLayers extends RenderLayer {
    private CosmeticRenderLayers() { super("unused", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 1536, false, false, () -> {}, () -> {}); }
    private static net.minecraft.client.gl.ShaderProgram shield, bandana;
    private static final long EPOCH = System.nanoTime();
    private record Key(Identifier texture, MeshCosmetics.Category category) {}
    private static final Map<Key, RenderLayer> LAYERS = new LinkedHashMap<>();
    public static long effectDraws;
    public static void register() {
        CoreShaderRegistrationCallback.EVENT.register(context -> {
            context.register(Identifier.of("badlioncosmeticport", "shield_fx"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, program -> {
                shield = program; configure(program, true); LAYERS.clear();
                LoggerFactory.getLogger("BadlionCosmeticPort").info("BCP_SHADER_READY shieldFX");
            });
            context.register(Identifier.of("badlioncosmeticport", "bandana_fx"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, program -> {
                bandana = program; configure(program, false); LAYERS.clear();
                LoggerFactory.getLogger("BadlionCosmeticPort").info("BCP_SHADER_READY bandanaFX");
            });
        });
    }
    private static void configure(net.minecraft.client.gl.ShaderProgram program, boolean isShield) {
        Objects.requireNonNull(program.getUniform("shieldFX")).set(isShield ? 1 : 0);
        Objects.requireNonNull(program.getUniform("bandanaFX")).set(isShield ? 0 : 1);
    }
    public static RenderLayer layer(MeshCosmetics.Pack pack, Identifier texture) {
        boolean special = pack.type == MeshCosmetics.Category.SHIELD && pack.id == 250 || pack.type == MeshCosmetics.Category.BANDANA && pack.id == 345;
        if (!special || shield == null || bandana == null) return RenderLayer.getEntityTranslucent(texture);
        effectDraws++;
        Key key = new Key(texture, pack.type);
        if (!LAYERS.containsKey(key) && LAYERS.size() >= 64) LAYERS.remove(LAYERS.keySet().iterator().next());
        return LAYERS.computeIfAbsent(key, ignored -> {
            RenderPhase.ShaderProgram phase = new RenderPhase.ShaderProgram(() -> {
                var program = pack.type == MeshCosmetics.Category.SHIELD ? shield : bandana;
                Objects.requireNonNull(program.getUniform("iTime")).set((float) ((System.nanoTime() - EPOCH) / 1e9));
                return program;
            });
            return RenderLayer.of("badlion_cosmetic_fx", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 1536, true, true,
                    RenderLayer.MultiPhaseParameters.builder().program(phase).texture(new RenderPhase.Texture(texture, false, false))
                            .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY).cull(RenderPhase.DISABLE_CULLING)
                            .lightmap(RenderPhase.ENABLE_LIGHTMAP).overlay(RenderPhase.ENABLE_OVERLAY_COLOR).build(false));
        });
    }
}
