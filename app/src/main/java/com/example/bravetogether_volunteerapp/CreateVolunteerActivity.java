package com.example.bravetogether_volunteerapp;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.basgeekball.awesomevalidation.AwesomeValidation;
import com.basgeekball.awesomevalidation.ValidationStyle;
import com.basgeekball.awesomevalidation.utility.RegexTemplate;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;
import androidmads.library.qrgenearator.QRGSaver;


@RequiresApi(api = Build.VERSION_CODES.N)
public class CreateVolunteerActivity extends AppCompatActivity {

    private String apiKey = "AIzaSyA0hReShDEqNU3cdSm9eot1atb8-CKBy0Q";
    private String url;
    private String address;
    String strDate;
    String start_time;
    Context mcontext = this;
    private int minVolNum;
    private int maxVolNum;
    private final String sharedPrefFile = "com.example.android.BraveTogether_VolunteerApp";
    private SharedPreferences mPreferences;
    private ToggleButton toggleButton;
    AwesomeValidation awesomeValidation;
    boolean your_date_is_outdated = true;
    Button mButtonAddPicture;
    String manager;
    String name;
    String about_place;
    String about_volunteering;
    String duration;
    String min_volunteer = "-1";
    String max_volunteers = "100000000";
    String value_in_coins;
    String picture = "NULL";
    String online = "0";
    EditText nameTextView;
    EditText pdescriptEditText;
    EditText tdescriptEditText;
    EditText valueInCoinsEditText;

    // Firebase
    StorageReference mStorageRef;
    Uri imgUri;
    ImageView img;
    private final int PICK_IMAGE_REQUEST = 71;

    private void initQRCode(int credits, String date) {
        // this function creates a QRCode with the relevant data and saves it to the gallery
        // Initializing the QR Encoder with your value to be encoded, type you required and Dimension
        String data = credits + " " + date + " " + address;
        QRGEncoder qrgEncoder = new QRGEncoder(data, null, QRGContents.Type.TEXT, 1000);
        // Getting QR-Code as Bitmap
        Bitmap bitmap = qrgEncoder.getBitmap();
        // Setting Bitmap to ImageView
        QRGSaver qrgSaver = new QRGSaver();
//      qrgSaver.save(getGalleryPath(), "QRCode".trim(), bitmap, QRGContents.ImageType.IMAGE_JPEG);
        MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "mytitle" , "descriptionhello");
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_volunteer);
        url = getString(R.string.apiUrl);

        nameTextView = (EditText) findViewById(R.id.volunteerName);
        pdescriptEditText = (EditText) findViewById(R.id.placeDescrition);
        tdescriptEditText = (EditText) findViewById(R.id.todoeDescrition);
        valueInCoinsEditText = (EditText) findViewById(R.id.valueInCoins);

        Button selectDate = findViewById(R.id.btnDate);
        Button selectTIme = findViewById(R.id.btnTime);
//        TextView addPicView = findViewById(R.id.addPicText);
        mButtonAddPicture = (Button)findViewById(R.id.addImageButtonImageView);
        final TextView dateView = findViewById(R.id.dateView);
        final TextView hourView = findViewById(R.id.hourView);
        final TextView durationView = findViewById(R.id.durationView);
        mPreferences = getSharedPreferences(sharedPrefFile, MODE_PRIVATE);
        manager = "8"; //mPreferences.getString("uid", "-1");
        toggleButton = (ToggleButton) findViewById(R.id.online_button);

        // Firebase
        mStorageRef = FirebaseStorage.getInstance().getReference();
        img = (ImageView) findViewById(R.id.profile_image_2);


        mButtonAddPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImg();
            }
        });


        // Initialize Validation Style
        awesomeValidation = new AwesomeValidation(ValidationStyle.BASIC);

        // Validation for volunteer name
        awesomeValidation.addValidation(this, R.id.volunteerName,
                RegexTemplate.NOT_EMPTY, R.string.invalid_volunteer_name);
