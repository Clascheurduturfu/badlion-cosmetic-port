package local.badlioncosmeticport.render;

import java.util.ArrayDeque;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import net.minecraft.client.model.ModelRenderer;

/** CPU matrix stack matching the modern adapter's post-multiplication order. */
public final class MatrixStack {
    private final ArrayDeque<Matrix4f> stack = new ArrayDeque<>();
    public MatrixStack() { stack.push(new Matrix4f()); }
    public void push() { stack.push(new Matrix4f(stack.peek())); }
    public void pop() { stack.pop(); }
    public Matrix4f peek() { return stack.peek(); }
    public void translate(double x, double y, double z) { peek().translate((float)x, (float)y, (float)z); }
    public void scale(float x, float y, float z) { peek().scale(x,y,z); }
    public void multiply(Quaternionf rotation) { peek().rotate(rotation); }
    public void part(ModelRenderer part) {
        translate(part.offsetX,part.offsetY,part.offsetZ);
        translate(part.rotationPointX/16f,part.rotationPointY/16f,part.rotationPointZ/16f);
        peek().rotateZ(part.rotateAngleZ).rotateY(part.rotateAngleY).rotateX(part.rotateAngleX);
    }
}
