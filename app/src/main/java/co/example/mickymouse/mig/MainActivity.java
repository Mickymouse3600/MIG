package co.example.mickymouse.mig;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private ImageAdapter mAdapter;
    private ProgressBar mProgressCircle;
    private DatabaseReference mDatabaseRef;
    private FirebaseStorage mStorage;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private List<Upload> mUploads;
    private FloatingActionButton Fab;

    private int ITEM_LOAD_COUNT=5;
    private int Total_item=0,last_visible_item;
    private boolean isLoading=false,isMaxData=false;
    private   String last_node="",last_key="";
    private static final String TAG = "MainActivity";
    private String mUsername;


    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
    private static final int RC_SIGN_IN =1;
    private static final int RC_PHOTO_PICKER =2;






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    //user is signed in
                    Toast.makeText(MainActivity.this, "Welcome to Made In GhanaApp Gold Cost of Africa", Toast.LENGTH_SHORT).show();
                    OnSignedInInitialize(user.getDisplayName());
                } else {
                    //user is signed out
                    OnSignedOutCleanup();
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(true)
                                    .setAvailableProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.GoogleBuilder().build(),
                                            new AuthUI.IdpConfig.EmailBuilder().build(),
                                            new AuthUI.IdpConfig.AnonymousBuilder().build()))
                                    .build(),
                            RC_SIGN_IN);
                }

            }
        };

        Fab=findViewById(R.id.fab);
        mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);
        final LinearLayoutManager LayoutManager= new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(LayoutManager);

        mProgressCircle = findViewById(R.id.progress_circle);



        mUsername = ANONYMOUS;


        Fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this,UploadActivity.class));
            }
        });

        mUploads = new ArrayList<>();
        mStorage = FirebaseStorage.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mDatabaseRef = FirebaseDatabase.getInstance().getReference("uploads");



        getLastKeyFromFirebase();
        getUploads();

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                Total_item= LayoutManager.getItemCount();
                last_visible_item=LayoutManager.findLastVisibleItemPosition();

                if (!isLoading && Total_item <=((last_visible_item + ITEM_LOAD_COUNT ))){
                    getUploads();
                    isLoading=true;
                }

            }
        });
        

        mDatabaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                     Upload upload = postSnapshot.getValue(Upload.class);
                     mUploads.add(upload);
                     //Log.i("URL",upload.getImageUrl());

                }
                Collections.reverse(mUploads);

                mAdapter = new ImageAdapter(MainActivity.this, mUploads);

                mRecyclerView.setAdapter(mAdapter);
                mProgressCircle.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                mProgressCircle.setVisibility(View.INVISIBLE);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.toolbarmenu, menu );
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if (requestCode==RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Signed In", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Sign In Canceled", Toast.LENGTH_SHORT).show();
                finish();
            }
        }


        }



    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener!=null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        //detachDatabaseReadListener();
        mUploads.clear();

    }


    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }


    private void OnSignedInInitialize (String username){
        mUsername=username;
       getUploads();


    }
    private void OnSignedOutCleanup (){
        mUsername=ANONYMOUS;
        mUploads.clear();
        //detachDatabaseReadListener();


    }

    private void getUploads() {

        if(!isMaxData){

            Query query;
            if(TextUtils.isEmpty(last_node))
                query= FirebaseDatabase.getInstance().getReference()
                        .child("Uploads")
                        .orderByKey()
                        .limitToFirst(ITEM_LOAD_COUNT);

        else

            query= FirebaseDatabase.getInstance().getReference()
                    .child("Uploads")
                    .orderByKey()
                    .startAt(last_node)
                    .limitToFirst(ITEM_LOAD_COUNT);


        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChildren())
                {

                    List<Upload> newUploads= new ArrayList<>();
                    for(DataSnapshot uploadsnapshot:dataSnapshot.getChildren())
                    {
                        newUploads.add(uploadsnapshot.getValue(Upload.class));

                    }

                     last_node=newUploads.get(newUploads.size()-1).getKey();
                    if (!last_node.equals(last_key))
                        newUploads.remove(newUploads.size()-1);
                    else
                        last_node="end";
                    mAdapter.addAll(newUploads);
                    isLoading=false;
                }
                else
                    {
                        isLoading=false;
                        isMaxData=true;
             }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

                isLoading=false;
            }
        });


        }
    }

    private void getLastKeyFromFirebase() {

        final Query getLastKey= FirebaseDatabase.getInstance().getReference()
                .child("Uploads")
                .orderByKey()
                .limitToLast(1);

        getLastKey.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot lastkey : dataSnapshot.getChildren())
                    last_key=lastkey.getKey();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MainActivity.this,"Cannot get last key",Toast.LENGTH_LONG).show();

            }
        });

    }


}
