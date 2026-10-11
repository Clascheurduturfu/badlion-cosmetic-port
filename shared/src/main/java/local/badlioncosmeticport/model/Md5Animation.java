package local.badlioncosmeticport.model;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** MD5 hierarchy reconstruction and global-pose interpolation observed in aGg/aGh. */
public final class Md5Animation {
    private static <T> java.util.List<T> immutable(java.util.Collection<T> values) { return java.util.Collections.unmodifiableList(new java.util.ArrayList<T>(values)); }

    public static final class Hierarchy {
        private final String name;
        private final int parent;
        private final int flags;
        private final int componentStart;
public Hierarchy(String name, int parent, int flags, int componentStart) {
this.name = name; this.parent = parent; this.flags = flags; this.componentStart = componentStart; }
        public String name() { return name; }
        public int parent() { return parent; }
        public int flags() { return flags; }
        public int componentStart() { return componentStart; }
public boolean equals(Object value) { if (this==value) return true; if (!(value instanceof Hierarchy)) return false; Hierarchy other=(Hierarchy)value; return java.util.Objects.equals(name,other.name) && parent==other.parent && flags==other.flags && componentStart==other.componentStart; }
public int hashCode() { return java.util.Objects.hash(name,parent,flags,componentStart); }
}
    public static final class Bounds {
        private final Md5Model.Vec3 minimum;
        private final Md5Model.Vec3 maximum;
public Bounds(Md5Model.Vec3 minimum, Md5Model.Vec3 maximum) {
this.minimum = minimum; this.maximum = maximum; }
        public Md5Model.Vec3 minimum() { return minimum; }
        public Md5Model.Vec3 maximum() { return maximum; }
public boolean equals(Object value) { if (this==value) return true; if (!(value instanceof Bounds)) return false; Bounds other=(Bounds)value; return java.util.Objects.equals(minimum,other.minimum) && java.util.Objects.equals(maximum,other.maximum); }
public int hashCode() { return java.util.Objects.hash(minimum,maximum); }
}
    private final int frameRate;
    private final List<Hierarchy> hierarchy;
    private final List<Bounds> bounds;
    private final List<Md5Model.Pose> frames;

    private Md5Animation(int frameRate, List<Hierarchy> hierarchy, List<Bounds> bounds,
                         List<Md5Model.Transform> base, float[][] components) {
        this.frameRate = frameRate;
        this.hierarchy = immutable(hierarchy);
        this.bounds = immutable(bounds);
        List<Md5Model.Pose> poses = new ArrayList<>(components.length);
        for (float[] frame : components) {
            List<Md5Model.Transform> joints = new ArrayList<>(hierarchy.size());
            for (int i = 0; i < hierarchy.size(); i++) {
                Hierarchy h = hierarchy.get(i);
                Md5Model.Transform b = base.get(i);
                float[] values = {b.position().x(), b.position().y(), b.position().z(),
                        b.rotation().x(), b.rotation().y(), b.rotation().z()};
                int source = h.componentStart;
                // Badlion assets use flags 63; honoring sparse flags additionally supports standard MD5 files.
                for (int bit = 0; bit < 6; bit++) if ((h.flags & (1 << bit)) != 0) values[bit] = frame[source++];
                Vector3f position = new Vector3f(values[0], values[1], values[2]);
                Quaternionf orientation = Md5Model.Rotation.fromXyz(new Md5Model.Vec3(values[3], values[4], values[5])).mutable();
                if (h.parent >= 0) {
                    Md5Model.Transform parent = joints.get(h.parent);
                    position.rotate(parent.rotation().mutable()).add(parent.position().mutable());
                    orientation = parent.rotation().mutable().mul(orientation).normalize();
                }
                joints.add(new Md5Model.Transform(Md5Model.Vec3.of(position), Md5Model.Rotation.of(orientation)));
            }
            poses.add(new Md5Model.Pose(joints));
        }
        frames = immutable(poses);
    }

    public int frameRate() { return frameRate; }
    public int frameCount() { return frames.size(); }
    public int jointCount() { return hierarchy.size(); }
    public double durationSeconds() { return (double) frameCount() / frameRate; }
    public List<Hierarchy> hierarchy() { return hierarchy; }
    public List<Bounds> bounds() { return bounds; }
    public Md5Model.Pose frame(int index) { return frames.get(index); }

    public void validateFor(Md5Model model) throws IOException {
        if (model.joints().size() != hierarchy.size()) throw new IOException("Animation/model joint counts differ");
        for (int i = 0; i < hierarchy.size(); i++) {
            Hierarchy animation = hierarchy.get(i);
            Md5Model.Joint geometry = model.joints().get(i);
            // Captured aGl skins by weight joint index. Some original clips rename or
            // swap left/right labels while retaining the same indexed hierarchy.
            if (animation.parent != geometry.parent()) {
                throw new IOException("Animation/model joint mismatch at " + i + ": " + animation.name);
            }
        }
    }

    public Md5Model.Pose sample(double seconds, boolean loop) { return sampleFrame(seconds * frameRate, loop); }

