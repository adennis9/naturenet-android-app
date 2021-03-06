package org.naturenet.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import org.naturenet.data.model.Users;
import org.naturenet.R;

public class LoginActivity extends AppCompatActivity {

    static String FRAGMENT_TAG_LOGIN = "login_fragment";
    static String LOGIN = "login";
    static String JOIN = "join";
    static String SIGNED_USER = "signed_user";
    static String EMAIL = "email";
    static String PASSWORD = "password";

    String signed_user_email, signed_user_password;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        goToLoginFragment();
    }

    public void continueAsSignedUser(Users signed_user) {
        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.putExtra(LOGIN, LOGIN);
        resultIntent.putExtra(SIGNED_USER, signed_user);
        resultIntent.putExtra(EMAIL, signed_user_email);
        resultIntent.putExtra(PASSWORD, signed_user_password);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    public void goToLoginFragment() {
        getFragmentManager().
                beginTransaction().
                replace(R.id.fragment_container, new LoginFragment(), FRAGMENT_TAG_LOGIN).
                addToBackStack(null).
                commit();
    }

    public void goToJoinActivity() {
        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.putExtra(LOGIN, JOIN);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
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