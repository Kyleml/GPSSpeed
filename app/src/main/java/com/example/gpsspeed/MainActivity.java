package com.example.gpsspeed;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private TextView speedTextView;
    private TextView gpsStatusTextView;
    private LocationManager locationManager;
    private Location previousLocation;
    private float currentSpeed = 0; // 米/秒
    private int satelliteCount = 0;
    private GpsStatus.Listener gpsStatusListener;
    private LocationListener locationListener;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        speedTextView = findViewById(R.id.speed);
        gpsStatusTextView = findViewById(R.id.gps_status);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            startGps();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startGps();
        }
    }

    @SuppressWarnings("deprecation")
    private void startGps() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        // 监听 GPS 卫星状态，获取卫星数量
        gpsStatusListener = event -> {
            GpsStatus status = event.getGPSStatus();
            if (status != null) {
                int count = 0;
                for (GpsSatellite sat : status.getSatellites()) {
                    if (sat.usedInFix()) count++;
                }
                satelliteCount = count;
            }
        };
        locationManager.addGpsStatusListener(gpsStatusListener);

        // 每秒请求一次 GPS 位置更新
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                if (location.hasSpeed() && location.getSpeed() >= 0) {
                    currentSpeed = location.getSpeed(); // 系统提供速度
                } else if (previousLocation != null) {
                    float distance = previousLocation.distanceTo(location); // 米
                    long timeDelta = (location.getTime() - previousLocation.getTime()) / 1000; // 秒
                    if (timeDelta > 0) {
                        currentSpeed = distance / timeDelta;
                    }
                }
                previousLocation = location;
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override
            public void onProviderEnabled(String provider) {}
            @Override
            public void onProviderDisabled(String provider) {
                currentSpeed = 0;
            }
        };
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                1000, 0, locationListener);

        // 每秒刷新一次界面（即使没有新位置）
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateUI();
                handler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    private void updateUI() {
        float speedKmh = currentSpeed * 3.6f;
        speedTextView.setText(String.format(Locale.getDefault(), "%.1f km/h", speedKmh));
        gpsStatusTextView.setText("卫星数: " + satelliteCount);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (locationManager != null) {
            if (locationListener != null) locationManager.removeUpdates(locationListener);
            if (gpsStatusListener != null) locationManager.removeGpsStatusListener(gpsStatusListener);
        }
    }
}