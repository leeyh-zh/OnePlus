package net.oneplus.weather.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.oneplus.weather.api.helper.LogUtils;
import net.oneplus.weather.db.CityWeatherDBHelper.CityListEntry;
import net.oneplus.weather.model.OpCity;
import net.oneplus.weather.util.StringUtils;
import net.oneplus.weather.util.WeatherLog;
import net.oneplus.weather.widget.openglbase.RainSurfaceView;

public class ChinaCityDB {
    public static final String CITY_DB_NAME = "china_city.db";
    private static final String CITY_TABLE_NAME = "city";
    private static final int CURRENT_VERSION = 13;
    private static final String OP_CITY_TABLE_NAME = "area";
    private static final String OP_REGION_TABLE_NAME = "region";
    private static final String TAG;
    private static SQLiteDatabase city_list_db;
    private static SQLiteDatabase db;
    private static File dbFile;
    private static ChinaCityDB mSelf;

    static {
        TAG = ChinaCityDB.class.getSimpleName();
    }

    public static ChinaCityDB openCityDB(Context context) {
        return openCityDB(context, false);
    }

    public static ChinaCityDB openCityDB(Context context, boolean alwaysCopy) {
        String path = getDBPath(context);
        dbFile = new File(path);
        if (!dbFile.exists() || alwaysCopy) {
            copyAssetDbFile(context);
            if (mSelf == null) {
                mSelf = new ChinaCityDB(context, path);
                db.setVersion(CURRENT_VERSION);
            }
        }
        return createDbAndSetVersion(context, path);
    }

    public static String getDBPath(Context context) {
        return context.getFilesDir().getAbsolutePath() + File.separator + CITY_DB_NAME;
    }

    private static synchronized ChinaCityDB createDbAndSetVersion(Context context, String path) {
        ChinaCityDB chinaCityDB;
        synchronized (ChinaCityDB.class) {
            if (mSelf == null) {
                mSelf = new ChinaCityDB(context, path);
                if (13 != db.getVersion() && dbFile.exists()) {
                    copyAssetDbFile(context);
                    db.setVersion(CURRENT_VERSION);
                    updateDatabase(context);
                }
            }
            chinaCityDB = mSelf;
        }
        return chinaCityDB;
    }

    private static void updateDatabase(Context context) {
        int i;
        Cursor c = city_list_db.rawQuery("select * from city", null);
        Cursor cur = null;
        List<Integer> ids = new ArrayList();
        List<String> names = new ArrayList();
        List<String> names_city_db = new ArrayList();
        if (c != null) {
            for (i = 0; i < c.getCount(); i++) {
                c.moveToPosition(i);
                int id = c.getInt(RainSurfaceView.RAIN_LEVEL_DOWNPOUR);
                String name = c.getString(RainSurfaceView.RAIN_LEVEL_RAINSTORM);
                ids.add(Integer.valueOf(id));
                names.add(name);
            }
            if (ids.size() > 0) {
                for (Integer id2 : ids) {
                    cur = db.rawQuery("select * from area where city_code=" + id2, null);
                    if (cur != null) {
                        for (i = 0; i < cur.getCount(); i++) {
                            cur.moveToPosition(i);
                            names_city_db.add(c.getString(cur.getColumnIndex("city_name")));
                        }
                    }
                }
            }
        }
        if (cur != null) {
            i = 0;
            while (i < ids.size()) {
                for (int j = 0; j < cur.getCount() && !names_city_db.contains(names.get(i)); j++) {
                    cur.moveToPosition(j);
                    String cityname = cur.getString(cur.getColumnIndex("city_name"));
                    WeatherLog.d("city_name: " + cityname);
                    String[] whereArgs = new String[]{ids.get(i) + StringUtils.EMPTY_STRING};
                    ContentValues cv = new ContentValues();
                    cv.put(CityListEntry.COLUMN_2_NAME, cityname);
                    cv.put(CityListEntry.COLUMN_3_DISPLAY_NAME, cityname);
                    city_list_db.update(CITY_TABLE_NAME, cv, "locationId=?", whereArgs);
                }
                c.close();
                cur.close();
                i++;
            }
        }
        city_list_db.close();
    }

