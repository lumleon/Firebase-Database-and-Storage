package com.anrex.raceblogger;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.anrex.raceblogger.models.BankerPost;
import com.anrex.raceblogger.models.User;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BankerPostNewActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "BankerPostNewActivity";
    private static final String REQUIRED = "Required";

    private static final int GALLERY_REQUEST = 1;
    //set Firebase Storage Reference
    private StorageReference mStorageTipsImage;

    //Uri for Image in phone when chosen
    private Uri mImageUri;

    //Uri for image in Firebase Storage
    private Uri mDownloadUrl;

    // [START declare_database_ref]
    private DatabaseReference mDatabaseReference;
    // [END declare_database_ref]

    private EditText mTitleField;
    private EditText mBodyField;
    private FloatingActionButton mSubmitButton;

    private EditText mCapitalField;
    private EditText mRoiField;
    private EditText mChargeField;
    private EditText mRacedateField;
    private ImageView mSelectImage;
    private DatePickerDialog inputDatePickerDialog;

    private SimpleDateFormat dateFormatter;
    private EditText mBrandnoField;
    private String mColorURL;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bankerpost_new);

        // [START initialize_database_ref]
         mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        // [END initialize_database_ref]

        mBrandnoField = (EditText)  findViewById(R.id.field_brandno);
        mTitleField = (EditText) findViewById(R.id.field_title);
        mBodyField = (EditText) findViewById(R.id.field_body);
        mRacedateField = (EditText) findViewById(R.id.field_racedate);
        mCapitalField = (EditText) findViewById(R.id.field_capital);
        mRoiField = (EditText) findViewById(R.id.field_roi);
        mChargeField = (EditText)  findViewById(R.id.field_charge);
        mSelectImage =  (ImageButton) findViewById(R.id.imageSelect);

        mRacedateField.setInputType(InputType.TYPE_NULL);
        mCapitalField.setInputType(InputType.TYPE_NULL);
        mRoiField.setInputType(InputType.TYPE_NULL);
        setDateTimeField();

        mSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, GALLERY_REQUEST);
            }
        });

        mSubmitButton = (FloatingActionButton) findViewById(R.id.fab_submit_post);
        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postImage();
            }
        });

    }

    /**
     * Method to update image to Storage
     */
    private void postImage() {

        if (mImageUri != null) {
            //start uploading to DB
            //authenticate before letting upload(not in this app)
            //   mProgress.show();

            mStorageTipsImage = FirebaseStorage.getInstance().getReference(CHILD_BANKERPOST_TIPS_IMAGE);
            StorageReference filePath = mStorageTipsImage.child(mImageUri.getLastPathSegment());
            filePath.putFile(mImageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    mDownloadUrl = taskSnapshot.getDownloadUrl();
                    mColorURL= mDownloadUrl.toString();
                    //post tips to database

                    submitPost();
                }
            });

        } else {
                        //  "http://racing.hkjc.com/racing/content/Images/RaceColor/A170.gif"
            mColorURL =   "http://racing.hkjc.com/racing/content/Images/RaceColor/"
                            + mBrandnoField.getText().toString().trim()
                            +"." +"gif";

                submitPost();
        }
    }


    /**
     *  OnActivity Result for choosing image
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_REQUEST && resultCode == RESULT_OK) {
            mImageUri = data.getData();
            mSelectImage.setImageURI(mImageUri);
        }
    }

    /**
     * Method to set the datepicker
     */
    private void setDateTimeField() {

        dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        mRacedateField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inputDatePickerDialog.show();
            }
        });

        Calendar newCalendar = Calendar.getInstance();
        inputDatePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {

            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                mRacedateField.setText(dateFormatter.format(newDate.getTime()));
            }

        },newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
    }

    @Override
    public void onClick(View view) {
        if(view == mRacedateField) {
            inputDatePickerDialog.show();
        } else if(view == mRacedateField) {
            inputDatePickerDialog.show();
        }
    }

    private void submitPost() {
        final String title = mTitleField.getText().toString().trim();
        final String body = mBodyField.getText().toString().trim();
        final String racedate = mRacedateField.getText().toString().trim();
      //  final int capital = Integer.parseInt(mCapitalField.getText().toString().trim());
        final String roi = mRoiField.getText().toString().trim();
        final String colorurl = mColorURL.toString().trim();
        final String charge = mChargeField.getText().toString().trim();
        final String brandno = mBrandnoField.getText().toString().trim();

        // Brandno is required
        if (TextUtils.isEmpty(brandno)) {
            mBrandnoField.setError(REQUIRED);
            return;
        }

        // Title is required
        if (TextUtils.isEmpty(title)) {
            mTitleField.setError(REQUIRED);
            return;
        }
        // Racedate is required
        if (TextUtils.isEmpty(racedate)) {
            mRacedateField.setError(REQUIRED);
            return;
        }

        // Body is required
        if (TextUtils.isEmpty(body)) {
            mBodyField.setError(REQUIRED);
            return;
        }

        // Charge is required
        if (TextUtils.isEmpty(charge)) {
            mChargeField.setError(REQUIRED);
            return;
        }

        // Disable button so there are no multi-posts
        setEditingEnabled(false);
        Toast.makeText(this, "Posting...", Toast.LENGTH_SHORT).show();

        // [START single_value_read]
      //  final String userId = getUid();

        final String userId  =  FirebaseAuth.getInstance().getCurrentUser().getUid();


        mDatabaseReference.child(CHILD_USER).child(userId).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // Get user value
                    //    User user = dataSnapshot.getValue(User.class);

                        String authorName =  FirebaseAuth.getInstance().getCurrentUser().getDisplayName();

                        // [START_EXCLUDE]
                        if (userId == null) {
                            // User is null, error out
                            Log.e(TAG, "User " + userId + " is unexpectedly null");
                            Toast.makeText(BankerPostNewActivity.this,
                                    "Error: could not fetch user.",
                                    Toast.LENGTH_SHORT).show();
                        } else {

                            //set status to "活躍" tab
                            String status ="1";
                            int profit = 0;
                            int capital =0;
                            // Write new post
                            writeNewPost(userId, authorName, brandno, title, body, racedate, capital, roi,status, colorurl, charge, profit);
                        }

                        // Finish this Activity, back to the stream
                        setEditingEnabled(true);
                        finish();
                        // [END_EXCLUDE]
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w(TAG, "getUser:onCancelled", databaseError.toException());
                        // [START_EXCLUDE]
                        setEditingEnabled(true);
                        // [END_EXCLUDE]
                    }
                });
        // [END single_value_read]
    }



    private void setEditingEnabled(boolean enabled) {
        mTitleField.setEnabled(enabled);
        mBodyField.setEnabled(enabled);
        if (enabled) {
            mSubmitButton.setVisibility(View.VISIBLE);
        } else {
            mSubmitButton.setVisibility(View.GONE);
        }
    }

    // [START write_fan_out]
    private void writeNewPost(String userId, String username, String brandno, String title, String body, String racedate, int capital, String roi , String status , String colorurl, String charge, int profit) {
        // Create new bankerPost at /user-posts/$userid/$postid and at
        // /posts/$postid simultaneously


        String key = mDatabaseReference.child(CHILD_BANKERPOSTS).push().getKey();
        BankerPost bankerPost = new BankerPost(userId, username, brandno, title, body, racedate, capital, roi, status , colorurl , charge, profit);
        Map<String, Object> postValues = bankerPost.toMap();

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/"+CHILD_BANKERPOSTS+"/" + key, postValues);
       // childUpdates.put("/"+CHILD_USER_POSTS+"/" + userId + "/" + key, postValues);

        mDatabaseReference.updateChildren(childUpdates);
    }

    // [END write_fan_out]


}
