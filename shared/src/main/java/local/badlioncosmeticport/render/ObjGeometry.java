package local.badlioncosmeticport.render;

import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** CPU-only OBJ geometry. UV orientation and model transforms remain renderer decisions. */
public final class ObjGeometry {
    private static <T> java.util.List<T> immutable(java.util.Collection<T> values) { return java.util.Collections.unmodifiableList(new java.util.ArrayList<T>(values)); }

    private static final long MAX_BYTES = 32L * 1024 * 1024;
    private static final int MAX_ATTRIBUTES = 250_000;
    private static final int MAX_TRIANGLES = 500_000;
    private static final int MAX_FACE_VERTICES = 256;
    private static final int MAX_LINE_LENGTH = 32_768;
    private static final float MAX_COMPONENT = 1_000_000;

    private ObjGeometry() {}

    public static final class Vertex {
        private final float x;
        private final float y;
        private final float z;
        private final float u;
        private final float v;
        private final float nx;
        private final float ny;
        private final float nz;
public Vertex(float x, float y, float z, float u, float v,
                         float nx, float ny, float nz) {
this.x = x; this.y = y; this.z = z; this.u = u; this.v = v; this.nx = nx; this.ny = ny; this.nz = nz; }
        public float x() { return x; }
        public float y() { return y; }
        public float z() { return z; }
        public float u() { return u; }
        public float v() { return v; }
        public float nx() { return nx; }
        public float ny() { return ny; }
        public float nz() { return nz; }
public boolean equals(Object value) { if (this==value) return true; if (!(value instanceof Vertex)) return false; Vertex other=(Vertex)value; return Float.compare(x,other.x)==0 && Float.compare(y,other.y)==0 && Float.compare(z,other.z)==0 && Float.compare(u,other.u)==0 && Float.compare(v,other.v)==0 && Float.compare(nx,other.nx)==0 && Float.compare(ny,other.ny)==0 && Float.compare(nz,other.nz)==0; }
public int hashCode() { return java.util.Objects.hash(x,y,z,u,v,nx,ny,nz); }
}

    public static final class Triangle {
        private final Vertex a;
        private final Vertex b;
        private final Vertex c;
public Triangle(Vertex a, Vertex b, Vertex c) {
this.a = a; this.b = b; this.c = c; }
        public Vertex a() { return a; }
        public Vertex b() { return b; }
        public Vertex c() { return c; }
public boolean equals(Object value) { if (this==value) return true; if (!(value instanceof Triangle)) return false; Triangle other=(Triangle)value; return java.util.Objects.equals(a,other.a) && java.util.Objects.equals(b,other.b) && java.util.Objects.equals(c,other.c); }
public int hashCode() { return java.util.Objects.hash(a,b,c); }
}

    /** Consecutive triangle range with its OBJ object, group and material labels. */
    public static final class Part {
        private final String objectName;
        private final String groupName;
        private final String materialName;
        private final int firstTriangle;
        private final int triangleCount;
public Part(String objectName, String groupName, String materialName,
                       int firstTriangle, int triangleCount) {
this.objectName = objectName; this.groupName = groupName; this.materialName = materialName; this.firstTriangle = firstTriangle; this.triangleCount = triangleCount; }
        public String objectName() { return objectName; }
        public String groupName() { return groupName; }
        public String materialName() { return materialName; }
        public int firstTriangle() { return firstTriangle; }
        public int triangleCount() { return triangleCount; }
public boolean equals(Object value) { if (this==value) return true; if (!(value instanceof Part)) return false; Part other=(Part)value; return java.util.Objects.equals(objectName,other.objectName) && java.util.Objects.equals(groupName,other.groupName) && java.util.Objects.equals(materialName,other.materialName) && firstTriangle==other.firstTriangle && triangleCount==other.triangleCount; }
public int hashCode() { return java.util.Objects.hash(objectName,groupName,materialName,firstTriangle,triangleCount); }
}