    /** Interpolates already reconstructed GLOBAL joints, matching Badlion's aGh interpolation. */
    public Md5Model.Pose sampleFrame(double framePosition, boolean loop) {
        if (!Double.isFinite(framePosition)) throw new IllegalArgumentException("Non-finite frame position");
        double position = loop ? positiveModulo(framePosition, frames.size())
                : Math.max(0, Math.min(frames.size() - 1, framePosition));
        int first = (int) Math.floor(position);
        int second = loop ? (first + 1) % frames.size() : Math.min(first + 1, frames.size() - 1);
        return interpolateFrames(first, second, (float) (position - first));
    }

    public Md5Model.Pose interpolateFrames(int first, int second, float fraction) {
        return Md5Model.Pose.interpolate(frames.get(first), frames.get(second), fraction);
    }

    private static double positiveModulo(double value, int divisor) {
        double result = value % divisor;
        return result < 0 ? result + divisor : result;
    }

    public static Md5Animation read(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { return read(reader); }
    }

    public static Md5Animation read(Reader reader) throws IOException {
        Md5Tokens t = new Md5Tokens(reader);
        int frameCount = -1, jointCount = -1, frameRate = -1, componentCount = -1;
        boolean version = false;
        List<Hierarchy> hierarchy = null;
        List<Bounds> bounds = null;
        List<Md5Model.Transform> base = null;
        float[][] components = null;
        int framesRead = 0;
        parsing: while (t.hasNext()) {
            String token = t.next();
            switch (token) {
                case "MD5Version": { if (version || t.integer() != 10) throw t.error("Expected MD5Version 10"); version = true;  break; }
                case "commandline": t.next(); break;
                case "numFrames": { if (frameCount != -1) throw t.error("Duplicate numFrames"); frameCount = t.count(16384, "frames");  break; }
                case "numJoints": { if (jointCount != -1) throw t.error("Duplicate numJoints"); jointCount = t.count(4096, "joints");  break; }
                case "frameRate": { if (frameRate != -1) throw t.error("Duplicate frameRate"); frameRate = t.count(1000, "frame rate");  break; }
                case "numAnimatedComponents": { if (componentCount != -1) throw t.error("Duplicate animated component count"); componentCount = t.count(24576, "components");  break; }
                case "hierarchy": {
                    if (hierarchy != null || jointCount < 1 || componentCount < 0) throw t.error("Missing or duplicate hierarchy declaration");
                    t.expect("{");
                    hierarchy = new ArrayList<>(jointCount);
                    for (int i = 0; i < jointCount; i++) {
                        String name = t.next();
                        int parent = t.integer(), flags = t.integer(), start = t.integer();
                        if (parent < -1 || parent >= i || flags < 0 || flags > 63 || start < 0
                                || (long) start + Integer.bitCount(flags) > componentCount) throw t.error("Invalid animation hierarchy range");
                        hierarchy.add(new Hierarchy(name, parent, flags, start));
                    }
                    t.expect("}");
                 break; }
                case "bounds": {
                    if (bounds != null || frameCount < 1) throw t.error("Missing or duplicate bounds declaration");
                    t.expect("{"); bounds = new ArrayList<>(frameCount);
                    // bone_wings_v2 declares 80 frames but includes 82 bounds. Captured aGd
                    // keeps only the declared count and ignores the extra export records.
                    int records = 0;
                    while (!t.peek().equals("}")) {
                        if (++records > 16384) throw t.error("Excessive bounds");
                        Bounds record = new Bounds(t.vector(), t.vector());
                        if (bounds.size() < frameCount) bounds.add(record);
                    }
                    if (bounds.size() != frameCount) throw t.error("Incomplete bounds");
                    t.expect("}");
                 break; }
                case "baseframe": {
                    if (base != null || jointCount < 1) throw t.error("Missing or duplicate base frame declaration");
                    t.expect("{"); base = new ArrayList<>(jointCount);
                    for (int i = 0; i < jointCount; i++) base.add(new Md5Model.Transform(t.vector(), Md5Model.Rotation.fromXyz(t.vector())));
                    t.expect("}");
                 break; }
                case "frame": {
                    if (frameCount < 1 || jointCount < 1 || componentCount < 0
                            || (long) frameCount * jointCount > 1_000_000 || (long) frameCount * componentCount > 6_000_000) {
                        throw t.error("Missing or excessive frame dimensions");
                    }
                    if (components == null) components = new float[frameCount][];
                    int index = t.count(frameCount - 1, "frame index");
                    if (components[index] != null) throw t.error("Duplicate frame");
                    t.expect("{"); float[] values = new float[componentCount];
                    for (int i = 0; i < componentCount; i++) values[i] = t.number();
                    t.expect("}"); components[index] = values; framesRead++;
                    // aGd returns after the declared frames, even if an exporter left a tail.
                    if (framesRead == frameCount && hierarchy != null && bounds != null && base != null) break parsing;
                 break; }
                default: throw t.error("Unexpected animation token: " + token);
            }
        }
        if (!version || hierarchy == null || bounds == null || base == null || frameRate < 1 || framesRead != frameCount) {
            throw t.error("Incomplete MD5 animation");
        }
        return new Md5Animation(frameRate, hierarchy, bounds, base, components);
    }
}
