package local.badlioncosmeticport.model;

import java.util.ArrayList;
import java.util.List;

/** The aCP/aCR procedural cloak path used when the catalog has no model name. */
public final class LegacyCloak {
    public enum Shape { DYNAMIC, CURVED, RECT }
    public static final class Vertex {
        private final float x;
        private final float y;
        private final float z;
        private final float u;
        private final float v;
public Vertex(float x, float y, float z, float u, float v) {
this.x = x; this.y = y; this.z = z; this.u = u; this.v = v; }
        public float x() { return x; }
        public float y() { return y; }
        public float z() { return z; }
        public float u() { return u; }
        public float v() { return v; }
public boolean equals(Object value) { if (this==value) return true; if (!(value instanceof Vertex)) return false; Vertex other=(Vertex)value; return Float.compare(x,other.x)==0 && Float.compare(y,other.y)==0 && Float.compare(z,other.z)==0 && Float.compare(u,other.u)==0 && Float.compare(v,other.v)==0; }
public int hashCode() { return java.util.Objects.hash(x,y,z,u,v); }
}
    public static final class Motion {
        private final float lift;
        private final float forward;
        private final float sideways;
        private final float bend;
        private final float sway;
        private final float particleLift;
        private final float particleLength;
        private final float wave;
public Motion(float lift, float forward, float sideways, float bend, float sway, float particleLift,
                         float particleLength, float wave) {
this.lift = lift; this.forward = forward; this.sideways = sideways; this.bend = bend; this.sway = sway; this.particleLift = particleLift; this.particleLength = particleLength; this.wave = wave; }
        public float lift() { return lift; }
        public float forward() { return forward; }
        public float sideways() { return sideways; }
        public float bend() { return bend; }
        public float sway() { return sway; }
        public float particleLift() { return particleLift; }
        public float particleLength() { return particleLength; }
        public float wave() { return wave; }
public boolean equals(Object value) { if (this==value) return true; if (!(value instanceof Motion)) return false; Motion other=(Motion)value; return Float.compare(lift,other.lift)==0 && Float.compare(forward,other.forward)==0 && Float.compare(sideways,other.sideways)==0 && Float.compare(bend,other.bend)==0 && Float.compare(sway,other.sway)==0 && Float.compare(particleLift,other.particleLift)==0 && Float.compare(particleLength,other.particleLength)==0 && Float.compare(wave,other.wave)==0; }
public int hashCode() { return java.util.Objects.hash(lift,forward,sideways,bend,sway,particleLift,particleLength,wave); }
}
    private static final float[] SIN = new float[65536];
    static { for (int i = 0; i < SIN.length; i++) SIN[i] = (float) Math.sin(i * Math.PI * 2 / SIN.length); }
    private LegacyCloak() {}
    private static double sin(double angle) { return SIN[(int) (angle * 10430.3779296875) & 65535]; }
    private static double cos(double angle) { return SIN[(int) (angle * 10430.3779296875 + 16384) & 65535]; }
    public static Motion motion(double lagX, double lagY, double lagZ, float bodyYaw,
                                float stride, double distance, Shape shape, float wave, boolean crouch) {
        double yaw = bodyYaw * (float) Math.PI / 180;
        double forwardX = sin(yaw), forwardZ = -cos(yaw);
        float vertical = clamp((float) lagY * 10, -6, 32);
        float forward = clamp((float) (lagX * forwardX + lagZ * forwardZ) * 100, 0, 165);
        float sideways = (float) (lagX * forwardZ - lagZ * forwardX) * 100;
        vertical += (float) sin(distance * 6) * (shape == Shape.DYNAMIC ? 16 : 32) * stride;
        if (crouch) { forward += 5; vertical += 25; }
        float bend = (forward - vertical) / 2 / (float) Math.PI - 1;
        float length = (-forward + vertical) / 4 / (float) Math.PI + 1;
        float amplitude = Math.min(forward + vertical, 90) / 55;
        float rootWave = (float) Math.sin(Math.PI * 1.5 + wave) * amplitude;
        return new Motion(vertical, forward, sideways, bend, Math.max(Math.min(0, vertical), -3),
                shape == Shape.CURVED ? 8 : 4 + bend * (shape == Shape.DYNAMIC ? -rootWave / 2 + 1 : 1),
                18 + length * (shape == Shape.DYNAMIC ? -rootWave / 2 + 1 : 1), amplitude);
    }
    private static float clamp(float value, float low, float high) { return Math.max(low, Math.min(high, value)); }
    /** Original ten-segment ribbon, dimensions 10 x 16 x 1 model units, original 22:23 UVs. */
    public static List<Vertex> strip(Shape shape, Motion motion, float phase) {
        List<Vertex> result = new ArrayList<>(98);
        float topV = 1f / 23, bottomV = 17f / 23;
        // Back face, bottom to top; top cap; right edge; bottom cap; front; left edge.
        int segments = shape == Shape.DYNAMIC ? 10 : shape == Shape.CURVED ? 6 : 1;
        for (int i = 0; i <= segments; i++) {
            float t = i / (float) segments, y = height(1 - t, shape, motion);
            float x = depth(1 - t, true, shape, motion, phase);
            float v = bottomV - (bottomV - topV) * (shape == Shape.DYNAMIC ? clamp(1 + y / 16, 0, 1) : t);
            result.add(new Vertex(x, y, 5, 1f / 22, v)); result.add(new Vertex(x, y, -5, 11f / 22, v));
        }
        result.add(new Vertex(1, 0, 5, 1f / 22, topV)); result.add(new Vertex(1, 0, -5, 11f / 22, topV));
        result.add(new Vertex(0, 0, 5, 1f / 22, 0)); result.add(new Vertex(0, 0, -5, 11f / 22, 0));
        for (int i = 0; i <= segments; i++) {
            float t = i / (float) segments, y = height(t, shape, motion), v = topV + (bottomV - topV) * (shape == Shape.DYNAMIC ? clamp(-y / 16, 0, 1) : t);
            result.add(new Vertex(depth(t, false, shape, motion, phase), y, -5, 12f / 22, v));
            result.add(new Vertex(depth(t, true, shape, motion, phase), y, -5, 11f / 22, v));
        }
        float offset = depth(1, false, shape, motion, phase);
        result.add(new Vertex(1 + offset, -16, -5, 21f / 22, 0)); result.add(new Vertex(1 + offset, -16, 5, 11f / 22, 0));
        result.add(new Vertex(offset, -16, -5, 21f / 22, topV)); result.add(new Vertex(offset, -16, 5, 11f / 22, topV));
        for (int i = 0; i <= segments; i++) {
            float t = i / (float) segments, y = height(1 - t, shape, motion);
            float v = bottomV - (bottomV - topV) * (shape == Shape.DYNAMIC ? clamp(1 + y / 16, 0, 1) : t);
            float x = depth(1 - t, false, shape, motion, phase);
            result.add(new Vertex(x, y, -5, 12f / 22, v)); result.add(new Vertex(x, y, 5, 1, v));
        }
        for (int i = 0; i <= segments; i++) {
            float t = i / (float) segments, y = height(t, shape, motion), v = topV + (bottomV - topV) * (shape == Shape.DYNAMIC ? clamp(-y / 16, 0, 1) : t);
            if (i == 0) { result.add(new Vertex(0, y, 5, 0, v)); result.add(new Vertex(0, y, 5, 0, v)); }
            result.add(new Vertex(depth(t, true, shape, motion, phase), y, 5, 1f / 22, v));
            result.add(new Vertex(depth(t, false, shape, motion, phase), y, 5, 0, v));
        }
        return result;
    }
    private static float height(float t, Shape shape, Motion motion) {
        if (shape == Shape.RECT) return -16 * t;
        // aCR quadratic Bezier uses y=-10 at the control point and y=-16 at the hem.
        return -20 * (1 - t) * t - 16 * t * t;
    }
    private static float depth(float t, boolean back, Shape shape, Motion motion, float phase) {
        if (shape == Shape.DYNAMIC) return (float) Math.sin((1 - t) * Math.PI * 1.5 + phase) * motion.wave
                + (back ? 1 : 0) - (float) Math.sin(Math.PI * 1.5 + phase) * motion.wave;
        if (shape == Shape.RECT) return back ? 1 : 0;
        return 5 * t * t + (back ? 1 : 0) * (1 - 2 * (1 - t) * t);
    }
}
