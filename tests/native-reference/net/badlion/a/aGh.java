/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package net.badlion.a;

import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class aGh {
    public int dML;
    public Vector3f dMF;
    public Quaternionf dMP;

    public aGh() {
    }

    public aGh(Vector3f vector3f, Vector3f vector3f2) {
        this.dMF = new Vector3f((Vector3fc)vector3f);
        this.dMP = new Quaternionf(vector3f2.x, vector3f2.y, vector3f2.z, 0.0f);
    }

    public aGh a(aGh aGh2) {
        this.dML = aGh2.dML;
        Vector3f vector3f = this.dMF;
        if (vector3f == null) {
            this.dMF = new Vector3f((Vector3fc)aGh2.dMF);
        } else {
            vector3f.set((Vector3fc)aGh2.dMF);
        }
        Quaternionf quaternionf = this.dMP;
        Quaternionf quaternionf2 = aGh2.dMP;
        if (quaternionf == null) {
            this.dMP = new Quaternionf(quaternionf2.x, quaternionf2.y, quaternionf2.z, quaternionf2.w);
        } else {
            quaternionf.set(quaternionf2.x, quaternionf2.y, quaternionf2.z, quaternionf2.w);
        }
        return this;
    }

    public static aGh a(aGh aGh2, aGh aGh3, float f, aGh aGh4) {
        aGh4.a(aGh2);
        aGh4.dML = aGh2.dML;
        Vector3f vector3f = aGh4.dMF;
        if (vector3f == null) {
            vector3f = new Vector3f();
        }
        aGh4.dMF = aGh2.dMF.lerp((Vector3fc)aGh3.dMF, f, vector3f);
        Quaternionf quaternionf = aGh4.dMP;
        if (quaternionf == null) {
            quaternionf = new Quaternionf();
        }
        aGh4.dMP = aGh2.dMP.slerp((Quaternionfc)aGh3.dMP, f, quaternionf);
        return aGh4;
    }

    public static aGh a(aGh aGh2, aGh aGh3, float f) {
        return aGh.a(aGh2, aGh3, f, new aGh());
    }
}