//        // Validation for address
//        awesomeValidation.addValidation(this, R.id.familyNameEditText,
//                RegexTemplate.NOT_EMPTY, R.string.invalid_family_name);

        // Validation for value in coins
        awesomeValidation.addValidation(this, R.id.valueInCoins,
                RegexTemplate.NOT_EMPTY, R.string.invalid_value_in_coins);

        // Validation for about volunteer
        awesomeValidation.addValidation(this, R.id.todoeDescrition,
                RegexTemplate.NOT_EMPTY, R.string.invalid_about_volunteer);


        //AutoComplete Place text

        Places.initialize(getApplicationContext(), apiKey); //Initialize SDK
        PlacesClient placesClient = Places.createClient(this);

        // Initialize the AutocompleteSupportFragment.
        final AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_address);

//        autocompleteFragment.setHint("כתובת");

        final EditText etPlace = (EditText) autocompleteFragment.getView().findViewById(R.id.places_autocomplete_search_input);
        etPlace.setVisibility(View.VISIBLE);
        etPlace.setTextDirection(View.TEXT_DIRECTION_ANY_RTL);
        etPlace.setBackgroundResource(R.drawable.textfield_d);
        etPlace.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.map_pin_line, 0);
        etPlace.setHint("כתובת");
        //TODO: change to rtl text direction
        autocompleteFragment.setLocationBias(RectangularBounds.newInstance(
                new LatLng(29.4533796, 34.2674994),
                new LatLng(33.3356317, 35.8950234)));

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ADDRESS));

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                address = place.getAddress();
                etPlace.setText(address);
                Log.i("place", "Place: " + place.getAddress());
            }


            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i("place.error", "An error occurred: " + status);
            }
        });

        //check and change if the toggleButton is checked
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked)
                    online = "1";
                else
                    online = "0";
            }
        });



        //Date Picker

        selectDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get Current Time
                final Calendar c = Calendar.getInstance();
                int cday = c.get(Calendar.DAY_OF_MONTH);
                int cmonth = c.get(Calendar.MONTH);
                int cyear = c.get(Calendar.YEAR);

                DatePickerDialog datePickerDialog = new DatePickerDialog(mcontext,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                                        Calendar calendar = Calendar.getInstance();
                                        calendar.set(year, month, day);


                                        @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
                                        strDate = format.format(calendar.getTime()); // Date for database
                                        Date parsed_date = null;
                                        try {
                                            parsed_date = format.parse(strDate);
                                            if (new Date().after(parsed_date)) {
                                                your_date_is_outdated = true;
                                            }
                                            else{
                                                your_date_is_outdated = false;
                                            }
                                        } catch (ParseException e) {
                                            e.printStackTrace();
                                        }
                                        Log.d("date", strDate);
                                        dateView.setText(strDate);
                            }
                        }, cyear, cmonth, cday);
                datePickerDialog.show();
            }
        });

        //Time Picker
        selectTIme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get Current Time
                final Calendar c = Calendar.getInstance();
                int mHour = c.get(Calendar.HOUR_OF_DAY);
                int mMinute = c.get(Calendar.MINUTE);

                // Launch Time Picker Dialog
                TimePickerDialog timePickerDialog = new TimePickerDialog(mcontext,
                        new TimePickerDialog.OnTimeSetListener() {

                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay,
                                                  int minute) {
                                start_time = new StringBuilder().append(hourOfDay).append(":").append(minute).toString();
                                hourView.setText(start_time);
                                start_time = new StringBuilder().append(hourOfDay).append(minute).append("00").toString();
                                Log.d("time", start_time);

                                AlertDialog.Builder alert = new AlertDialog.Builder(mcontext);
                                alert.setTitle("כמה זמן תמשך ההתנדבות");
                                final EditText input = new EditText(mcontext);
                                input.setInputType(InputType.TYPE_CLASS_NUMBER);
//                                input.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                                alert.setView(input);
                                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        duration = input.getText().toString();
                                        durationView.setText(new StringBuilder().append(duration).
                                                append(" שעות").toString());
                                    }
                                });
