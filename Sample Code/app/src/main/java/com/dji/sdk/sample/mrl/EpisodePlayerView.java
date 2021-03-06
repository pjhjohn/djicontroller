package com.dji.sdk.sample.mrl;

import android.content.Context;
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
import com.dji.sdk.sample.mrl.network.api.Api;
import com.dji.sdk.sample.mrl.network.model.Episode;
import com.dji.sdk.sample.mrl.network.model.SimulatorLog;
import com.dji.sdk.sample.mrl.network.model.TrajectoryOptimizationFeedback;
import com.dji.sdk.sample.utils.DJIModuleVerificationUtil;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dji.common.error.DJIError;
import dji.common.flightcontroller.DJISimulatorInitializationData;
import dji.common.flightcontroller.DJIVirtualStickFlightCoordinateSystem;
import dji.common.flightcontroller.DJIVirtualStickRollPitchControlMode;
import dji.common.flightcontroller.DJIVirtualStickVerticalControlMode;
import dji.common.flightcontroller.DJIVirtualStickYawControlMode;
import dji.sdk.flightcontroller.DJIFlightController;
import dji.sdk.flightcontroller.DJISimulator;
import dji.thirdparty.retrofit2.Call;
import dji.thirdparty.retrofit2.Callback;
import dji.thirdparty.retrofit2.Response;
import dji.thirdparty.rx.Observable;
import dji.thirdparty.rx.android.schedulers.AndroidSchedulers;
import dji.thirdparty.rx.functions.Action1;
import dji.thirdparty.rx.schedulers.Schedulers;
import timber.log.Timber;

public class EpisodePlayerView extends RelativeLayout {

    private final double SIM_LATITUDE = 20;
    private final double SIM_LONGITUDE = 20;
    private final int SIM_STATE_UPDATE_FREQUENCY = 25; // in HZ with range [2, 150]
    private final int SIM_SATELLITES = 10;

    @BindView(R.id.episode_list) protected ListView mEpisodeList;
    @BindView(R.id.btn_initialize_flight_config) protected Button mButtonConfigInitializer;
    @BindView(R.id.btn_finalize_flight_config) protected Button mButtonConfigFinalizer;
    @BindView(R.id.btn_take_off) protected Button mButtonTakeOff;
    @BindView(R.id.btn_auto_landing) protected Button mButtonAutoLanding;

    @BindView(R.id.simulator_feedback) protected TextView mSimulatorFeedback;
    @BindView(R.id.trajectory_optimization_current_iteration) protected TextView mTrajectoryOptimizationCurrentIteration;
    @BindView(R.id.trajectory_optimization_status) protected TextView mTrajectoryOptimizationStatus;
    @BindView(R.id.trajectory_optimization_stop) protected TextView mButtonTrajectoryOptimizationStop;

    private Unbinder mUnbinder;
    private ArrayList<Episode> mEpisodes;
    private EpisodeAdapter mEpisodeAdapter;
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