    private static void copyAssetDbFile(Context context) {
        try {
            InputStream is = context.getAssets().open(CITY_DB_NAME);
            FileOutputStream fos = new FileOutputStream(dbFile);
            byte[] buffer = new byte[1024];
            while (true) {
                int len = is.read(buffer);
                if (len != -1) {
                    fos.write(buffer, 0, len);
                    fos.flush();
                } else {
                    fos.close();
                    is.close();
                    return;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, e.getMessage());
            System.exit(0);
        }
    }

    public Cursor getCursor(String query) {
        return db.rawQuery(query, null);
    }

    private ChinaCityDB(Context context, String path) {
        db = context.openOrCreateDatabase(path, 0, null);
        city_list_db = new CityWeatherDBHelper(context).getWritableDatabase();
    }

    public List<OpCity> getAllCity() {
        List<OpCity> list = new ArrayList();
        Cursor c = db.rawQuery("SELECT * from city", null);
        while (c != null && c.moveToNext()) {
            list.add(getOpCityFromCursor(c));
        }
        if (c != null) {
            c.close();
        }
        return list;
    }

    public OpCity getChinaCity(Context context, String adCode, String cityName) {
        if (TextUtils.isEmpty(cityName) && TextUtils.isEmpty(adCode)) {
            return null;
        }
        adCode = adCode == null ? StringUtils.EMPTY_STRING : adCode.trim();
        OpCity chinaCity = getCityByRegionCode(adCode);
        if (chinaCity != null) {
            return chinaCity;
        }
        chinaCity = getCityByRegionCode(getParentCode(adCode));
        if (chinaCity != null) {
            return chinaCity;
        }
        chinaCity = getCityInfo(parseName(context, cityName));
        return chinaCity == null ? getCityInfo(cityName) : chinaCity;
    }

    public OpCity getChinaCityByPinyin(Context context, String cityName) {
        if (TextUtils.isEmpty(cityName)) {
            return null;
        }
        String[] names = cityName.split(" ");
        String regionName = names[0];
        String cityPinyin = names[1];
        Cursor city_c = db.rawQuery("SELECT * from area where city_province_english LIKE? and city_pinyin like?", new String[]{regionName + "%", cityPinyin + "%"});
        OpCity opCity = null;
        if (city_c != null && city_c.moveToFirst()) {
            opCity = getOpCityFromCursor(city_c);
        }
        if (city_c == null) {
            return opCity;
        }
        city_c.close();
        return opCity;
    }

    private OpCity getCityByRegionCode(String code) {
        if (TextUtils.isEmpty(code)) {
            return null;
        }
        Cursor c = db.rawQuery("select * from region left join area on region.search_code = area.city_code where region_code = ?", new String[]{code});
        Log.i(TAG, "getCity:select * from region left join area on region.search_code = area.city_code where region_code = " + code);
        OpCity item = null;
        if (c != null && c.getCount() > 1 && c.moveToFirst()) {
            while (c.moveToNext()) {
                if (c.getString(RainSurfaceView.RAIN_LEVEL_DOWNPOUR).contains(c.getString(CURRENT_VERSION))) {
                    item = getOpCityFromCursor(c);
                    break;
                }
            }
        }
        if (item == null && c != null && c.moveToFirst()) {
            item = getOpCityFromCursor(c);
        }
        if (c == null) {
            return item;
        }
        c.close();
        return item;
    }

    private String getParentCode(String code) {
        return (TextUtils.isEmpty(code) || code.length() != 6) ? code : code.substring(0, RainSurfaceView.RAIN_LEVEL_RAINSTORM) + "00";
    }

    public void close() {
        if (db != null) {
            db.close();
        }
        db = null;
        mSelf = null;
    }

    private String parseName(Context context, String city) {
        if (city.contains("\u5e02")) {
            return city.split("\u5e02")[0];
        }
        return city.contains("\u53bf") ? city.split("\u53bf")[0] : city;
    }

    private OpCity getCityInfo(String city) {
        if (TextUtils.isEmpty(city)) {
            return null;
        }
        Cursor c = db.rawQuery("SELECT * from area where city_name LIKE ?", new String[]{"%" + city + "%"});
        OpCity opCity = null;
        if (c != null && c.moveToFirst()) {
            opCity = getOpCityFromCursor(c);
        }
        if (c == null) {
            return opCity;
        }
        c.close();
        return opCity;
    }

    public String getCityTimeZone(String locationId) {
        String timeZone = "8";
        Cursor cursor = db.query(OP_CITY_TABLE_NAME, null, "city_code = ?", new String[]{locationId}, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                timeZone = cursor.getString(cursor.getColumnIndex("time_zone"));
            }
            cursor.close();
        }
        return timeZone;
    }

