import local.badlioncosmeticport.model.*;
import local.badlioncosmeticport.render.ObjGeometry;
import java.nio.file.*;
import java.util.*;
import java.io.*;

/** Exercises the actual exported assets and an independently specified two-joint skinning fixture. */
public final class MeshCoreAudit {
    static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        String mesh = """
                MD5Version 10
                numJoints 2
                numMeshes 1
                joints {
                  "root" -1 ( 0 0 0 ) ( 0 0 0 )
                  "child" 0 ( 0 2 0 ) ( 0 0 0 )
                }
                mesh {
                  shader "fixture"
                  numverts 3
                  vert 0 ( 0 0 ) 0 2
                  vert 1 ( 1 0 ) 2 1
                  vert 2 ( 0 1 ) 3 1
                  numtris 1
                  tri 0 0 1 2
                  numweights 4
                  weight 0 0 0.25 ( 4 0 0 )
                  weight 1 1 0.75 ( 0 0 0 )
                  weight 2 0 1 ( 1 0 0 )
                  weight 3 0 1 ( 0 1 0 )
                }
                """;
        Md5Model fixture = Md5Model.read(new StringReader(mesh));
        var vertex = fixture.skin(fixture.restPose()).getFirst().vertices().getFirst();
        require(Math.abs(vertex.x() - 1) < 1e-6 && Math.abs(vertex.y() - 1.5) < 1e-6, "Two-joint weighted translation disagrees with 25/75 blend");
        String clip = """
                MD5Version 10
                numFrames 2
                numJoints 2
                frameRate 20
                numAnimatedComponents 1
                hierarchy {
                  "root" -1 32 0
                  "child" 0 0 1
                }
                bounds { ( -5 -5 -5 ) ( 5 5 5 ) ( -5 -5 -5 ) ( 5 5 5 ) }
                baseframe { ( 0 0 0 ) ( 0 0 0 ) ( 0 2 0 ) ( 0 0 0 ) }
                frame 0 { 0 }
                frame 1 { -0.70710678 }
                """;
        Md5Animation fixtureClip = Md5Animation.read(new StringReader(clip)); fixtureClip.validateFor(fixture);
        Md5Animation.read(new StringReader(clip.replace("\"child\"", "\"renamed_child\""))).validateFor(fixture);
        vertex = fixture.skin(fixtureClip.frame(1)).getFirst().vertices().getFirst();
        require(Math.abs(vertex.x() + 1.5) < 1e-5 && Math.abs(vertex.y() - 1) < 1e-5, "90 degree parent rotation or quaternion convention is incorrect");
        require(fixtureClip.sample(-.05, true).equals(fixtureClip.sample(.05, true)), "Negative looping time mismatch");
        try { Md5Model.read(new StringReader(mesh.replace("tri 0 0 1 2", "tri 0 0 1 99"))); throw new AssertionError("Invalid triangle accepted"); } catch (IOException expected) {}
        var objFixture = ObjGeometry.read(new ByteArrayInputStream("v 0 0 0\nv 1 0 0\nv 1 1 0\nv 0 1 0\nf -4 -3 -2 -1\n".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        require(objFixture.triangles().size() == 2, "OBJ quad triangulation/negative indices failed");
        Path root = Path.of(args[0]); List<String> failures = new ArrayList<>();
        int md5 = 0, obj = 0, animation = 0, sampledVertices = 0, compatibleClips = 0, incompatiblePairs = 0, unusedClips = 0;
        Map<Path, Md5Model> parsedModels = new HashMap<>();
        Map<Path, Md5Animation> parsedClips = new HashMap<>();
        List<Path> files;
        try (var walk = Files.walk(root)) { files = walk.filter(Files::isRegularFile).sorted().toList(); }
        for (Path file : files) {
            String name = file.getFileName().toString();
            try {
                if (name.endsWith(".md5mesh")) {
                    Md5Model model = Md5Model.read(file); md5++;
                    parsedModels.put(file, model);
                    sampledVertices += model.skin(model.restPose()).stream().mapToInt(m -> m.vertices().size()).sum();
                } else if (name.endsWith(".obj")) { ObjGeometry.read(file); obj++; }
                else if (name.endsWith(".md5anim")) {
                    Md5Animation anim = Md5Animation.read(file); animation++;
                    parsedClips.put(file, anim);
                    for (int frame = 0; frame < anim.frameCount(); frame++) anim.sampleFrame(frame + .5, true);
                }
            } catch (Exception failure) { failures.add(root.relativize(file) + ": " + failure); }
        }
        for (var entry : parsedClips.entrySet()) {
            Path file = entry.getKey(); Md5Animation clipForModel = entry.getValue();
            boolean matched = false;
            for (var candidate : parsedModels.entrySet()) {
                if (!candidate.getKey().getParent().equals(file.getParent())) continue;
                Md5Model model = candidate.getValue();
                try { clipForModel.validateFor(model); }
                catch (IOException expectedVariantMismatch) { incompatiblePairs++; continue; }
                model.skin(clipForModel.frame(0));
                model.skin(clipForModel.sample(clipForModel.durationSeconds() / 2, true));
                model.skin(clipForModel.frame(clipForModel.frameCount() - 1));
                compatibleClips++; matched = true;
            }
            if (!matched) {
                // aCJ requests only flapping.md5anim. Constellation's cache also
                // contains three unrelated, unused 12-joint cloak clips.
                if (root.relativize(file).toString().replace('\\', '/').startsWith("wing/constellation_wings/")
                        && Set.of("curved.md5anim", "dynamic.md5anim", "rect.md5anim").contains(file.getFileName().toString())) unusedClips++;
                else failures.add(root.relativize(file) + ": no compatible sibling mesh");
            }
        }
        Path cloak = root.resolve("cloak/cloak_v2/cloak_v2.md5mesh");
        Md5Model cloakModel = Md5Model.read(cloak);
        Md5Animation dynamic = Md5Animation.read(cloak.resolveSibling("dynamic.md5anim")); dynamic.validateFor(cloakModel);
        require(cloakModel.joints().size() == 12 && cloakModel.meshes().getFirst().vertices().size() == 121, "Recovered cloak geometry counts changed");
        double difference = 0;
        var rest = cloakModel.skin(dynamic.frame(0)).getFirst().vertices();
        var moved = cloakModel.skin(dynamic.frame(30)).getFirst().vertices();
        for (int i = 0; i < rest.size(); i++) difference += Math.abs(rest.get(i).y() - moved.get(i).y()) + Math.abs(rest.get(i).z() - moved.get(i).z());
        require(difference > 1, "Dynamic cloak clip does not deform the actual mesh");
        Md5MotionClock clock = new Md5MotionClock(dynamic); clock.configureDynamic(30, 45, 36);
        for (int i = 0; i < 5000; i++) { clock.advance(i % 200 < 100 ? .02f : -.027f); cloakModel.skin(clock.pose()); }
        System.out.println("{\"md5Models\":" + md5 + ",\"objModels\":" + obj + ",\"animations\":" + animation + ",\"compatibleClipsSkinned\":" + compatibleClips + ",\"incompatibleCrossPairs\":" + incompatiblePairs + ",\"unusedCachedClips\":" + unusedClips + ",\"bindVertices\":" + sampledVertices + ",\"cloakJoints\":12,\"cloakVertices\":121,\"dynamicDeformation\":" + difference + ",\"motionSteps\":5000,\"fixture\":\"passed\",\"failures\":" + failures.size() + "}");
        for (String failure : failures) System.err.println(failure);
        require(failures.isEmpty(), "Some exported models/animations failed");
    }
}
