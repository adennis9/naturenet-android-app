package org.naturenet.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import com.google.common.collect.Lists;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.naturenet.R;
import org.naturenet.data.model.Observation;
import org.naturenet.data.model.Project;
import org.naturenet.data.model.Users;

import java.util.Collections;
import java.util.List;

public class AddObservationActivity extends AppCompatActivity {

    static String FRAGMENT_TAG_ADD_OBSERVATION = "add_observation_fragment";
    static String OBSERVATION = "observation";
    static String OBSERVATION_PATH = "observation_path";
    static String SIGNED_USER = "signed_user";
    static String EMAIL = "email";
    static String PASSWORD = "password";
    static String LATEST_CONTRIBUTION = "latest_contribution";
    static String LOADING = "Loading...";
    static String DEFAULT_PROJECT_ID = "-ACES_a38";
    static String EMPTY = "";

    DatabaseReference fbRef;
    ProgressDialog pd;
    String signed_user_email, signed_user_password;
    Uri observationPath;
    Observation newObservation;
    Project defaultProject;
    Users signedUser;
    List<Project> mProjects = Lists.newArrayList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_observation);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(EMPTY);
        setSupportActionBar(toolbar);
        newObservation = getIntent().getParcelableExtra(OBSERVATION);
        observationPath = getIntent().getParcelableExtra(OBSERVATION_PATH);
        signedUser = getIntent().getParcelableExtra(SIGNED_USER);
        signed_user_email = getIntent().getStringExtra(EMAIL);
        signed_user_password = getIntent().getStringExtra(PASSWORD);
        pd = new ProgressDialog(this);
        defaultProject = null;
        goToAddObservationFragment();
    }

    public void goToAddObservationFragment() {
        if (signedUser != null) {
            fbRef = FirebaseDatabase.getInstance().getReference();
            pd.setMessage(LOADING);
            pd.setCancelable(false);
            pd.show();
            fbRef.child(Project.NODE_NAME).orderByChild(LATEST_CONTRIBUTION).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {

                    for (DataSnapshot child : snapshot.getChildren()) {
                        Project project = child.getValue(Project.class);
                        if (project.id.equals(DEFAULT_PROJECT_ID)) {
                            defaultProject = project;
                        }

                        // TODO: decide on site-project submission rule
                        //Map<String, Object> sites = (Map<String, Object>) map.get(SITES);
                        //if (sites.containsKey(signedUser.affiliation)) {
                            mProjects.add(project);
                        //}
                    }

                    // Timestamps sort in ascending order
                    Collections.reverse(mProjects);

                    if (mProjects.size() != 0) {
                        getFragmentManager().
                                beginTransaction().
                                replace(R.id.fragment_container, new AddObservationFragment(), FRAGMENT_TAG_ADD_OBSERVATION).
                                commit();
                    }

                    pd.dismiss();
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {
                    pd.dismiss();
                    Toast.makeText(AddObservationActivity.this, "Could not get projects: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            pd.dismiss();
            getFragmentManager().
                    beginTransaction().
                    replace(R.id.fragment_container, new AddObservationFragment(), FRAGMENT_TAG_ADD_OBSERVATION).
                    addToBackStack(null).
                    commit();
        }
    }

    public void goBackToMainActivity() {
        findViewById(R.id.toolbar_send).setVisibility(View.GONE);
        findViewById(R.id.toolbar_busy).setVisibility(View.VISIBLE);
        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.putExtra(OBSERVATION, newObservation);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }
}