    public static final class Bounds {
        private final float minX;
        private final float minY;
        private final float minZ;
        private final float maxX;
        private final float maxY;
        private final float maxZ;
public Bounds(float minX, float minY, float minZ,
                         float maxX, float maxY, float maxZ) {
this.minX = minX; this.minY = minY; this.minZ = minZ; this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ; }
        public float minX() { return minX; }
        public float minY() { return minY; }
        public float minZ() { return minZ; }
        public float maxX() { return maxX; }
        public float maxY() { return maxY; }
        public float maxZ() { return maxZ; }
public boolean equals(Object value) { if (this==value) return true; if (!(value instanceof Bounds)) return false; Bounds other=(Bounds)value; return Float.compare(minX,other.minX)==0 && Float.compare(minY,other.minY)==0 && Float.compare(minZ,other.minZ)==0 && Float.compare(maxX,other.maxX)==0 && Float.compare(maxY,other.maxY)==0 && Float.compare(maxZ,other.maxZ)==0; }
public int hashCode() { return java.util.Objects.hash(minX,minY,minZ,maxX,maxY,maxZ); }
}

    public static final class Mesh {
        private final List<Triangle> triangles;
        private final List<Part> parts;
        private final Bounds bounds;
public Mesh(List<Triangle> triangles, List<Part> parts, Bounds bounds) {
            triangles = immutable(triangles);
            parts = immutable(parts);
        
this.triangles = triangles; this.parts = parts; this.bounds = bounds; }
        public List<Triangle> triangles() { return triangles; }
        public List<Part> parts() { return parts; }
        public Bounds bounds() { return bounds; }
public boolean equals(Object value) { if (this==value) return true; if (!(value instanceof Mesh)) return false; Mesh other=(Mesh)value; return java.util.Objects.equals(triangles,other.triangles) && java.util.Objects.equals(parts,other.parts) && java.util.Objects.equals(bounds,other.bounds); }
public int hashCode() { return java.util.Objects.hash(triangles,parts,bounds); }

        
    }

    public static Mesh read(Path path) throws IOException {
        if (Files.size(path) > MAX_BYTES) throw new IOException("OBJ exceeds byte limit");
        try (InputStream stream = Files.newInputStream(path)) {
            return read(stream);
        }
    }

