package com.google.android.gms.location;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Status;

@Deprecated
public final class LocationStatusCodes {
    public static final int ERROR = 1;
    public static final int GEOFENCE_NOT_AVAILABLE = 1000;
    public static final int GEOFENCE_TOO_MANY_GEOFENCES = 1001;
    public static final int GEOFENCE_TOO_MANY_PENDING_INTENTS = 1002;
    public static final int SUCCESS = 0;

    private LocationStatusCodes() {
    }

    public static int zzbi(int i) {
        return (i < 0 || i > 1) ? (1000 > i || i > 1002) ? 1 : i : i;
    }

    public static Status zzbj(int i) {
        switch (i) {
            case ERROR:
                i = ConnectionResult.CANCELED;
                break;
        }
        return new Status(i);
    }
}
