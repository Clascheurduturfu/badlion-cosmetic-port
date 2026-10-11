import local.badlioncosmeticport.model.LegacyCloak;
public final class NativeRibbonParity {
    public static void main(String[] args) {
        int comparisons = 0; double maxError = 0;
        for (int phase = 0; phase < 600; phase += 3) for (int angle : new int[]{0, 6, 20, 45, 90}) {
            NativeRibbonReference reference = new NativeRibbonReference(); reference.build(angle, phase);
            var motion = new LegacyCloak.Motion(0, 0, 0, 0, 0, 0, 0, angle / 55f);
            var strip = LegacyCloak.strip(LegacyCloak.Shape.DYNAMIC, motion, phase / 10f);
            if (strip.size() != 98) throw new AssertionError("Original strip vertex count changed");
            for (int i = 0; i < strip.size(); i++) {
                float[] expected = reference.vertex(i); var v = strip.get(i);
                float[] actual = {v.x(), v.y(), v.z(), v.u(), v.v()};
                for (int j = 0; j < 5; j++) {
                    double error = Math.abs(expected[j] - actual[j]); maxError = Math.max(maxError, error); comparisons++;
                    if (error > .00002) throw new AssertionError("Ribbon vertex " + i + "/" + j + ": " + expected[j] + " != " + actual[j]);
                }
            }
        }
        for (var shape : new LegacyCloak.Shape[]{LegacyCloak.Shape.CURVED, LegacyCloak.Shape.RECT}) {
            NativeCurvedReference reference = new NativeCurvedReference(shape == LegacyCloak.Shape.CURVED ? 6 : 1,
                    shape == LegacyCloak.Shape.CURVED ? 5 : 0);
            reference.bFL();
            var strip = LegacyCloak.strip(shape, new LegacyCloak.Motion(0,0,0,0,0,0,0,0), 0);
            if (strip.size() != reference.size()) throw new AssertionError("Static cloak vertex count differs");
            for (int i=0;i<strip.size();i++) {
                var v=strip.get(i); float[] actual={v.x(),v.y(),v.z(),v.u(),v.v()}, expected=reference.vertex(i);
                for (int c=0;c<5;c++) {
                    double error=Math.abs(expected[c]-actual[c]); maxError=Math.max(maxError,error); comparisons++;
                    if(error>.00002)throw new AssertionError(shape+" vertex "+i+"/"+c+": "+expected[c]+" != "+actual[c]);
                }
            }
        }
        System.out.printf(java.util.Locale.ROOT, "{\"cases\":1002,\"comparisons\":%d,\"maxError\":%.9f,\"passed\":true}%n", comparisons, maxError);
    }
}
