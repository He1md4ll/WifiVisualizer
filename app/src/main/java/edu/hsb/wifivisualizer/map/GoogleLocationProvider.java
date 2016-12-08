package edu.hsb.wifivisualizer.map;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

public class GoogleLocationProvider implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, ResultCallback<LocationSettingsResult> {

    private static final String TAG = GoogleLocationProvider.class.getSimpleName();
    private static final int LOCATION_UPDATE_MIN_TIME = 2 * 1000;
    private static final int LOCATION_UPDATE_MIN_DISTANCE = 1;

    private ILocationListener listener;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;

    public GoogleLocationProvider(Context context) {
        googleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        locationRequest = new LocationRequest();
        locationRequest.setInterval(LOCATION_UPDATE_MIN_TIME);
        locationRequest.setSmallestDisplacement(LOCATION_UPDATE_MIN_DISTANCE);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        final PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(googleApiClient,
                        builder.build());

        result.setResultCallback(this);
    }

    public void startListening(final ILocationListener listener) {
        this.listener = listener;
        googleApiClient.connect();
    }

    public void stopListening() {
        if (googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            googleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        try {
            onLocationChanged(LocationServices.FusedLocationApi.getLastLocation(googleApiClient));
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        } catch (SecurityException e) {
            Log.e(TAG, "Could not acquire phone location.");
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.w(TAG, "Connection to GoolgeApi suspended");
        listener.onLostConnection();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.w(TAG, "Connection failed: " + connectionResult.getErrorMessage());
        listener.onLostConnection();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            listener.onLocationChanged(location);
        }
    }

    @Override
    public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
        final Status status = locationSettingsResult.getStatus();
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                break;
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                listener.onResolutionNeeded(status);
                break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                listener.onProviderUnavailable();
                break;
        }
    }
}
