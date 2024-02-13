package com.example.cloud.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;

import com.example.cloud.MapManager;
import com.example.cloud.R;
import com.example.cloud.sensors.SensorFusion;
import com.example.cloud.sensors.SensorTypes;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

/**
 * A simple {@link Fragment} subclass. The recording fragment is displayed while the app is actively
 * saving data, with some UI elements indicating current PDR status.
 *
 * @see HomeFragment the previous fragment in the nav graph.
 * @see CorrectionFragment the next fragment in the nav graph.
 * @see SensorFusion the class containing sensors and recording.
 *
 * @author Mate Stodulka
 * @author Ryan Wiebe
 */
public class RecordingFragment extends Fragment {

    //Button to end PDR recording
    private Button stopButton;
    private Button cancelButton;
    //Button to toggle map type
    private Button mapToggleButton;
    // Button to toggle ability to see indoors
    private Button indoorToggleButton;
    // Button to see next floor in building
    private Button floorUpButton;
    // Button to see previous floor in building
    private Button floorDownButton;
    // Button to toggle poly outline of available buildings
    private Button polyBuildingButton;
    //Recording icon to show user recording is in progress
    private ImageView recIcon;
    //Compass icon to show user direction of heading
    private ImageView compassIcon;
    // Elevator icon to show elevator usage
    private ImageView elevatorIcon;
    //Loading bar to show time remaining before recording automatically ends
    private ProgressBar timeRemaining;
    //Text views to display indoor view, long/lat, user position and elevation since beginning of recording
    private TextView positionX;
    private TextView positionY;
    private TextView elevation;
    private TextView distanceTravelled;
    private TextView currentEstLat;
    private TextView currentEstLng;
    private TextView currentGNSSLat;
    private TextView currentGNSSLng;
    private TextView indoorViewText;

    //App settings
    private SharedPreferences settings;
    //Singleton class to collect all sensor data
    private SensorFusion sensorFusion;
    //Timer to end recording
    private CountDownTimer autoStop;
    //?
    private Handler refreshDataHandler;

    //variables to calculate and store data of the trajectory
    private float distance;
    private float previousPosX;
    private float previousPosY;

    // Google maps
    MapManager mapManager; // object used to manage google maps and marker
    private boolean isMapInitialised; // bool to determine whether google map is done initialising
    private boolean isIndoorViewEnabled; // bool to control whether indoor building views are visible
    private boolean isPolyViewEnabled; // bool to control whether poly of available buildings is visible
    private boolean isNormalMap; // bool to control whether satellite or normal map is displayed

    /**
     * Public Constructor for the class.
     * Left empty as not required
     */
    public RecordingFragment() {
        // Required empty public constructor
    }

    /**
     * {@inheritDoc}
     * Gets an instance of the {@link SensorFusion} class, and initialises the context and settings.
     * Creates a handler for periodically updating the displayed data.
     *
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.sensorFusion = SensorFusion.getInstance();
        Context context = getActivity();
        this.settings = PreferenceManager.getDefaultSharedPreferences(context);
        this.refreshDataHandler = new Handler();

        isMapInitialised = false; // set map initialised to false until map is ready
        isIndoorViewEnabled = false; // set indoor view to disabled until inside a boundary
        isPolyViewEnabled = false; // disable polyViewButton until map is ready
        isNormalMap = false; // disable map view changes until map is ready
    }

    /**
     * {@inheritDoc}
     * Set title in action bar to "Recording". Initialise {@link SupportMapFragment} with google map.
     * Once ready, a {@link MapManager} object will be initiliazed with the created {@link GoogleMap} object.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_recording, container, false);
        // Inflate the layout for this fragment
        ((AppCompatActivity)getActivity()).getSupportActionBar().hide();
        getActivity().setTitle("Recording...");

        // Initialize map fragment
        SupportMapFragment supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.recordingMap);

        supportMapFragment.getMapAsync(new OnMapReadyCallback() {
            /**
             * {@inheritDoc}
             * Controls to allow scrolling, tilting, rotating and a compass view of the
             * map are enabled. A marker is added to the map with the start position and a marker
             * drag listener is generated to detect when the marker has moved to obtain the new
             * location.
             */
            @Override
            public void onMapReady(GoogleMap mMap) {
                // get initial coordinate position
                float[] startPosition = sensorFusion.getGNSSLatitude(true); // set to true to get start position

                // get drawable "navigation" vector image for map marker use
                Drawable markerDrawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_baseline_navigation_24);

