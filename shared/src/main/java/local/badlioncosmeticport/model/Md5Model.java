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
import java.util.Objects;

/**
 * Immutable MD5Version 10 geometry in original asset units. No Minecraft state or GL calls.
 * Skinning and quaternion conventions follow the captured Badlion aGk/aGl renderer.
 */
public final class Md5Model {
    private static <T> java.util.List<T> immutable(java.util.Collection<T> values) { return java.util.Collections.unmodifiableList(new java.util.ArrayList<T>(values)); }

    public static final class Vec3 {
        private final float x;
        private final float y;
        private final float z;
public Vec3(float x, float y, float z) {
            if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
                throw new IllegalArgumentException("Non-finite vector");
            }
        
this.x = x; this.y = y; this.z = z; }
        public float x() { return x; }
        public float y() { return y; }
        public float z() { return z; }
public boolean equals(Object value) { if (this==value) return true; if (!(value instanceof Vec3)) return false; Vec3 other=(Vec3)value; return Float.compare(x,other.x)==0 && Float.compare(y,other.y)==0 && Float.compare(z,other.z)==0; }
public int hashCode() { return java.util.Objects.hash(x,y,z); }

        
        Vector3f mutable() { return new Vector3f(x, y, z); }
        static Vec3 of(Vector3f value) { return new Vec3(value.x, value.y, value.z); }
    }

    public static final class Rotation {
        private final float x;
        private final float y;
        private final float z;
        private final float w;
public Rotation(float x, float y, float z, float w) {
            if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z) || !Float.isFinite(w)) {
                throw new IllegalArgumentException("Non-finite quaternion");
            }
        
this.x = x; this.y = y; this.z = z; this.w = w; }
        public float x() { return x; }
        public float y() { return y; }
        public float z() { return z; }
        public float w() { return w; }
