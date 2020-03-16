package com.example.joohonga.microdust;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.joohonga.microdust.microdustInfo.List;
import com.example.joohonga.microdust.microdustInfo.Microdust;
import com.example.joohonga.microdust.network.APIService;
import com.example.joohonga.microdust.network.NetworkManager;
import com.example.joohonga.microdust.stationInfo.Station;

import java.io.IOException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MainActivity extends AppCompatActivity
{
    private GpsTracker gpsTracker;
    private static final String TAG = "MainActivity Log";

    private static final String DATA_TERM = "DAILY";
    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    String[] REQUIRED_PERMISSIONS  = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    private APIService apiService;
    private GeoPoint tm_pt;
    private Call<Station> stationCall;
    private String stationName;
    private Call<Microdust> microDustCall;
    private Microdust microdust;
    private boolean done;
    private LinearLayout constraintLayout;

    private ImageView statusImage;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, LoadingActivity.class);
        startActivity(intent);
        setContentView(R.layout.activity_main);
        TextView tv = findViewById(R.id.address_textview);
        statusImage = findViewById(R.id.microdust_status_img);
        constraintLayout = findViewById(R.id.main_background);
        status = findViewById(R.id.status_textview);
        Button addAddressBtn = findViewById(R.id.search_address_btn);
        Button removeAddressBtn = findViewById(R.id.erase_address_btn);
        removeAddressBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                removeAddress();
            }
        });
        addAddressBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent1 = new Intent(getApplicationContext(), SeachingAddressActivity.class);
                startActivity(intent1);
            }
        });

        Animation a = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.sample_animation);
        statusImage.setAnimation(a);

        apiService = NetworkManager.getInstance().getApiService();
        if (!checkLocationServicesStatus()) {

            showDialogForLocationServiceSetting();
        } else {

            checkRunTimePermission();
        }


        gpsTracker = new GpsTracker(MainActivity.this);

        double latitude = gpsTracker.getLatitude();
        double longitude = gpsTracker.getLongitude();
        GeoPoint in_pt = new GeoPoint();
        in_pt.x = longitude;    //경도
        in_pt.y = latitude;     //위도
        tm_pt = GeoTrans.convert(GeoTrans.GEO, GeoTrans.TM, in_pt);
        getStationList();
        done = false;



        String test = "";
        String address = getCurrentAddress(latitude, longitude);


        tv.setText(address);


    }


    @Override
    public void onResume() {
        super.onResume();
        java.util.List<String> savedAddresses = readAddressesFromSP();
        for(String a: savedAddresses){
            System.out.println(a);
        }
    }


    /*
     * ActivityCompat.requestPermissions를 사용한 퍼미션 요청의 결과를 리턴받는 메소드입니다.
     */
    @Override
    public void onRequestPermissionsResult(int permsRequestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grandResults) {

        if ( permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.length == REQUIRED_PERMISSIONS.length) {

            // 요청 코드가 PERMISSIONS_REQUEST_CODE 이고, 요청한 퍼미션 개수만큼 수신되었다면

            boolean check_result = true;


            // 모든 퍼미션을 허용했는지 체크합니다.

            for (int result : grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }


            if ( check_result ) {

                //위치 값을 가져올 수 있음
                ;
            }
            else {
                // 거부한 퍼미션이 있다면 앱을 사용할 수 없는 이유를 설명해주고 앱을 종료합니다.2 가지 경우가 있습니다.

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])) {

                    Toast.makeText(MainActivity.this, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요.", Toast.LENGTH_LONG).show();
                    finish();


                }else {

                    Toast.makeText(MainActivity.this, "퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용해야 합니다. ", Toast.LENGTH_LONG).show();

                }
            }

        }
    }

    void checkRunTimePermission(){

        //런타임 퍼미션 처리
        // 1. 위치 퍼미션을 가지고 있는지 체크합니다.
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION);


        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {

            // 2. 이미 퍼미션을 가지고 있다면
            // ( 안드로이드 6.0 이하 버전은 런타임 퍼미션이 필요없기 때문에 이미 허용된 걸로 인식합니다.)


            // 3.  위치 값을 가져올 수 있음



        } else {  //2. 퍼미션 요청을 허용한 적이 없다면 퍼미션 요청이 필요합니다. 2가지 경우(3-1, 4-1)가 있습니다.

            // 3-1. 사용자가 퍼미션 거부를 한 적이 있는 경우에는
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, REQUIRED_PERMISSIONS[0])) {

                // 3-2. 요청을 진행하기 전에 사용자가에게 퍼미션이 필요한 이유를 설명해줄 필요가 있습니다.
                Toast.makeText(MainActivity.this, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.", Toast.LENGTH_LONG).show();
                // 3-3. 사용자게에 퍼미션 요청을 합니다. 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);


            } else {
                // 4-1. 사용자가 퍼미션 거부를 한 적이 없는 경우에는 퍼미션 요청을 바로 합니다.
                // 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }

        }

    }





    //여기부터는 GPS 활성화를 위한 메소드들
    private void showDialogForLocationServiceSetting() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"
                + "위치 설정을 수정하실래요?");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case GPS_ENABLE_REQUEST_CODE:

                //사용자가 GPS 활성 시켰는지 검사
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {

                        Log.d("@@@", "onActivityResult : GPS 활성화 되있음");
                        checkRunTimePermission();
                        return;
                    }
                }

                break;
        }
    }

    public String getCurrentAddress( double latitude, double longitude) {

        //지오코더... GPS를 주소로 변환
        Geocoder geocoder = new Geocoder(this, Locale.KOREAN);

        java.util.List<Address> addresses;

        try {

            addresses = geocoder.getFromLocation(
                    latitude,
                    longitude,
                    7);
        } catch (IOException ioException) {
            //네트워크 문제
            Toast.makeText(this, "지오코더 서비스 사용불가", Toast.LENGTH_LONG).show();
            return "지오코더 서비스 사용불가";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "잘못된 GPS 좌표", Toast.LENGTH_LONG).show();
            return "잘못된 GPS 좌표";

        }



        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "주소 미발견", Toast.LENGTH_LONG).show();
            return "주소 미발견";

        }

        Address address = addresses.get(0);
        return parseAddress(address.getAddressLine(0));

    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }


    private void getStationList(){
        stationCall = apiService.getStationList("json",tm_pt.x, tm_pt.y);
        stationCall.enqueue(new Callback<Station>() {
            @Override
            public void onResponse(Call<Station> call, Response<Station> response) {
                Station stationList = response.body();
                stationName = stationList.list.get(0).stationName;

                System.out.println(stationName);
                getMicroDustStatus(stationName);
            }

            @Override
            public void onFailure(Call<Station> call, Throwable t) {
                Log.d(TAG, t.getMessage());
            }
        });
    }

    private void getMicroDustStatus(String stationName){
        microDustCall = apiService.getMicroDustSatus("json",stationName, DATA_TERM);
        microDustCall.enqueue(new Callback<Microdust>() {
            @Override
            public void onResponse(Call<Microdust> call, Response<Microdust> response) {
                microdust = response.body();
                DateFormat df = new SimpleDateFormat("yyyy-mm-dd hh:mm");
                Date date = null;
                try {
                     date = df.parse(microdust.list.get(1).getDataTime());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                date.setMonth(date.getMonth()+1);
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                System.out.println(cal.getTime());
                System.out.println(microdust.list.get(1).getDataTime());
                switch (getMicrodustGrade(microdust.list.get(0))){
                    case 1:
                        statusImage.setImageResource(R.drawable.ic_emoji);
                        constraintLayout.setBackgroundResource(R.color.bestStatus);
                        status.setText("좋음");
                        break;
                    case 2:
                        statusImage.setImageResource(R.drawable.ic_emoji_2);
                        constraintLayout.setBackgroundResource(R.color.goodStatus);
                        status.setText("양호");
                        break;
                    case 3:
                        statusImage.setImageResource(R.drawable.ic_normal_status);
                        constraintLayout.setBackgroundResource(R.color.sosoStatus);
                        status.setText("보통");
                        break;
                    case 4:
                        statusImage.setImageResource(R.drawable.ic_sad);
                        constraintLayout.setBackgroundResource(R.color.badStatus);
                        status.setText("나쁨");
                        break;
                    case 5:
                        statusImage.setImageResource(R.drawable.ic_worst_status);
                        constraintLayout.setBackgroundResource(R.color.worstStatus);
                        status.setText("최악");
                        break;
                }

            }

            @Override
            public void onFailure(Call<Microdust> call, Throwable t) {
                Log.d(TAG, t.getMessage());

            }
        });
    }

    private int getMicrodustGrade(List microdust){
        int day = Integer.parseInt(microdust.getDataTime().split(" ")[0].split("-")[2]);
        int hour = Integer.parseInt(microdust.getDataTime().split(" ")[1].split(":")[0]);
        if(!microdust.getPm10Value().equals("-")){
            int pm10 = Integer.parseInt(microdust.getPm10Value());
            if (pm10<30){
                return 1;
            }
            else if(pm10<60){
                return 2;
            }else if(pm10 < 90){
                return 3;
            }else if (pm10 < 120){
                return 4;
            }else{
                return 5;
            }
        }
        else
            return -1;
    }

    private String parseAddress(String add){
        String[] address = add.split(" ");
        return address[2] + " " + address[3];
    }

    private java.util.List<String> readAddressesFromSP(){
        SharedPreferences sp = this.getSharedPreferences("addresses", Context.MODE_PRIVATE);
        String addresses = sp.getString("add", "");
        if(!addresses.isEmpty()){
            java.util.List<String> addes = Arrays.asList(addresses.split(" /// "));
            return addes;
        }
        else
            return new ArrayList<String>();

    }
    private void removeAddress() {

    }


}