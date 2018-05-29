package com.google.android.gms.internal;

import android.os.Bundle;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.Api.zzb;
import com.google.android.gms.common.api.Result;

public interface zzbei {
    void begin();

    void connect();

    boolean disconnect();

    void onConnected(Bundle bundle);

    void onConnectionSuspended(int i);

    void zza(ConnectionResult connectionResult, Api<?> api, boolean z);

    <A extends zzb, R extends Result, T extends zzbck<R, A>> T zzd(T t);

    <A extends zzb, T extends zzbck<? extends Result, A>> T zze(T t);
}
