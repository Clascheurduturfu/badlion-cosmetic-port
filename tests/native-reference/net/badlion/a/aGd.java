/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package net.badlion.a;

import java.io.BufferedReader;
import java.io.InputStream;
import java.util.ArrayList;
import net.badlion.a.aGg;
import net.badlion.clientcommon.util.x;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class aGd {
    private static final Logger dMu = LogManager.getLogger(aGd.class);

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public aGg mB(String string) {
        InputStream inputStream = x.ne(string);
        if (inputStream == null) {
            dMu.error("Failed loading animation model for the following location: " + string);
            return null;
        }
        BufferedReader bufferedReader = x.l(inputStream);
        try {
            int n = 0;
            int n2 = 0;
            int n3 = 0;
            int n4 = 0;
            ArrayList<aGg.d> arrayList = new ArrayList<aGg.d>();
            ArrayList<aGg.b> arrayList2 = new ArrayList<aGg.b>();
            ArrayList<aGg.a> arrayList3 = new ArrayList<aGg.a>();
            ArrayList<aGg.c> arrayList4 = new ArrayList<aGg.c>();
            block9: while (true) {
                try {
                    String string2;
                    while ((string2 = bufferedReader.readLine()) != null) {
                        if (string2.isEmpty() || string2.startsWith("commandline") || string2.startsWith("}")) continue;
                        if (string2.startsWith("MD5Version")) {
                            if (Integer.parseInt(string2.split("\\s+")[1]) != 10) {
                                System.out.println("Incorrect MD5Version used");
                            }
                        } else if (string2.startsWith("numFrames")) {
                            n = Integer.parseInt(string2.split("\\s+")[1]);
                        } else if (string2.startsWith("numJoints")) {
                            n2 = Integer.parseInt(string2.split("\\s+")[1]);
                        } else if (string2.startsWith("frameRate")) {
                            n4 = Integer.parseInt(string2.split("\\s+")[1]);
                        } else if (string2.startsWith("numAnimatedComponents")) {
                            n3 = Integer.parseInt(string2.split("\\s+")[1]);
                        } else if (string2.startsWith("hierarchy {")) {
                            for (int i = 0; i < n2; ++i) {
                                string2 = bufferedReader.readLine();
                                arrayList.add(new aGg.d(string2));
                            }
                        } else if (string2.startsWith("bounds {")) {
                            for (int i = 0; i < n; ++i) {
                                string2 = bufferedReader.readLine();
                                arrayList2.add(new aGg.b(string2));
                            }
                        } else if (string2.startsWith("baseframe {")) {
                            for (int i = 0; i < n2; ++i) {
                                string2 = bufferedReader.readLine();
                                arrayList3.add(new aGg.a(string2));
                            }
                        } else if (string2.startsWith("frame ")) {
                            ArrayList<Float> arrayList5 = new ArrayList<Float>();
                            while (arrayList5.size() != n3) {
                                string2 = bufferedReader.readLine();
                                for (String string3 : string2.split("\\s+")) {
                                    try {
                                        arrayList5.add(Float.valueOf(Float.parseFloat(string3)));
                                    }
                                    catch (NumberFormatException numberFormatException) {
                                        // empty catch block
                                    }
                                }
                            }
                            arrayList4.add(new aGg.c(arrayList5));
                            if (arrayList4.size() == n) {
                                aGg aGg2 = new aGg(n, n2, n4, n3, arrayList, arrayList2, arrayList3, arrayList4);
                                return aGg2;
                            }
                        }
                        continue block9;
                    }
                    break;
                }
                catch (Exception exception) {
                    dMu.error("Unable to parse MD5 animation model from files " + string);
                    exception.printStackTrace();
                    aGg aGg3 = null;
                    return aGg3;
                }
            }
            dMu.error("Unable to parse MD5 animation model from file " + string);
            aGg aGg2 = null;
            return aGg2;
        }
        finally {
            x.T(inputStream);
        }
    }
}
