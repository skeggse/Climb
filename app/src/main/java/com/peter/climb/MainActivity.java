package com.peter.climb;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SearchView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.StringRequest;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.peter.Climb.Msgs;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {

    static final int FIND_GYM_CODE = 1;
    private static final String GYM_ID_PREF_KEY = "gym_id_pref_key";
    static final String FIND_GYM_KEY = "GYM_KEY";
    public static final String PREFS_NAME = "MyPrefsFile";
    private int notification_id = 1;
    private AppState app_state;

    private ImageView large_icon_image_view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        app_state = ((MyApplication) getApplicationContext()).getState();

        // look up the currently selected gym
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        app_state.current_gym_id = settings.getInt(GYM_ID_PREF_KEY, -1);

        large_icon_image_view = (ImageView) findViewById(R.id.large_icon_image_view);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(this);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // make HTTP request to server for all the gym data
        fetchGymData();
    }

    private void fetchGymData() {
        String url = "http://www.google.com";
        StringRequest gym_data_request = new StringRequest(url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            Msgs.Gyms gyms = Msgs.Gyms.parseFrom(ByteString.copyFromUtf8(response));
                        } catch (InvalidProtocolBufferException e) {
                            // could not parse message. 100 % Sad Panda
                        }

                        // mock of what the server would return
                        Msgs.Gyms gyms = Msgs.Gyms.newBuilder().addGyms(
                                Msgs.Gym.newBuilder().setName("Ascend PGH").addWalls(
                                        Msgs.Wall.newBuilder().setPolygon(
                                                Msgs.Polygon.newBuilder().addPoints(
                                                        Msgs.Point2D.newBuilder().setX(0).setY(0)
                                                ).addPoints(
                                                        Msgs.Point2D.newBuilder().setX(10).setY(0)
                                                ).addPoints(
                                                        Msgs.Point2D.newBuilder().setX(10).setY(10)
                                                ).addPoints(
                                                        Msgs.Point2D.newBuilder().setX(0).setY(10)
                                                )
                                        ).addRoutes(
                                                Msgs.Route.newBuilder().setName("Lappnor Project").setPosition(
                                                        Msgs.Point2D.newBuilder().setX(0).setY(0)
                                                ).setGrade(17)
                                        ).addRoutes(
                                                Msgs.Route.newBuilder().setName("La Dura Dura").setPosition(
                                                        Msgs.Point2D.newBuilder().setX(1).setY(0)
                                                ).setGrade(16)
                                        ).setName("The Dawn Wall")
                                ).setSmallIconUrl(
                                        "https://www.ascendpgh.com/sites/default/files/logo.png"
                                ).setLargeIconUrl(
                                        "https://www.ascendpgh.com/sites/all/themes/ascend_foundation/images/header-images/02-Header-Visiting-Ascend.jpg"
                                ).setMapUrl(
                                        "https://www.guthrie.org/sites/default/files/TCH_AreaMap.gif"
                                ).setId(1)
                        ).build();

                        handleGymData(gyms);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });
        RequestorSingleton.getInstance(this).addToRequestQueue(gym_data_request);
    }

    private void handleGymData(Msgs.Gyms gyms) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        app_state.current_gym_id = 1;

        if (app_state.current_gym_id == -1) {
            // they have no gym selected...
        } else {
            for (Msgs.Gym gym : gyms.getGymsList()) {
                if (gym.getId() == app_state.current_gym_id) {
                    // set the logo
                    String url = gym.getLargeIconUrl();
                    ImageLoader.ImageListener listener = ImageLoader.getImageListener(
                            large_icon_image_view,
                            0,
                            R.drawable.ic_error_black_24dp);
                    RequestorSingleton.getInstance(getApplicationContext()).getImageLoader().get(url, listener);

                    // mark this as the current gym
                    app_state.current_gym = gym;
                }
            }
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // add an item to the menu?
        return true;
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the options menu from XML
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.toolbar_menu, menu);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.gym_search_view).getActionView();
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default

        return true;
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
    protected void onStop() {
        super.onStop();

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(GYM_ID_PREF_KEY, app_state.current_gym_id);
        editor.apply();
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.add_new_gym) {
            // open up some gym selector to search for a gym
            Intent add_gym_intent = new Intent(this, FindGymActivity.class);
            startActivityForResult(add_gym_intent, FIND_GYM_CODE);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onClick(View v) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_menu_camera)
                        .setContentTitle("My notification")
                        .setContentText("Hello World!");

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MapActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MapActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // mId allows you to update the notification later on.
        Notification notification = mBuilder.build();
        notification.flags = Notification.FLAG_ONGOING_EVENT;
//        mNotificationManager.notify(notification_id, notification);

        Intent start_session_intent = new Intent(this, MapActivity.class);
        startActivity(start_session_intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FIND_GYM_CODE) {
            if (resultCode == RESULT_OK) {
                Bundle extras = data.getExtras();
                int gym_position = extras.getInt(FIND_GYM_KEY);
                app_state.current_gym = app_state.gyms.getGyms(gym_position);
                app_state.current_gym_id = app_state.current_gym.getId();
            }
        }
    }
}
