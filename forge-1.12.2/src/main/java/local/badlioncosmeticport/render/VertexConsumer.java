package local.badlioncosmeticport.render;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.joml.Matrix4f;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

/** Batched legacy vertex adapter; world/player matrices are still owned by Minecraft. */
public final class VertexConsumer implements AutoCloseable {
    private final BufferBuilder out = Tessellator.getInstance().getBuffer();
    private final Vector3f position = new Vector3f(), normal = new Vector3f();
    private final Matrix3f normals = new Matrix3f();
    private float u,v;
    private int red=255,green=255,blue=255,alpha=255;
    public VertexConsumer() { out.begin(GL11.GL_QUADS,DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL); }
    public VertexConsumer vertex(Matrix4f matrix,float x,float y,float z) { matrix.transformPosition(position.set(x,y,z)); return this; }
    public VertexConsumer color(int r,int g,int b,int a) { red=r;green=g;blue=b;alpha=a;return this; }
    public VertexConsumer texture(float u,float v) { this.u=u;this.v=v;return this; }
    public VertexConsumer overlay(int ignored) { return this; }
    public VertexConsumer light(int ignored) { return this; }
    public void normal(Matrix4f matrix,float x,float y,float z) {
        matrix.normal(normals).transform(normal.set(x,y,z)).normalize();
        out.pos(position.x,position.y,position.z).tex(u,v).color(red,green,blue,alpha).normal(normal.x,normal.y,normal.z).endVertex();
    }
    public void close() { Tessellator.getInstance().draw(); }
}
