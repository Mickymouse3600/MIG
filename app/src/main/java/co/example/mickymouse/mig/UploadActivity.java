package co.example.mickymouse.mig;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.File;

public class UploadActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int REQUEST_CAMERA=1, SELECT_FILE=0;

    private Button mButtonChooseImage;
    private Button mButtonUpload;
    private EditText mEditTextDescription;
    private EditText mEditTextContact;
    private EditText mEditTextFileName;
    private EditText mEditTextCompanyName;
    private ImageView mImageView;
    private ProgressBar mProgressBar;
    private Button mTextViewShowUploads;

    private Uri mImageUri;
    private String profileImageUrl;

    private StorageReference mStorageRef;
    private DatabaseReference mDatabaseRef;

    private StorageTask mUploadTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        //getSupportActionBar().setTitle("Upload");

       // getSupportActionBar().setDisplayHomeAsUpEnabled(true);
          

        mButtonChooseImage = (Button) findViewById(R.id.button_choose_image);
        mButtonUpload = (Button)findViewById(R.id.button_upload);
        mEditTextDescription = (EditText) findViewById(R.id.edit_description);
        mEditTextContact =(EditText) findViewById(R.id.edit_Contact);
        mEditTextFileName = (EditText)findViewById(R.id.edit_text_file_name);
        mEditTextCompanyName = (EditText)findViewById(R.id.companyname);
        mImageView = (ImageView) findViewById(R.id.image_view);
        mProgressBar =(ProgressBar) findViewById(R.id.progress_bar);
        mTextViewShowUploads = (Button) findViewById(R.id.button_show_uploads);

        mStorageRef = FirebaseStorage.getInstance().getReference("uploads");
        mDatabaseRef = FirebaseDatabase.getInstance().getReference("uploads");

        mButtonChooseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser();
            }
        });


        mButtonUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mUploadTask != null && mUploadTask.isInProgress()) {
                    Toast.makeText(UploadActivity.this, "Upload in progress", Toast.LENGTH_SHORT).show();
                } else {

                    uploadFile();
                }
            }
        });

            mTextViewShowUploads.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


            }
        });
    }

    private void openFileChooser() {
        final CharSequence[] items={"Camera","Gallery", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(UploadActivity.this);
        builder.setTitle("Add Image");

        builder.setItems(items, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (items[i].equals("Camera")) {

                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    File photo = new File(Environment.getExternalStorageDirectory(),  "Pic.jpg");
                    intent.putExtra(MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(photo));
                    mImageUri= Uri.fromFile(photo);
                    startActivityForResult(intent, REQUEST_CAMERA);

                   // Intent intent = new Intent();
                    //intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);

                } else if (items[i].equals("Gallery")) {

                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(intent, SELECT_FILE);

                } else if (items[i].equals("Cancel")) {
                    dialogInterface.dismiss();
                }
            }
        });
        builder.show();
    }

    @Override
    public  void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK ) {

            if (requestCode == REQUEST_CAMERA ) {

               /** Bundle bundle = data.getExtras();
                final Bitmap bmp = (Bitmap) bundle.get("data");
                mImageView.setImageBitmap(bmp);
                mImageUri=data.getData();
                 Log.i("imageURI", String.valueOf( bundle.get("data")));
                **/
               // Uri selectedImage = mImageUri;
                getContentResolver().notifyChange(mImageUri, null);
                ContentResolver cr = getContentResolver();
                Bitmap bitmap;
                try {
                    bitmap = android.provider.MediaStore.Images.Media
                            .getBitmap(cr, mImageUri);

                    mImageView.setImageBitmap(bitmap);
                    Toast.makeText(this, mImageUri.toString(),
                            Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Failed to load", Toast.LENGTH_SHORT)
                            .show();
                    Log.e("Camera", e.toString());
                }




            } else if (requestCode == SELECT_FILE && resultCode == RESULT_OK
                    && data != null && data.getData() != null) {
                mImageUri = data.getData();
                Log.i("imageURI", String.valueOf(  data.getData()));

                Picasso.get().load(mImageUri).into(mImageView);
            }

        }
    }

    private String getFileExtension(Uri uri) {
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }

    private void  uploadFile() {


        if (mImageUri != null) {
            mButtonUpload.setEnabled(false);
            mStorageRef= mStorageRef.child(System.currentTimeMillis()
                    + "." + getFileExtension(mImageUri));

            UploadTask uploadTask;
            uploadTask = mStorageRef.putFile(mImageUri );

            final Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    // Continue with the task to get the download URL
                    return mStorageRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        mImageUri = task.getResult();

                        Upload upload = new Upload(mEditTextFileName.getText().toString().trim(),mEditTextCompanyName.getText().toString().trim(),mImageUri.toString()
                                ,mEditTextDescription.getText().toString().trim(),mEditTextContact.getText().toString().trim());

                        String uploadId = mDatabaseRef.push().getKey();
                        mDatabaseRef.child(uploadId).setValue(upload);
                        profileImageUrl = mImageUri.toString();

                        Log.i("Url",profileImageUrl);
                        Toast.makeText(UploadActivity.this, "Upload successful", Toast.LENGTH_LONG).show();

                    } else {

                        Toast.makeText(UploadActivity.this, "Upload unsuccessful", Toast.LENGTH_LONG).show();
                    }
                }
            });

         }
         else {

            Toast.makeText(UploadActivity.this, "Problem Incared", Toast.LENGTH_LONG).show();

            mButtonUpload.setEnabled(true);
        }

}}