    public List<OpCity> queryCityByName(Context context, String keyword) {
        keyword = sqliteEscape(keyword);
        List<OpCity> list = queryCountyArea(context, keyword);
        return list.isEmpty() ? queryCityArea(context, keyword) : list;
    }

    private List<OpCity> queryCountyArea(Context context, String keyword) {
        String[] selectionArgs = new String[4];
        selectionArgs[0] = "%" + (keyword.length() < 2 ? keyword : parseName(context, keyword)) + "%";
        selectionArgs[1] = (keyword.length() < 2 ? keyword : parseName(context, keyword)) + "%";
        selectionArgs[2] = (keyword.length() < 2 ? keyword : parseName(context, keyword)) + "%";
        StringBuilder append = new StringBuilder().append("%");
        if (keyword.length() >= 2) {
            keyword = parseName(context, keyword);
        }
        selectionArgs[3] = append.append(keyword).append("%").toString();
        List<OpCity> list = new ArrayList();
        try {
            Cursor c = db.rawQuery("SELECT * from area WHERE (city_inchina = 1 OR city_inchina = 0) AND (city_name LIKE ? escape '/' OR city_short LIKE ? escape '/' OR city_pinyin LIKE ? escape '/' OR city_name_zhtw LIKE ? escape '/')", selectionArgs);
            if (c != null) {
                while (c.moveToNext()) {
                    list.add(getOpCityFromCursor(c));
                }
                c.close();
            }
        } catch (Exception e) {
            LogUtils.e("City database error.", e);
        }
        return list;
    }

    private List<OpCity> queryCityArea(Context context, String keyword) {
        String[] selectionArgs = new String[3];
        StringBuilder append = new StringBuilder().append("%");
        if (keyword.length() >= 2) {
            keyword = parseName(context, keyword);
        }
        Arrays.fill(selectionArgs, append.append(keyword).append("%").toString());
        List<OpCity> list = new ArrayList();
        try {
            Cursor c = db.rawQuery("SELECT * from area WHERE (city_inchina = 1 OR city_inchina = 0) AND (city_prefecture LIKE ? escape '/' OR city_prefecture_zhtw LIKE ? escape '/' OR city_prefecture_english LIKE ? escape '/')", selectionArgs);
            if (c != null) {
                while (c.moveToNext()) {
                    list.add(getOpCityFromCursor(c));
                }
                c.close();
            }
        } catch (Exception e) {
            LogUtils.e("City database error.", e);
        }
        return list;
    }

    private OpCity getOpCityFromCursor(Cursor cursor) {
        String areaId = cursor.getString(cursor.getColumnIndex("city_code"));
        if (TextUtils.isEmpty(areaId)) {
            return null;
        }
        String provinceChs = cursor.getString(cursor.getColumnIndex("city_province"));
        String provinceCht = cursor.getString(cursor.getColumnIndex("city_province_zhtw"));
        String provinceEn = cursor.getString(cursor.getColumnIndex("city_province_english"));
        String nameChs = cursor.getString(cursor.getColumnIndex("city_name"));
        String nameCht = cursor.getString(cursor.getColumnIndex("city_name_zhtw"));
        String nameEn = cursor.getString(cursor.getColumnIndex("city_pinyin"));
        String allPY = cursor.getString(cursor.getColumnIndex("city_pinyin"));
        String allFirstPY = cursor.getString(cursor.getColumnIndex("city_short"));
        String firstPY = (allFirstPY == null || allFirstPY.length() < 1) ? StringUtils.EMPTY_STRING : allFirstPY.substring(0, 1).toUpperCase(Locale.getDefault());
        return new OpCity(provinceChs, provinceCht, provinceEn, nameChs, nameCht, nameEn, areaId, allPY, allFirstPY, firstPY, cursor.getString(cursor.getColumnIndex("city_country")), cursor.getString(cursor.getColumnIndex("city_country_zhtw")), cursor.getString(cursor.getColumnIndex("city_country_english")), "1".equals(cursor.getString(cursor.getColumnIndex("city_inchina"))));
    }

    private String sqliteEscape(String keyword) {
        return keyword.replace("/", "//").replace("'", "''").replace("[", "/[").replace("]", "/]").replace("%", "/%").replace("&", "/&").replace("_", "/_").replace("(", "/(").replace(")", "/)");
    }
}
