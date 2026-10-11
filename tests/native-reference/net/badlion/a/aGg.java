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

import java.util.ArrayList;
import java.util.List;
import net.badlion.a.aGh;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class aGg {
    private final int dMw;
    private final int dMx;
    private final int dMy;
    private final int dMz;
    private final List<d> dMA;
    private final List<b> dMB;
    private final List<a> dMC;
    private final List<c> dMD;
    private List<List<aGh>> dME;

    public aGg(int n, int n2, int n3, int n4, List<d> list, List<b> list2, List<a> list3, List<c> list4) {
        this.dMw = n;
        this.dMx = n2;
        this.dMy = n3;
        this.dMz = n4;
        this.dMA = list;
        this.dMB = list2;
        this.dMC = list3;
        this.dMD = list4;
        this.bIz();
    }

    private void bIz() {
        ArrayList<List<aGh>> arrayList = new ArrayList<List<aGh>>();
        for (c c2 : this.dMD) {
            ArrayList<aGh> arrayList2 = new ArrayList<aGh>();
            for (int i = 0; i < this.dMA.size(); ++i) {
                int n = 0;
                d d2 = this.dMA.get(i);
                a a2 = this.dMC.get(i);
                aGh aGh2 = new aGh(a2.dMF, a2.dMG);
                aGh2.dML = d2.dML;
                aGh2.dMF.x = c2.dMJ.get(d2.dMN + n++).floatValue();
                aGh2.dMF.y = c2.dMJ.get(d2.dMN + n++).floatValue();
                aGh2.dMF.z = c2.dMJ.get(d2.dMN + n++).floatValue();
                aGh2.dMP.x = c2.dMJ.get(d2.dMN + n++).floatValue();
                aGh2.dMP.y = c2.dMJ.get(d2.dMN + n++).floatValue();
                aGh2.dMP.z = c2.dMJ.get(d2.dMN + n++).floatValue();
                arrayList2.add(aGh2);
                float f = 1.0f - aGh2.dMP.x * aGh2.dMP.x - aGh2.dMP.y * aGh2.dMP.y - aGh2.dMP.z * aGh2.dMP.z;
                float f2 = aGh2.dMP.w = f < 0.0f ? 0.0f : (float)(-Math.sqrt(f));
                if (aGh2.dML < 0) continue;
                aGh aGh3 = (aGh)arrayList2.get(aGh2.dML);
                Vector3f vector3f = aGh2.dMF.rotate((Quaternionfc)aGh3.dMP, new Vector3f());
                aGh2.dMF = aGh3.dMF.add((Vector3fc)vector3f, new Vector3f());
                aGh2.dMP = aGh3.dMP.mul((Quaternionfc)aGh2.dMP, new Quaternionf());
                aGh2.dMP = aGh2.dMP.normalize();
            }
            arrayList.add(arrayList2);
        }
        this.dME = arrayList;
    }

    public int bIA() {
        return this.dMy;
    }

    public int bIB() {
        return this.dMw;
    }

    public int bIC() {
        return this.dMx;
    }

    public List<List<aGh>> bID() {
        return this.dME;
    }

    public static class c {
        List<Float> dMJ;

        public c(List<Float> list) {
            this.dMJ = list;
        }
    }

    public static class a {
        private Vector3f dMF;
        private Vector3f dMG;

        public a(String string) {
            String[] stringArray = string.split("\\s+");
            this.dMF = new Vector3f(Float.parseFloat(stringArray[2]), Float.parseFloat(stringArray[3]), Float.parseFloat(stringArray[4]));
            this.dMG = new Vector3f(Float.parseFloat(stringArray[7]), Float.parseFloat(stringArray[8]), Float.parseFloat(stringArray[9]));
        }
    }

    public static class b {
        private final Vector3f dMH;
        private final Vector3f dMI;

        public b(String string) {
            String[] stringArray = string.split("\\s+");
            this.dMH = new Vector3f(Float.parseFloat(stringArray[2]), Float.parseFloat(stringArray[3]), Float.parseFloat(stringArray[4]));
            this.dMI = new Vector3f(Float.parseFloat(stringArray[7]), Float.parseFloat(stringArray[8]), Float.parseFloat(stringArray[9]));
        }
    }

    public static class d {
        private final String dMK;
        private final int dML;
        private final int dMM;
        private final int dMN;
        private final String dMO;

        public d(String string) {
            String[] stringArray = string.split("\\s+");
            this.dMK = stringArray[1].replace("\"", "");
            this.dML = Integer.parseInt(stringArray[2]);
            this.dMM = Integer.parseInt(stringArray[3]);
            this.dMN = Integer.parseInt(stringArray[4]);
            this.dMO = stringArray.length == 7 ? stringArray[6] : null;
        }
    }
}
