package local.badlioncosmeticport.model;

/**
 * Per-player motion-driven phase controller recovered from Badlion aGi.
 * Signed advance values are the original caller's motion delta, not unconditional elapsed seconds.
 * A clock is mutable and belongs to one cosmetic/player; models and animations remain shared immutable data.
 */
public final class Md5MotionClock {
    private final Md5Animation animation;
    private int offsetFrames;
    private int loopStart;
    private int loopEnd;
    private int transitionFrame;
    private boolean mirroredTail;
    private boolean returning;
    private float phase;
    private float duration;
    private Md5Model.Pose pose;

    public Md5MotionClock(Md5Animation animation) {
        this.animation = java.util.Objects.requireNonNull(animation);
        duration = (float) animation.frameCount() / animation.frameRate();
        loopEnd = animation.frameCount() - 1;
        pose = animation.frame(0);
    }

    /** aGi.am(start,end,transition), used by dynamic cloaks with (30,45,36). */
    public void configureDynamic(int start, int end, int transition) {
        if (start < 0 || start > end || end + 1 >= animation.frameCount() || transition < 0 || transition > end) {
            throw new IllegalArgumentException("Invalid motion clip ranges");
        }
        offsetFrames = 0;
        duration = (float) animation.frameCount() / animation.frameRate();
        phase = 0;
        loopStart = start; loopEnd = end; transitionFrame = transition;
        mirroredTail = true; returning = false; pose = animation.frame(0);
    }

    /** aGi.v(offset,mirror), used for the 30-frame curved/rectangular cape tail. */
    public void configureOffset(int offset, boolean mirror) {
        if (offset < 1 || offset > animation.frameCount()) throw new IllegalArgumentException("Invalid animation offset");
        offsetFrames = offset;
        duration = (float) (animation.frameCount() + offset) / animation.frameRate();
        phase = duration - 0.001f;
        loopEnd = animation.frameCount(); loopStart = offset;
        mirroredTail = mirror; returning = false;
        // Captured bIE copies frame zero without sampling the initialized tail phase.
        pose = animation.frame(0);
    }

    public Md5Model.Pose pose() { return pose; }
    public float phaseSeconds() { return phase; }
    public float phaseFrames() { return phase * animation.frameRate(); }
    public float durationSeconds() { return duration; }
    public boolean returning() { return returning; }
    public void reset() { phase = 0; returning = false; pose = animation.frame(0); }

    /** Exact eA attenuation; caller applies this angle around X only for crouching. */
    public float crouchAngle(float requestedDegrees) {
        float frame = phaseFrames();
        float outside = frame < loopStart ? loopStart - frame : frame > loopEnd ? loopEnd - frame : 0;
        outside = Math.min(20, Math.abs(outside));
        return requestedDegrees * (20 - outside) / 20;
    }

    /** Advance and sample; returns whether the phase changed, as the captured renderer does. */
    public boolean advance(float delta) {
        if (!Float.isFinite(delta)) throw new IllegalArgumentException("Non-finite motion delta");
        return offsetFrames == 0 ? advanceDynamic(delta) : advanceOffset(delta);
    }

    private boolean advanceDynamic(float input) {
        int count = animation.frameCount(), fps = animation.frameRate();
        float before = phase;
        int nextLoop = loopEnd + 1;
        float endTime = (float) loopEnd / fps;
        float delta = (float) ((double) input / 1.5);
        if (delta > 0 || !returning && phase > (float) transitionFrame / fps && phase < (float) (loopEnd + 2) / fps) {
            if (!returning && phase > endTime && phase < (float) (loopEnd + 2) / fps) {
                phase -= (float) (loopEnd - loopStart + 1) / fps;
            } else if (returning && phase > endTime) {
                phase -= delta * 2;
                if (phase < 0) phase = 0;
                if (phase < endTime) returning = false;
            }
            phase += Math.abs(delta);
            phase %= duration;
        } else if (delta <= 0) {
            if (phase < (float) (transitionFrame / 2) / fps) {
                if (phase + delta > 0) { phase += delta; returning = true; }
                else returning = false;
            } else if (phase - delta < duration) {
                if (phase - delta > (float) (transitionFrame + 1) / fps && phase < (float) (transitionFrame + 1) / fps) {
                    phase += (float) (nextLoop - transitionFrame - 1) / fps;
                }
                phase -= delta;
                if (phase >= duration) phase = duration - 0.001f;
                returning = true;
            } else returning = false;
        }
        int first = (int) (phase * fps);
        float alpha = phase % (1.0f / fps) * fps;
        if (returning && first == transitionFrame) pose = animation.interpolateFrames(transitionFrame, nextLoop, alpha);
        else pose = animation.interpolateFrames(Math.floorMod(first, count), Math.floorMod(first + 1, count), alpha);
        return before != phase;
    }

    private boolean advanceOffset(float delta) {
        int count = animation.frameCount(), fps = animation.frameRate();
        float before = phase;
        if (delta < 0) {
            if (phase > duration / 2) {
                if (phase - delta < duration) phase -= delta;
                returning = phase - delta < duration;
            } else {
                if (phase + delta > 0) phase += delta;
                returning = phase + delta > 0;
            }
        } else if (delta > 0) {
            if (!returning && phase > (float) count / fps) {
                if (mirroredTail) {
                    if (phase < (float) (count + offsetFrames - 2) / fps) phase -= (float) (count - offsetFrames) / fps;
                } else phase -= (float) count / fps;
            } else if (returning && phase > duration / 2) phase = duration - phase;
            returning = false;
            phase += delta;
            phase %= duration;
        }
        int first = (int) Math.floor(phase * fps);
        float alpha = phase * fps % 1.0f;
        pose = animation.interpolateFrames(mapFrame(first) % count, mapFrame(first + 1) % count, alpha);
        return before != phase;
    }

    /** eC direct frame input, with the configured offset-tail mapping. */
    public Md5Model.Pose sampleMotionFrame(float frame) {
        if (!Float.isFinite(frame) || frame < 0) throw new IllegalArgumentException("Invalid motion frame");
        int first = (int) frame;
        pose = animation.interpolateFrames(mapFrame(first) % animation.frameCount(),
                mapFrame(first + 1) % animation.frameCount(), frame % 1.0f);
        return pose;
    }

    private int mapFrame(int frame) {
        int count = animation.frameCount();
        int mapped = Math.floorMod(frame, count + offsetFrames);
        if (mapped >= count) mapped = mirroredTail ? count + offsetFrames - mapped : mapped - count;
        return mapped;
    }
}
