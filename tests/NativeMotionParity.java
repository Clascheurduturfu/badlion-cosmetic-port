import local.badlioncosmeticport.model.*;
import net.badlion.a.*;
import java.nio.file.*;

/** Differential audit against recovered aGi/aGg/aGh, with GL and logging stubbed. */
public final class NativeMotionParity {
    private static double maxError;
    private static int comparisons;
    private static void compare(float expected, float actual, String label) {
        double error = Math.abs(expected - actual);
        maxError = Math.max(maxError, error);
        comparisons++;
        if (error > .00002) throw new AssertionError(label + ": " + expected + " != " + actual);
    }
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]);
        String[] files = {"cloak/cloak_v2/dynamic.md5anim", "cloak/cloak_v2/curved.md5anim",
                "cloak/cloak_v2/rect.md5anim", "wing/dragon_wings_v3/flapping.md5anim",
                "pet/pet_11/frame-0.md5anim"};
        for (int mode = 0; mode < files.length; mode++) {
            Path file = root.resolve(files[mode]);
            Md5Animation clip = Md5Animation.read(file);
            aGg nativeClip = new aGd().mB(file.toString());
            aGi reference = new aGi(nativeClip, new aCE.b());
            Md5MotionClock port = new Md5MotionClock(clip);
            if (mode == 0) { reference.am(30, 45, 36); port.configureDynamic(30, 45, 36); }
            else if (mode < 3) { reference.v(30, true); port.configureOffset(30, true); }
            else if (mode == 3) { reference.v(80, false); port.configureOffset(80, false); }
            else { reference.hP(false); port.configureOffset(clip.frameCount(), false); }
            java.util.Random random = new java.util.Random(1234);
            for (int step = 0; step < 2000; step++) {
                float delta = step % 270 < 150 ? .012f : -.017f;
                if (step % 11 == 0) delta = 0;
                if (step > 1000) delta = (random.nextFloat() - .4f) * .08f;
                reference.ez(delta); port.advance(delta);
                compare(reference.bIy(), port.phaseSeconds(), files[mode] + " phase");
                var actual = port.pose().joints();
                for (int j = 0; j < actual.size(); j++) {
                    var expected = reference.bIx().get(j);
                    var joint = actual.get(j);
                    compare(expected.dMF.x, joint.position().x(), "joint x");
                    compare(expected.dMF.y, joint.position().y(), "joint y");
                    compare(expected.dMF.z, joint.position().z(), "joint z");
                    compare(expected.dMP.x, joint.rotation().x(), "quaternion x");
                    compare(expected.dMP.y, joint.rotation().y(), "quaternion y");
                    compare(expected.dMP.z, joint.rotation().z(), "quaternion z");
                    compare(expected.dMP.w, joint.rotation().w(), "quaternion w");
                }
            }
        }
        System.out.printf(java.util.Locale.ROOT,
                "{\"clips\":5,\"steps\":10000,\"comparisons\":%d,\"maxError\":%.9f,\"passed\":true}%n", comparisons, maxError);
    }
}
