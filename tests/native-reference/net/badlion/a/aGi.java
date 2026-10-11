/*
 * Decompiled with CFR 0.152.
 */
package net.badlion.a;

import java.util.ArrayList;
import java.util.List;
import net.badlion.a.aCE;
import net.badlion.a.aGf;
import net.badlion.a.aGg;
import net.badlion.a.aGh;
import net.badlion.a.bT;

public class aGi
implements aGf {
    private final List<aGh> dMQ = new ArrayList<aGh>();
    private final aGg dMR;
    private int dFY;
    private boolean dMS;
    private float dMT;
    private float dMU;
    private int dFZ;
    private int dGa;
    private int dGb;
    private boolean dGc;
    private final aCE.b dMV;

    public aGi(aGg aGg2, aCE.b b2) {
        this.dMR = aGg2;
        this.dMV = b2;
    }

    public void hP(boolean bl) {
        aGg aGg2 = this.dMR;
        int n = aGg2.bIB();
        int n2 = aGg2.bIA();
        this.dFY = n;
        this.dMU = (float)(n + this.dFY) / (float)n2;
        this.dMT = this.dMU - 0.001f;
        this.dGa = n;
        this.dFZ = this.dFY;
        this.dGc = bl;
        this.bIE();
    }

    public void am(int n, int n2, int n3) {
        aGg aGg2 = this.dMR;
        int n4 = aGg2.bIB();
        int n5 = aGg2.bIA();
        this.dMU = (float)n4 / (float)n5;
        this.dMT = 0.0f;
        this.dGa = n2;
        this.dFZ = n;
        this.dGb = n3;
        this.dGc = true;
        this.bIE();
    }

    public void v(int n, boolean bl) {
        aGg aGg2 = this.dMR;
        int n2 = aGg2.bIB();
        int n3 = aGg2.bIA();
        this.dFY = n;
        this.dMU = (float)(n2 + this.dFY) / (float)n3;
        this.dMT = this.dMU - 0.001f;
        this.dGa = n2;
        this.dFZ = this.dFY;
        this.dGc = bl;
        this.bIE();
    }

    public void bIE() {
        List<aGh> list = this.dMQ;
        list.clear();
        aGg aGg2 = this.dMR;
        int n = aGg2.bIC();
        List<List<aGh>> list2 = aGg2.bID();
        List<aGh> list3 = list2.get(0);
        for (int i = 0; i < n; ++i) {
            list.add(new aGh().a(list3.get(i)));
        }
    }

    public void eA(float f) {
        float f2 = 20.0f;
        float f3 = this.dMT * (float)this.dMR.bIA();
        float f4 = f3 < (float)this.dFZ ? (float)this.dFZ - f3 : (f3 > (float)this.dGa ? (float)this.dGa - f3 : 0.0f);
        f4 = Math.min(f2, Math.abs(f4));
        bT.lM.y(f * (f2 - f4) / f2, 1.0f, 0.0f, 0.0f);
    }

    public boolean eB(float f) {
        aGg aGg2 = this.dMR;
        int n2 = aGg2.bIB();
        int n3 = aGg2.bIA();
        float f2 = this.dMT;
        int n4 = this.dGb;
        int n5 = this.dGa + 1;
        float f3 = (float)this.dGa / (float)n3;
        if ((f = (float)((double)f / 1.5)) > 0.0f || !this.dMS && this.dMT > (float)n4 / (float)n3 && this.dMT < (float)(this.dGa + 2) / (float)n3) {
            if (!this.dMS && this.dMT > f3 && this.dMT < (float)(this.dGa + 2) / (float)n3) {
                this.dMT -= (float)(this.dGa - this.dFZ + 1) / (float)n3;
            } else if (this.dMS && this.dMT > f3) {
                this.dMT -= f * 2.0f;
                if (this.dMT < 0.0f) {
                    this.dMT = 0.0f;
                }
                if (this.dMT < f3) {
                    this.dMS = false;
                }
            }
            this.dMT += Math.abs(f);
            this.dMT %= this.dMU;
        } else if (f <= 0.0f) {
            if (this.dMT < (float)(n4 / 2) / (float)n3) {
                if (this.dMT + f > 0.0f) {
                    this.dMT += f;
                    this.dMS = true;
                } else {
                    this.dMS = false;
                }
            } else if (this.dMT + -f < this.dMU) {
                if (this.dMT + -f > (float)(n4 + 1) / (float)n3 && this.dMT < (float)(n4 + 1) / (float)n3) {
                    this.dMT += (float)(n5 - n4 - 1) / (float)n3;
                }
                this.dMT -= f;
                if (this.dMT >= this.dMU) {
                    this.dMT = this.dMU - 0.001f;
                }
                this.dMS = true;
            } else {
                this.dMS = false;
            }
        }
        int n6 = (int)(this.dMT * (float)n3);
        float f4 = this.dMT % (1.0f / (float)n3) * (float)n3;
        if (this.dMS && n6 == n4) {
            this.i(n4, n5, f4);
        } else {
            this.i(n6 % n2, (n6 + 1) % n2, f4);
        }
        return f2 != this.dMT;
    }

    @Override
    public boolean ez(float f) {
        if (this.dFY == 0) {
            return this.eB(f);
        }
        aGg aGg2 = this.dMR;
        int n2 = aGg2.bIB();
        if (n2 < 1) {
            return false;
        }
        int n3 = aGg2.bIA();
        float f2 = this.dMT;
        if (f < 0.0f) {
            if (this.dMT > this.dMU / 2.0f) {
                if (this.dMT + -f < this.dMU) {
                    this.dMT += -f;
                }
                this.dMS = this.dMT + -f < this.dMU;
            } else {
                if (this.dMT + f > 0.0f) {
                    this.dMT += f;
                }
                this.dMS = this.dMT + f > 0.0f;
            }
        } else if (f > 0.0f) {
            if (!this.dMS && this.dMT > (float)n2 / (float)n3) {
                if (this.dGc) {
                    if (this.dMT < (float)(n2 + this.dFY - 2) / (float)n3) {
                        this.dMT -= (float)(n2 - this.dFY) / (float)n3;
                    }
                } else {
                    this.dMT -= (float)n2 / (float)n3;
                }
            } else if (this.dMS && this.dMT > this.dMU / 2.0f) {
                this.dMT = this.dMU - this.dMT;
            }
            this.dMS = false;
            this.dMT += f;
            this.dMT %= this.dMU;
        }
        int n4 = (int)Math.floor(this.dMT * (float)n3);
        float f3 = this.dMT * (float)n3 % 1.0f;
        int n5 = this.iA(n4) % n2;
        int n6 = this.iA(n4 + 1) % n2;
        this.i(n5, n6, f3);
        return f2 != this.dMT;
    }

    public void eC(float f) {
        int n2 = this.dMR.bIB();
        if (n2 < 1) {
            return;
        }
        this.i(this.iA((int)f), this.iA((int)f + 1) % n2, f % 1.0f);
    }

    private void i(int n2, int n3, float f) {
        aGg aGg2 = this.dMR;
        List<aGh> list = this.dMQ;
        int n4 = aGg2.bIC();
        List<List<aGh>> list2 = aGg2.bID();
        for (int i = 0; i < n4; ++i) {
            aGh aGh2 = list2.get(n2).get(i);
            aGh aGh3 = list2.get(n3).get(i);
            aGh.a(aGh2, aGh3, f, list.get(i));
        }
    }

    private int iA(int n2) {
        int n3 = this.dMR.bIB();
        if ((n2 %= n3 + this.dFY) >= n3) {
            n2 = this.dGc ? n3 + this.dFY - n2 : (n2 -= n3);
        }
        return n2;
    }

    @Override
    public void bIw() {
        this.dMT = 0.0f;
    }

    @Override
    public float bIy() {
        return this.dMT;
    }

    public float bIF() {
        return this.dMU;
    }

    @Override
    public List<aGh> bIx() {
        return this.dMQ;
    }

    public void iB(int n2) {
        this.dFY = n2;
    }

    public aCE.b bIG() {
        return this.dMV;
    }

    public aGg bIH() {
        return this.dMR;
    }
}