    /** Reads without closing the caller-owned stream. Material-library files are never opened. */
    public static Mesh read(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new LimitedStream(stream), StandardCharsets.UTF_8));
        return new Parser().read(reader);
    }

    private static final class Index {
        private final int position;
        private final int uv;
        private final int normal;
private Index(int position, int uv, int normal) {
this.position = position; this.uv = uv; this.normal = normal; }
        public int position() { return position; }
        public int uv() { return uv; }
        public int normal() { return normal; }
public boolean equals(Object value) { if (this==value) return true; if (!(value instanceof Index)) return false; Index other=(Index)value; return position==other.position && uv==other.uv && normal==other.normal; }
public int hashCode() { return java.util.Objects.hash(position,uv,normal); }
}

    private static final class Parser {
        private final List<float[]> positions = new ArrayList<>();
        private final List<float[]> uvs = new ArrayList<>();
        private final List<float[]> normals = new ArrayList<>();
        private final List<Triangle> triangles = new ArrayList<>();
        private final List<Part> parts = new ArrayList<>();
        private String object = "", group = "", material = "";
        private int partStart, lineNumber;
        private float minX = Float.POSITIVE_INFINITY, minY = minX, minZ = minX;
        private float maxX = Float.NEGATIVE_INFINITY, maxY = maxX, maxZ = maxX;

        Mesh read(BufferedReader reader) throws IOException {
            String line;
            while ((line = boundedLine(reader)) != null) {
                lineNumber++;
                int comment = line.indexOf('#');
                if (comment >= 0) line = line.substring(0, comment);
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] fields = line.split("\\s+");
                try {
                    switch (fields[0]) {
                        case "v": add(positions, vector(fields, 3)); break;
                        case "vt": add(uvs, vector(fields, 2)); break;
                        case "vn": add(normals, vector(fields, 3)); break;
                        case "f": face(fields); break;
                        case "o": case "g": case "usemtl": {
                            finishPart();
                            String label = line.substring(fields[0].length()).trim();
                            switch (fields[0]) {
                                case "o": object = label; break;
                                case "g": group = label; break;
                                default: material = label; break;
                            }
                         break; }
                        default: { /* Unsupported metadata never causes external file access. */  break; }
                    }
                } catch (IllegalArgumentException e) {
                    throw new IOException("Invalid OBJ at line " + lineNumber + ": " + e.getMessage(), e);
                }
            }
            if (triangles.isEmpty()) throw new IOException("OBJ contains no polygon geometry");
            finishPart();
            return new Mesh(triangles, parts, new Bounds(minX, minY, minZ, maxX, maxY, maxZ));
        }

        private static void add(List<float[]> list, float[] value) {
            if (list.size() == MAX_ATTRIBUTES) throw new IllegalArgumentException("Attribute limit exceeded");
            list.add(value);
        }

        private static float[] vector(String[] fields, int dimensions) {
            if (fields.length < dimensions + 1) throw new IllegalArgumentException("Missing coordinate");
            float[] result = new float[dimensions];
            for (int i = 0; i < dimensions; i++) {
                float value = Float.parseFloat(fields[i + 1]);
                if (!Float.isFinite(value) || Math.abs(value) > MAX_COMPONENT)
                    throw new IllegalArgumentException("Non-finite or excessive coordinate");
                result[i] = value;
            }
            return result;
        }

        private void face(String[] fields) {
            if (fields.length < 4 || fields.length > MAX_FACE_VERTICES + 1)
                throw new IllegalArgumentException("Invalid face size");
            List<Index> polygon = new ArrayList<>(fields.length - 1);
            for (int i = 1; i < fields.length; i++) {
                String[] indices = fields[i].split("/", -1);
                if (indices.length > 3) throw new IllegalArgumentException("Invalid face index");
                polygon.add(new Index(index(indices[0], positions.size()),
                        indices.length > 1 && !indices[1].isEmpty() ? index(indices[1], uvs.size()) : -1,
                        // A cached xewento_dragon face uses vn index 0: treat its missing
                        // normal as absent and calculate it; position/UV indices remain strict.
                        indices.length > 2 && !indices[2].isEmpty() && !indices[2].equals("0") ? index(indices[2], normals.size()) : -1));
            }
            if (polygon.size() > 3 && polygon.get(0).position == polygon.get(polygon.size()-1).position)
                polygon.remove(polygon.size()-1);
            if (polygon.size() == 3) {
                triangle(polygon.get(0), polygon.get(1), polygon.get(2));
                return;
            }
            triangulate(polygon);
        }

        private static int index(String token, int count) {
            int value = Integer.parseInt(token);
            long resolved = value > 0 ? (long) value - 1 : (long) count + value;
            if (value == 0 || resolved < 0 || resolved >= count)
                throw new IllegalArgumentException("Face index outside available attributes");
            return (int) resolved;
        }

        /** Ear clipping retains winding and handles concave planar OBJ polygons. */
        private void triangulate(List<Index> polygon) {
            double[] normal = new double[3];
            for (int i = 0; i < polygon.size(); i++) {
                float[] a = positions.get(polygon.get(i).position);
                float[] b = positions.get(polygon.get((i + 1) % polygon.size()).position);
                normal[0] += ((double) a[1] - b[1]) * (a[2] + b[2]);
                normal[1] += ((double) a[2] - b[2]) * (a[0] + b[0]);
                normal[2] += ((double) a[0] - b[0]) * (a[1] + b[1]);
            }
            int dropped = Math.abs(normal[0]) > Math.abs(normal[1]) ? 0 : 1;
            if (Math.abs(normal[2]) > Math.abs(normal[dropped])) dropped = 2;
            int axisX = (dropped + 1) % 3, axisY = (dropped + 2) % 3;
            double area = 0;
            for (int i = 0; i < polygon.size(); i++) {
                float[] a = positions.get(polygon.get(i).position);
                float[] b = positions.get(polygon.get((i + 1) % polygon.size()).position);
                area += (double) a[axisX] * b[axisY] - (double) b[axisX] * a[axisY];
            }
            // Some Blockbench exports include collapsed faces. They have no visible area.
            if (area == 0) return;
            double direction = Math.signum(area), epsilon = Math.abs(area) * 1e-12;
            while (polygon.size() > 3) {
                boolean clipped = false;
                for (int i = 0; i < polygon.size(); i++) {
                    int before = (i + polygon.size() - 1) % polygon.size();
                    int after = (i + 1) % polygon.size();
                    Index ia = polygon.get(before), ib = polygon.get(i), ic = polygon.get(after);
                    float[] a = positions.get(ia.position), b = positions.get(ib.position), c = positions.get(ic.position);
                    double turn = cross(a, b, c, axisX, axisY) * direction;
                    if (Math.abs(turn) <= epsilon) {
                        polygon.remove(i);
                        clipped = true;
                        break;
                    }
                    if (turn < 0) continue;
                    boolean occupied = false;
                    for (int j = 0; j < polygon.size(); j++) {
                        if (j == before || j == i || j == after) continue;
                        float[] p = positions.get(polygon.get(j).position);
                        if (cross(a, b, p, axisX, axisY) * direction >= -epsilon
                                && cross(b, c, p, axisX, axisY) * direction >= -epsilon
                                && cross(c, a, p, axisX, axisY) * direction >= -epsilon) {
                            occupied = true;
                            break;
                        }
                    }
                    if (!occupied) {
                        triangle(ia, ib, ic);
                        polygon.remove(i);
                        clipped = true;
                        break;
                    }
                }
                if (!clipped) throw new IllegalArgumentException("Non-simple or unsupported polygon");
            }
            triangle(polygon.get(0), polygon.get(1), polygon.get(2));
        }

        private static double cross(float[] a, float[] b, float[] c, int x, int y) {
            return ((double) b[x] - a[x]) * ((double) c[y] - a[y])
                    - ((double) b[y] - a[y]) * ((double) c[x] - a[x]);
        }

        private void triangle(Index ia, Index ib, Index ic) {
            if (triangles.size() == MAX_TRIANGLES) throw new IllegalArgumentException("Triangle limit exceeded");
            float[] a = positions.get(ia.position), b = positions.get(ib.position), c = positions.get(ic.position);
            float[] normal = normalized((double) (b[1] - a[1]) * (c[2] - a[2]) - (double) (b[2] - a[2]) * (c[1] - a[1]),
                    (double) (b[2] - a[2]) * (c[0] - a[0]) - (double) (b[0] - a[0]) * (c[2] - a[2]),
                    (double) (b[0] - a[0]) * (c[1] - a[1]) - (double) (b[1] - a[1]) * (c[0] - a[0]));
            triangles.add(new Triangle(vertex(ia, normal), vertex(ib, normal), vertex(ic, normal)));
        }

        private Vertex vertex(Index index, float[] fallback) {
            float[] p = positions.get(index.position);
            float[] uv = index.uv < 0 ? new float[2] : uvs.get(index.uv);
            float[] n = index.normal < 0 ? fallback : normals.get(index.normal);
            if (index.normal >= 0) n = normalized(n[0], n[1], n[2]);
            minX = Math.min(minX, p[0]); minY = Math.min(minY, p[1]); minZ = Math.min(minZ, p[2]);
            maxX = Math.max(maxX, p[0]); maxY = Math.max(maxY, p[1]); maxZ = Math.max(maxZ, p[2]);
            return new Vertex(p[0], p[1], p[2], uv[0], uv[1], n[0], n[1], n[2]);
        }

        private static float[] normalized(double x, double y, double z) {
            double length = Math.sqrt(x * x + y * y + z * z);
            if (length == 0) return new float[]{0, 1, 0};
            return new float[]{(float) (x / length), (float) (y / length), (float) (z / length)};
        }

        private void finishPart() {
            if (partStart < triangles.size()) {
                parts.add(new Part(object, group, material, partStart, triangles.size() - partStart));
                partStart = triangles.size();
            }
        }

        private static String boundedLine(BufferedReader reader) throws IOException {
            StringBuilder line = new StringBuilder();
            int ch;
            while ((ch = reader.read()) != -1) {
                if (ch == '\n') break;
                if (line.length() == MAX_LINE_LENGTH) throw new IOException("OBJ line exceeds limit");
                line.append((char) ch);
            }
            return ch == -1 && line.length()==0 ? null : line.toString();
        }
    }

    private static final class LimitedStream extends FilterInputStream {
        private long count;
        LimitedStream(InputStream stream) { super(stream); }
        private void counted(long bytes) throws IOException {
            count += bytes;
            if (count > MAX_BYTES) throw new IOException("OBJ exceeds byte limit");
        }
        @Override public int read() throws IOException {
            int result = in.read();
            if (result >= 0) counted(1);
            return result;
        }
        @Override public int read(byte[] buffer, int offset, int length) throws IOException {
            int result = in.read(buffer, offset, length);
            if (result > 0) counted(result);
            return result;
        }
    }
}
