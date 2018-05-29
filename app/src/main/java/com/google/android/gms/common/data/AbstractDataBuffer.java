package com.google.android.gms.common.data;

import android.os.Bundle;
import java.util.Iterator;

public abstract class AbstractDataBuffer<T> implements DataBuffer<T> {
    protected final DataHolder zzaCZ;

    protected AbstractDataBuffer(DataHolder dataHolder) {
        this.zzaCZ = dataHolder;
    }

    @Deprecated
    public final void close() {
        release();
    }

    public abstract T get(int i);

    public int getCount() {
        return this.zzaCZ == null ? 0 : this.zzaCZ.zzaFI;
    }

    @Deprecated
    public boolean isClosed() {
        return this.zzaCZ == null || this.zzaCZ.isClosed();
    }

    public Iterator<T> iterator() {
        return new zzb(this);
    }

    public void release() {
        if (this.zzaCZ != null) {
            this.zzaCZ.close();
        }
    }

    public Iterator<T> singleRefIterator() {
        return new zzh(this);
    }

    public final Bundle zzqL() {
        return this.zzaCZ.zzqL();
    }
}
