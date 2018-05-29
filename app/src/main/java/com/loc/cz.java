package com.loc;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

// compiled from: Reflect.java
public final class cz {
    public static Object a(Object obj, Class<?> cls, String str, Object... objArr) throws Exception {
        Class[] clsArr = new Class[objArr.length];
        int length = objArr.length;
        for (int i = 0; i < length; i++) {
            clsArr[i] = objArr[i].getClass();
            if (clsArr[i] == Integer.class) {
                clsArr[i] = Integer.TYPE;
            }
        }
        Method declaredMethod = cls.getDeclaredMethod(str, clsArr);
        if (!declaredMethod.isAccessible()) {
            declaredMethod.setAccessible(true);
        }
        try {
            return declaredMethod.invoke(obj, objArr);
        } catch (Throwable th) {
            try {
                if (th instanceof InvocationTargetException) {
                    cw.a(((InvocationTargetException) th).getTargetException(), "Reflect", new StringBuilder("invokeMethod ").append(str).toString());
                }
                return null;
            } catch (Throwable th2) {
                return null;
            }
        }
    }

    public static Object a(Object obj, String str, Object... objArr) {
        try {
            return a(obj, obj.getClass(), str, objArr);
        } catch (Throwable th) {
            return null;
        }
    }

    public static Object a(String str, String str2) throws Exception {
        Class forName = Class.forName(str);
        Field field = forName.getField(str2);
        field.setAccessible(true);
        return field.get(forName);
    }

    public static Object a(String str, String str2, Object[] objArr, Class<?>[] clsArr) throws Exception {
        Method declaredMethod = Class.forName(str).getDeclaredMethod(str2, clsArr);
        if (!declaredMethod.isAccessible()) {
            declaredMethod.setAccessible(true);
        }
        return declaredMethod.invoke(null, objArr);
    }

    public static int b(Object obj, String str, Object... objArr) throws Exception {
        return ((Integer) a(obj, str, objArr)).intValue();
    }
}
