package local.badlioncosmeticport.model;

/** Timed supplier windows and texture groups recovered from aEi and aBQ. */
public final class PetEffects {
    private PetEffects() {}
    public static int batchCount(int id, float phaseSeconds) {
        if (id == 19 && phaseSeconds > 5.7 && phaseSeconds < 6.7) return 3;
        if (id == 70 && phaseSeconds > 6.3 && phaseSeconds < 6.4) return 8;
        return 0;
    }
    public static int thirtyVirusFrame(long milliseconds) {
        return (int) (milliseconds / 10_000 % 4) * 8 + (int) (milliseconds / 100 % 8);
    }
}
