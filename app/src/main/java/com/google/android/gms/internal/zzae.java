package com.google.android.gms.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public final class zzae {
    private static Comparator<byte[]> zzau;
    private List<byte[]> zzaq;
    private List<byte[]> zzar;
    private int zzas;
    private final int zzat;

    static {
        zzau = new zzaf();
    }

    public zzae(int i) {
        this.zzaq = new LinkedList();
        this.zzar = new ArrayList(64);
        this.zzas = 0;
        this.zzat = i;
    }

    private final synchronized void zzm() {
        while (this.zzas > this.zzat) {
            byte[] bArr = (byte[]) this.zzaq.remove(0);
            this.zzar.remove(bArr);
            this.zzas -= bArr.length;
        }
    }

    public final synchronized void zza(byte[] bArr) {
        if (bArr != null) {
            if (bArr.length <= this.zzat) {
                this.zzaq.add(bArr);
                int binarySearch = Collections.binarySearch(this.zzar, bArr, zzau);
                if (binarySearch < 0) {
                    binarySearch = (-binarySearch) - 1;
                }
                this.zzar.add(binarySearch, bArr);
                this.zzas += bArr.length;
                zzm();
            }
        }
    }

    public final synchronized byte[] zzb(int i) {
        byte[] bArr;
        for (int i2 = 0; i2 < this.zzar.size(); i2++) {
            bArr = (byte[]) this.zzar.get(i2);
            if (bArr.length >= i) {
                this.zzas -= bArr.length;
                this.zzar.remove(i2);
                this.zzaq.remove(bArr);
                break;
            }
        }
        bArr = new byte[i];
        return bArr;
    }
}