public boolean equals(Object value) { if (this==value) return true; if (!(value instanceof Rotation)) return false; Rotation other=(Rotation)value; return Float.compare(x,other.x)==0 && Float.compare(y,other.y)==0 && Float.compare(z,other.z)==0 && Float.compare(w,other.w)==0; }
public int hashCode() { return java.util.Objects.hash(x,y,z,w); }

        
        /** MD5 stores xyz only; Badlion reconstructs the negative square root for w. */
        public static Rotation fromXyz(Vec3 xyz) {
            float remainder = 1 - xyz.x * xyz.x - xyz.y * xyz.y - xyz.z * xyz.z;
            return new Rotation(xyz.x, xyz.y, xyz.z, remainder < 0 ? 0 : (float) -Math.sqrt(remainder));
        }
        Quaternionf mutable() { return new Quaternionf(x, y, z, w); }
        static Rotation of(Quaternionf value) { return new Rotation(value.x, value.y, value.z, value.w); }
    }

    public static final class Joint {
        private final String name;
        private final int parent;
        private final Vec3 position;
        private final Rotation rotation;
public Joint(String name, int parent, Vec3 position, Rotation rotation) { Objects.requireNonNull(name); Objects.requireNonNull(position); Objects.requireNonNull(rotation); 
this.name = name; this.parent = parent; this.position = position; this.rotation = rotation; }
        public String name() { return name; }
        public int parent() { return parent; }
        public Vec3 position() { return position; }
        public Rotation rotation() { return rotation; }
public boolean equals(Object value) { if (this==value) return true; if (!(value instanceof Joint)) return false; Joint other=(Joint)value; return java.util.Objects.equals(name,other.name) && parent==other.parent && java.util.Objects.equals(position,other.position) && java.util.Objects.equals(rotation,other.rotation); }
public int hashCode() { return java.util.Objects.hash(name,parent,position,rotation); }

        
    }
    public static final class Transform {
        private final Vec3 position;
        private final Rotation rotation;
public Transform(Vec3 position, Rotation rotation) { Objects.requireNonNull(position); Objects.requireNonNull(rotation); 
this.position = position; this.rotation = rotation; }
        public Vec3 position() { return position; }
        public Rotation rotation() { return rotation; }
public boolean equals(Object value) { if (this==value) return true; if (!(value instanceof Transform)) return false; Transform other=(Transform)value; return java.util.Objects.equals(position,other.position) && java.util.Objects.equals(rotation,other.rotation); }
public int hashCode() { return java.util.Objects.hash(position,rotation); }

        
    }
    /** Joint transforms are GLOBAL, not parent-local. */
    public static final class Pose {
        private final List<Transform> joints;
public Pose(List<Transform> joints) { joints = immutable(joints); 
this.joints = joints; }
        public List<Transform> joints() { return joints; }
public boolean equals(Object value) { if (this==value) return true; if (!(value instanceof Pose)) return false; Pose other=(Pose)value; return java.util.Objects.equals(joints,other.joints); }
public int hashCode() { return java.util.Objects.hash(joints); }

        
        public static Pose interpolate(Pose from, Pose to, float fraction) {
            if (!Float.isFinite(fraction) || fraction < 0 || fraction > 1) throw new IllegalArgumentException("Invalid interpolation fraction");
            if (from.joints.size() != to.joints.size()) throw new IllegalArgumentException("Pose joint counts differ");
            List<Transform> result = new ArrayList<>(from.joints.size());
            for (int i = 0; i < from.joints.size(); i++) {
                Transform a = from.joints.get(i), b = to.joints.get(i);
                result.add(new Transform(Vec3.of(a.position.mutable().lerp(b.position.mutable(), fraction)),
                        Rotation.of(a.rotation.mutable().slerp(b.rotation.mutable(), fraction))));
            }
            return new Pose(result);
        }
    }
    public static final class Vertex {
        private final float u;
        private final float v;
        private final int firstWeight;
        private final int weightCount;
public Vertex(float u, float v, int firstWeight, int weightCount) {
            if (!Float.isFinite(u) || !Float.isFinite(v) || firstWeight < 0 || weightCount < 0) {
                throw new IllegalArgumentException("Invalid vertex");
            }
        
this.u = u; this.v = v; this.firstWeight = firstWeight; this.weightCount = weightCount; }
        public float u() { return u; }
        public float v() { return v; }
        public int firstWeight() { return firstWeight; }
        public int weightCount() { return weightCount; }
public boolean equals(Object value) { if (this==value) return true; if (!(value instanceof Vertex)) return false; Vertex other=(Vertex)value; return Float.compare(u,other.u)==0 && Float.compare(v,other.v)==0 && firstWeight==other.firstWeight && weightCount==other.weightCount; }
public int hashCode() { return java.util.Objects.hash(u,v,firstWeight,weightCount); }

        
    }
    public static final class Triangle {
        private final int a;
        private final int b;
        private final int c;
public Triangle(int a, int b, int c) {
this.a = a; this.b = b; this.c = c; }
        public int a() { return a; }
        public int b() { return b; }
        public int c() { return c; }
public boolean equals(Object value) { if (this==value) return true; if (!(value instanceof Triangle)) return false; Triangle other=(Triangle)value; return a==other.a && b==other.b && c==other.c; }
public int hashCode() { return java.util.Objects.hash(a,b,c); }
}
    public static final class Weight {
        private final int joint;
        private final float bias;
        private final Vec3 position;
public Weight(int joint, float bias, Vec3 position) {
            Objects.requireNonNull(position);
            if (joint < 0 || !Float.isFinite(bias) || bias < 0 || bias > 1.001f) {
                throw new IllegalArgumentException("Invalid weight");
            }
        
this.joint = joint; this.bias = bias; this.position = position; }
        public int joint() { return joint; }
        public float bias() { return bias; }
        public Vec3 position() { return position; }
public boolean equals(Object value) { if (this==value) return true; if (!(value instanceof Weight)) return false; Weight other=(Weight)value; return joint==other.joint && Float.compare(bias,other.bias)==0 && java.util.Objects.equals(position,other.position); }
public int hashCode() { return java.util.Objects.hash(joint,bias,position); }

        
    }
    public static final class Mesh {
        private final String shader;
        private final List<Vertex> vertices;
        private final List<Triangle> triangles;
        private final List<Weight> weights;
public Mesh(String shader, List<Vertex> vertices, List<Triangle> triangles, List<Weight> weights) {
            Objects.requireNonNull(shader);
            vertices = immutable(vertices); triangles = immutable(triangles); weights = immutable(weights);
        
this.shader = shader; this.vertices = vertices; this.triangles = triangles; this.weights = weights; }
        public String shader() { return shader; }
        public List<Vertex> vertices() { return vertices; }
        public List<Triangle> triangles() { return triangles; }
        public List<Weight> weights() { return weights; }
public boolean equals(Object value) { if (this==value) return true; if (!(value instanceof Mesh)) return false; Mesh other=(Mesh)value; return java.util.Objects.equals(shader,other.shader) && java.util.Objects.equals(vertices,other.vertices) && java.util.Objects.equals(triangles,other.triangles) && java.util.Objects.equals(weights,other.weights); }
public int hashCode() { return java.util.Objects.hash(shader,vertices,triangles,weights); }

        
    }
    public static final class SkinnedVertex {
        private final Vec3 position;
        private final float u;
        private final float v;
        private final Vec3 normal;
public SkinnedVertex(Vec3 position, float u, float v, Vec3 normal) {
this.position = position; this.u = u; this.v = v; this.normal = normal; }
        public Vec3 position() { return position; }
        public float u() { return u; }
        public float v() { return v; }
        public Vec3 normal() { return normal; }
public boolean equals(Object value) { if (this==value) return true; if (!(value instanceof SkinnedVertex)) return false; SkinnedVertex other=(SkinnedVertex)value; return java.util.Objects.equals(position,other.position) && Float.compare(u,other.u)==0 && Float.compare(v,other.v)==0 && java.util.Objects.equals(normal,other.normal); }
public int hashCode() { return java.util.Objects.hash(position,u,v,normal); }

        public float x() { return position.x; }
        public float y() { return position.y; }
        public float z() { return position.z; }
        public float nx() { return normal.x; }
        public float ny() { return normal.y; }
        public float nz() { return normal.z; }
    }
    public static final class SkinnedMesh {
        private final String shader;
        private final List<SkinnedVertex> vertices;
        private final List<Triangle> triangles;
public SkinnedMesh(String shader, List<SkinnedVertex> vertices, List<Triangle> triangles) { vertices = immutable(vertices); triangles = immutable(triangles); 
this.shader = shader; this.vertices = vertices; this.triangles = triangles; }
        public String shader() { return shader; }
        public List<SkinnedVertex> vertices() { return vertices; }
        public List<Triangle> triangles() { return triangles; }
public boolean equals(Object value) { if (this==value) return true; if (!(value instanceof SkinnedMesh)) return false; SkinnedMesh other=(SkinnedMesh)value; return java.util.Objects.equals(shader,other.shader) && java.util.Objects.equals(vertices,other.vertices) && java.util.Objects.equals(triangles,other.triangles); }
public int hashCode() { return java.util.Objects.hash(shader,vertices,triangles); }

        
        public String material() { return shader; }
    }
    public enum Normals { BADLION_CPU, GEOMETRIC }

    private final List<Joint> joints;
    private final List<Mesh> meshes;
    private final Pose restPose;
    private final List<List<Vec3>> localNormals;

    private Md5Model(List<Joint> joints, List<Mesh> meshes) throws IOException {
        this.joints = immutable(joints);
        this.meshes = immutable(meshes);
        List<Transform> transforms = joints.stream().map(j -> new Transform(j.position, j.rotation)).collect(java.util.stream.Collectors.toList());
        restPose = new Pose(transforms);
        List<List<Vec3>> normalParts = new ArrayList<>();
        for (Mesh mesh : meshes) {
            validateMesh(mesh, joints.size());
            List<Vec3> positions = positions(mesh, restPose);
            List<Vec3> faceNormals = geometricNormals(mesh, positions);
            List<Vec3> normals = new ArrayList<>();
            for (int i = 0; i < mesh.vertices.size(); i++) {
                Vertex vertex = mesh.vertices.get(i);
                Vector3f local = new Vector3f();
                for (int w = 0; w < vertex.weightCount; w++) {
                    Weight weight = mesh.weights.get(vertex.firstWeight + w);
                    // Captured aGl rotates by the bind quaternion here (not its inverse).
                    local.add(faceNormals.get(i).mutable().rotate(joints.get(weight.joint).rotation.mutable()).mul(weight.bias));
                }
                normals.add(Vec3.of(local));
            }
            normalParts.add(immutable(normals));
        }
        localNormals = immutable(normalParts);
    }

    public List<Joint> joints() { return joints; }
    public List<Mesh> meshes() { return meshes; }
    public Pose restPose() { return restPose; }

    public static Md5Model read(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { return read(reader); }
    }

    /** Does not close a caller-owned Reader. */
    public static Md5Model read(Reader reader) throws IOException {
        Md5Tokens tokens = new Md5Tokens(reader);
        int jointCount = -1, meshCount = -1;
        boolean version = false;
        List<Joint> joints = null;
        List<Mesh> meshes = new ArrayList<>();
        while (tokens.hasNext()) {
            String token = tokens.next();
            switch (token) {
                case "MD5Version": { if (version || tokens.integer() != 10) throw tokens.error("Expected MD5Version 10"); version = true;  break; }
                case "commandline": tokens.next(); break;
                case "numJoints": { if (jointCount != -1) throw tokens.error("Duplicate numJoints"); jointCount = tokens.count(4096, "numJoints");  break; }
                case "numMeshes": { if (meshCount != -1) throw tokens.error("Duplicate numMeshes"); meshCount = tokens.count(4096, "numMeshes");  break; }
                case "joints": {
                    if (joints != null || jointCount < 1) throw tokens.error("Missing or duplicate joint declaration");
                    tokens.expect("{");
                    joints = new ArrayList<>(jointCount);
                    for (int i = 0; i < jointCount; i++) {
                        String name = tokens.next();
                        int parent = tokens.integer();
                        if (parent < -1 || parent >= i) throw tokens.error("Joint parent must precede child");
                        joints.add(new Joint(name, parent, tokens.vector(), Rotation.fromXyz(tokens.vector())));
                    }
                    tokens.expect("}");
                 break; }
                case "mesh": {
                    if (meshCount < 0 || meshes.size() >= meshCount) throw tokens.error("Unexpected mesh");
                    meshes.add(readMesh(tokens));
                 break; }
                default: throw tokens.error("Unexpected mesh token: " + token);
            }
        }
        if (!version || joints == null || meshes.size() != meshCount) throw tokens.error("Incomplete MD5 mesh");
        return new Md5Model(joints, meshes);
    }

    private static Mesh readMesh(Md5Tokens t) throws IOException {
        t.expect("{");
        String shader = "";
        List<Vertex> vertices = null;
        List<Triangle> triangles = null;
        List<Weight> weights = null;
        for (String token = t.next(); !token.equals("}"); token = t.next()) {
            switch (token) {
                case "shader": shader = t.next(); break;
                case "numverts": {
                    if (vertices != null) throw t.error("Duplicate vertices");
                    int count = t.count(1_000_000, "vertices");
                    vertices = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        t.expect("vert"); if (t.integer() != i) throw t.error("Vertex indices must be sequential");
                        t.expect("("); float u = t.number(), v = t.number(); t.expect(")");
                        vertices.add(new Vertex(u, v, t.count(2_000_000, "first weight"), t.count(4096, "weight count")));
                    }
                 break; }
                case "numtris": {
                    if (triangles != null) throw t.error("Duplicate triangles");
                    int count = t.count(2_000_000, "triangles");
                    triangles = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        t.expect("tri"); if (t.integer() != i) throw t.error("Triangle indices must be sequential");
                        triangles.add(new Triangle(t.integer(), t.integer(), t.integer()));
                    }
                 break; }
                case "numweights": {
                    if (weights != null) throw t.error("Duplicate weights");
                    int count = t.count(2_000_000, "weights");
                    weights = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        t.expect("weight"); if (t.integer() != i) throw t.error("Weight indices must be sequential");
                        int joint = t.count(4095, "weight joint"); float bias = t.number();
                        if (bias < 0 || bias > 1.001f) throw t.error("Invalid weight bias");
                        weights.add(new Weight(joint, bias, t.vector()));
                    }
                 break; }
                default: throw t.error("Unexpected mesh block token: " + token);
            }
        }
        if (vertices == null || triangles == null || weights == null) throw t.error("Incomplete mesh block");
        return new Mesh(shader, vertices, triangles, weights);
    }

    private static void validateMesh(Mesh mesh, int jointCount) throws IOException {
        for (Weight weight : mesh.weights) if (weight.joint >= jointCount) throw new IOException("MD5 weight joint out of range");
        for (Triangle triangle : mesh.triangles) {
            if (triangle.a < 0 || triangle.b < 0 || triangle.c < 0 || triangle.a >= mesh.vertices.size()
                    || triangle.b >= mesh.vertices.size() || triangle.c >= mesh.vertices.size()) throw new IOException("MD5 triangle vertex out of range");
        }
        for (Vertex vertex : mesh.vertices) {
            if ((long) vertex.firstWeight + vertex.weightCount > mesh.weights.size()) throw new IOException("MD5 vertex weight range out of bounds");
            float bias = 0;
            for (int w = 0; w < vertex.weightCount; w++) bias += mesh.weights.get(vertex.firstWeight + w).bias;
            if (vertex.weightCount == 0 || Math.abs(bias - 1) > 0.02f) throw new IOException("MD5 vertex weights do not sum to one");
        }
    }

    /** Original CPU normal path, retaining every weight; no four/eight-weight truncation. */
    public List<SkinnedMesh> skin(Pose pose) { return skin(pose, Normals.BADLION_CPU); }

    public List<SkinnedMesh> skin(Pose pose, Normals normals) {
        Objects.requireNonNull(pose); Objects.requireNonNull(normals);
        if (pose.joints.size() != joints.size()) throw new IllegalArgumentException("Pose/model joint counts differ");
        List<SkinnedMesh> result = new ArrayList<>(meshes.size());
        for (int m = 0; m < meshes.size(); m++) {
            Mesh mesh = meshes.get(m);
            List<Vec3> positions = positions(mesh, pose);
            List<Vec3> geometric = normals == Normals.GEOMETRIC ? geometricNormals(mesh, positions) : null;
            List<SkinnedVertex> vertices = new ArrayList<>(mesh.vertices.size());
            for (int i = 0; i < mesh.vertices.size(); i++) {
                Vertex vertex = mesh.vertices.get(i);
                Vec3 normal;
                if (geometric != null) normal = geometric.get(i);
                else {
                    Vector3f sum = new Vector3f();
                    for (int w = 0; w < vertex.weightCount; w++) {
                        Weight weight = mesh.weights.get(vertex.firstWeight + w);
                        sum.add(localNormals.get(m).get(i).mutable().rotate(pose.joints.get(weight.joint).rotation.mutable()).mul(weight.bias));
                    }
                    normal = Vec3.of(sum);
                }
                vertices.add(new SkinnedVertex(positions.get(i), vertex.u, vertex.v, normal));
            }
            result.add(new SkinnedMesh(mesh.shader, vertices, mesh.triangles));
        }
        return immutable(result);
    }

    private static List<Vec3> positions(Mesh mesh, Pose pose) {
        List<Vec3> result = new ArrayList<>(mesh.vertices.size());
        for (Vertex vertex : mesh.vertices) {
            Vector3f position = new Vector3f();
            for (int w = 0; w < vertex.weightCount; w++) {
                Weight weight = mesh.weights.get(vertex.firstWeight + w);
                Transform joint = pose.joints.get(weight.joint);
                position.add(weight.position.mutable().rotate(joint.rotation.mutable()).add(joint.position.mutable()).mul(weight.bias));
            }
            result.add(Vec3.of(position));
        }
        return immutable(result);
    }

    private static List<Vec3> geometricNormals(Mesh mesh, List<Vec3> positions) {
        Vector3f[] normals = new Vector3f[positions.size()];
        for (int i = 0; i < normals.length; i++) normals[i] = new Vector3f();
        for (Triangle tri : mesh.triangles) {
            Vector3f a = positions.get(tri.a).mutable();
            Vector3f face = positions.get(tri.c).mutable().sub(a).cross(positions.get(tri.b).mutable().sub(a));
            normals[tri.a].add(face); normals[tri.b].add(face); normals[tri.c].add(face);
        }
        List<Vec3> result = new ArrayList<>(normals.length);
        for (Vector3f normal : normals) {
            // Isolated/degenerate triangles must not produce NaN normals.
            if (normal.lengthSquared() > 1.0e-20f) normal.normalize();
            result.add(Vec3.of(normal));
        }
        return immutable(result);
    }
}
