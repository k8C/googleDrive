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
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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
        //DataProcessingTask.ac = getApplicationContext();
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
        new DataProcessingTask(statusText, getApplicationContext()).execute(startTime.clone(), endTime.clone());
    }

    static class DataProcessingTask extends AsyncTask<Object, String, String> {
        String token;
        Context appContext;
        WeakReference<TextView> statusTextReference;

        public DataProcessingTask(TextView statusText, Context applicationContext) {
            statusTextReference = new WeakReference<TextView>(statusText);
            appContext = applicationContext;
            token = (String) statusText.getTag();
        }

        void getToken(HttpsURLConnection connection, InputStream stream) {
            publishProgress(null, "getting token");
            int c;
            StringBuilder s;
            byte[] tokenRequestBody = "client_id=463875113005-icovngqrabn2hass5tug5ik5m436ks2k.apps.googleusercontent.com&client_secret=8PWn96NTst2-rbkaXToWoi6F&refresh_token=1/VRNPHjn46h-4vuR8Emw754daGgEx9VmCFWFWronfIO8&grant_type=refresh_token".getBytes();
            for (; ; ) {
                try {
                    connection = (HttpsURLConnection) new URL("https://www.googleapis.com/oauth2/v4/token").openConnection();
                    connection.setDoOutput(true);
                    connection.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    connection.setFixedLengthStreamingMode(tokenRequestBody.length);
                    connection.getOutputStream().write(tokenRequestBody);
                    s = new StringBuilder();
                    if (connection.getResponseCode() == 200) {
                        stream = connection.getInputStream();
                        while ((c = stream.read()) != -1) {
                            if (c == 107 && stream.read() == 101 && stream.read() == 110 && stream.read() == 34 && stream.read() == 58) {
                                while ((c = stream.read()) != 34) {
                                    s.append((char) c);
                                }
                                break;
                            }
                        }
                        stream.close();
                        token = s.toString();
                        Log.e(TAG, "new token: " + token);
                        publishProgress(null, "token refreshed");
                    } else {
                        Log.e(TAG, "refreshToken fail");
                        publishProgress(null, "refreshToken failed, exit");
                        cancel(true);
                    }
                    return;
                } catch (IOException e) {
                    Log.e(TAG, "refreshToken exception");
                    e.printStackTrace();
                    publishProgress(null, "refreshToken error, retrying");
                    SystemClock.sleep(5000);
                }
            }
        }

        @Override
        protected String doInBackground(Object... params) {
            if (token == null) getToken(null, null);
            StringBuilder s = new StringBuilder("https://www.googleapis.com/drive/v3/files?fields=files(id,name)&q=");
            Calendar startTime = (Calendar) params[0];
            int i = 0;
            String name;//, lastIncompleteFileName = incompleteFiles.get(incompleteFiles.size()-1);
            String appDirectory = appContext.getExternalFilesDir(null).getPath();
            List<String> incompleteFiles;
            String incompleteFilesJson = PreferenceManager.getDefaultSharedPreferences(appContext).getString("incompleteFiles", null);
            if (incompleteFilesJson != null) {
                incompleteFiles = new Gson().fromJson(incompleteFilesJson, new TypeToken<List<String>>() {
                }.getType());
            } else {
                incompleteFiles = new ArrayList<>();
            }
            while (!startTime.after(params[1])) {
                name = String.format("%1$tY%<tm%<td%<tH%<tM", startTime);
                if (!new File(appDirectory, name).exists() || incompleteFiles.contains(name)) {
                    if (i++ == 0) {
                        s.append("name%3D%27");
                    } else {
                        s.append("%27orname%3D%27");
                    }
                    s.append(name);
                } else {
                    publishProgress(name, null);
                }
                startTime.add(Calendar.MINUTE, 1);
            }
            if (i==0) return "all files exist";
            s.append("%27");

            InputStream stream;
            HttpsURLConnection connection;
            int c;
            ByteArrayOutputStream baos;
            byte[] buffer = new byte[1024];
            List<String> ids = new ArrayList<>();
            List<String> names = new ArrayList<>();
            boolean isId = true;
            String searchUrl = s.toString();
            publishProgress(null, "searching for files");
            for (; ; ) {
                try {
                    connection = (HttpsURLConnection) new URL(searchUrl).openConnection();
                    connection.addRequestProperty("Authorization", "Bearer " + token);
                    if (connection.getResponseCode() == 200) {
                        stream = connection.getInputStream();
                        while ((c = stream.read()) != -1) {
                            if (c == 58 && stream.read() == 32 && stream.read() == 34) {
                                s = new StringBuilder();
                                while ((c = stream.read()) != 34) {
                                    s.append((char) c);
                                }
                                if (isId) {
                                    isId = false;
                                    ids.add(s.toString());
                                } else {
                                    isId = true;
                                    names.add(s.toString());
                                }
                            }
                        }
                        stream.close();
                        if (names.size() == 0) return "found no files";
                        publishProgress(null, "Downloading 0/" + ids.size() + " files");
                        for (i = 0; i < ids.size(); i++) {
                            name = names.get(i);
                            Log.e(TAG, name + ": " + ids.get(i));
                            for (; ; ) {
                                try {
                                    connection = (HttpsURLConnection) new URL("https://www.googleapis.com/drive/v3/files/" + ids.get(i) + "/export?mimeType=text/plain").openConnection();
                                    connection.addRequestProperty("Authorization", "Bearer " + token);
                                    if (connection.getResponseCode() == 200) {
                                        stream = connection.getInputStream();
                                        baos = new ByteArrayOutputStream();
                                        while ((c = stream.read(buffer)) != -1) {
                                            baos.write(buffer, 0, c);
                                        }
                                        Log.e(TAG, baos.toString());
                                        baos.close();
                                        stream.close();
                                        publishProgress(null, "Downloading " + (i + 1) + "/" + ids.size() + " files");
                                        incompleteFiles.remove(name);
                                        if (i == 0 && (incompleteFilesJson == null || name.compareTo(incompleteFiles.get(incompleteFiles.size() - 1)) >= 0))
                                            incompleteFiles.add(name);
                                        break;
                                    } else {
                                        getToken(connection, stream);
                                    }
                                } catch (IOException e) {
                                    Log.e(TAG, "download file " + names.get(i) + " exception");
                                    e.printStackTrace();
                                    publishProgress(null, "download file error, retrying");
                                    SystemClock.sleep(5000);
                                }
                            }
                        }
                        PreferenceManager.getDefaultSharedPreferences(appContext).edit().putString("incompleteFiles", new Gson().toJson(incompleteFiles)).apply();
                        break;
                    } else {
                        stream = connection.getErrorStream();
                        s = new StringBuilder();
                        while ((c = stream.read()) != -1) {
                            s.append((char) c);
                        }
                        Log.e(TAG, "error stream: " + s.toString());
                        stream.close();
                        publishProgress(null, "searching files failed, reacquiring token");
                        getToken(connection, stream);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "searching files exception");
                    publishProgress(null, "searching files error, retrying");
                    e.printStackTrace();
                    SystemClock.sleep(5000);
                }
            }

            //if (isCancelled()) return "";
            return searchUrl;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            if (!isCancelled()) {
                TextView statusText = statusTextReference.get();
                if (values[0] == null) {
                    statusText.setText(values[1]);
                } else {
                    Log.e(TAG, "file " + values[0] + " exists");
                }
            }
        }

        @Override
        protected void onPostExecute(String s) {
            //PreferenceManager.getDefaultSharedPreferences(ac).edit().apply();
            TextView statusText = statusTextReference.get();
            if (statusText != null) {
                Log.e(TAG, s);
                statusText.setText("done");
                statusText.setTag(token);
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