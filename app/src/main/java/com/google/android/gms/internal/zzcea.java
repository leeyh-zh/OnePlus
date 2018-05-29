package com.google.android.gms.internal;

import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.internal.safeparcel.zza;
import com.google.android.gms.common.internal.safeparcel.zzd;

public final class zzcea extends zza implements Result {
    public static final Creator<zzcea> CREATOR;
    private static zzcea zzbiN;
    private final Status mStatus;

    static {
        zzbiN = new zzcea(Status.zzaBo);
        CREATOR = new zzceb();
    }

    public zzcea(Status status) {
        this.mStatus = status;
    }

    public final Status getStatus() {
        return this.mStatus;
    }

    public final void writeToParcel(Parcel parcel, int i) {
        int zze = zzd.zze(parcel);
        zzd.zza(parcel, 1, getStatus(), i, false);
        zzd.zzI(parcel, zze);
    }
}
