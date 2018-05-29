package com.google.android.gms.internal;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;

public final class zzbfg extends Fragment implements zzbff {
    private static WeakHashMap<Activity, WeakReference<zzbfg>> zzaEJ;
    private int zzLj;
    private Map<String, zzbfe> zzaEK;
    private Bundle zzaEL;

    static {
        zzaEJ = new WeakHashMap();
    }

    public zzbfg() {
        this.zzaEK = new ArrayMap();
        this.zzLj = 0;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static com.google.android.gms.internal.zzbfg zzo(android.app.Activity r3) {
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.gms.internal.zzbfg.zzo(android.app.Activity):com.google.android.gms.internal.zzbfg");
        /*
        r0 = zzaEJ;
        r0 = r0.get(r3);
        r0 = (java.lang.ref.WeakReference) r0;
        if (r0 == 0) goto L_0x0013;
    L_0x000a:
        r0 = r0.get();
        r0 = (com.google.android.gms.internal.zzbfg) r0;
        if (r0 == 0) goto L_0x0013;
    L_0x0012:
        return r0;
    L_0x0013:
        r0 = r3.getFragmentManager();	 Catch:{ ClassCastException -> 0x0048 }
        r1 = "LifecycleFragmentImpl";
        r0 = r0.findFragmentByTag(r1);	 Catch:{ ClassCastException -> 0x0048 }
        r0 = (com.google.android.gms.internal.zzbfg) r0;	 Catch:{ ClassCastException -> 0x0048 }
        if (r0 == 0) goto L_0x0027;
    L_0x0021:
        r1 = r0.isRemoving();
        if (r1 == 0) goto L_0x003d;
    L_0x0027:
        r0 = new com.google.android.gms.internal.zzbfg;
        r0.<init>();
        r1 = r3.getFragmentManager();
        r1 = r1.beginTransaction();
        r2 = "LifecycleFragmentImpl";
        r1 = r1.add(r0, r2);
        r1.commitAllowingStateLoss();
    L_0x003d:
        r1 = zzaEJ;
        r2 = new java.lang.ref.WeakReference;
        r2.<init>(r0);
        r1.put(r3, r2);
        goto L_0x0012;
    L_0x0048:
        r0 = move-exception;
        r1 = new java.lang.IllegalStateException;
        r2 = "Fragment with tag LifecycleFragmentImpl is not a LifecycleFragmentImpl";
        r1.<init>(r2, r0);
        throw r1;
        */
    }

    public final void dump(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        super.dump(str, fileDescriptor, printWriter, strArr);
        for (zzbfe com_google_android_gms_internal_zzbfe : this.zzaEK.values()) {
            com_google_android_gms_internal_zzbfe.dump(str, fileDescriptor, printWriter, strArr);
        }
    }

    public final void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        for (zzbfe com_google_android_gms_internal_zzbfe : this.zzaEK.values()) {
            com_google_android_gms_internal_zzbfe.onActivityResult(i, i2, intent);
        }
    }

    public final void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.zzLj = 1;
        this.zzaEL = bundle;
        for (Entry entry : this.zzaEK.entrySet()) {
            ((zzbfe) entry.getValue()).onCreate(bundle != null ? bundle.getBundle((String) entry.getKey()) : null);
        }
    }

    public final void onDestroy() {
        super.onDestroy();
        this.zzLj = 5;
        for (zzbfe com_google_android_gms_internal_zzbfe : this.zzaEK.values()) {
            com_google_android_gms_internal_zzbfe.onDestroy();
        }
    }

    public final void onResume() {
        super.onResume();
        this.zzLj = 3;
        for (zzbfe com_google_android_gms_internal_zzbfe : this.zzaEK.values()) {
            com_google_android_gms_internal_zzbfe.onResume();
        }
    }

    public final void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        if (bundle != null) {
            for (Entry entry : this.zzaEK.entrySet()) {
                Bundle bundle2 = new Bundle();
                ((zzbfe) entry.getValue()).onSaveInstanceState(bundle2);
                bundle.putBundle((String) entry.getKey(), bundle2);
            }
        }
    }

    public final void onStart() {
        super.onStart();
        this.zzLj = 2;
        for (zzbfe com_google_android_gms_internal_zzbfe : this.zzaEK.values()) {
            com_google_android_gms_internal_zzbfe.onStart();
        }
    }

    public final void onStop() {
        super.onStop();
        this.zzLj = 4;
        for (zzbfe com_google_android_gms_internal_zzbfe : this.zzaEK.values()) {
            com_google_android_gms_internal_zzbfe.onStop();
        }
    }

    public final <T extends zzbfe> T zza(String str, Class<T> cls) {
        return (zzbfe) cls.cast(this.zzaEK.get(str));
    }

    public final void zza(String str, @NonNull zzbfe com_google_android_gms_internal_zzbfe) {
        if (this.zzaEK.containsKey(str)) {
            throw new IllegalArgumentException(new StringBuilder(String.valueOf(str).length() + 59).append("LifecycleCallback with tag ").append(str).append(" already added to this fragment.").toString());
        }
        this.zzaEK.put(str, com_google_android_gms_internal_zzbfe);
        if (this.zzLj > 0) {
            new Handler(Looper.getMainLooper()).post(new zzbfh(this, com_google_android_gms_internal_zzbfe, str));
        }
    }

    public final Activity zzqD() {
        return getActivity();
    }
}
