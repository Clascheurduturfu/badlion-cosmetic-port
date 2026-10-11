import java.nio.FloatBuffer; import org.joml.Vector3f; public final class NativeCurvedReference {
private int dHl,dHp; private float dHm,dHn; private FloatBuffer bky;
public NativeCurvedReference(int segments,float offset) { dHl=segments; dHm=offset; }
    public void bFL() {
        float f;
        float f2;
        float f3;
        int n2;
        FloatBuffer floatBuffer = FloatBuffer.allocate((12 + (this.dHl + 1) * 8) * 5);
        float f4 = 22.0f;
        float f5 = 23.0f;
        float f6 = 1.0f / f5;
        float f7 = 17.0f / f5;
        float f8 = 1.0f / f4;
        float f9 = 11.0f / f4;
        float f10 = 12.0f / f4;
        float f11 = 22.0f / f4;
        float f12 = 21.0f / f4;
        Vector3f vector3f = new Vector3f(0.0f, 0.0f, 5.0f);
        Vector3f vector3f2 = new Vector3f(0.0f + this.dHm, -16.0f + this.dHn, 5.0f);
        Vector3f vector3f3 = new Vector3f(0.0f + this.dHm, -16.0f + this.dHn, -5.0f);
        Vector3f vector3f4 = new Vector3f(0.0f, 0.0f, -5.0f);
        Vector3f vector3f5 = new Vector3f(1.0f, 0.0f, 5.0f);
        Vector3f vector3f6 = new Vector3f(1.0f + this.dHm, -16.0f + this.dHn, 5.0f);
        Vector3f vector3f7 = new Vector3f(1.0f + this.dHm, -16.0f + this.dHn, -5.0f);
        Vector3f vector3f8 = new Vector3f(1.0f, 0.0f, -5.0f);
        Vector3f vector3f9 = new Vector3f(0.0f, -10.0f, -5.0f);
        Vector3f vector3f10 = new Vector3f(0.0f, -10.0f, -5.0f);
        for (n2 = 0; n2 <= this.dHl; ++n2) {
            f3 = (float)n2 / (float)this.dHl;
            f2 = (1.0f - f3) * (1.0f - f3) * vector3f7.x + 2.0f * (1.0f - f3) * f3 * vector3f9.x + f3 * f3 * vector3f8.x;
            f = (1.0f - f3) * (1.0f - f3) * vector3f7.y + 2.0f * (1.0f - f3) * f3 * vector3f9.y + f3 * f3 * vector3f8.y;
            floatBuffer.put(f2);
            floatBuffer.put(f);
            floatBuffer.put(vector3f5.z);
            floatBuffer.put(f8);
            floatBuffer.put(f7 - (f7 - f6) * f3);
            floatBuffer.put(f2);
            floatBuffer.put(f);
            floatBuffer.put(vector3f8.z);
            floatBuffer.put(f9);
            floatBuffer.put(f7 - (f7 - f6) * f3);
        }
        floatBuffer.put(vector3f5.x);
        floatBuffer.put(vector3f5.y);
        floatBuffer.put(vector3f5.z);
        floatBuffer.put(f8);
        floatBuffer.put(f6);
        floatBuffer.put(vector3f8.x);
        floatBuffer.put(vector3f8.y);
        floatBuffer.put(vector3f8.z);
        floatBuffer.put(f9);
        floatBuffer.put(f6);
        floatBuffer.put(vector3f.x);
        floatBuffer.put(vector3f.y);
        floatBuffer.put(vector3f.z);
        floatBuffer.put(f8);
        floatBuffer.put(0.0f);
        floatBuffer.put(vector3f4.x);
        floatBuffer.put(vector3f4.y);
        floatBuffer.put(vector3f4.z);
        floatBuffer.put(f9);
        floatBuffer.put(0.0f);
        for (n2 = 0; n2 <= this.dHl; ++n2) {
            f3 = (float)n2 / (float)this.dHl;
            f2 = (1.0f - f3) * (1.0f - f3) * vector3f4.x + 2.0f * (1.0f - f3) * f3 * vector3f10.x + f3 * f3 * vector3f3.x;
            f = (1.0f - f3) * (1.0f - f3) * vector3f4.y + 2.0f * (1.0f - f3) * f3 * vector3f10.y + f3 * f3 * vector3f3.y;
            floatBuffer.put(f2);
            floatBuffer.put(f);
            floatBuffer.put(vector3f8.z);
            floatBuffer.put(f10);
            floatBuffer.put(f6 + (f7 - f6) * f3);
            f2 = (1.0f - f3) * (1.0f - f3) * vector3f8.x + 2.0f * (1.0f - f3) * f3 * vector3f9.x + f3 * f3 * vector3f7.x;
            f = (1.0f - f3) * (1.0f - f3) * vector3f8.y + 2.0f * (1.0f - f3) * f3 * vector3f9.y + f3 * f3 * vector3f7.y;
            floatBuffer.put(f2);
            floatBuffer.put(f);
            floatBuffer.put(vector3f8.z);
            floatBuffer.put(f9);
            floatBuffer.put(f6 + (f7 - f6) * f3);
        }
        floatBuffer.put(vector3f7.x);
        floatBuffer.put(vector3f7.y);
        floatBuffer.put(vector3f7.z);
        floatBuffer.put(f12);
        floatBuffer.put(0.0f);
        floatBuffer.put(vector3f6.x);
        floatBuffer.put(vector3f6.y);
        floatBuffer.put(vector3f6.z);
        floatBuffer.put(f9);
        floatBuffer.put(0.0f);
        floatBuffer.put(vector3f3.x);
        floatBuffer.put(vector3f3.y);
        floatBuffer.put(vector3f3.z);
        floatBuffer.put(f12);
        floatBuffer.put(f6);
        floatBuffer.put(vector3f2.x);
        floatBuffer.put(vector3f2.y);
        floatBuffer.put(vector3f2.z);
        floatBuffer.put(f9);
        floatBuffer.put(f6);
        for (n2 = 0; n2 <= this.dHl; ++n2) {
            f3 = (float)n2 / (float)this.dHl;
            f2 = (1.0f - f3) * (1.0f - f3) * vector3f3.x + 2.0f * (1.0f - f3) * f3 * vector3f10.x + f3 * f3 * vector3f4.x;
            f = (1.0f - f3) * (1.0f - f3) * vector3f3.y + 2.0f * (1.0f - f3) * f3 * vector3f10.y + f3 * f3 * vector3f4.y;
            floatBuffer.put(f2);
            floatBuffer.put(f);
            floatBuffer.put(vector3f8.z);
            floatBuffer.put(f10);
            floatBuffer.put(f7 - (f7 - f6) * f3);
            floatBuffer.put(f2);
            floatBuffer.put(f);
            floatBuffer.put(vector3f5.z);
            floatBuffer.put(f11);
            floatBuffer.put(f7 - (f7 - f6) * f3);
        }
        for (n2 = 0; n2 <= this.dHl; ++n2) {
            f3 = (float)n2 / (float)this.dHl;
            if (n2 == 0) {
                f2 = (1.0f - f3) * (1.0f - f3) * vector3f4.x + 2.0f * (1.0f - f3) * f3 * vector3f10.x + f3 * f3 * vector3f3.x;
                f = (1.0f - f3) * (1.0f - f3) * vector3f4.y + 2.0f * (1.0f - f3) * f3 * vector3f10.y + f3 * f3 * vector3f3.y;
                floatBuffer.put(f2);
                floatBuffer.put(f);
                floatBuffer.put(vector3f5.z);
                floatBuffer.put(0.0f);
                floatBuffer.put(f6 + (f7 - f6) * f3);
                floatBuffer.put(f2);
                floatBuffer.put(f);
                floatBuffer.put(vector3f5.z);
                floatBuffer.put(0.0f);
                floatBuffer.put(f6 + (f7 - f6) * f3);
            }
            f2 = (1.0f - f3) * (1.0f - f3) * vector3f8.x + 2.0f * (1.0f - f3) * f3 * vector3f9.x + f3 * f3 * vector3f7.x;
            f = (1.0f - f3) * (1.0f - f3) * vector3f8.y + 2.0f * (1.0f - f3) * f3 * vector3f9.y + f3 * f3 * vector3f7.y;
            floatBuffer.put(f2);
            floatBuffer.put(f);
            floatBuffer.put(vector3f5.z);
            floatBuffer.put(f8);
            floatBuffer.put(f6 + (f7 - f6) * f3);
            f2 = (1.0f - f3) * (1.0f - f3) * vector3f4.x + 2.0f * (1.0f - f3) * f3 * vector3f10.x + f3 * f3 * vector3f3.x;
            f = (1.0f - f3) * (1.0f - f3) * vector3f4.y + 2.0f * (1.0f - f3) * f3 * vector3f10.y + f3 * f3 * vector3f3.y;
            floatBuffer.put(f2);
            floatBuffer.put(f);
            floatBuffer.put(vector3f5.z);
            floatBuffer.put(0.0f);
            floatBuffer.put(f6 + (f7 - f6) * f3);
        }
        floatBuffer.flip();
        this.bky = floatBuffer;
        this.dHp = this.bky.limit() / 5;
    }

public float[] vertex(int i) { float[] result=new float[5]; for(int c=0;c<5;c++)result[c]=bky.get(i*5+c);return result;}
public int size(){return dHp;}
}