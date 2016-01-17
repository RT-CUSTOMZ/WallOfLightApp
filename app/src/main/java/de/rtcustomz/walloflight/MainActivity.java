package de.rtcustomz.walloflight;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import de.rtcustomz.walloflight.activities.SettingsActivity;
import de.rtcustomz.walloflight.fragments.DrawingFragment;
import de.rtcustomz.walloflight.fragments.ProcessImageFragment;
import de.rtcustomz.walloflight.fragments.ProcessImageFragment.Mode;
import de.rtcustomz.walloflight.fragments.TabbedFragment;
import de.rtcustomz.walloflight.util.Client;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    public static final int READ_EXTERNAL_STORAGE_REQUEST = 1;

    private SharedPreferences sharedPref;

    Fragment drawingFragment;
    Fragment normalImages;
    Fragment animatedImages;
    Fragment gifImages;
//    Fragment tabbedFragment;

    SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    Log.d("WallOfLightApp", "Preference changed: " + key);
                    updateClientSettings();
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        drawingFragment = DrawingFragment.newInstance();
        normalImages = ProcessImageFragment.newInstance(Mode.NORMAL);
        animatedImages = ProcessImageFragment.newInstance(Mode.ANIMATING);
        gifImages = ProcessImageFragment.newInstance(Mode.GIF);
//        tabbedFragment = TabbedFragment.newInstance();

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

        updateClientSettings();

        loadFragment(normalImages);
        setTitle(getResources().getString(R.string.Images));
        navigationView.setCheckedItem(R.id.nav_images);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        sharedPref.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_paint) {
            loadFragment(drawingFragment);
            setTitle(getResources().getString(R.string.androidPaint));
//        } else if (id == R.id.nav_tabs) {
//            loadFragment(tabbedFragment);
        } else if(id == R.id.nav_images) {
            loadFragment(normalImages);
            setTitle(getResources().getString(R.string.Images));
        } else if(id == R.id.nav_animating) {
            loadFragment(animatedImages);
            setTitle(getResources().getString(R.string.animate));
        } else if(id == R.id.nav_gif) {
            loadFragment(gifImages);
            setTitle(getResources().getString(R.string.gif));
        } else {
            return false;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case READ_EXTERNAL_STORAGE_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    // user is stupid, without permissions we cannot open image, so just close the app
                    finish();
                }
                break;
            }
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.replace(R.id.content_frame, fragment);
        //ft.addToBackStack(null);
        ft.commit();
    }

    private void updateClientSettings() {
        Client.setGamma(Float.valueOf(sharedPref.getString(getString(R.string.pref_key_gamma), getString(R.string.pref_default_gamma))));
        Client.setIP(sharedPref.getString(getString(R.string.pref_key_ip), getString(R.string.pref_default_ip)));
    }
}
