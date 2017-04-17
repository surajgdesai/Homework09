package com.example.surajgdesai.homework09a;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, GoogleApiClient.OnConnectionFailedListener {

    GoogleApiClient googleApiClient;
    int SignIn = 9001;
    SharedPreferences preferences;
    SharedPreferences.Editor prefEditor;
    User user;
    EditText username, pwd;
    FirebaseAuth firebaseAuth;
    DatabaseReference refDatabase;
    FirebaseDatabase refDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        refDb = FirebaseDatabase.getInstance();
        refDatabase = refDb.getReference().child("Users");

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        prefEditor = preferences.edit();

        prefEditor.clear();
        prefEditor.commit();

        firebaseAuth = FirebaseAuth.getInstance();
        Gson gson = new Gson();
        String json = preferences.getString(getResources().getString(R.string.activeUsr), "");
        if (json != null && !json.equals("") && !json.equals("null")) {
            user = gson.fromJson(json, User.class);
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.putExtra(getResources().getString(R.string.activeUsr), user);
            startActivity(intent);
        } else {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail().build();
            googleApiClient = new GoogleApiClient.Builder(this).enableAutoManage(this, this)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso).addApi(Plus.API).build();
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.gsigninbtn:
                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
                startActivityForResult(signInIntent, SignIn);
                break;
            case R.id.loginbtn:
                username = (EditText) findViewById(R.id.usrnmedt);
                pwd = (EditText) findViewById(R.id.pwdedt);
                if (!username.getText().toString().trim().equals("") && !pwd.getText().toString().trim().equals("")) {
                    firebaseAuth.signInWithEmailAndPassword(username.getText().toString().trim(), pwd.getText().toString().trim()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()) {
                                Toast.makeText(MainActivity.this, getResources().getString(R.string.NotSignedIn),
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                final FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                                DatabaseReference userQuery = refDatabase.child(firebaseUser.getUid());
                                userQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        user = dataSnapshot.getValue(User.class);
                                        MainActivity.this.navigateToDashboard(user);
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });

                            }
                        }
                    });

                } else {
                    Toast.makeText(this, getResources().getString(R.string.ProvideValidInput), Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.signupbtn:
                Intent intent = new Intent(this, SignUpActivity.class);
                startActivity(intent);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SignIn) {
            GoogleSignInResult googleSignInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            Person person = Plus.PeopleApi.getCurrentPerson(googleApiClient);

            final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setCancelable(false);
            final FirebaseUser firebaseuser = firebaseAuth.getCurrentUser();

            user = new User();
            user.setFname(googleSignInResult.getSignInAccount().getGivenName());
            user.setLname(googleSignInResult.getSignInAccount().getFamilyName());
            user.setGender(person.getGender() == 0 ? "Male" : "Female");
            user.setProfilePicUrl(googleSignInResult.getSignInAccount().getPhotoUrl().toString());
            user.setDisplayName(googleSignInResult.getSignInAccount().getDisplayName());
            user.setKey(firebaseuser.getUid());


            refDatabase.child(user.getKey()).setValue(user);

            Gson gson = new Gson();
            String json = gson.toJson(user);
            prefEditor.putString(getResources().getString(R.string.activeUsr), json);
            prefEditor.commit();

            Intent intent = new Intent(this, DashboardActivity.class);
            intent.putExtra(getResources().getString(R.string.activeUsr), user);
            startActivity(intent);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void navigateToDashboard(User thisUser) {
        Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
        intent.putExtra(getResources().getString(R.string.activeUsr), thisUser);
        startActivity(intent);
    }
}
