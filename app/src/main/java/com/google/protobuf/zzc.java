package com.google.protobuf;

final class zzc {
    private static Class<?> zzcuw;

    static {
        zzcuw = zzLG();
    }

    private static Class<?> zzLG() {
        try {
            return Class.forName("com.google.protobuf.ExtensionRegistry");
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public static zzd zzLH() {
        if (zzcuw != null) {
            try {
                return (zzd) zzcuw.getMethod("getEmptyRegistry", new Class[0]).invoke(null, new Object[0]);
            } catch (Exception e) {
            }
        }
        return zzd.zzcuz;
    }
}