    private void pushSimulatorLog(Episode episode) {
        // Send Command to VirtualStick
        episode.getVirtualStickCommandsObservable().subscribe(EpisodePlayerView.this.sendVirtualStickCommand);

        // Record & Send back to server
        SimulatorLog.getInstance().startRecording(episode.timestep, episode.commands.size())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(logSize -> {
                Call<Void> call = Api.controller().pushSimulatorLog(episode.id, SimulatorLog.getInstance());
                call.enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        trace(String.format("Successfully posted %d simulator events", logSize));
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable throwable) {
                        trace(throwable, "Failed to post simulator log");
                    }
                });
            });
    }
    private void initTrajectoryOptimization(Episode episode) {
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
                continueIterateTrajectoryOptimization(response.body());
            }

            @Override
            public void onFailure(Call<TrajectoryOptimizationFeedback> call, Throwable throwable) {
                trace(throwable, "Failed to initialize Trajectory Optimization");
            }
        });
    }
    private void continueIterateTrajectoryOptimization(TrajectoryOptimizationFeedback optimization) {
        // Check Client-side Termination
        if(!isTrajectoryOptimizationRunning) {
            mTrajectoryOptimizationStatus.setText("Terminated from client-side");
            mButtonTrajectoryOptimizationStop.setEnabled(false);
            return;
        }

        // Check Server-side Validity
        if(optimization.success) {
            // Check Server-side Termination : Empty commands are considered as server-side termination
            if(optimization.commands.isEmpty()) {
                mTrajectoryOptimizationStatus.setText("Terminated from server-side");
                mButtonTrajectoryOptimizationStop.setEnabled(false);
                return;
            }

            // Update Trajectory Optimization UI
            mTrajectoryOptimizationCurrentIteration.setText(String.format("Iteration#%d", optimization.current_iteration_index));
            mTrajectoryOptimizationStatus.setText(String.format("Executing %d Commands...", optimization.commands.size()));

            // Send Command to VirtualStick
            optimization.getVirtualStickCommandsObservable().subscribe(EpisodePlayerView.this.sendVirtualStickCommand);

            // Record & Send back to server
            SimulatorLog.getInstance().startRecording(optimization.timestep, optimization.commands.size())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(logSize -> {
                    // Update Trajectory Optimization UI
                    mTrajectoryOptimizationStatus.setText(String.format("Sending %d SimulatorEvents...", logSize));

                    Call<TrajectoryOptimizationFeedback> call = Api.controller().continueTrajectoryOptimization(optimization.id, SimulatorLog.getInstance());
                    call.enqueue(new Callback<TrajectoryOptimizationFeedback>() {
                        @Override
                        public void onResponse(Call<TrajectoryOptimizationFeedback> call, Response<TrajectoryOptimizationFeedback> response) {
                            Observable.just(null).delay(5, TimeUnit.SECONDS).observeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(unused -> continueIterateTrajectoryOptimization(response.body()));
                        }

                        @Override
                        public void onFailure(Call<TrajectoryOptimizationFeedback> call, Throwable throwable) {
                            trace(throwable, "Unhandled Server Error");
                        }
                    });
                });
        } else trace("Server Error : " + optimization.error_message);
    }

    private void initUI(Context context, AttributeSet attrs) {
        /* View Inflation & ButterKnife Binding */
        View content = LayoutInflater.from(context).inflate(R.layout.view_episode_player, null, false);
        addView(content, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mUnbinder = ButterKnife.bind(this, content);

        /* Initialize Simulator Callback */
        DJISimulator simulator = DJISampleApplication.getAircraftInstance().getFlightController().getSimulator();
        if (null == simulator) trace("Simulator is NULL");
        else {
            simulator.setUpdatedSimulatorStateDataCallback(djiSimulatorStateData -> {
                SimulatorLog.getInstance().add(djiSimulatorStateData);
                Observable.just(null).observeOn(AndroidSchedulers.mainThread()).subscribe(unused ->
                mSimulatorFeedback.setText(String.format("Position : [%.4f, %.4f, %.4f]\nOrientation : [%.4f, %.4f, %.4f]\n",
                    djiSimulatorStateData.getPositionX(), djiSimulatorStateData.getPositionY(), djiSimulatorStateData.getPositionZ(),
                    djiSimulatorStateData.getYaw(), djiSimulatorStateData.getPitch(), djiSimulatorStateData.getRoll())
                ));
            });
            mButtonConfigInitializer.setEnabled(!simulator.hasSimulatorStarted());
            mButtonConfigFinalizer.setEnabled(simulator.hasSimulatorStarted());
        }

        /* Initialize Episode List */
        mEpisodes = new ArrayList<>();
        mEpisodeAdapter = new EpisodeAdapter();
        mEpisodeList.setAdapter(mEpisodeAdapter);
        mEpisodeList.setOnItemClickListener((parent, view, position, id) -> new MaterialDialog.Builder(parent.getContext())
            .title(String.format("Episode #%d", mEpisodes.get(position).id))
            .content("Choose options below")
            .positiveText("New Optimization")
            .onPositive((dialog, which) -> initTrajectoryOptimization(mEpisodes.get(position)))
            .neutralText("Just Once")
            .onNeutral((dialog, which) -> pushSimulatorLog(mEpisodes.get(position)))
            .show()
        );
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
                trace(String.format("Successfully loaded %d episodes", episodes.size()));
            }

            @Override
            public void onFailure(Call<ArrayList<Episode>> call, Throwable throwable) {
                trace(throwable, "Failed to load episodes");
            }
        });

        /* Initialize Trajectory Optimization Dashboard */
        mButtonTrajectoryOptimizationStop.setOnClickListener(unused -> isTrajectoryOptimizationRunning = false);

        /* Set Listeners for 'Availiable Actions' */

        mButtonConfigInitializer.setOnClickListener(unused -> {
            if (!DJIModuleVerificationUtil.isFlightControllerAvailable()) return;
            DJIFlightController controller = DJISampleApplication.getAircraftInstance().getFlightController();
            controller.enableVirtualStickControlMode(this::toast);
            controller.getSimulator().startSimulator(new DJISimulatorInitializationData(SIM_LATITUDE, SIM_LONGITUDE, SIM_STATE_UPDATE_FREQUENCY, SIM_SATELLITES), this::toast);
            controller.getFlightLimitation().setMaxFlightHeight(500, this::toast); // in range [20, 500] m
            controller.getFlightLimitation().setMaxFlightRadius(500, this::toast); // in range [15, 500] m
            mButtonConfigInitializer.setEnabled(false);
            mButtonConfigFinalizer.setEnabled(true);
        });

        mButtonConfigFinalizer.setOnClickListener(unused -> {
            if (!DJIModuleVerificationUtil.isFlightControllerAvailable()) return;
            DJIFlightController controller = DJISampleApplication.getAircraftInstance().getFlightController();
            controller.disableVirtualStickControlMode(this::toast);
            controller.getSimulator().stopSimulator(this::toast);
            mButtonConfigInitializer.setEnabled(true);
            mButtonConfigFinalizer.setEnabled(false);
        });

        mButtonTakeOff.setOnClickListener(unused -> {
            if (!DJIModuleVerificationUtil.isFlightControllerAvailable()) return;
            DJISampleApplication.getAircraftInstance().getFlightController().takeOff(this::toast);
        });

        mButtonAutoLanding.setOnClickListener(unused -> {
            if (!DJIModuleVerificationUtil.isFlightControllerAvailable()) return;
            DJISampleApplication.getAircraftInstance().getFlightController().autoLanding(this::toast);
        });
    }

    private Action1<VirtualStickCommand> sendVirtualStickCommand = (cmd) -> {
        // Check FlightController Accessibility
        if (!DJIModuleVerificationUtil.isFlightControllerAvailable()) return;
        DJIFlightController controller = DJISampleApplication.getAircraftInstance().getFlightController();

        controller.sendVirtualStickFlightControlData(cmd.toDJIVirtualStickFlightControlData(), this::silence); // Ignore status feedback
    };

    /* MISC : Toast, Trace, Log */
    private void silence(DJIError djiError) {}
    private void toast(DJIError djiError) {
        Observable.just(null == djiError ? this.getContext().getResources().getString(R.string.success) : djiError.getDescription())
            .subscribeOn(AndroidSchedulers.mainThread())
            .subscribe(this::trace);
    }
    private void trace(String message) {
        Timber.d(message);
        Toast.makeText(this.getContext(), message, Toast.LENGTH_SHORT).show();
    }
    private void trace(Throwable throwable, String prefix) {
        throwable.printStackTrace();
        Toast.makeText(this.getContext(), String.format("%s : %s", prefix, throwable.getMessage()), Toast.LENGTH_SHORT).show();
    }
}
