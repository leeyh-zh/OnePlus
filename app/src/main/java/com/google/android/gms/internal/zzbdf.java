package com.google.android.gms.internal;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.TaskCompletionSource;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;

public final class zzbdf {
    private final Map<zzbcq<?>, Boolean> zzaCT;
    private final Map<TaskCompletionSource<?>, Boolean> zzaCU;

    public zzbdf() {
        this.zzaCT = Collections.synchronizedMap(new WeakHashMap());
        this.zzaCU = Collections.synchronizedMap(new WeakHashMap());
    }

    private final void zza(boolean z, Status status) {
        synchronized (this.zzaCT) {
            Map hashMap = new HashMap(this.zzaCT);
        }
        synchronized (this.zzaCU) {
            Map hashMap2 = new HashMap(this.zzaCU);
        }
        for (Entry entry : hashMap.entrySet()) {
            if (z || ((Boolean) entry.getValue()).booleanValue()) {
                ((zzbcq) entry.getKey()).zzs(status);
            }
        }
        for (Entry entry2 : hashMap2.entrySet()) {
            if (z || ((Boolean) entry2.getValue()).booleanValue()) {
                ((TaskCompletionSource) entry2.getKey()).trySetException(new ApiException(status));
            }
        }
    }

    final void zza(zzbcq<? extends Result> com_google_android_gms_internal_zzbcq__extends_com_google_android_gms_common_api_Result, boolean z) {
        this.zzaCT.put(com_google_android_gms_internal_zzbcq__extends_com_google_android_gms_common_api_Result, Boolean.valueOf(z));
        com_google_android_gms_internal_zzbcq__extends_com_google_android_gms_common_api_Result.zza(new zzbdg(this, com_google_android_gms_internal_zzbcq__extends_com_google_android_gms_common_api_Result));
    }

    final <TResult> void zza(TaskCompletionSource<TResult> taskCompletionSource, boolean z) {
        this.zzaCU.put(taskCompletionSource, Boolean.valueOf(z));
        taskCompletionSource.getTask().addOnCompleteListener(new zzbdh(this, taskCompletionSource));
    }

    final boolean zzpM() {
        return (this.zzaCT.isEmpty() && this.zzaCU.isEmpty()) ? false : true;
    }

    public final void zzpN() {
        zza(false, zzben.zzaEe);
    }

    public final void zzpO() {
        zza(true, zzbgh.zzaFl);
    }
}
