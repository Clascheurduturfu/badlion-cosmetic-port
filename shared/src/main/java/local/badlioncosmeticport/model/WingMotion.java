package local.badlioncosmeticport.model;

import java.util.ArrayList;

/** Preserve the original child-local animation while restricting only the wing root. */
public final class WingMotion {
    private WingMotion() {}
    public static Md5Model.Pose withAnimatedChildren(Md5Animation animation, Md5Model.Pose roots, Md5Model.Pose source) {
        java.util.List<Md5Model.Transform> result = new ArrayList<Md5Model.Transform>(roots.joints().size());
        for (int i = 0; i < roots.joints().size(); i++) {
            int parent = animation.hierarchy().get(i).parent();
            if (parent < 0) { result.add(roots.joints().get(i)); continue; }
            Md5Model.Transform oldParent = source.joints().get(parent);
            Md5Model.Transform oldChild = source.joints().get(i);
            org.joml.Quaternionf inverse = oldParent.rotation().mutable().conjugate();
            org.joml.Vector3f localPosition = oldChild.position().mutable().sub(oldParent.position().mutable()).rotate(inverse);
            org.joml.Quaternionf localRotation = new org.joml.Quaternionf(inverse).mul(oldChild.rotation().mutable());
            Md5Model.Transform newParent = result.get(parent);
            result.add(new Md5Model.Transform(Md5Model.Vec3.of(localPosition.rotate(newParent.rotation().mutable())
                    .add(newParent.position().mutable())), Md5Model.Rotation.of(newParent.rotation().mutable().mul(localRotation).normalize())));
        }
        return new Md5Model.Pose(result);
    }
}
