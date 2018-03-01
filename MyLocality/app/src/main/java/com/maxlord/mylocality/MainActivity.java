package com.maxlord.mylocality;

// android app stuff

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
//location stuff
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
//import android.support.v7.widget.Toolbar;
import android.text.LoginFilter;
import android.util.Log;
//layout stuff
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
//http request stuff
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
//google services stuff
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
//firebase auth stuff
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
//spotify stuff
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;
//java objects
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//Todo: make asset folder and test spotify flow
//Todo: spotify account must only work for one user
//Todo: Google places integration
//Todo: Eventful integration
//Todo: Weather integration


public class MainActivity extends AppCompatActivity implements
        SpotifyPlayer.NotificationCallback, ConnectionStateCallback, com.google.android.gms.location.LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener { //spotify and google interfaces

    //spotify objects
    //spotify clientid
    private static final String CLIENT_ID = "5eea0338e20a487d844c55d357b4e684";
    //redirect for spotify connection
    private static final String REDIRECT_URI = "mylocality-auth.com://callback";
    //private static final int MY_PERMISSIONS_REQUEST_LOCATION = 2109;
    //spotify player
    private Player mPlayer;
    // Request code that will be used to verify if the result comes from correct activity
    // Can be any integer
    private static final int REQUEST_CODE = 1337;

    //layout objects for firebase user profile operations
    private Button btnChangeEmail, btnChangePassword, btnSendResetEmail, btnRemoveUser,
            changeEmail, changePassword, sendEmail, remove, signOut, spotifyauth;
    private EditText oldEmail, newEmail, password, newPassword;
    private ProgressBar progressBar;
    //firebase objects
    private FirebaseAuth.AuthStateListener authListener;
    private FirebaseAuth auth;
    //connection to google services
    private GoogleApiClient mLocationClient;
    //location of phone
    private Location mLastLocation;
    private String mCity;
    private String mPlaylist;
    //lists of playlist ids and cities with a playlist
    private List<String> ids, locations;
    private List<LatLng> coordinates;
    //map a city to its locationdata object
    private Map<String, LocationData> citymap;
    //map a latlng to its locationdata object
    private Map<LatLng, LocationData> latlngmap;
    //check if spotify user is logged in
    private boolean spotifyLoggedIn;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //load layout
        setContentView(R.layout.activity_main);
        String[] perms = {"android.permission.ACCESS_COURSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION", "android.permission.INTERNET"};
        requestPermissions(perms, 1000);
        try {
            readLocationData();
            Log.i("spotifyFlow", "File read successful");

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("spotifyFlow", "ids and cities not found");
        }
        //if not connected to google api
        if (mLocationClient == null) {
            //connect to google api
            mLocationClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(15000);  //MS
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //mLocationClient = new LocationClient(arg1, arg2 , arg3);
        mLocationClient.connect();
        FusedLocationProviderClient mFuseLocationClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            mFuseLocationClient.getLastLocation().addOnSuccessListener(
                    new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {

                            if(location != null) {
                                Log.i("location", "location not null");
                                mLastLocation = location;

                                LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                                //find closest city to latlng
                                String city = findClosestCity(userLatLng);

                                mCity = city;
                                Log.i("spotifyflow", city);
                                //get playlist id of city
                                String id = getPlaylistFromCity(city);
                                mPlaylist = id;
                                Log.i("spotifyflow", id);
                                //build request
                                String request = "https://api.spotify.com/v1/users/thesoundsofspotify/playlists/" + id + "/tracks";
                                Log.i("spotifyflow", request);


// Instantiate the RequestQueue.
                                RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

// Request a string response from the provided URL.
                                JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET,request,
                                        null, new Response.Listener<JSONObject>() {

                                    @Override
                                    public void onResponse(JSONObject response) {
                                        Log.i("request", response.toString());

                                    }
                                }, new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        VolleyLog.d("request", "Error: " + error.getMessage());
                                    }
                                }) {

                                    /**
                                     * Passing some request headers
                                     */
                                    @Override
                                    public Map<String, String> getHeaders() throws AuthFailureError {
                                        HashMap<String, String> headers = new HashMap<String, String>();
                                        headers.put("Accept", "application/json");
                                        headers.put("Authorization", "Bearer BQBfIp1g9DwRJxJ4PMavFmwn7Lcf2wluuHDKUieRx9WAUEfB__dCH2F_yFEyXyVjNTdQGyrb9HP76PQIZKzdgSGBCtPWxKzL5rGzueR46Wf865MS4Fcg_Zj0KjDhE22cqkv74R_pqCMGgA4Nx3JIj_ymveOltTrBMwXkpZo");
                                        return headers;
                                    }
                                };
// Add the request to the RequestQueue
                                queue.add(req);
                            }
                            else {
                                Log.i("location", "location null");
                            }
                        }
                    }
            );
        } catch (SecurityException e) {
            e.printStackTrace();
        }


        /*
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.app_name));
        setSupportActionBar(toolbar);
        */
        //get firebase auth instance
        auth = FirebaseAuth.getInstance();

        //get current user
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        //check auth status
        authListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                //if logged out
                if (user == null) {
                    // user auth state is changed - user is null
                    // launch login activity
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                }
            }
        };

        //instantiate firebase profile operation interface
        btnChangeEmail = (Button) findViewById(R.id.change_email_button);
        btnChangePassword = (Button) findViewById(R.id.change_password_button);
        btnSendResetEmail = (Button) findViewById(R.id.sending_pass_reset_button);
        btnRemoveUser = (Button) findViewById(R.id.remove_user_button);
        changeEmail = (Button) findViewById(R.id.changeEmail);
        changePassword = (Button) findViewById(R.id.changePass);
        sendEmail = (Button) findViewById(R.id.send);
        remove = (Button) findViewById(R.id.remove);
        signOut = (Button) findViewById(R.id.sign_out);

        //instantiate button to trigger spotify
        spotifyauth = (Button) findViewById(R.id.spotifyauth);

        //instantiate firebase profile input interface
        oldEmail = (EditText) findViewById(R.id.old_email);
        newEmail = (EditText) findViewById(R.id.new_email);
        password = (EditText) findViewById(R.id.password);
        newPassword = (EditText) findViewById(R.id.newPassword);

        //invisible view and doesn't take up layout space
        oldEmail.setVisibility(View.GONE);
        newEmail.setVisibility(View.GONE);
        password.setVisibility(View.GONE);
        newPassword.setVisibility(View.GONE);
        changeEmail.setVisibility(View.GONE);
        changePassword.setVisibility(View.GONE);
        sendEmail.setVisibility(View.GONE);
        remove.setVisibility(View.GONE);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        //button listeners
        btnChangeEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                oldEmail.setVisibility(View.GONE);
                newEmail.setVisibility(View.VISIBLE);
                password.setVisibility(View.GONE);
                newPassword.setVisibility(View.GONE);
                changeEmail.setVisibility(View.VISIBLE);
                changePassword.setVisibility(View.GONE);
                sendEmail.setVisibility(View.GONE);
                remove.setVisibility(View.GONE);
            }
        });


        changeEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                if (user != null && !newEmail.getText().toString().trim().equals("")) { //user logged in with a valid email
                    user.updateEmail(newEmail.getText().toString().trim())
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(MainActivity.this, "Email address is updated. Please sign in with new email id!", Toast.LENGTH_LONG).show();
                                        signOut();
                                        progressBar.setVisibility(View.GONE);
                                    } else {
                                        Toast.makeText(MainActivity.this, "Failed to update email!", Toast.LENGTH_LONG).show();
                                        progressBar.setVisibility(View.GONE);
                                    }
                                }
                            });
                } else if (newEmail.getText().toString().trim().equals("")) {
                    //if email field blank
                    newEmail.setError("Enter email");
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        btnChangePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                oldEmail.setVisibility(View.GONE);
                newEmail.setVisibility(View.GONE);
                password.setVisibility(View.GONE);
                newPassword.setVisibility(View.VISIBLE);
                changeEmail.setVisibility(View.GONE);
                changePassword.setVisibility(View.VISIBLE);
                sendEmail.setVisibility(View.GONE);
                remove.setVisibility(View.GONE);
            }
        });

        changePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                if (user != null && !newPassword.getText().toString().trim().equals("")) {
                    if (newPassword.getText().toString().trim().length() < 6) {
                        newPassword.setError("Password too short, enter minimum 6 characters");
                        progressBar.setVisibility(View.GONE);
                    } else {
                        user.updatePassword(newPassword.getText().toString().trim())
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(MainActivity.this, "Password is updated, sign in with new password!", Toast.LENGTH_SHORT).show();
                                            signOut();
                                            progressBar.setVisibility(View.GONE);
                                        } else {
                                            Toast.makeText(MainActivity.this, "Failed to update password!", Toast.LENGTH_SHORT).show();
                                            progressBar.setVisibility(View.GONE);
                                        }
                                    }
                                });
                    }
                } else if (newPassword.getText().toString().trim().equals("")) {
                    newPassword.setError("Enter password");
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        btnSendResetEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                oldEmail.setVisibility(View.VISIBLE);
                newEmail.setVisibility(View.GONE);
                password.setVisibility(View.GONE);
                newPassword.setVisibility(View.GONE);
                changeEmail.setVisibility(View.GONE);
                changePassword.setVisibility(View.GONE);
                sendEmail.setVisibility(View.VISIBLE);
                remove.setVisibility(View.GONE);
            }
        });

        sendEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                if (!oldEmail.getText().toString().trim().equals("")) {
                    auth.sendPasswordResetEmail(oldEmail.getText().toString().trim())
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(MainActivity.this, "Reset password email is sent!", Toast.LENGTH_SHORT).show();
                                        progressBar.setVisibility(View.GONE);
                                    } else {
                                        Toast.makeText(MainActivity.this, "Failed to send reset email!", Toast.LENGTH_SHORT).show();
                                        progressBar.setVisibility(View.GONE);
                                    }
                                }
                            });
                } else {
                    oldEmail.setError("Enter email");
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        btnRemoveUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                if (user != null) {
                    user.delete()
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(MainActivity.this, "Your profile is deleted:( Create a account now!", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(MainActivity.this, SignupActivity.class));
                                        finish();
                                        progressBar.setVisibility(View.GONE);
                                    } else {
                                        Toast.makeText(MainActivity.this, "Failed to delete your account!", Toast.LENGTH_SHORT).show();
                                        progressBar.setVisibility(View.GONE);
                                    }
                                }
                            });
                }
            }
        });

        signOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });

        spotifyauth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                spotifyAuthenticate();
            }
        });



        //runTest();

    }

    public void runTest() {
        spotifyAuthenticate();

        /*
        expected output:
        "onActivityResult", "spotify flow engaged"    //spotify login check
        "spotifyFlow", "File read successful" //file read check
        "//latitude", "//longitude"  //at tj
        "spotifyFlow", "city" (should be DC area)
        "spotifyFlow", "https://api.spotify.com/v1/users/thesoundsofspotify/playlists/someID
        JSON of that playlist


         */
    }

    public void spotifyAuthenticate() {
        /*Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://accounts.spotify.com"));
                startActivity(browserIntent);*/


        //authenticate spotify
        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID,
                AuthenticationResponse.Type.TOKEN,
                REDIRECT_URI);
        //spotify permissions
        //thing that asks if the logged in user is you
        builder.setShowDialog(true);
        //permissions
        builder.setScopes(new String[]{"streaming", "playlist-modify-private", "user-library-modify", "user-read-private"}); //permissions
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(MainActivity.this, REQUEST_CODE, request);
    }

    public void onLocationChanged(Location location) {

        Log.d(location.getLatitude() + "", location.getLongitude() + "");
        //create latlng object
        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        //find closest city to latlng
        String city = findClosestCity(userLatLng);
        if(!city.equals(mCity)) { //closest city has changed
            mCity = city;
            Log.i("spotifyflow", city);
            //get playlist id of city
            String id = getPlaylistFromCity(city);
            mPlaylist = id;
            Log.i("spotifyflow", id);
            //build request
            String request = "https://api.spotify.com/v1/users/thesoundsofspotify/playlists/" + id;
            Log.i("spotifyflow", request);


// Instantiate the RequestQueue.
            RequestQueue queue = Volley.newRequestQueue(this);

// Request a string response from the provided URL.
            StringRequest stringRequest = new StringRequest(Request.Method.GET, request,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            // Display the request response
                            Log.i("spotify json", response);
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("spotifyFlow", "Volley error");
                }
            });
