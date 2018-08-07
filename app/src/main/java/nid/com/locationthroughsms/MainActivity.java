package nid.com.locationthroughsms;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private  static final int RESULT_PICK_CONTACT=85500;
    private Button btnsendSMS;
    private Button button;
    private TextView tv;
    private final static int REQUEST_LOCATION=1;
    LocationManager locationManager;
    String latitude, longitude;
    Geocoder geocoder;
    List<Address> addresses;
    private EditText etPhoneNum,etText;
    private final static int REQUEST_CODE_PERMISSION_SEND_SMS=123;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //permission for accessing gps
        if (!checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        }
        etPhoneNum = (EditText) findViewById(R.id.etPhoneNum);
        etText = (EditText) findViewById(R.id.etText);
        btnsendSMS = (Button) findViewById(R.id.btnsendSMS);
        button=(Button)findViewById(R.id.button);
        geocoder=new Geocoder(this, Locale.getDefault());
        tv=(TextView)findViewById(R.id.tv);
        btnsendSMS.setEnabled(false);
        button.setEnabled(true);
        //permission for sending sms
        if (checkPermission(Manifest.permission.SEND_SMS)) {
            btnsendSMS.setEnabled(true);
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{(Manifest.permission.SEND_SMS)}, REQUEST_CODE_PERMISSION_SEND_SMS);
        }
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //contact picking intent
                Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                startActivityForResult(contactPickerIntent, RESULT_PICK_CONTACT);
            }
        });
        btnsendSMS.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //Dialog box for turning on GPS/Location
                locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("Please turn on your GPS").setCancelable(false).setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    }).setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    final AlertDialog alert = builder.create();
                    alert.show();
                }
                else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                            PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                            PackageManager.PERMISSION_GRANTED) {

                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
                    } else {
                        //obkect to get location
                        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                        if (location != null) {
                            //latitude and longitude information
                            Double lati = location.getLatitude();
                            Double longi = location.getLongitude();
                            latitude = String.valueOf(lati);
                            longitude = String.valueOf(longi);
                            try {
                                //get Geocoded location that is location in human readable form
                                addresses = geocoder.getFromLocation(lati, longi, 1);
                                String address = addresses.get(0).getAddressLine(0);
                                etText.setMovementMethod(LinkMovementMethod.getInstance());
                                String text = "My current location is:\n" + address + "\nLatitude:" + latitude + "\nLongitude:" + longitude + "\n" + ".Link:<a href='https://maps.google.com/?q=" + lati + "," + longi + "'>https://maps.google.com/?q=" + lati + "," + longi + "</a>";
                                etText.setText(Html.fromHtml(text));
                                String txt=etText.getText().toString();
                                String phoneNum = etPhoneNum.getText().toString();
                                SmsManager smsManager = SmsManager.getDefault();
                                smsManager.sendTextMessage(phoneNum, null, txt, null, null);
                                Toast.makeText(MainActivity.this, "SMS Send to" + phoneNum, Toast.LENGTH_LONG).show();

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "Unable to trace location", Toast.LENGTH_SHORT).show();
                        }
                    }

                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // check whether the result is ok
        if (resultCode ==RESULT_OK) {
            // Check for the request code, we might be usign multiple startActivityForReslut
            switch (requestCode) {
                case RESULT_PICK_CONTACT:
                    contactPicked(data);
                    break;
            }
        } else {
            Log.e("MainActivity", "Failed to pick contact");
        }
    }

    private void contactPicked(Intent data) {
        Cursor cursor = null;
        try {
            String phoneNo = null ;
            String name = null;
            // getData() method will have the Content Uri of the selected contact
            Uri uri = data.getData();
            //Query the content uri
            cursor = getContentResolver().query(uri, null, null, null, null);
            cursor.moveToFirst();
            // column index of the phone number
            int  phoneIndex =cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            // column index of the contact name
            int  nameIndex =cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            phoneNo = cursor.getString(phoneIndex);
            name = cursor.getString(nameIndex);
            // Set the value to the textviews
            etPhoneNum.setText(phoneNo);
            tv.setText("Name:"+name);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean checkPermission(String permission)
    {

        int checkPermission= ContextCompat.checkSelfPermission(this,permission);
        return checkPermission == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case REQUEST_CODE_PERMISSION_SEND_SMS:
                if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    btnsendSMS.setEnabled(true);
                }
                break;
        }
    }
}
