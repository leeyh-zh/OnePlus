package android.support.v4.app;

import android.os.Bundle;
import java.util.Set;

@Deprecated
class RemoteInputCompatBase {

    public static abstract class RemoteInput {

        public static interface Factory {
            android.support.v4.app.RemoteInputCompatBase.RemoteInput build(String str, CharSequence charSequence, CharSequence[] charSequenceArr, boolean z, Bundle bundle, Set<String> set);

            android.support.v4.app.RemoteInputCompatBase.RemoteInput[] newArray(int i);
        }

        protected abstract boolean getAllowFreeFormInput();

        protected abstract Set<String> getAllowedDataTypes();

        protected abstract CharSequence[] getChoices();

        protected abstract Bundle getExtras();

        protected abstract CharSequence getLabel();

        protected abstract String getResultKey();
    }

    RemoteInputCompatBase() {
    }
}