//                                alert.setNegativeButton(, new DialogInterface.OnClickListener() {
//                                    public void onClick(DialogInterface dialog, int whichButton) {
//                                        //Put actions for CANCEL button here, or leave in blank
//                                    }
//                                });
                                alert.show();
                            }
                        }, mHour, mMinute, true);
                timePickerDialog.show();
            }
        });


    }


    public void volunteerNumber(View view) {
        final TextView minVolView = findViewById(R.id.minVolView);
        final TextView maxVolView = findViewById(R.id.maxVolView);
        AlertDialog.Builder alert = new AlertDialog.Builder(mcontext);
        alert.setTitle("מספר מתנדבים מינימלי");
        final EditText input = new EditText(mcontext);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setRawInputType(Configuration.KEYBOARD_12KEY);
        alert.setView(input);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                min_volunteer = input.getText().toString();
                AlertDialog.Builder alert = new AlertDialog.Builder(mcontext);
                alert.setTitle("מספר מתנדבים מקסימלי");
                final EditText input = new EditText(mcontext);
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                input.setRawInputType(Configuration.KEYBOARD_12KEY);
                alert.setView(input);
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        max_volunteers = input.getText().toString();
                        minVolView.setText(new StringBuilder().append(min_volunteer).
                                append(" מינימום").toString());
                        maxVolView.setText(new StringBuilder().append(max_volunteers).
                                append(" מקסימום").toString());
                    }
                });
//                                alert.setNegativeButton(, new DialogInterface.OnClickListener() {
//                                    public void onClick(DialogInterface dialog, int whichButton) {
//                                        //Put actions for CANCEL button here, or leave in blank
//                                    }
//                                });
                alert.show();
            }
        });
//                                alert.setNegativeButton(, new DialogInterface.OnClickListener() {
//                                    public void onClick(DialogInterface dialog, int whichButton) {
//                                        //Put actions for CANCEL button here, or leave in blank
//                                    }
//                                });
        alert.show();
    }

    // ------------------------------- Photo Uploading ------------------------------- //

    // Function for creating the intent that choose the photo to be uploaded to firebase
    void chooseImg(){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    // uploading the image to firebase
    private void uploadImg() {
        if(imgUri != null) {
            picture = imgUri.toString();
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading...");
            progressDialog.show();

            StorageReference ref = mStorageRef.child("images/"+ UUID.randomUUID().toString());
            ref.putFile(imgUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressDialog.dismiss();
                            Toast.makeText(CreateVolunteerActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(CreateVolunteerActivity.this, "Failed " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0 * taskSnapshot.getBytesTransferred()/taskSnapshot
                                    .getTotalByteCount());
                            progressDialog.setMessage("Uploaded "+(int)progress+"%");
                        }
                    });
        }
    }

    // setting the chosen picture inside the imageView and calling uploadImg method
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imgUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imgUri);
                img.setImageBitmap(bitmap);
                uploadImg();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void sendToConfirm(View view) {
        name = nameTextView.getText().toString();
        about_place = pdescriptEditText.getText().toString(); // about the place
        about_volunteering = tdescriptEditText.getText().toString(); // about the volunteer itself
        value_in_coins = valueInCoinsEditText.getText().toString();

        if (your_date_is_outdated) {
            AlertDialog.Builder alert = new AlertDialog.Builder(mcontext);
            final TextView tv = new TextView(mcontext);
            tv.setText("התאריך שבחרת לא תקין");
            alert.setView(tv);
            alert.show();
        }
        else if (min_volunteer.equals("-1")){
            AlertDialog.Builder alert = new AlertDialog.Builder(mcontext);
            final TextView tv = new TextView(mcontext);
            tv.setText("בחר מינימום מתנדבים");
            alert.setView(tv);
            alert.show();
        }
        else if (awesomeValidation.validate())
        {
            StringRequest stringRequest = new StringRequest(Request.Method.POST, url + "volunteers/events/",
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.d("Response", response.toString());
                            Intent intent = new Intent(mcontext, MainActivity.class);
                            startActivity(intent);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
//                            Log.d("error.response", error.getMessage());
                        }
                    }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("name", name);
                    params.put("manager", manager);
                    params.put("picture", picture);
                    params.put("address", address);
                    params.put("online", online);
                    params.put("duration", duration);
                    params.put("about_place", about_place);
                    params.put("about_volunteering", about_volunteering);
                    params.put("min_volunteer", min_volunteer);
                    params.put("max_volunteers", max_volunteers);
                    params.put("value_in_coins", value_in_coins);
                    params.put("start_time", start_time);
                    return params;
                }
            };
            VolleySingleton.getInstance(this).addToRequestQueue(stringRequest);
        }
        else {
            Toast.makeText(getApplicationContext(),
                    "Validation Failed - Please fill all fields correctly", Toast.LENGTH_SHORT).show();
        }
    }
}