                // initialise mapManager to control GoogleMap obj
                mapManager = new MapManager(mMap, startPosition[0], startPosition[1],
                                markerDrawable,
                                ContextCompat.getColor(getContext(), R.color.pastelBlue),
                                ContextCompat.getColor(getContext(), R.color.goldYellow),
                                ContextCompat.getColor(getContext(), R.color.lightLogoBlue));

                isMapInitialised = true;
            }
        });

        return rootView;
    }

    /**
     * {@inheritDoc}
     * Text Views and Icons initialised to display the current PDR to the user. A Button onClick
     * listener is enabled to detect when to go to next fragment and allow the user to correct PDR.
     * A runnable thread is called to update the UI every 0.5 seconds.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set autoStop to null for repeat recordings
        this.autoStop = null;

        //Initialise UI components
        this.positionX = getView().findViewById(R.id.currentXPos);
        this.positionY = getView().findViewById(R.id.currentYPos);
        this.elevation = getView().findViewById(R.id.currentElevation);
        this.distanceTravelled = getView().findViewById(R.id.currentDistanceTraveled);
        this.compassIcon = getView().findViewById(R.id.compass);
        this.elevatorIcon = getView().findViewById(R.id.elevatorImage);
        this.currentEstLat = getView().findViewById(R.id.currentEstLat);
        this.currentEstLng = getView().findViewById(R.id.currentEstLng);
        this.currentGNSSLat = getView().findViewById(R.id.currentGNSSLat);
        this.currentGNSSLng = getView().findViewById(R.id.currentGNSSLng);
        this.indoorViewText = getView().findViewById(R.id.indoorViewText);

        //Set default text of TextViews to 0
        this.positionX.setText(getString(R.string.x, "0"));
        this.positionY.setText(getString(R.string.y, "0"));
        this.positionY.setText(getString(R.string.elevation, "0"));
        this.distanceTravelled.setText(getString(R.string.meter, "0"));
        this.currentEstLat.setText(getString(R.string.currentEstLat, 0.0f));
        this.currentEstLng.setText(getString(R.string.currentEstLat, 0.0f));
        this.currentGNSSLat.setText(getString(R.string.currentEstLat, 0.0f));
        this.currentGNSSLng.setText(getString(R.string.currentEstLat, 0.0f));

        // Set default text of indoorViewMapText Textview to Unknown
        this.indoorViewText.setText(getString(R.string.indoorViewText, "Unknown"));
        this.indoorViewText.setVisibility(View.GONE); // hide indoorViewText until indoorView enabled

        //Reset variables to 0
        this.distance = 0f;
        this.previousPosX = 0f;
        this.previousPosY = 0f;

        // Stop button to save trajectory and move to corrections
        this.stopButton = getView().findViewById(R.id.stopButton);
        this.stopButton.setOnClickListener(new View.OnClickListener() {
            /**
             * {@inheritDoc}
             * OnClick listener for button to go to next fragment.
             * When button clicked the PDR recording is stopped and the {@link CorrectionFragment} is loaded.
             */
            @Override
            public void onClick(View view) {
                if(autoStop != null) autoStop.cancel();
                sensorFusion.stopRecording();
                NavDirections action = RecordingFragmentDirections.actionRecordingFragmentToCorrectionFragment();
                Navigation.findNavController(view).navigate(action);
            }
        });

        // Cancel button to discard trajectory and return to Home
        this.cancelButton = getView().findViewById(R.id.cancelButton);
        this.cancelButton.setOnClickListener(new View.OnClickListener() {
            /**
             * {@inheritDoc}
             * OnClick listener for button to go to home fragment.
             * When button clicked the PDR recording is stopped and the {@link HomeFragment} is loaded.
             * The trajectory is not saved.
             */
            @Override
            public void onClick(View view) {
                sensorFusion.stopRecording();
                NavDirections action = RecordingFragmentDirections.actionRecordingFragmentToHomeFragment();
                Navigation.findNavController(view).navigate(action);
                if(autoStop != null) autoStop.cancel();
            }
        });

        // mapToggleButton to toggle map view between normal and satellite
        this.mapToggleButton = getView().findViewById(R.id.mapToggleButton);
        this.mapToggleButton.setOnClickListener(new View.OnClickListener() {
            /**
             * {@inheritDoc}
             * OnClick listener for button to swap between normal and satellite map modes.
             * When clicked, the button's state is updated and {@link SupportMapFragment} is updated
             * using {@link MapManager}.
             */
            @Override
            public void onClick(View view) {
                if (isMapInitialised) {
                    if (isNormalMap) {
                        mapManager.showSatelliteMap();
                        isNormalMap = false;
                    }
                    else {
                        mapManager.showNormalMap();
                        isNormalMap = true;
                    }
                };
            }
        });

        // buildingToggleButton to toggle whether to use indoor buildings
        this.indoorToggleButton = getView().findViewById(R.id.indoorToggleButton);
        this.indoorToggleButton.setVisibility(View.GONE); // keep button invisible until an indoor view is available
        this.indoorToggleButton.setOnClickListener(new View.OnClickListener() {
            /**
             * {@inheritDoc}
             * OnClick listener for button to show/hide indoor views
             * When clicked, the button's state is updated, the {@link TextView} for the current indoor view is
             * displayed/hidden, the floor up/down buttons are displayed/hidden and the current indoor view is displayed/hidden
             * on the recording {@link SupportMapFragment} using {@link com.google.android.gms.maps.model.GroundOverlay}
             * objects inside {@link MapManager}.
             */
            @Override
            public void onClick(View view) {
                if (isMapInitialised) {
                    if (isIndoorViewEnabled) { // hide indoor view as already shown
                        mapManager.hideIndoorView();
                        isIndoorViewEnabled = false;

                        // hide floor up/down buttons and text as indoor view disabled
                        floorUpButton.setVisibility(View.GONE);
                        floorDownButton.setVisibility(View.GONE);
                        indoorViewText.setVisibility(View.GONE);
                    }
                    else { // show indoor view as already hidden
                        mapManager.showIndoorView();
                        isIndoorViewEnabled = true;

                        // show floor view buttons and text as indoor view enabled
                        updateIndoorViewButtons();
                        updateIndoorViewText();
                    }
                };
            }
        });

        // floorUpButton to see next floor in building
        this.floorUpButton = getView().findViewById(R.id.floorUpButton);
        this.floorUpButton.setVisibility(View.GONE); // keep button invisible until an indoor view is available
        this.floorUpButton.setOnClickListener(new View.OnClickListener() {
            /**
             * {@inheritDoc}
             * OnClick listener for button to show indoor view above the current indoor view.
             * When clicked, the next indoor is displayed on the recording {@link SupportMapFragment}
             * using {@link com.google.android.gms.maps.model.GroundOverlay} objects inside {@link MapManager}.
             * This button is only displayed when a view above is available.
             */
            @Override
            public void onClick(View view) {
                if (isMapInitialised) {
                    mapManager.showNextIndoorView();
                    // viewed floor changed thus need to update floor view buttons
                    updateIndoorViewButtons();
                    updateIndoorViewText();
                };
            }
        });

        // floorDownButton to see next floor in building
        this.floorDownButton = getView().findViewById(R.id.floorDownButton);
        this.floorDownButton.setVisibility(View.GONE); // keep button invisible until an indoor view is available
        this.floorDownButton.setOnClickListener(new View.OnClickListener() {
            /**
             * {@inheritDoc}
             * OnClick listener for button to show indoor view above the below indoor view.
             * When clicked, the previous indoor is displayed on the recording {@link SupportMapFragment}
             * using {@link com.google.android.gms.maps.model.GroundOverlay} objects inside {@link MapManager}.
             * This button is only displayed when a view below is available.
             */
            @Override
            public void onClick(View view) {
                if (isMapInitialised) {
                    mapManager.showPrevIndoorView();
                    // viewed floor changed thus need to update floor view buttons
                    updateIndoorViewButtons();
                };
            }
        });

        // polyBuildingButton to toggle polygon outline of available buildings
        this.polyBuildingButton = getView().findViewById(R.id.polyBuildingButton);
        this.polyBuildingButton.setOnClickListener(new View.OnClickListener() {
            /**
             * {@inheritDoc}
             * OnClick listener for button to show/hide polygons outlining buidlings.
             * When clicked, the button's state is updated and the polygons for each building are shown/hidden
             * on the recording {@link SupportMapFragment} using {@link com.google.android.gms.maps.model.GroundOverlay}
             * objects inside {@link MapManager}.
             */
            @Override
            public void onClick(View view) {
                if (isMapInitialised) {
                    if (isPolyViewEnabled){
                        mapManager.hideBuildingPolygons();
                        isPolyViewEnabled = false;
                    }
                    else {
                        mapManager.showBuildingPolygons();
                        isPolyViewEnabled = true;
                    }
                };
            }
        });

        // Display the progress of the recording when a max record length is set
        this.timeRemaining = getView().findViewById(R.id.timeRemainingBar);

        // Display a blinking red dot to show recording is in progress
        blinkingRecording();

        // Check if there is manually set time limit:
        if(this.settings.getBoolean("split_trajectory", false)) {
            // If that time limit has been reached:
            long limit = this.settings.getInt("split_duration", 30) * 60000L;
            // Set progress bar
            this.timeRemaining.setMax((int) (limit/1000));
            this.timeRemaining.setScaleY(3f);

            // Create a CountDownTimer object to adhere to the time limit
            this.autoStop = new CountDownTimer(limit, 1000) {
                /**
                 * {@inheritDoc}
                 * Increment the progress bar to display progress and remaining time. Update the
                 * observed PDR values, and animate icons based on the data.
                 */
                @Override
                public void onTick(long l) {
                    // increment progress bar
                    timeRemaining.incrementProgressBy(1);
                    // Get new position
                    float[] pdrValues = sensorFusion.getSensorValueMap().get(SensorTypes.PDR);
                    positionX.setText(getString(R.string.x, String.format("%.1f", pdrValues[0])));
                    positionY.setText(getString(R.string.y, String.format("%.1f", pdrValues[1])));
                    // Calculate distance travelled
                    distance += Math.sqrt(Math.pow(pdrValues[0] - previousPosX, 2) + Math.pow(pdrValues[1] - previousPosY, 2));
                    distanceTravelled.setText(getString(R.string.meter, String.format("%.2f", distance)));
                    previousPosX = pdrValues[0];
                    previousPosY = pdrValues[1];
                    // Display elevation and elevator icon when necessary
                    float elevationVal = sensorFusion.getElevation();
                    elevation.setText(getString(R.string.elevation, String.format("%.1f", elevationVal)));
                    if(sensorFusion.getElevator()) elevatorIcon.setVisibility(View.VISIBLE);
                    else elevatorIcon.setVisibility(View.GONE);

                    //Rotate compass image to heading angle
                    compassIcon.setRotation((float) -Math.toDegrees(sensorFusion.passOrientation()));
                }

                /**
                 * {@inheritDoc}
                 * Finish recording and move to the correction fragment.
                 *
                 * @see CorrectionFragment
                 */
                @Override
                public void onFinish() {
                    // Timer done, move to next fragment automatically - will stop recording
                    sensorFusion.stopRecording();
                    NavDirections action = RecordingFragmentDirections.actionRecordingFragmentToCorrectionFragment();
                    Navigation.findNavController(view).navigate(action);
                }
            }.start();
        }
        else {
            // No time limit - use a repeating task to refresh UI.
            this.refreshDataHandler.post(refreshDataTask);
        }
    }

    /**
     * Runnable task used to refresh UI elements with live data.
     * Has to be run through a Handler object to be able to alter UI elements
     */
    private final Runnable refreshDataTask = new Runnable() {
        @Override
        public void run() {
            // Get new position
            float[] pdrValues = sensorFusion.getSensorValueMap().get(SensorTypes.PDR);
            positionX.setText(getString(R.string.x, String.format("%.1f", pdrValues[0])));
            positionY.setText(getString(R.string.y, String.format("%.1f", pdrValues[1])));
            // Calculate distance travelled
            float xDist = pdrValues[0] - previousPosX;
            float yDist = pdrValues[1] - previousPosY;
            distance += Math.sqrt(Math.pow(xDist, 2) + Math.pow(yDist, 2));
            distanceTravelled.setText(getString(R.string.meter, String.format("%.2f", distance)));
            previousPosX = pdrValues[0];
            previousPosY = pdrValues[1];
            // Display elevation and elevator icon when necessary
            float elevationVal = sensorFusion.getElevation();
            elevation.setText(getString(R.string.elevation, String.format("%.1f", elevationVal)));
            if(sensorFusion.getElevator()) elevatorIcon.setVisibility(View.VISIBLE);
            else elevatorIcon.setVisibility(View.GONE);

            //Rotate compass image to heading angle
            float compassRotation = (float) Math.toDegrees(sensorFusion.passOrientation());
            compassIcon.setRotation(compassRotation);

            // update current GNSS text
            float[] gnssLatLong = sensorFusion.getGNSSLatitude(false);
            currentGNSSLat.setText(getString(R.string.currentGNSSLat, gnssLatLong[0]));
            currentGNSSLng.setText(getString(R.string.currentGNSSLng, gnssLatLong[1]));

            // only update is map is initialised
            if (isMapInitialised) {
                mapManager.updateMarker(yDist, xDist, compassRotation); // update map marker using calculated movement distances
                mapManager.updateViewableIndoorViews(); // update currently available indoor views
                // update estimated GNSS text
                float[] estLatLong = mapManager.getEstimatedLatLng();
                currentEstLat.setText(getString(R.string.currentEstLat, estLatLong[0]));
                currentEstLng.setText(getString(R.string.currentEstLng, estLatLong[1]));
                if (!mapManager.isIndoorViewViewable()) {
                    // hide indoor view button and indoor view if no indoor views available
                    mapManager.hideIndoorView();
                    isIndoorViewEnabled = false;
                    indoorToggleButton.setVisibility(View.GONE);
                    floorUpButton.setVisibility(View.GONE);
                    floorDownButton.setVisibility(View.GONE);
                }
                else {
                    // show indoor view button as an indoor view as indoor view available
                    indoorToggleButton.setVisibility(View.VISIBLE);
                    // update floor view buttons and text when indoor view has been enabled
                    if (isIndoorViewEnabled) {
                        updateIndoorViewButtons();
                        updateIndoorViewText();
                    }
                }
            }

            // Loop the task again to keep refreshing the data
            refreshDataHandler.postDelayed(refreshDataTask, 500);
        }
    };


    /**
     * Function to manage state of floor view up and floor down buttons. Up/down buttons should only be visible
     * when there is a indoor view above/below the current indoor view.
     */
    private void updateIndoorViewButtons() {
        // show/hide floorUpButton as floors above based on current view availability
        if (mapManager.isNextIndoorView()) {
            floorUpButton.setVisibility(View.VISIBLE);
        } else {
            floorUpButton.setVisibility(View.GONE);
        }
        // show/hide floorDownButton as floors below based on current view availability
        if (mapManager.isPrevIndoorView()) {
            floorDownButton.setVisibility(View.VISIBLE);
        } else {
            floorDownButton.setVisibility(View.GONE);
        }
    }
    /**
     * Function which updates the indoor view {@link TextView} with the ID associated with the currenlty
     * visible indoor view.
     */
    private void updateIndoorViewText() {
        this.indoorViewText.setVisibility(View.VISIBLE);
        this.indoorViewText.setText(getString(R.string.indoorViewText, mapManager.getIndoorViewID()));
    }

    /**
     * Displays a blinking red dot to signify an ongoing recording.
     *
     * @see Animation for makin the red dot blink.
     */
    private void blinkingRecording() {
        //Initialise Image View
        this.recIcon = getView().findViewById(R.id.redDot);
        //Configure blinking animation
        Animation blinking_rec = new AlphaAnimation(1, 0);
        blinking_rec.setDuration(800);
        blinking_rec.setInterpolator(new LinearInterpolator());
        blinking_rec.setRepeatCount(Animation.INFINITE);
        blinking_rec.setRepeatMode(Animation.REVERSE);
        recIcon.startAnimation(blinking_rec);
    }

    /**
     * {@inheritDoc}
     * Stops ongoing refresh task, but not the countdown timer which stops automatically
     */
    @Override
    public void onPause() {
        refreshDataHandler.removeCallbacks(refreshDataTask);
        super.onPause();
    }

    /**
     * {@inheritDoc}
     * Restarts UI refreshing task when no countdown task is in progress
     */
    @Override
    public void onResume() {
        if(!this.settings.getBoolean("split_trajectory", false)) {
            refreshDataHandler.postDelayed(refreshDataTask, 500);
        }
        super.onResume();
    }
}