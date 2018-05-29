package com.google.android.gms.common;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;
import com.google.android.gms.common.internal.zzbr;
import com.google.android.gms.internal.zzbim;
import net.oneplus.weather.api.WeatherRequest.Type;

public class zzp {
    private static zzp zzaAw;
    private final Context mContext;

    private zzp(Context context) {
        this.mContext = context.getApplicationContext();
    }

    static zzg zza(PackageInfo packageInfo, zzg... com_google_android_gms_common_zzgArr) {
        int i = 0;
        if (packageInfo.signatures == null) {
            return null;
        }
        if (packageInfo.signatures.length != 1) {
            Log.w("GoogleSignatureVerifier", "Package has more than one signature.");
            return null;
        }
        zzh com_google_android_gms_common_zzh = new zzh(packageInfo.signatures[0].toByteArray());
        while (i < com_google_android_gms_common_zzgArr.length) {
            if (com_google_android_gms_common_zzgArr[i].equals(com_google_android_gms_common_zzh)) {
                return com_google_android_gms_common_zzgArr[i];
            }
            i++;
        }
        return null;
    }

    private static boolean zza(PackageInfo packageInfo, boolean z) {
        if (!(packageInfo == null || packageInfo.signatures == null)) {
            zzg zza;
            if (z) {
                zza = zza(packageInfo, zzj.zzaAm);
            } else {
                zza = zza(packageInfo, zzj.zzaAm[0]);
            }
            if (zza != null) {
                return true;
            }
        }
        return false;
    }

    public static zzp zzax(Context context) {
        zzbr.zzu(context);
        synchronized (zzp.class) {
            if (zzaAw == null) {
                zzf.zzav(context);
                zzaAw = new zzp(context);
            }
        }
        return zzaAw;
    }

    private static boolean zzb(PackageInfo packageInfo, boolean z) {
        boolean z2 = false;
        if (packageInfo.signatures.length != 1) {
            Log.w("GoogleSignatureVerifier", "Package has more than one signature.");
        } else {
            zzg com_google_android_gms_common_zzh = new zzh(packageInfo.signatures[0].toByteArray());
            String str = packageInfo.packageName;
            z2 = z ? zzf.zzb(str, com_google_android_gms_common_zzh) : zzf.zza(str, com_google_android_gms_common_zzh);
            if (!z2) {
                Log.d("GoogleSignatureVerifier", new StringBuilder(27).append("Cert not in list. atk=").append(z).toString());
            }
        }
        return z2;
    }

    private final boolean zzct(String str) {
        try {
            PackageInfo packageInfo = zzbim.zzaP(this.mContext).getPackageInfo(str, Type.SUCCESS);
            if (packageInfo == null) {
                return false;
            }
            if (zzo.zzaw(this.mContext)) {
                return zzb(packageInfo, true);
            }
            boolean zzb = zzb(packageInfo, false);
            if (!zzb && zzb(packageInfo, true)) {
                Log.w("GoogleSignatureVerifier", "Test-keys aren't accepted on this build.");
            }
            return zzb;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    @Deprecated
    public final boolean zza(PackageManager packageManager, int i) {
        String[] packagesForUid = zzbim.zzaP(this.mContext).getPackagesForUid(i);
        if (packagesForUid == null || packagesForUid.length == 0) {
            return false;
        }
        int length = packagesForUid.length;
        for (int i2 = 0; i2 < length; i2++) {
            if (zzct(packagesForUid[i2])) {
                return true;
            }
        }
        return false;
    }

    @Deprecated
    public final boolean zza(PackageManager packageManager, PackageInfo packageInfo) {
        if (packageInfo != null) {
            if (zza(packageInfo, false)) {
                return true;
            }
            if (zza(packageInfo, true)) {
                if (zzo.zzaw(this.mContext)) {
                    return true;
                }
                Log.w("GoogleSignatureVerifier", "Test-keys aren't accepted on this build.");
            }
        }
        return false;
    }
}
