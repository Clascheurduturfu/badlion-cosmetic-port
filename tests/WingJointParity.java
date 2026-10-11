package local.badlioncosmeticport.model;

import java.nio.file.Path;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Child-local motion must survive root restriction without changing the root. */
public final class WingJointParity {
    private static float maxError;
    private static int comparisons;
    private static void compare(float expected, float actual) {
        float error = Math.abs(expected - actual);
        maxError = Math.max(maxError, error); comparisons++;
        if (error > .00003f) throw new AssertionError("Wing pose error " + error);
    }
    private static void quaternion(Quaternionf expected, Quaternionf actual) {
        if (expected.dot(actual) < 0) actual.mul(-1);
        compare(expected.x, actual.x); compare(expected.y, actual.y);
        compare(expected.z, actual.z); compare(expected.w, actual.w);
    }
    public static void main(String[] args) throws Exception {
        var clip = Md5Animation.read(Path.of(args[0]).resolve("wing/dragon_wings_v3/flapping.md5anim"));
        var restricted = clip.frame(9);
        float excursion = 0;
        for (int frame = 0; frame < clip.frameCount(); frame++) {
            var source = clip.frame(frame);
            var actual = WingMotion.withAnimatedChildren(clip, restricted, source);
            var identity = WingMotion.withAnimatedChildren(clip, source, source);
            for (int joint = 0; joint < clip.jointCount(); joint++) {
                var original = source.joints().get(joint);
                var same = identity.joints().get(joint);
                compare(original.position().x(), same.position().x()); compare(original.position().y(), same.position().y());
                compare(original.position().z(), same.position().z()); quaternion(original.rotation().mutable(), same.rotation().mutable());
                int parent = clip.hierarchy().get(joint).parent();
                if (parent < 0) {
                    quaternion(restricted.joints().get(joint).rotation().mutable(), actual.joints().get(joint).rotation().mutable());
                } else {
                    var sourceParent = source.joints().get(parent);
                    var actualParent = actual.joints().get(parent);
                    Quaternionf expectedLocal = sourceParent.rotation().mutable().conjugate().mul(original.rotation().mutable());
                    Quaternionf actualLocal = actualParent.rotation().mutable().conjugate().mul(actual.joints().get(joint).rotation().mutable());
                    quaternion(expectedLocal, actualLocal);
                    Vector3f expectedPosition = original.position().mutable().sub(sourceParent.position().mutable()).rotate(sourceParent.rotation().mutable().conjugate());
                    Vector3f actualPosition = actual.joints().get(joint).position().mutable().sub(actualParent.position().mutable()).rotate(actualParent.rotation().mutable().conjugate());
                    compare(expectedPosition.x, actualPosition.x); compare(expectedPosition.y, actualPosition.y); compare(expectedPosition.z, actualPosition.z);
                    var baseline = restricted.joints().get(parent).rotation().mutable().conjugate().mul(restricted.joints().get(joint).rotation().mutable());
                    excursion = Math.max(excursion, 1 - Math.abs(expectedLocal.dot(baseline)));
                }
            }
        }
        if (excursion < .01f) throw new AssertionError("Outer wing animation has no motion");
        System.out.println("{\"test\":\"wing-child-local-parity\",\"frames\":" + clip.frameCount() + ",\"comparisons\":" + comparisons
                + ",\"maxError\":" + maxError + ",\"outerJointExcursion\":" + excursion + ",\"passed\":true}");
    }
}
