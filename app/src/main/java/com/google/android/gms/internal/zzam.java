package com.google.android.gms.internal;

import com.google.android.gms.location.DetectedActivity;
import java.util.Map;
import net.oneplus.weather.R;
import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;

public final class zzam {
    public static String zza(Map<String, String> map) {
        String str = "ISO-8859-1";
        String str2 = (String) map.get("Content-Type");
        if (str2 != null) {
            String[] split = str2.split(";");
            for (int i = 1; i < split.length; i++) {
                String[] split2 = split[i].trim().split("=");
                if (split2.length == 2 && split2[0].equals("charset")) {
                    return split2[1];
                }
            }
        }
        return str;
    }

    public static zzc zzb(zzn com_google_android_gms_internal_zzn) {
        Object obj;
        long j;
        long currentTimeMillis = System.currentTimeMillis();
        Map map = com_google_android_gms_internal_zzn.zzy;
        long j2 = 0;
        long j3 = 0;
        long j4 = 0;
        String str = (String) map.get("Date");
        if (str != null) {
            j2 = zzf(str);
        }
        str = (String) map.get("Cache-Control");
        if (str != null) {
            String[] split = str.split(",");
            int i = 0;
            obj = null;
            j = 0;
            j4 = 0;
            while (i < split.length) {
                String trim = split[i].trim();
                if (!trim.equals("no-cache") && !trim.equals("no-store")) {
                    if (trim.startsWith("max-age=")) {
                        try {
                            j4 = Long.parseLong(trim.substring(DetectedActivity.RUNNING));
                        } catch (Exception e) {
                        }
                    } else if (trim.startsWith("stale-while-revalidate=")) {
                        try {
                            j = Long.parseLong(trim.substring(R.styleable.Toolbar_titleMarginEnd));
                        } catch (Exception e2) {
                        }
                    } else if (trim.equals("must-revalidate") || trim.equals("proxy-revalidate")) {
                        obj = 1;
                    }
                    i++;
                }
                return null;
            }
            j3 = j4;
            j4 = j;
            int i2 = 1;
        } else {
            obj = null;
            Object obj2 = null;
        }
        str = (String) map.get("Expires");
        long zzf = str != null ? zzf(str) : 0;
        str = (String) map.get("Last-Modified");
        long zzf2 = str != null ? zzf(str) : 0;
        str = (String) map.get("ETag");
        if (obj2 != null) {
            j3 = currentTimeMillis + (1000 * j3);
            j = obj != null ? j3 : (1000 * j4) + j3;
        } else if (j2 <= 0 || zzf < j2) {
            j = 0;
            j3 = 0;
        } else {
            j = (zzf - j2) + currentTimeMillis;
            j3 = j;
        }
        zzc com_google_android_gms_internal_zzc = new zzc();
        com_google_android_gms_internal_zzc.data = com_google_android_gms_internal_zzn.data;
        com_google_android_gms_internal_zzc.zza = str;
        com_google_android_gms_internal_zzc.zze = j3;
        com_google_android_gms_internal_zzc.zzd = j;
        com_google_android_gms_internal_zzc.zzb = j2;
        com_google_android_gms_internal_zzc.zzc = zzf2;
        com_google_android_gms_internal_zzc.zzf = map;
        return com_google_android_gms_internal_zzc;
    }

    private static long zzf(String str) {
        try {
            return DateUtils.parseDate(str).getTime();
        } catch (DateParseException e) {
            return 0;
        }
    }
}
