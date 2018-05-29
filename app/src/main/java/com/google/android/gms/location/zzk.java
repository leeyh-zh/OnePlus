package com.google.android.gms.location;

import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.google.android.gms.internal.zzee;
import com.google.android.gms.internal.zzef;
import net.oneplus.weather.widget.openglbase.RainSurfaceView;

public abstract class zzk extends zzee implements zzj {
    public zzk() {
        attachInterface(this, "com.google.android.gms.location.ILocationCallback");
    }

    public static zzj zzY(IBinder iBinder) {
        if (iBinder == null) {
            return null;
        }
        IInterface queryLocalInterface = iBinder.queryLocalInterface("com.google.android.gms.location.ILocationCallback");
        return queryLocalInterface instanceof zzj ? (zzj) queryLocalInterface : new zzl(iBinder);
    }

    public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
        if (zza(i, parcel, parcel2, i2)) {
            return true;
        }
        switch (i) {
            case RainSurfaceView.RAIN_LEVEL_NORMAL_RAIN:
                onLocationResult((LocationResult) zzef.zza(parcel, LocationResult.CREATOR));
                break;
            case RainSurfaceView.RAIN_LEVEL_SHOWER:
                onLocationAvailability((LocationAvailability) zzef.zza(parcel, LocationAvailability.CREATOR));
                break;
            default:
                return false;
        }
        return true;
    }
}
