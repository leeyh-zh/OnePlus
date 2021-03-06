package net.oneplus.weather.app;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.oneplus.lib.app.OPProgressDialog;
import com.oneplus.lib.util.loading.DialogLoadingAsyncTask;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.oneplus.weather.api.nodes.Alarm;
import net.oneplus.weather.util.BitmapUtils;
import net.oneplus.weather.util.MediaUtil;
import net.oneplus.weather.util.PermissionUtil;
import net.oneplus.weather.util.ScreenShot;
import net.oneplus.weather.util.UIUtil;
import net.oneplus.weather.util.Utilities;
import net.oneplus.weather.widget.openglbase.RainSurfaceView;

public class WeatherWarningActivity extends BaseActivity {
    public static final String INTENT_PARA_CITY = "city";
    public static final String INTENT_PARA_WARNING = "warning";
    private WarningAdapter mAdapter;
    private String mCity;
    private ListView mWarningList;
    private int viewWidth;

    class AnonymousClass_2 implements OnPreDrawListener {
        final /* synthetic */ View val$layout;
        final /* synthetic */ ImageView val$shareButton;
        final /* synthetic */ TextView val$titleText;

        AnonymousClass_2(View view, ImageView imageView, TextView textView) {
            this.val$layout = view;
            this.val$shareButton = imageView;
            this.val$titleText = textView;
        }

        public boolean onPreDraw() {
            this.val$layout.getViewTreeObserver().removeOnPreDrawListener(this);
            WeatherWarningActivity.this.viewWidth = UIUtil.dip2px(this.val$layout.getContext(), 50.0f) - this.val$shareButton.getWidth();
            Utilities.measureTextLengthAndSet(this.val$titleText, WeatherWarningActivity.this.getString(R.string.weather_warning_title, new Object[]{WeatherWarningActivity.this.mCity}), WeatherWarningActivity.this.viewWidth, ConnectionResult.RESTRICTED_PROFILE);
            return true;
        }
    }

    class WarningAdapter extends BaseAdapter {
        private boolean isShare;
        private List<Alarm> mAlarms;
        LayoutInflater mInflater;

        class ViewHolder {
            public TextView content;
            public TextView title;

            ViewHolder() {
            }
        }

        public WarningAdapter(Context context, List<Alarm> alarms) {
            this.mInflater = LayoutInflater.from(context);
            this.mAlarms = alarms;
        }

        public int getCount() {
            return this.mAlarms == null ? 0 : this.mAlarms.size();
        }

        public Object getItem(int position) {
            return Integer.valueOf(position);
        }

