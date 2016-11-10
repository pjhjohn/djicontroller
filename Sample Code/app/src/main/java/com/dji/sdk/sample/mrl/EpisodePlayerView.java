package com.dji.sdk.sample.mrl;

import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.dji.sdk.sample.R;
import com.dji.sdk.sample.common.DJISampleApplication;
import com.dji.sdk.sample.common.Utils;
import com.dji.sdk.sample.mrl.network.api.Api;
import com.dji.sdk.sample.mrl.network.model.Episode;
import com.dji.sdk.sample.mrl.network.model.TrajectoryOptimizationFeedback;
import com.dji.sdk.sample.mrl.network.model.SimulatorLog;
import com.dji.sdk.sample.utils.DJIModuleVerificationUtil;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dji.common.flightcontroller.DJISimulatorInitializationData;
import dji.common.flightcontroller.DJIVirtualStickFlightCoordinateSystem;
import dji.common.flightcontroller.DJIVirtualStickRollPitchControlMode;
import dji.common.flightcontroller.DJIVirtualStickVerticalControlMode;
import dji.common.flightcontroller.DJIVirtualStickYawControlMode;
import dji.sdk.flightcontroller.DJIFlightController;
import dji.thirdparty.retrofit2.Call;
import dji.thirdparty.retrofit2.Callback;
import dji.thirdparty.retrofit2.Response;
import dji.thirdparty.rx.Observable;
import dji.thirdparty.rx.android.schedulers.AndroidSchedulers;
import dji.thirdparty.rx.functions.Action1;
import dji.thirdparty.rx.schedulers.Schedulers;
import timber.log.Timber;

public class EpisodePlayerView extends RelativeLayout {

    private final double SIMULATOR_LATITUDE = 20;
    private final double SIMULATOR_LONGITUDE = 20;
    private final int SIMULATOR_STATE_UPDATE_FREQUENCY = 50; // in HZ with range [2, 150]
    private final int SIMULATOR_NUM_OF_SATELLITES = 10;

    @BindView(R.id.episode_list) protected ListView mEpisodeList;
    @BindView(R.id.btn_initialize_flight_config) protected Button mButtonConfigInitializer;
    @BindView(R.id.btn_finalize_flight_config) protected Button mButtonConfigFinalizer;
    @BindView(R.id.btn_take_off) protected Button mButtonTakeOff;
    @BindView(R.id.btn_auto_landing) protected Button mButtonAutoLanding;

    @BindView(R.id.trajectory_optimization_current_iteration) protected TextView mTrajectoryOptimizationCurrentIteration;
    @BindView(R.id.trajectory_optimization_status) protected TextView mTrajectoryOptimizationStatus;
    @BindView(R.id.trajectory_optimization_stop) protected TextView mButtonTrajectoryOptimizationStop;

    private Unbinder mUnbinder;
    private ArrayList<Episode> mEpisodes;
    private EpisodeAdapter mEpisodeAdapter;
    private SimulatorLog mSimulatorLog;
    private boolean isTrajectoryOptimizationRunning;

