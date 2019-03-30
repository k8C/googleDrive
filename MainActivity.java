package project.thesis.vgu.drive;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;

import javax.net.ssl.HttpsURLConnection;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    static final String TAG = "drive";
    TextView statusText, fromText, toText;
    Calendar startTime, endTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        statusText = findViewById(R.id.status);
        fromText = findViewById(R.id.from);
        toText = findViewById(R.id.to);
        startTime = Calendar.getInstance();
        endTime = (Calendar) startTime.clone();
        fromText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        startTime.set(year, month, dayOfMonth);
                        fromText.setText(String.format("%1$tY/%<tm/%<td %<tR", startTime));
                        new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                startTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                startTime.set(Calendar.MINUTE, minute);
                                fromText.setText(String.format("%1$tY/%<tm/%<td %<tR", startTime));
                            }
                        }, startTime.get(Calendar.HOUR_OF_DAY), startTime.get(Calendar.MINUTE), true).show();
                    }
                }, startTime.get(Calendar.YEAR), startTime.get(Calendar.MONTH), startTime.get(Calendar.DATE)).show();
            }
        });
        toText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        endTime.set(year, month, dayOfMonth);
                        toText.setText(String.format("%1$tY/%<tm/%<td %<tR", endTime));
                        new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                endTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                endTime.set(Calendar.MINUTE, minute);
                                toText.setText(String.format("%1$tY/%<tm/%<td %<tR", endTime));
                            }
                        }, endTime.get(Calendar.HOUR_OF_DAY), endTime.get(Calendar.MINUTE), true).show();
                    }
                }, endTime.get(Calendar.YEAR), endTime.get(Calendar.MONTH), endTime.get(Calendar.DATE)).show();
            }
        });
        String s = String.format("%1$tY/%<tm/%<td %<tR", startTime);
        fromText.setText(s);
        toText.setText(s);
        //statusText.setTag(PreferenceManager.getDefaultSharedPreferences(this).getString("token", null));
    }

    public void executeTask(View button) {
        statusText.setText(null);
        if (startTime.after(endTime)) {
            statusText.setText("Start Time must not be later than End Time");
            return;
        }
//        HttpsURLConnection u;
//        u.getInputStream().;
        new DataProcessingTask(statusText).execute(startTime.clone(), endTime.clone());
    }

    static class DataProcessingTask extends AsyncTask<Object, Integer, String> {
        //String token;
        WeakReference<TextView> statusTextReference;

        public DataProcessingTask(TextView statusText) {
            statusTextReference = new WeakReference<TextView>(statusText);
        }

        /*void refreshToken() {
        }*/

        @Override
        protected String doInBackground(Object... params) {
            StringBuilder searchUrl = new StringBuilder("https://www.googleapis.com/drive/v3/files?fields=files(id,name)&q=");
            Calendar startTime = (Calendar) params[0];
            int i = 0;
            while (!startTime.after(params[1])) {
                if (i++ == 0) {
                    searchUrl.append("name%3D%27");
                } else {
                    searchUrl.append("%27orname%3D%27");
                }
                searchUrl.append(String.format("%1$tY%<tm%<td%<tH%<tM", startTime));
                startTime.add(Calendar.MINUTE, 1);
            }
            searchUrl.append("%27");

            InputStream stream;
            HttpsURLConnection connection;
            StringBuilder s= new StringBuilder();;
            int b, c;
            String id = null, name = null;
            try {
                connection = (HttpsURLConnection) new URL(searchUrl.toString()).openConnection();
                connection.addRequestProperty("Authorization", "Bearer ya29.GlzcBnqrpvoTAaqPWpnn02uUKLa1LhKj7vVeCV-alcDttAmvJhs02DZ2twXQDrpY-oZtpwBErp50jMCT8K_ZvNTdFKUST5IBjkNDh0evDaqztGe8KZQDuPDQaD35rA");;
                if (connection.getResponseCode() == 200) {
                    stream = connection.getInputStream();
                    while ((c = stream.read()) != -1) {
                    if ((c == 100 || c == 101) && stream.read() == 34 && stream.read() == 58) {
                        stream.read();
                        stream.read();
                        s = new StringBuilder();
                        while ((b = stream.read()) != 34) {
                            s.append((char) b);
                        }
                        if (c == 100) {
                            id = s.toString();
                        } else {
                            name = s.toString();
                            Log.e(TAG, name + ": " + id);
                        }
                    }
                    }
                } else {
                    stream = connection.getErrorStream();
                    while ((c = stream.read()) != -1) {
                        s.append((char) c);
                    }
                    Log.e(TAG, "error stream: " + s.toString());
                }
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            //if (isCancelled()) return "";
            //publishProgress(9);
            return searchUrl.toString();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            TextView statusText = statusTextReference.get();

            if (statusText != null) {
                //statusText.setTag(token);
                //PreferenceManager.getDefaultSharedPreferences(statusText.getContext()).edit().putString("token", token).apply();
            }
        }

        @Override
        protected void onPostExecute(String s) {
            TextView statusText = statusTextReference.get();
            if (statusText != null) {
                Log.e(TAG, s);
                statusText.setText("done");
            }
        }

    }
}
//https://accounts.google.com/o/oauth2/v2/auth?scope=https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fdrive.file&response_type=code&redirect_uri=urn:ietf:wg:oauth:2.0:oob&client_id=463875113005-1att4hu76j0ac7ta17mdjniinfgg2di2.apps.googleusercontent.com
//1/0gh8dGi_1bkC-g5BFGlvRRJHKFwUYjQMpW59nv56ZVA

//client_id=463875113005-icovngqrabn2hass5tug5ik5m436ks2k.apps.googleusercontent.com&client_secret=8PWn96NTst2-rbkaXToWoi6F&refresh_token=1/7C-dMwDk771wT5lads8os4_mziPZspcIU6ndw_ZJpi4&grant_type=refresh_token
//4/FwG9vGUJa37B48T-SRWo4_PxxBxJNg_vbWul86O7ICEQHqEgg36SjGYnl84Ls2VQBsw5-hYzbFlMDhtYjKaFlFs
//1/VRNPHjn46h-4vuR8Emw754daGgEx9VmCFWFWronfIO8
    /*if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},0);}
    fot = new FileOutputStream(new File(Environment.getExternalStorageDirectory(), "raw.txt"));
    tof = new FileOutputStream(new File(Environment.getExternalStorageDirectory(), "kalman.txt"));
    rawtxt = new BufferedReader(new FileReader(new File(Environment.getExternalStorageDirectory(),"raw.txt")));*/