        public long getItemId(int position) {
            return (long) position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = this.mInflater.inflate(R.layout.weather_warning_item, null);
                holder = new ViewHolder();
                holder.content = (TextView) convertView.findViewById(R.id.weather_warning_content);
                holder.title = (TextView) convertView.findViewById(R.id.weather_warning_title);
            } else {
                holder = convertView.getTag();
            }
            convertView.setTag(holder);
            Alarm alarm = (Alarm) this.mAlarms.get(position);
            if (this.isShare) {
                holder.title.setTextColor(ContextCompat.getColor(convertView.getContext(), R.color.oneplus_contorl_text_color_primary_light));
                holder.content.setTextColor(ContextCompat.getColor(convertView.getContext(), R.color.oneplus_contorl_text_color_primary_light));
            } else {
                holder.title.setTextColor(ContextCompat.getColor(convertView.getContext(), R.color.city_search_item_text));
                holder.content.setTextColor(ContextCompat.getColor(convertView.getContext(), R.color.city_search_item_text));
            }
            if (!alarm.getTypeName().equalsIgnoreCase("null")) {
                holder.title.setText(alarm.getTypeName());
            }
            if (!alarm.getContentText().equalsIgnoreCase("null")) {
                holder.content.setText(alarm.getContentText());
            }
            return convertView;
        }
    }

    private class SavePic extends DialogLoadingAsyncTask<String, Void, String> {
        private SavePic(OPProgressDialog dialog) {
            super(dialog);
        }

        protected void onPreExecuteExtend() {
            super.onPreExecuteExtend();
        }

        protected String doInBackground(String... params) {
            if (!TextUtils.isEmpty(params[0])) {
                BitmapUtils.savePicByLimit(ScreenShot.createBitmap(WeatherWarningActivity.this, WeatherWarningActivity.this.mWarningList, WeatherWarningActivity.this.mCity), params[0]);
            }
            return params[0];
        }

        protected void onPostExecuteExtend(String path) {
            if (TextUtils.isEmpty(path)) {
                Toast.makeText(WeatherWarningActivity.this, WeatherWarningActivity.this.getString(R.string.no_weather_data), 0).show();
                return;
            }
            File f = new File(path);
            Intent intent = new Intent("android.intent.action.SEND");
            intent.setType("image/*");
            intent.putExtra("android.intent.extra.SUBJECT", WeatherWarningActivity.this.getString(R.string.share_subject));
            intent.setFlags(268435456);
            intent.putExtra("android.intent.extra.STREAM", MediaUtil.getInstace().getImageContentUri(WeatherWarningActivity.this, f));
            WeatherWarningActivity.this.startActivity(Intent.createChooser(intent, WeatherWarningActivity.this.getString(R.string.share_title)));
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.weather_warning_activity);
        View layout = findViewById(16908290);
        this.mCity = getIntent().getStringExtra(INTENT_PARA_CITY);
        ActionBar bar = getActionBar();
        if (bar != null) {
            View actionbarLayout = LayoutInflater.from(this).inflate(R.layout.top_bar, null);
            bar.setDisplayShowCustomEnabled(true);
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setCustomView(actionbarLayout);
            TextView titleText = (TextView) actionbarLayout.findViewById(R.id.top_bar_title);
            ImageView shareButton = (ImageView) actionbarLayout.findViewById(R.id.top_bar_button);
            titleText.setText(getString(R.string.weather_warning_title, new Object[]{this.mCity}));
            shareButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    WeatherWarningActivity.this.share();
                }
            });
            layout.getViewTreeObserver().addOnPreDrawListener(new AnonymousClass_2(layout, shareButton, titleText));
        }
        ArrayList<Parcelable> weather = getIntent().getParcelableArrayListExtra(INTENT_PARA_WARNING);
        List<Alarm> alarms = new ArrayList();
        if (weather != null) {
            for (int i = 0; i < weather.size(); i++) {
                alarms.add((Alarm) weather.get(i));
            }
        }
        this.mAdapter = new WarningAdapter(this, alarms);
        this.mWarningList = (ListView) findViewById(R.id.weather_warning_list);
        this.mWarningList.setAdapter(this.mAdapter);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() != 16908332) {
            return super.onOptionsItemSelected(item);
        }
        finish();
        overridePendingTransition(R.anim.alpha_in, R.anim.citylist_translate_down);
        return true;
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case RainSurfaceView.RAIN_LEVEL_NORMAL_RAIN:
                if (grantResults.length > 0 && grantResults[0] == 0) {
                    share();
                }
            default:
                break;
        }
    }

    private void share() {
        if (this.mWarningList != null && PermissionUtil.check(this, "android.permission.WRITE_EXTERNAL_STORAGE", getString(R.string.request_permission_storage), 1)) {
            String shareIamgePath = BitmapUtils.getPicFileName(this.mCity, this);
            OPProgressDialog dialog = new OPProgressDialog(this);
            dialog.setMessage(getResources().getString(R.string.generate_image));
            new SavePic(dialog, null).execute(new String[]{shareIamgePath});
        }
    }

    private List<Alarm> getAlarms(ArrayList<Parcelable> weather) {
        if (weather == null || weather.size() < 1) {
            return null;
        }
        List<Alarm> resAlarms = new ArrayList();
        resAlarms.add((Alarm) weather.get(0));
        int countWeather = weather.size();
        int i = 1;
        while (i < countWeather) {
            try {
                Alarm tempAlarm = (Alarm) weather.get(i);
                int count = resAlarms.size();
                int j = 0;
                while (j < count && !((Alarm) resAlarms.get(j)).getTypeName().equals(tempAlarm.getTypeName())) {
                    resAlarms.add(tempAlarm);
                    j++;
                }
                i++;
            } catch (Exception e) {
                return null;
            }
        }
        return resAlarms;
    }
}