    /* Initializers */
    public EpisodePlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initUI(context, attrs);
    }
    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.setControlMode();
    }
    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.restoreOldControlMode();
        mUnbinder.unbind();
    }


    /* ControlMode */
    private boolean mPrevVirtualStickAdvancedModeEnabled;
    private DJIVirtualStickYawControlMode mPrevYawControlMode;
    private DJIVirtualStickRollPitchControlMode mPrevRollPitchControlMode;
    private DJIVirtualStickVerticalControlMode mPrevVerticalControlMode;
    private DJIVirtualStickFlightCoordinateSystem mPrevFlightCoordinateSystem;
    private void setControlMode() {
        // Check FlightController Accessibility
        if (!DJIModuleVerificationUtil.isFlightControllerAvailable()) return;
        DJIFlightController controller = DJISampleApplication.getAircraftInstance().getFlightController();

        // VirtualStickAdvancedModeEnabled
        mPrevVirtualStickAdvancedModeEnabled = controller.getVirtualStickAdvancedModeEnabled();
        controller.setVirtualStickAdvancedModeEnabled(true);

        // HorizontalFlightCoordinateSystem : Body by default
        mPrevFlightCoordinateSystem = controller.getHorizontalCoordinateSystem();
        controller.setHorizontalCoordinateSystem(DJIVirtualStickFlightCoordinateSystem.Body);

        // YawControlMode : AngularVelocity (Palstance) by default
        mPrevYawControlMode = controller.getYawControlMode();
        controller.setYawControlMode(DJIVirtualStickYawControlMode.AngularVelocity);

        // RollPitchControlMode : Angle by default
        mPrevRollPitchControlMode = controller.getRollPitchControlMode();
        controller.setRollPitchControlMode(DJIVirtualStickRollPitchControlMode.Velocity);

        // VerticalControlMode : Velocity by default
        mPrevVerticalControlMode = controller.getVerticalControlMode();
        controller.setVerticalControlMode(DJIVirtualStickVerticalControlMode.Velocity);
    }
    private void restoreOldControlMode() {
        // Check FlightController Accessibility
        if (!DJIModuleVerificationUtil.isFlightControllerAvailable()) return;
        DJIFlightController controller = DJISampleApplication.getAircraftInstance().getFlightController();

        // Restore old control modes
        controller.setVirtualStickAdvancedModeEnabled(mPrevVirtualStickAdvancedModeEnabled);
        controller.setHorizontalCoordinateSystem(mPrevFlightCoordinateSystem);
        controller.setYawControlMode(mPrevYawControlMode);
        controller.setRollPitchControlMode(mPrevRollPitchControlMode);
        controller.setVerticalControlMode(mPrevVerticalControlMode);
    }

    private void checkAndIterateTrajectoryOptimization(Context context, TrajectoryOptimizationFeedback optimization) {
        // Check Client-side Termination
        if(!isTrajectoryOptimizationRunning) {
            mTrajectoryOptimizationStatus.setText("Terminated Trajectory Optimization from client-side");
            mButtonTrajectoryOptimizationStop.setEnabled(false);
            return;
        }

        // Check Server-side Validity
        if(optimization.success) {
            // Check Server-side Termination
            if(optimization.commands.isEmpty()) {
                mTrajectoryOptimizationStatus.setText("Terminated Trajectory Optimization from server-side");
                mButtonTrajectoryOptimizationStop.setEnabled(false);
                return;
            }

            // Update Trajectory Optimization UI
            mTrajectoryOptimizationCurrentIteration.setText(String.format("Iteration#%d", optimization.current_iteration_index));
            mTrajectoryOptimizationStatus.setText(String.format("Executing %d Commands...", optimization.commands.size()));

            // Send VirtualStick Commands to Drone
            optimization.getVirtualStickCommandsObservable().subscribe(EpisodePlayerView.this.sendVirtualStickCommand);

            // Record & Send back to server
            mSimulatorLog.startRecording();
            Observable.just(optimization.id)
                .delay((long) (optimization.commands.get(optimization.commands.size() - 1).t + 1000), TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(episodeId -> {
                    // Stop Recording
                    mSimulatorLog.stopRecording();

                    // Update Trajectory Optimization UI
                    mTrajectoryOptimizationStatus.setText(String.format("Sending %d SimulatorEvents to server...", mSimulatorLog.events.size()));

                    Call<TrajectoryOptimizationFeedback> call = Api.controller().continueTrajectoryOptimization(episodeId, mSimulatorLog);
                    call.enqueue(new Callback<TrajectoryOptimizationFeedback>() {
                        @Override
                        public void onResponse(Call<TrajectoryOptimizationFeedback> call, Response<TrajectoryOptimizationFeedback> response) {
                            checkAndIterateTrajectoryOptimization(context, response.body());
                        }

                        @Override
                        public void onFailure(Call<TrajectoryOptimizationFeedback> call, Throwable throwable) {
                            throwable.printStackTrace();
                            Toast.makeText(context, "Unhandled Server Error : " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                });
        } else {
            Timber.d(optimization.error_message);
            Toast.makeText(context, "Server Error : " + optimization.error_message, Toast.LENGTH_SHORT).show();
        }
    }

    private void initUI(Context context, AttributeSet attrs) {
        /* Inflate & Initialize View */
        View content = LayoutInflater.from(context).inflate(R.layout.view_episode_player, null, false);
        addView(content, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mUnbinder = ButterKnife.bind(this, content);

        /* Initialize Simulator Callback */
        mSimulatorLog = new SimulatorLog();
        DJISampleApplication.getAircraftInstance().getFlightController().getSimulator().setUpdatedSimulatorStateDataCallback(
            djiSimulatorStateData -> mSimulatorLog.add(djiSimulatorStateData)
        );

        /* Initialize Trajectory Optimization Dashboard */
        mButtonTrajectoryOptimizationStop.setOnClickListener(unused -> isTrajectoryOptimizationRunning = false);

        /* Initialize Episode List */
        mEpisodes = new ArrayList<>();
        mEpisodeAdapter = new EpisodeAdapter();
        mEpisodeList.setAdapter(mEpisodeAdapter);
        mEpisodeList.setOnItemClickListener((parent, view, position, id) -> new MaterialDialog.Builder(parent.getContext())
            .title(String.format("Episode #%d", mEpisodes.get(position).id))
            .content("Choose options below")
            .autoDismiss(false)
            .positiveText("Trajectory Optimization")
            .onPositive((dialog, which) -> {
                // Start Trajectory Optimization
                Episode episode = mEpisodes.get(position);

                // Initialize Trajectory Optimization on Server
                mButtonTrajectoryOptimizationStop.setEnabled(true);
                mTrajectoryOptimizationCurrentIteration.setText("Iteration #-");
                mTrajectoryOptimizationStatus.setText("Initializing...");
                Call<TrajectoryOptimizationFeedback> call = Api.controller().initializeTrajectoryOptimization(episode.id);
                call.enqueue(new Callback<TrajectoryOptimizationFeedback>() {
                    @Override
                    public void onResponse(Call<TrajectoryOptimizationFeedback> call, Response<TrajectoryOptimizationFeedback> response) {
                        // Initialize Trajectory Optimization UI to First Iteration
                        mButtonTrajectoryOptimizationStop.setEnabled(true);
                        isTrajectoryOptimizationRunning = true;

                        checkAndIterateTrajectoryOptimization(context, response.body());
                    }

                    @Override
                    public void onFailure(Call<TrajectoryOptimizationFeedback> call, Throwable throwable) {
                        throwable.printStackTrace();
                        Toast.makeText(context, "Failed to initialize Trajectory Optimization : " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
                dialog.dismiss();
            })
            .neutralText("Execute Once")
            .onNeutral((dialog, which) -> {
                // Execute just this episode
                Episode episode = mEpisodes.get(position);

                // Pass command sequence to execute
                episode.getVirtualStickCommandsObservable().subscribe(EpisodePlayerView.this.sendVirtualStickCommand);

                // Last command's t + alpha time
                mSimulatorLog.startRecording();
                Observable.just(episode.id)
                    .delay((long) (episode.commands.get(episode.commands.size() - 1).t + 1000), TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(episodeId -> {
                        mSimulatorLog.stopRecording();
                        Call<Void> call = Api.controller().pushSimulatorLog(episodeId, mSimulatorLog);
                        call.enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                Toast.makeText(context, String.format("Successfully posted %d simulator events", mSimulatorLog.events.size()), Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(Call<Void> call, Throwable throwable) {
                                throwable.printStackTrace();
                                Toast.makeText(context, "Failed to post simulator log : " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                dialog.dismiss();
            })
            .negativeText("Dismiss")
            .onNegative((dialog, which) -> dialog.dismiss())
            .show()
        );

        /* Fetch Episode Data */
        Call<ArrayList<Episode>> call = Api.controller().readEpisodes();
        call.enqueue(new Callback<ArrayList<Episode>>() {
            @Override
            public void onResponse(Call<ArrayList<Episode>> call, Response<ArrayList<Episode>> response) {
                ArrayList<Episode> episodes = response.body();
                mEpisodes.clear();
                mEpisodes.addAll(episodes);
                mEpisodeAdapter.clear();
                mEpisodeAdapter.addAll(episodes);
                mEpisodeAdapter.notifyDataSetChanged();
                Toast.makeText(context, String.format("Successfully loaded %d episodes", episodes.size()), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<ArrayList<Episode>> call, Throwable throwable) {
                throwable.printStackTrace();
                Toast.makeText(context, "Failed to load episodes : " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        mButtonConfigInitializer.setOnClickListener(unused -> new MaterialDialog.Builder(context)
            .title("SimulatorStateUpdateFrequency")
            .content("Update Frequency in Hz with range [2, 150]")
            .inputType(InputType.TYPE_NUMBER_FLAG_DECIMAL)
            .input("Hz with range [2, 150]", String.format("%d", SIMULATOR_STATE_UPDATE_FREQUENCY), (dialog, input) -> {
                // Check FlightController Accessibility
                if (!DJIModuleVerificationUtil.isFlightControllerAvailable()) return;
                DJIFlightController controller = DJISampleApplication.getAircraftInstance().getFlightController();

                controller.enableVirtualStickControlMode(djiError -> Utils.showDialogBasedOnError(getContext(), djiError));
                controller.getSimulator().startSimulator(
                    new DJISimulatorInitializationData(SIMULATOR_LATITUDE, SIMULATOR_LONGITUDE, Integer.parseInt(input.toString()), SIMULATOR_NUM_OF_SATELLITES),
                    djiError -> Utils.showDialogBasedOnError(getContext(), djiError)
                );
            })
            .show()
        );

        mButtonConfigFinalizer.setOnClickListener(unused -> {
            // Check FlightController Accessibility
            if (!DJIModuleVerificationUtil.isFlightControllerAvailable()) return;
            DJIFlightController controller = DJISampleApplication.getAircraftInstance().getFlightController();

            controller.disableVirtualStickControlMode(djiError -> Utils.showDialogBasedOnError(getContext(), djiError));
            controller.getSimulator().stopSimulator(djiError -> Utils.showDialogBasedOnError(getContext(), djiError));
        });

        mButtonTakeOff.setOnClickListener(unused -> {
            // Check FlightController Accessibility
            if (!DJIModuleVerificationUtil.isFlightControllerAvailable()) return;
            DJIFlightController controller = DJISampleApplication.getAircraftInstance().getFlightController();

            controller.takeOff(djiError -> {}); // Ignore status feedback
        });

        mButtonAutoLanding.setOnClickListener(unused -> {
            // Check FlightController Accessibility
            if (!DJIModuleVerificationUtil.isFlightControllerAvailable()) return;
            DJIFlightController controller = DJISampleApplication.getAircraftInstance().getFlightController();

            controller.autoLanding(djiError -> {}); // Ignore status feedback
        });
    }

    private Action1<VirtualStickCommand> sendVirtualStickCommand = (cmd) -> {
        // Check FlightController Accessibility
        if (!DJIModuleVerificationUtil.isFlightControllerAvailable()) return;
        DJIFlightController controller = DJISampleApplication.getAircraftInstance().getFlightController();

        controller.sendVirtualStickFlightControlData(cmd.toDJIVirtualStickFlightControlData(), djiError -> {}); // Ignore status feedback
    };
}
