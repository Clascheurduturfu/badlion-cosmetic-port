import org.joml.Vector3f; public final class NativeRibbonReference {
private long dGH; private boolean deleted; private float dGO,dGP,dGQ,dGS,dGT;private int sK;
private final java.nio.FloatBuffer dGM=java.nio.FloatBuffer.allocate(294),dGN=java.nio.FloatBuffer.allocate(196);
private final org.joml.Vector3f dGR=new org.joml.Vector3f();
private final org.joml.Vector3f dGU=new org.joml.Vector3f();
private final org.joml.Vector3f dGV=new org.joml.Vector3f();
private final org.joml.Vector3f dGW=new org.joml.Vector3f();
private final org.joml.Vector3f dGX=new org.joml.Vector3f();
private final org.joml.Vector3f dGY=new org.joml.Vector3f();
private final org.joml.Vector3f dGZ=new org.joml.Vector3f();
private final org.joml.Vector3f dHa=new org.joml.Vector3f();
private final org.joml.Vector3f dHb=new org.joml.Vector3f();
private final org.joml.Vector3f dHc=new org.joml.Vector3f();
private final org.joml.Vector3f dHd=new org.joml.Vector3f();
    public void d(float f, float f2, boolean bl) {
        float f3;
        float f4;
        float f5;
        float f6;
        float f7;
        float f8 = (float)dGH / 10.0f;
        float f9 = f /= 55.0f;
        float f10 = 1.5f;
        if (this.deleted) {
            return;
        }
        if (!bl && f == this.dGO && f2 == this.dGP && this.dGQ == f9 && f9 == 0.0f) {
            return;
        }
        this.dGQ = f9;
        this.dGM.clear();
        this.sK = 0;
        if (bl) {
            this.dGN.clear();
        }
        float f11 = 22.0f;
        float f12 = 23.0f;
        float f13 = 1.0f / f12;
        float f14 = 17.0f / f12;
        float f15 = 1.0f / f11;
        float f16 = 11.0f / f11;
        float f17 = 12.0f / f11;
        float f18 = 22.0f / f11;
        float f19 = 21.0f / f11;
        Vector3f vector3f = this.dGU.set(0.0f, 0.0f, 5.0f);
        Vector3f vector3f2 = this.dGV.set(0.0f, -16.0f, 5.0f);
        Vector3f vector3f3 = this.dGW.set(0.0f, -16.0f, -5.0f);
        Vector3f vector3f4 = this.dGX.set(0.0f, 0.0f, -5.0f);
        Vector3f vector3f5 = this.dGY.set(1.0f, 0.0f, 5.0f);
        Vector3f vector3f6 = this.dGZ.set(1.0f, -16.0f, 5.0f);
        Vector3f vector3f7 = this.dHa.set(1.0f, -16.0f, -5.0f);
        Vector3f vector3f8 = this.dHb.set(1.0f, 0.0f, -5.0f);
        Vector3f vector3f9 = this.dHc.set(0.0f, -10.0f, -5.0f);
        Vector3f vector3f10 = this.dHd.set(0.0f, -10.0f, -5.0f);
        float f20 = 10.0f;
        boolean bl2 = true;
        this.dGS = (float)Math.sin(Math.PI * (double)f10 + (double)f8) * f9;
        float f21 = -this.dGS;
        this.dGT = (float)Math.sin(0.0 * (double)f10 + (double)f8) * f9 + 1.0f + f21;
        int n2 = 0;
        while ((float)n2 <= f20) {
            f7 = (float)n2 / f20;
            f6 = (1.0f - f7) * (1.0f - f7) * vector3f7.x + 2.0f * (1.0f - f7) * f7 * vector3f9.x + f7 * f7 * vector3f8.x;
            f5 = (1.0f - f7) * (1.0f - f7) * vector3f7.y + 2.0f * (1.0f - f7) * f7 * vector3f9.y + f7 * f7 * vector3f8.y;
            if (bl2) {
                f6 = (float)Math.sin((double)f7 * Math.PI * (double)f10 + (double)f8) * f9 + 1.0f + f21;
            }
            if (bl) {
                f4 = Wz.u(1.0f - (vector3f4.y - f5) / (vector3f4.y - vector3f3.y), 0.0f, 1.0f);
                this.dGN.put(new float[]{f15, f14 - (f14 - f13) * f4});
                this.dGN.put(new float[]{f16, f14 - (f14 - f13) * f4});
            }
            this.dGM.put(new float[]{f6, f5, vector3f5.z});
            this.dGM.put(new float[]{f6, f5, vector3f8.z});
            this.sK += 2;
            this.dGR.set((float)Math.sin((double)f7 * Math.PI * (double)f10 + (double)f8) * f9 + 1.0f + f21, -vector3f6.y + f9, vector3f6.z);
            ++n2;
        }
        if (bl) {
            this.dGN.put(new float[]{0.045454547f, 0.04347826f});
            this.dGN.put(new float[]{0.5f, 0.04347826f});
            this.dGN.put(new float[]{0.045454547f, 0.0f});
            this.dGN.put(new float[]{0.5f, 0.0f});
        }
        this.dGM.put(new float[]{vector3f5.x, vector3f5.y, vector3f5.z});
        this.dGM.put(new float[]{vector3f8.x, vector3f8.y, vector3f8.z});
        this.dGM.put(new float[]{vector3f.x, vector3f.y, vector3f.z});
        this.dGM.put(new float[]{vector3f4.x, vector3f4.y, vector3f4.z});
        this.sK += 4;
        n2 = 0;
        while ((float)n2 <= f20) {
            f7 = (float)n2 / f20;
            f6 = (1.0f - f7) * (1.0f - f7) * vector3f4.x + 2.0f * (1.0f - f7) * f7 * vector3f10.x + f7 * f7 * vector3f3.x;
            f5 = (1.0f - f7) * (1.0f - f7) * vector3f4.y + 2.0f * (1.0f - f7) * f7 * vector3f10.y + f7 * f7 * vector3f3.y;
            if (bl2) {
                f6 = (float)Math.sin((double)(1.0f - f7) * Math.PI * (double)f10 + (double)f8) * f9 + f21;
            }
            if (bl) {
                f4 = Wz.u((vector3f4.y - f5) / (vector3f4.y - vector3f3.y), 0.0f, 1.0f);
                this.dGN.put(new float[]{f17, f13 + (f14 - f13) * f4});
                this.dGN.put(new float[]{f16, f13 + (f14 - f13) * f4});
            }
            this.dGM.put(new float[]{f6, f5, vector3f8.z});
            f6 = (1.0f - f7) * (1.0f - f7) * vector3f8.x + 2.0f * (1.0f - f7) * f7 * vector3f9.x + f7 * f7 * vector3f7.x;
            f5 = (1.0f - f7) * (1.0f - f7) * vector3f8.y + 2.0f * (1.0f - f7) * f7 * vector3f9.y + f7 * f7 * vector3f7.y;
            if (bl2) {
                f6 = (float)Math.sin((double)(1.0f - f7) * Math.PI * (double)f10 + (double)f8) * f9 + 1.0f + f21;
            }
            this.dGM.put(new float[]{f6, f5, vector3f8.z});
            this.sK += 2;
            ++n2;
        }
        if (bl) {
            this.dGN.put(new float[]{0.95454544f, 0.0f});
            this.dGN.put(new float[]{0.5f, 0.0f});
            this.dGN.put(new float[]{0.95454544f, 0.04347826f});
            this.dGN.put(new float[]{0.5f, 0.04347826f});
        }
        float f22 = 0.0f;
        if (bl2) {
            f22 = (float)Math.sin(0.0 * (double)f10 + (double)f8) * f9 + f21;
        }
        this.dGM.put(new float[]{vector3f7.x + f22, vector3f7.y, vector3f7.z});
        this.dGM.put(new float[]{vector3f6.x + f22, vector3f6.y, vector3f6.z});
        this.dGM.put(new float[]{vector3f3.x + f22, vector3f3.y, vector3f3.z});
        this.dGM.put(new float[]{vector3f2.x + f22, vector3f2.y, vector3f2.z});
        this.sK += 4;
        int n3 = 0;
        while ((float)n3 <= f20) {
            f6 = (float)n3 / f20;
            f5 = (1.0f - f6) * (1.0f - f6) * vector3f3.x + 2.0f * (1.0f - f6) * f6 * vector3f10.x + f6 * f6 * vector3f4.x;
            f4 = (1.0f - f6) * (1.0f - f6) * vector3f3.y + 2.0f * (1.0f - f6) * f6 * vector3f10.y + f6 * f6 * vector3f4.y;
            if (bl2) {
                f5 = (float)Math.sin((double)f6 * Math.PI * (double)f10 + (double)f8) * f9 + f21;
            }
            if (bl) {
                f3 = Wz.u(1.0f - (vector3f4.y - f4) / (vector3f4.y - vector3f3.y), 0.0f, 1.0f);
                this.dGN.put(new float[]{f17, f14 - (f14 - f13) * f3});
                this.dGN.put(new float[]{f18, f14 - (f14 - f13) * f3});
            }
            this.dGM.put(new float[]{f5, f4, vector3f8.z});
            this.dGM.put(new float[]{f5, f4, vector3f5.z});
            this.sK += 2;
            ++n3;
        }
        n3 = 0;
        while ((float)n3 <= f20) {
            f6 = (float)n3 / f20;
            if (n3 == 0) {
                f5 = (1.0f - f6) * (1.0f - f6) * vector3f4.x + 2.0f * (1.0f - f6) * f6 * vector3f10.x + f6 * f6 * vector3f3.x;
                f4 = (1.0f - f6) * (1.0f - f6) * vector3f4.y + 2.0f * (1.0f - f6) * f6 * vector3f10.y + f6 * f6 * vector3f3.y;
                if (bl2) {
                    // empty if block
                }
                if (bl) {
                    f3 = Wz.u((vector3f4.y - f4) / (vector3f4.y - vector3f3.y), 0.0f, 1.0f);
                    this.dGN.put(new float[]{0.0f, f13 + (f14 - f13) * f3});
                    this.dGN.put(new float[]{0.0f, f13 + (f14 - f13) * f3});
                }
                this.dGM.put(new float[]{f5, f4, vector3f5.z});
                this.dGM.put(new float[]{f5, f4, vector3f5.z});
                this.sK += 2;
            }
            f5 = (1.0f - f6) * (1.0f - f6) * vector3f8.x + 2.0f * (1.0f - f6) * f6 * vector3f9.x + f6 * f6 * vector3f7.x;
            f4 = (1.0f - f6) * (1.0f - f6) * vector3f8.y + 2.0f * (1.0f - f6) * f6 * vector3f9.y + f6 * f6 * vector3f7.y;
            if (bl2) {
                f5 = (float)Math.sin((double)(1.0f - f6) * Math.PI * (double)f10 + (double)f8) * f9 + 1.0f + f21;
            }
            if (bl) {
                f3 = Wz.u((vector3f4.y - f4) / (vector3f4.y - vector3f3.y), 0.0f, 1.0f);
                this.dGN.put(new float[]{f15, f13 + (f14 - f13) * f3});
                this.dGN.put(new float[]{0.0f, f13 + (f14 - f13) * f3});
            }
            this.dGM.put(new float[]{f5, f4, vector3f5.z});
            f5 = (1.0f - f6) * (1.0f - f6) * vector3f4.x + 2.0f * (1.0f - f6) * f6 * vector3f10.x + f6 * f6 * vector3f3.x;
            f4 = (1.0f - f6) * (1.0f - f6) * vector3f4.y + 2.0f * (1.0f - f6) * f6 * vector3f10.y + f6 * f6 * vector3f3.y;
            if (bl2) {
                f5 = (float)Math.sin((double)(1.0f - f6) * Math.PI * (double)f10 + (double)f8) * f9 + f21;
            }
            this.dGM.put(new float[]{f5, f4, vector3f5.z});
            this.sK += 2;
            ++n3;
        }
        this.dGM.flip();
        this.dGN.flip();
    }

public void build(float amplitude,int phase){dGH=phase;d(amplitude,0,true);}
public float[] vertex(int i){return new float[]{dGM.get(i*3),dGM.get(i*3+1),dGM.get(i*3+2),dGN.get(i*2),dGN.get(i*2+1)};}
private static final class Wz {static float u(float v,float lo,float hi){return Math.max(lo,Math.min(hi,v));}}
}
