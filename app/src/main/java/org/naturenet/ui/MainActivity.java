package org.naturenet.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.FileProvider;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import org.naturenet.NatureNetApplication;
import org.naturenet.R;
import org.naturenet.UploadService;
import org.naturenet.data.ObserverInfo;
import org.naturenet.data.PreviewInfo;
import org.naturenet.data.model.Comment;
import org.naturenet.data.model.Observation;
import org.naturenet.data.model.Project;
import org.naturenet.data.model.Site;
import org.naturenet.data.model.Users;
import org.naturenet.util.CroppedCircleTransformation;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.functions.Consumer;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    final static int REQUEST_CODE_JOIN = 1;
    final static int REQUEST_CODE_LOGIN = 2;
    final static int REQUEST_CODE_ADD_OBSERVATION = 3;
    final static int REQUEST_CODE_PROJECT_ACTIVITY = 4;
    final static int REQUEST_CODE_OBSERVATION_ACTIVITY = 5;
    final static int NUM_OF_OBSERVATIONS = 20;
    static String FRAGMENT_TAG_LAUNCH = "launch_fragment";
    static String FRAGMENT_TAG_EXPLORE = "explore_fragment";
    static String FRAGMENT_TAG_PROJECTS = "projects_fragment";
    static String FRAGMENT_TAG_DESIGNIDEAS = "designideas_fragment";
    static String FRAGMENT_TAG_COMMUNITIES = "communities_fragment";
    static String LOGIN = "login";
    static String GUEST = "guest";
    static String LAUNCH = "launch";
    static String JOIN = "join";
    static String IDS = "ids";
    static String NAMES = "names";
    static String NEW_USER = "new_user";
    static String SIGNED_USER = "signed_user";
    static String UPDATED_AT = "updated_at";
    static String NAME = "name";
    static String OBSERVATION = "observation";
    static String OBSERVATION_PATH = "observation_path";
    static String PROJECT = "project";
    static String EMPTY = "";
    static String LOADING_OBSERVATIONS = "Loading Observations...";
    static String LOADING_DESIGN_IDEAS = "Loading Design Ideas...";
    static String LOADING_COMMUNITIES = "Loading Communities...";
    static String SIGNING_OUT = "Signing Out...";
    static String OBSERVERS = "observers";
    static String OBSERVATIONS = "observations";

    String[] affiliation_ids, affiliation_names;
    Observation newObservation, selectedObservation, previewSelectedObservation;
    ObserverInfo selectedObserverInfo;
    ArrayList<Observation> observations;
    List<ObserverInfo> observers;
    List<Comment> comments;
    List<String> ids, names;
    DatabaseReference mFirebase;
    Users signed_user;
    Site user_home_site;
    Uri observationPath;
    DrawerLayout drawer;
    Toolbar toolbar;
    NavigationView navigationView;
    View header;
    Button sign_in, join;
    TextView display_name, affiliation, licenses;
    ImageView nav_iv;
    MenuItem logout;
    ProgressDialog pd;
    public static FragmentManager fragmentManager;
    Map<Observation, PreviewInfo> previews = new HashMap<>();
    private Transformation mAvatarTransform = new CroppedCircleTransformation();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        fragmentManager = getSupportFragmentManager();
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        logout = navigationView.getMenu().findItem(R.id.nav_logout);
        header = navigationView.getHeaderView(0);
        sign_in = (Button) header.findViewById(R.id.nav_b_sign_in);
        join = (Button) header.findViewById(R.id.nav_b_join);
        nav_iv = (ImageView) header.findViewById(R.id.nav_iv);
        display_name = (TextView) header.findViewById(R.id.nav_tv_display_name);
        affiliation = (TextView) header.findViewById(R.id.nav_tv_affiliation);
        licenses = (TextView) navigationView.findViewById(R.id.licenses);
        toolbar.setTitle(EMPTY);
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        licenses.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(v.getContext())
                            .setView(View.inflate(MainActivity.this, R.layout.about, null))
                            .setNegativeButton("Dismiss", null)
                            .setCancelable(false)
                            .show();
                }
            }
        );

        this.invalidateOptionsMenu();
        pd = new ProgressDialog(this);
        pd.setCancelable(false);

        sign_in.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MainActivity.this.haveNetworkConnection()) {
                    MainActivity.this.goToLoginActivity();
                } else {
                    Toast.makeText(MainActivity.this, "No Internet Connection", Toast.LENGTH_SHORT).show();
                }
            }
        });

        join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MainActivity.this.haveNetworkConnection()) {
                    MainActivity.this.goToJoinActivity();
                } else {
                    Toast.makeText(MainActivity.this, "No Internet Connection", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mFirebase = FirebaseDatabase.getInstance().getReference();
        updateUINoUser();
        observations = null;
        observers = null;
        selectedObservation = null;
        selectedObserverInfo = null;
        comments = null;
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            new android.app.AlertDialog.Builder(this)
                .setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        MainActivity.this.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .create().show();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        ((NatureNetApplication)getApplication()).getCurrentUserObservable().subscribe(new Consumer<Optional<Users>>() {
            @Override
            public void accept(Optional<Users> user) throws Exception {
                if (user.isPresent()) {
                    MainActivity.this.onUserSignIn(user.get());
                } else {
                    if (signed_user != null) {
                        mFirebase.child(Users.NODE_NAME).child(signed_user.id).keepSynced(false);
                        MainActivity.this.onUserSignOut();
                    }
                    MainActivity.this.updateUINoUser();
                    MainActivity.this.goToLaunchFragment();
                }
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (haveNetworkConnection()) {
            int id = item.getItemId();
            switch(id) {
                case R.id.nav_explore:
                    goToExploreFragment();
                    drawer.closeDrawer(GravityCompat.START);
                    break;
                case R.id.nav_gallery:
                    goToGalleryFragment();
                    drawer.closeDrawer(GravityCompat.START);
                    break;
                case R.id.nav_projects:
                    goToProjectsFragment();
                    drawer.closeDrawer(GravityCompat.START);
                    break;
                case R.id.nav_design_ideas:
                    pd.setMessage(LOADING_DESIGN_IDEAS);
                    pd.setCancelable(false);
                    pd.show();
                    goToDesignIdeasFragment();
                    drawer.closeDrawer(GravityCompat.START);
                    pd.dismiss();
                    break;
                case R.id.nav_communities:
                    pd.setMessage(LOADING_COMMUNITIES);
                    pd.setCancelable(false);
                    pd.show();
                    goToCommunitiesFragment();
                    drawer.closeDrawer(GravityCompat.START);
                    pd.dismiss();
                    break;
                case R.id.nav_logout:
                    pd.setMessage(SIGNING_OUT);
                    pd.setCancelable(false);
                    pd.show();
                    FirebaseAuth.getInstance().signOut();
                    pd.dismiss();
                    break;
            }
        } else {
            Toast.makeText(MainActivity.this, "No Internet Connection", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    public void goToLaunchFragment() {
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new LaunchFragment(), FRAGMENT_TAG_LAUNCH)
                .addToBackStack(FRAGMENT_TAG_LAUNCH)
                .commit();
    }

    public void goToGalleryFragment() {
        if (haveNetworkConnection()) {
            pd.setMessage(LOADING_OBSERVATIONS);
            pd.setCancelable(false);
            pd.show();

            if (observations == null) {
                observations = Lists.newArrayList();
                observers = Lists.newArrayList();
                mFirebase = FirebaseDatabase.getInstance().getReference();

                mFirebase.child(Observation.NODE_NAME).orderByChild(UPDATED_AT).limitToLast(NUM_OF_OBSERVATIONS).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        for(DataSnapshot child : snapshot.getChildren()) {
                            final Observation observation = child.getValue(Observation.class);
                            observations.add(observation);
                            final PreviewInfo preview = new PreviewInfo();
                            preview.observationImageUrl = observation.data.image;

                            if (observation.data.text != null) {
                                preview.observationText = observation.data.text;
                            } else {
                                preview.observationText = "No Description";
                            }

                            if (observation.comments != null) {
                                preview.commentsCount = Integer.toString(observation.comments.size());
                            } else {
                                preview.commentsCount = "0";
                            }

                            if (observation.likes != null) {
                                preview.likesCount = String.valueOf(HashMultiset.create(observation.likes.values()).count(true));
                            } else {
                                preview.likesCount = "0";
                            }

                            boolean contains = false;
                            for (int i=0; i<observers.size(); i++) {
                                contains = observers.get(i).getObserverId().equals(observation.userId);
                                if (contains) {
                                    preview.observerAvatarUrl = observers.get(i).getObserverAvatar();
                                    preview.observerName = observers.get(i).getObserverName();
                                    preview.affiliation = observers.get(i).getObserverAffiliation();
                                    break;
                                }
                            }

                            if (!contains) {
                                final ObserverInfo observer = new ObserverInfo();
                                observer.setObserverId(observation.userId);
                                DatabaseReference f = FirebaseDatabase.getInstance().getReference();
                                f.child(Users.NODE_NAME).child(observation.userId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot snapshot) {
                                        Users user = snapshot.getValue(Users.class);
                                        observer.setObserverName(user.displayName);
                                        observer.setObserverAvatar(user.avatar);
                                        DatabaseReference fb = FirebaseDatabase.getInstance().getReference();
                                        fb.child(Site.NODE_NAME).child(user.affiliation).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot snapshot) {
                                                Site site =  snapshot.getValue(Site.class);
                                                observer.setObserverAffiliation(site.name);
                                                preview.observerAvatarUrl = observer.getObserverAvatar();
                                                preview.observerName = observer.getObserverName();
                                                preview.affiliation = observer.getObserverAffiliation();
                                                observers.add(observer);
                                                previews.put(observation, preview);
                                                if (observations.size() >= NUM_OF_OBSERVATIONS) {
                                                    pd.dismiss();
                                                    goToObservationActivity();
                                                }
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {}
                                        });
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {}
                                });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        pd.dismiss();
                        Toast.makeText(MainActivity.this, "Could not get observations: "+ databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                pd.dismiss();
                goToObservationActivity();
            }
        } else {
            Toast.makeText(MainActivity.this, "No Internet Connection", Toast.LENGTH_SHORT).show();
        }
    }

    public void goToExploreFragment() {
        if (haveNetworkConnection()) {
            pd.setMessage(LOADING_OBSERVATIONS);
            pd.setCancelable(false);
            pd.show();
            if (observations == null) {
                observations = Lists.newArrayList();
                observers = Lists.newArrayList();
                mFirebase = FirebaseDatabase.getInstance().getReference();
                mFirebase.child(Observation.NODE_NAME).orderByChild(UPDATED_AT).limitToLast(NUM_OF_OBSERVATIONS).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        for(DataSnapshot child : snapshot.getChildren()) {
                            final Observation observation = child.getValue(Observation.class);
                            observations.add(observation);
                            final PreviewInfo preview = new PreviewInfo();
                            preview.observationImageUrl = observation.data.image;
                            if (observation.data.text != null) {
                                preview.observationText = observation.data.text;
                            } else {
                                preview.observationText = "No Description";
                            }
                            if (observation.comments != null) {
                                preview.commentsCount = Integer.toString(observation.comments.size());
                            } else {
                                preview.commentsCount = "0";
                            }
                            if (observation.likes != null) {
                                preview.likesCount = String.valueOf(HashMultiset.create(observation.likes.values()).count(true));
                            } else {
                                preview.likesCount = "0";
                            }
                            boolean contains = false;
                            for (int i = 0; i < observers.size(); i++) {
                                contains = observers.get(i).getObserverId().equals(observation.userId);
                                if (contains) {
                                    preview.observerAvatarUrl = observers.get(i).getObserverAvatar();
                                    preview.observerName = observers.get(i).getObserverName();
                                    preview.affiliation = observers.get(i).getObserverAffiliation();
                                    break;
                                }
                            }
                            if (!contains) {
                                final ObserverInfo observer = new ObserverInfo();
                                observer.setObserverId(observation.userId);
                                DatabaseReference f = FirebaseDatabase.getInstance().getReference();
                                f.child(Users.NODE_NAME).child(observation.userId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot snapshot) {
                                        Users user = snapshot.getValue(Users.class);
                                        observer.setObserverName(user.displayName);
                                        observer.setObserverAvatar(user.avatar);
                                        DatabaseReference fb = FirebaseDatabase.getInstance().getReference();
                                        fb.child(Site.NODE_NAME).child(user.affiliation).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot snapshot) {
                                                Site site = snapshot.getValue(Site.class);
                                                observer.setObserverAffiliation(site.name);
                                                preview.observerAvatarUrl = observer.getObserverAvatar();
                                                preview.observerName = observer.getObserverName();
                                                preview.affiliation = observer.getObserverAffiliation();
                                                observers.add(observer);
                                                previews.put(observation, preview);
                                                if (observations.size() >= NUM_OF_OBSERVATIONS) {
                                                    pd.dismiss();
                                                    getFragmentManager().
                                                            beginTransaction().
                                                            replace(R.id.fragment_container, new ExploreFragment(), FRAGMENT_TAG_EXPLORE).
                                                            addToBackStack(FRAGMENT_TAG_EXPLORE).
                                                            commit();
                                                }
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {
                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                    }
                                });
                            }
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        pd.dismiss();
                        Toast.makeText(MainActivity.this, "Could not get observations: "+ databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                pd.dismiss();
                getFragmentManager().
                        beginTransaction().
                        replace(R.id.fragment_container, new ExploreFragment(), FRAGMENT_TAG_EXPLORE).
                        addToBackStack(FRAGMENT_TAG_EXPLORE).
                        commitAllowingStateLoss();
            }
        } else {
            Toast.makeText(MainActivity.this, "No Internet Connection", Toast.LENGTH_SHORT).show();
        }
    }

    public void goToProjectsFragment() {
        getFragmentManager().
                beginTransaction().
                replace(R.id.fragment_container, new ProjectsFragment(), FRAGMENT_TAG_PROJECTS).
                addToBackStack(FRAGMENT_TAG_PROJECTS).
                commit();
    }

    public void goToDesignIdeasFragment() {
        getFragmentManager().
                beginTransaction().
                replace(R.id.fragment_container, new IdeasFragment(), FRAGMENT_TAG_DESIGNIDEAS).
                addToBackStack(FRAGMENT_TAG_DESIGNIDEAS).
                commit();
    }

    public void goToCommunitiesFragment() {
        getFragmentManager().
                beginTransaction().
                replace(R.id.fragment_container, new CommunitiesFragment(), FRAGMENT_TAG_COMMUNITIES).
                addToBackStack(FRAGMENT_TAG_COMMUNITIES).
                commit();
    }

    public void onUserSignOut() {
        Toast.makeText(this, "You have been logged out.", Toast.LENGTH_SHORT).show();
        signed_user = null;
        user_home_site = null;
        logout.setVisible(false);
        this.invalidateOptionsMenu();
        Picasso.with(this).load(R.drawable.default_avatar)
                .transform(mAvatarTransform).fit().into(nav_iv);
        display_name.setText(EMPTY);
        affiliation.setText(EMPTY);
        sign_in.setVisibility(View.VISIBLE);
        join.setVisibility(View.VISIBLE);
        display_name.setVisibility(View.GONE);
        affiliation.setVisibility(View.GONE);
    }

    public void goToJoinActivity() {
        ids = new ArrayList<>();
        names = new ArrayList<>();
        mFirebase.child(Site.NODE_NAME).orderByKey().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Site site = postSnapshot.getValue(Site.class);
                    ids.add(site.id);
                    names.add(site.name);
                }
                if (ids.size() != 0 && names.size() != 0) {
                    affiliation_ids = ids.toArray(new String[ids.size()]);
                    affiliation_names = names.toArray(new String[names.size()]);
                    Intent join = new Intent(getApplicationContext(), JoinActivity.class);
                    join.putExtra(IDS, affiliation_ids);
                    join.putExtra(NAMES, affiliation_names);
                    startActivityForResult(join, REQUEST_CODE_JOIN);
                    overridePendingTransition(R.anim.slide_up, R.anim.stay);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.join_error_message_firebase_read) + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void goToLoginActivity() {
        Intent login = new Intent(this, LoginActivity.class);
        startActivityForResult(login, REQUEST_CODE_LOGIN);
    }

    public void goToAddObservationActivity() {
        Intent addObservation = new Intent(this, AddObservationActivity.class);
        addObservation.putExtra(OBSERVATION_PATH, observationPath);
        addObservation.putExtra(OBSERVATION, newObservation);
        addObservation.putExtra(SIGNED_USER, signed_user);
        startActivityForResult(addObservation, REQUEST_CODE_ADD_OBSERVATION);
        overridePendingTransition(R.anim.slide_up, R.anim.stay);
    }

    public void goToProjectActivity(Project p) {
        Intent project = new Intent(this, ProjectActivity.class);
        project.putExtra(PROJECT, p);
        project.putExtra(SIGNED_USER, signed_user);
        startActivityForResult(project, REQUEST_CODE_PROJECT_ACTIVITY);
        overridePendingTransition(R.anim.slide_up, R.anim.stay);
    }
    public void goToObservationActivity() {
        Intent observation = new Intent(this, ObservationActivity.class);
        observation.putExtra(SIGNED_USER, signed_user);
        observation.putParcelableArrayListExtra(OBSERVATIONS, observations);
        observation.putExtra(OBSERVERS, (Serializable)observers);
        if (previewSelectedObservation != null) {
            observation.putExtra(OBSERVATION, previewSelectedObservation);
        }
        startActivityForResult(observation, REQUEST_CODE_OBSERVATION_ACTIVITY);
        overridePendingTransition(R.anim.slide_up, R.anim.stay);
    }

    public List<Uri> getRecentImagesUris() {
        Uri uri;
        Cursor cursor;
        List<Uri> listOfAllImages = Lists.newArrayList();
        uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = new String[] { MediaStore.Images.ImageColumns.DATA, MediaStore.Images.ImageColumns.DATE_TAKEN };
        cursor = this.getContentResolver().query(uri, projection, null, null, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");
        if (cursor != null) {
            cursor.moveToFirst();
            try {
                do {
                    listOfAllImages.add(FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider",
                            new File(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)))));
                } while (cursor.moveToNext() && listOfAllImages.size() < 8);
            } catch (CursorIndexOutOfBoundsException ex) {
                Timber.e(ex, "Could not read data from MediaStore, image gallery may be empty");
            } finally {
                cursor.close();
            }
        }else {
            Timber.e("Could not get MediaStore content!");
        }
        return listOfAllImages;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case (REQUEST_CODE_JOIN): {
                if (resultCode == Activity.RESULT_OK) {
                    if (GUEST.equals(data.getExtras().getString(JOIN))) {
                        drawer.openDrawer(GravityCompat.START);
                    } else if (LAUNCH.equals(data.getExtras().getString(JOIN))) {
                        goToLaunchFragment();
                    } else if (LOGIN.equals(data.getExtras().getString(JOIN))) {
                        signed_user = data.getParcelableExtra(NEW_USER);
                        logout.setVisible(true);
                        this.supportInvalidateOptionsMenu();

                        if (signed_user.avatar != null) {
                            Picasso.with(this).load(Strings.emptyToNull(signed_user.avatar))
                                    .placeholder(R.drawable.default_avatar)
                                    .transform(mAvatarTransform).fit().into(nav_iv);
                        }

                        display_name.setText(signed_user.displayName);
                        mFirebase.child(Site.NODE_NAME).child(signed_user.affiliation).child(NAME).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                affiliation.setText((String)snapshot.getValue());
                            }
                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Timber.w("Could not get user's affiliation");
                            }
                        });
                        sign_in.setVisibility(View.GONE);
                        join.setVisibility(View.GONE);
                        display_name.setVisibility(View.VISIBLE);
                        affiliation.setVisibility(View.VISIBLE);
                        goToExploreFragment();
                        drawer.openDrawer(GravityCompat.START);
                    }
                }
                break;
            }
            case(REQUEST_CODE_LOGIN): {
                if (resultCode == Activity.RESULT_OK) {
                    if (data.getStringExtra(LOGIN).equals(JOIN)) {
                        goToJoinActivity();
                    } else if (data.getStringExtra(LOGIN).equals(GUEST)) {
                        drawer.openDrawer(GravityCompat.START);
                    }
                }
                break;
            }
            case(REQUEST_CODE_ADD_OBSERVATION): {
                if(resultCode == Activity.RESULT_OK) {
                    newObservation = data.getParcelableExtra(OBSERVATION);
                    newObservation.userId = signed_user.id;
                    Intent uploadIntent = new Intent(MainActivity.this, UploadService.class);
                    uploadIntent.putExtra(UploadService.EXTRA_OBSERVATION, newObservation);
                    uploadIntent.putExtra(UploadService.EXTRA_URI_PATH, observationPath);
                    startService(uploadIntent);
                    goToExploreFragment();
                }
                break;
            }
            case(REQUEST_CODE_PROJECT_ACTIVITY): {
                if(resultCode == Activity.RESULT_OK) {
                    goToProjectsFragment();
                }
                break;
            }
            case(REQUEST_CODE_OBSERVATION_ACTIVITY): {
                if(resultCode == Activity.RESULT_OK) {
                    previewSelectedObservation = null;
                    goToExploreFragment();
                }
                break;
            }
        }
    }
    public void onUserSignIn(@NonNull Users user) {
        signed_user = user;
        mFirebase.child(Site.NODE_NAME).child(signed_user.affiliation).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                user_home_site = dataSnapshot.getValue(Site.class);
                updateUIUser(signed_user);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, getString(R.string.login_error_message_firebase_read), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void updateUINoUser() {
        Picasso.with(this).load(R.drawable.default_avatar)
                .transform(mAvatarTransform).fit().into(nav_iv);
        logout.setVisible(false);
        display_name.setText(EMPTY);
        affiliation.setText(EMPTY);
        sign_in.setVisibility(View.VISIBLE);
        join.setVisibility(View.VISIBLE);
        display_name.setVisibility(View.GONE);
        affiliation.setVisibility(View.GONE);
    }

    public void updateUIUser(final Users user) {
        Picasso.with(MainActivity.this).load(Strings.emptyToNull(user.avatar))
                .placeholder(R.drawable.default_avatar)
                .transform(mAvatarTransform).fit().into(nav_iv);
        logout.setVisible(true);
        display_name.setText(user.displayName);
        affiliation.setText(user_home_site.name);
        sign_in.setVisibility(View.GONE);
        join.setVisibility(View.GONE);
        display_name.setVisibility(View.VISIBLE);
        affiliation.setVisibility(View.VISIBLE);
        goToExploreFragment();
        drawer.openDrawer(GravityCompat.START);
    }

    public boolean haveNetworkConnection() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }

}