// Add the request to the RequestQueue.
            queue.add(stringRequest);
        }



    }



    public String findClosestCity(LatLng current) {
        String closest = "";
        double closestDist = 1000000.0;
        Location curr = new Location("Phone");
        curr.setLatitude(current.latitude);
        curr.setLongitude(current.longitude);
        for ( LatLng key : latlngmap.keySet() ) {
            Location city = new Location("Map");
            city.setLatitude(key.latitude);
            city.setLongitude(key.longitude);
            float distToCity = curr.distanceTo(city);
            if(distToCity < closestDist) {
                closestDist = distToCity;
                closest = latlngmap.get(key).getLocation();
            }
            //System.out.println( key );
        }
        return closest;


    }

    public String getPlaylistFromCity(String city) {
        LocationData loc = citymap.get(city);
        return loc.getID();
    }

    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults) {
        switch(permsRequestCode) {
            case 1000:
                boolean coarseLocationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean fineLocationAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                boolean internet = grantResults[2] == PackageManager.PERMISSION_GRANTED;
                break;
        }
    }

    public void readLocationData() throws IOException {
        ids = new ArrayList<>();
        locations = new ArrayList<>();
        coordinates = new ArrayList<>();
        citymap = new HashMap<>();
        latlngmap = new HashMap<>();

        //playlist ids
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open("PlaylistIDs")))) {
            String line = br.readLine();
            while (line != null) {
                ids.add(line);
                line = br.readLine();
            }
            //String everything = sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //locations

        try (BufferedReader br2 = new BufferedReader(new InputStreamReader(getAssets().open("Cities")))) {
            String line = br2.readLine();

            while (line != null) {
                locations.add(line);
                line = br2.readLine();
            }
            //String everything = sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //coordinates
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open("coordinates.txt")))) {
            String line = br.readLine();
            while (line != null) {
                int comma = line.indexOf(",");
                int end = line.indexOf("#");
                double latitude = Double.parseDouble(line.substring(0, comma));
                double longitude = Double.parseDouble(line.substring(comma+1, end));
                LatLng coordinate = new LatLng(latitude, longitude);
                coordinates.add(coordinate);
                line = br.readLine();
            }
            //String everything = sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.i("readData", ids.get(0));
        Log.i("readData", locations.get(0));
        Log.i("readData", coordinates.get(0).toString());


        for(int citynum = 0; citynum < ids.size(); citynum++) {

            String id = ids.get(citynum);
            String location = locations.get(citynum);
            LatLng coordinate = coordinates.get(citynum);
            double latitude = coordinate.latitude;
            double longitude = coordinate.longitude;

            LocationData loc = new LocationData(id, location, latitude, longitude);

            citymap.put(location, loc);
            latlngmap.put(coordinate, loc);
        }



    }



    //sign out method
    public void signOut() {
        auth.signOut();
    }

    @Override
    protected void onResume() {
        super.onResume();
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onStart() {
        mLocationClient.connect();
        auth.addAuthStateListener(authListener);
        super.onStart();

    }

    @Override
    public void onStop() {
        mLocationClient.disconnect();
        if (authListener != null) {
            auth.removeAuthStateListener(authListener);
        }
        super.onStop();
    }
/*
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Uri uri = intent.getData();
        if (uri != null) {
            AuthenticationResponse response = AuthenticationResponse.fromUri(uri);

            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    // Handle successful response
                    Config playerConfig = new Config(this, response.getAccessToken(), CLIENT_ID);
                    Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver() {
                        @Override
                        public void onInitialized(SpotifyPlayer spotifyPlayer) {
                            mPlayer = spotifyPlayer;
                            mPlayer.addConnectionStateCallback(MainActivity.this);
                            mPlayer.addNotificationCallback(MainActivity.this);
                            Log.d("onNewIntent", "spotify flow");
                            spotifyFlow();
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
                        }
                    });
                    break;

                // Auth flow returned an error
                case ERROR:
                    // Handle error response
                    Log.d("MyLocality", response.getError());
                    break;

                // Most likely auth flow was cancelled
                default:
                    Log.d("MyLocality", "Auth flow cancelled");
                    // Handle other cases
            }
        }
    }
    */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) { //from spotify login
        super.onActivityResult(requestCode, resultCode, intent);
        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {

            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);

            if (response.getType() == AuthenticationResponse.Type.TOKEN) {

                Config playerConfig = new Config(this, response.getAccessToken(), CLIENT_ID);
                Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver() {
                    @Override
                    public void onInitialized(SpotifyPlayer spotifyPlayer) {
                        mPlayer = spotifyPlayer;
                        mPlayer.addConnectionStateCallback(MainActivity.this);
                        mPlayer.addNotificationCallback(MainActivity.this);
                        Log.d("onActivityResult", "spotify flow engaged");
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
                    }
                });
            }
        }
    }

    @Override
    protected void onDestroy() {
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }

    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {
        Log.d("MainActivity", "Playback event received: " + playerEvent.name());
        switch (playerEvent) {
            // Handle event type as necessary
            default:
                break;
        }
    }

    @Override
    public void onPlaybackError(Error error) {
        Log.d("MainActivity", "Playback error received: " + error.name());
        switch (error) {
            // Handle error type as necessary
            default:
                break;
        }
    }

    @Override
    public void onLoggedIn() {
        Log.d("MainActivity", "User logged in");
        spotifyauth.setText(R.string.signoutspotify);
        spotifyLoggedIn = true;
        //mPlayer.playUri(null, "spotify:track:7rdUtXasA973gmrr2Xxh3E", 0, 0); //play spotify track with given URI on login
    }

    @Override
    public void onLoggedOut() {
        Log.d("MainActivity", "User logged out");
    }
/*  SPOTIFY HAS SWITCHED PARAMETER FROM INT TO ERROR
    @Override
    public void onLoginFailed(int i) {
        Log.d("MainActivity", "Login failed");
    }
*/

    @Override
    public void onLoginFailed(Error error) {
        Log.d("MainActivity", "Login failed- error");
    }

    @Override
    public void onTemporaryError() {
        Log.d("MainActivity", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("MainActivity", "Received connection message: " + message);
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(mLocationClient, mLocationRequest, this);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}