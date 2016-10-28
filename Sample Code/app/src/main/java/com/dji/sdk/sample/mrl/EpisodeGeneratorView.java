package com.dji.sdk.sample.mrl;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ToggleButton;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.common.DJISampleApplication;
import com.dji.sdk.sample.common.Utils;
import com.dji.sdk.sample.mrl.network.model.Episode;
import com.dji.sdk.sample.utils.DJIModuleVerificationUtil;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dji.common.flightcontroller.DJISimulatorInitializationData;
import dji.common.flightcontroller.DJIVirtualStickFlightCoordinateSystem;
import dji.common.flightcontroller.DJIVirtualStickRollPitchControlMode;
import dji.common.flightcontroller.DJIVirtualStickVerticalControlMode;
import dji.common.flightcontroller.DJIVirtualStickYawControlMode;
import dji.sdk.flightcontroller.DJIFlightController;
import dji.thirdparty.rx.functions.Action1;
import timber.log.Timber;

public class EpisodeGeneratorView extends RelativeLayout {

    private final double SIMULATOR_INITIAL_LATITUDE = 20;
    private final double SIMULATOR_INITIAL_LONGITUDE = 20;
    private final int SIMULATION_UPDATE_FREQUENCY = 10;
    private final int SIMULATOR_INITIAL_NUM_OF_SATELLITES = 10;

    @BindView(R.id.logger_textlist) protected ListView mListLogger;

    @BindView(R.id.btn_toggle_take_off) protected ToggleButton mToggleTakeOff;
    @BindView(R.id.btn_move_up) protected Button mBtnMoveUp;
    @BindView(R.id.btn_move_down) protected Button mBtnMoveDown;
    @BindView(R.id.btn_move_left) protected Button mBtnMoveLeft;
    @BindView(R.id.btn_move_right) protected Button mBtnMoveRight;
    @BindView(R.id.btn_move_forward) protected Button mBtnMoveForward;
    @BindView(R.id.btn_move_backward) protected Button mBtnMoveBackward;
    @BindView(R.id.btn_turn_left) protected Button mBtnTurnLeft;
    @BindView(R.id.btn_turn_right) protected Button mBtnTurnRight;

    @BindView(R.id.btn_toggle_advanced_flight_mode) protected ToggleButton mToggleAdvFlightMode;
    @BindView(R.id.btn_toggle_virtual_stick) protected ToggleButton mToggleVirtualStick;
    @BindView(R.id.btn_toggle_episode) protected ToggleButton mToggleEpisode;
    @BindView(R.id.btn_toggle_simulator) protected ToggleButton mToggleSimulator;

    private Unbinder mUnbinder;

    private ArrayList<String> mLog;
    private ArrayAdapter<String> mAdapter;
    private Episode mEpisode;

    public EpisodeGeneratorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initUI(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.setControlMode();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.restoreOldControlMode();
        mUnbinder.unbind();
    }

    private DJIVirtualStickYawControlMode mPrevYawControlMode;
    private DJIVirtualStickRollPitchControlMode mPrevRollPitchControlMode;
    private DJIVirtualStickVerticalControlMode mPrevVerticalControlMode;
    private DJIVirtualStickFlightCoordinateSystem mPrevFlightCoordinateSystem;
    private void setControlMode() {
        // Check FlightController Accessibility
        if (DJISampleApplication.getAircraftInstance() == null || DJISampleApplication.getAircraftInstance().getFlightController() == null) return;
        DJIFlightController controller = DJISampleApplication.getAircraftInstance().getFlightController();

        // HorizontalFlightCoordinateSystem : Body by default
        mPrevFlightCoordinateSystem = controller.getHorizontalCoordinateSystem();
        controller.setHorizontalCoordinateSystem(DJIVirtualStickFlightCoordinateSystem.Body);
        this.log2ListLogger("FlightCoordinateSystem : %s => %s", mPrevFlightCoordinateSystem.name(), controller.getHorizontalCoordinateSystem().name());

        // YawControlMode : AngularVelocity (Palstance) by default
        mPrevYawControlMode = controller.getYawControlMode();
        controller.setYawControlMode(DJIVirtualStickYawControlMode.AngularVelocity);
        this.log2ListLogger("YawControlMode : %s => %s", mPrevYawControlMode.name(), controller.getYawControlMode().name());

        // RollPitchControlMode : Angle by default
        mPrevRollPitchControlMode = controller.getRollPitchControlMode();
        controller.setRollPitchControlMode(DJIVirtualStickRollPitchControlMode.Velocity);
        this.log2ListLogger("RollPitchControlMode : %s => %s", mPrevRollPitchControlMode.name(), controller.getRollPitchControlMode().name());

        // VerticalControlMode : Velocity by default
        mPrevVerticalControlMode = controller.getVerticalControlMode();
        controller.setVerticalControlMode(DJIVirtualStickVerticalControlMode.Velocity);
        this.log2ListLogger("VerticalControlMode : %s => %s", mPrevVerticalControlMode.name(), controller.getVerticalControlMode().name());
    }

    private void restoreOldControlMode() {
        // Check FlightController Accessibility
        if (DJISampleApplication.getAircraftInstance() == null || DJISampleApplication.getAircraftInstance().getFlightController() == null) return;
        DJIFlightController controller = DJISampleApplication.getAircraftInstance().getFlightController();

        // Restore old control modes
        controller.setHorizontalCoordinateSystem(mPrevFlightCoordinateSystem);
        controller.setYawControlMode(mPrevYawControlMode);
        controller.setRollPitchControlMode(mPrevRollPitchControlMode);
        controller.setVerticalControlMode(mPrevVerticalControlMode);
    }

    private void initUI(Context context, AttributeSet attrs) {
        /* Inflate & Initialize View */
        View content = LayoutInflater.from(context).inflate(R.layout.view_episode_generator, null, false);
        addView(content, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mUnbinder = ButterKnife.bind(this, content);

        /* Initialize Loggers */
        mLog = new ArrayList<>();
        mAdapter = new ArrayAdapter<>(this.getContext(), R.layout.list_logger_item, android.R.id.text1, mLog);
        mListLogger.setAdapter(mAdapter);

        mToggleAdvFlightMode.setOnCheckedChangeListener((buttonView, checked) -> {
            if (this.isFlightControllerNotAvailiable()) return;
            if (checked) {
                DJISampleApplication.getAircraftInstance().getFlightController().setVirtualStickAdvancedModeEnabled(true);
                this.log2ListLogger(R.string.advanced_flight_mode_on);
            } else {
                DJISampleApplication.getAircraftInstance().getFlightController().setVirtualStickAdvancedModeEnabled(false);
                this.log2ListLogger(R.string.advanced_flight_mode_off);
            }

        });

        mToggleVirtualStick.setOnCheckedChangeListener((buttonView, checked) -> {
            if (this.isFlightControllerNotAvailiable()) return;
            if (checked) {
                DJISampleApplication.getAircraftInstance().getFlightController().enableVirtualStickControlMode(
                    djiError -> Utils.showDialogBasedOnError(getContext(), djiError)
                );
            } else {
                DJISampleApplication.getAircraftInstance().getFlightController().disableVirtualStickControlMode(
                    djiError -> Utils.showDialogBasedOnError(getContext(), djiError)
                );
            }
        });

        mToggleSimulator.setOnCheckedChangeListener((buttonView, checked) -> {
            if (checked) {
                DJISampleApplication.getAircraftInstance().getFlightController().getSimulator().startSimulator(
                    new DJISimulatorInitializationData(
                        SIMULATOR_INITIAL_LATITUDE,
                        SIMULATOR_INITIAL_LONGITUDE,
                        SIMULATION_UPDATE_FREQUENCY,
                        SIMULATOR_INITIAL_NUM_OF_SATELLITES
                    ),
                    djiError -> Utils.showDialogBasedOnError(getContext(), djiError)
                );
            } else {
                DJISampleApplication.getAircraftInstance().getFlightController().getSimulator().stopSimulator(
                    djiError -> Utils.showDialogBasedOnError(getContext(), djiError)
                );
            }
        });

        mToggleEpisode.setOnCheckedChangeListener((buttonView, checked) -> {
            if (this.isFlightControllerNotAvailiable()) return;
            if (checked) mEpisode = new Episode();
            else mEpisode.getEpisodeObservable().subscribe(this.sendVirtualStickCommand);
        });

        mToggleTakeOff.setOnCheckedChangeListener((buttonView, checked) -> {
            if (this.isFlightControllerNotAvailiable()) return;
            if (checked) {
                DJISampleApplication.getAircraftInstance().getFlightController().takeOff(
                    djiError -> Utils.showDialogBasedOnError(getContext(), djiError)
                );
            } else {
                DJISampleApplication.getAircraftInstance().getFlightController().autoLanding(
                    djiError -> Utils.showDialogBasedOnError(getContext(), djiError)
                );
            }
        });

        /* Add Command to Episode */
        mBtnMoveForward.setOnClickListener  (v -> mEpisode.push(new VirtualStickCommand(VirtualStickCommand.Direction.FORWARD)));
        mBtnMoveBackward.setOnClickListener (v -> mEpisode.push(new VirtualStickCommand(VirtualStickCommand.Direction.BACKWARD)));
        mBtnMoveLeft.setOnClickListener     (v -> mEpisode.push(new VirtualStickCommand(VirtualStickCommand.Direction.LEFT)));
        mBtnMoveRight.setOnClickListener    (v -> mEpisode.push(new VirtualStickCommand(VirtualStickCommand.Direction.RIGHT)));
        mBtnMoveUp.setOnClickListener       (v -> mEpisode.push(new VirtualStickCommand(VirtualStickCommand.Direction.UP)));
        mBtnMoveDown.setOnClickListener     (v -> mEpisode.push(new VirtualStickCommand(VirtualStickCommand.Direction.DOWN)));
        mBtnTurnLeft.setOnClickListener     (v -> mEpisode.push(new VirtualStickCommand(VirtualStickCommand.Direction.CCW)));
        mBtnTurnRight.setOnClickListener    (v -> mEpisode.push(new VirtualStickCommand(VirtualStickCommand.Direction.CW)));
    }

    private Action1<VirtualStickCommand> sendVirtualStickCommand = (cmd) -> {
        if (EpisodeGeneratorView.this.isFlightControllerNotAvailiable()) return;
        EpisodeGeneratorView.this.log2ListLogger(cmd.toString()); // Yaw, Pitch, Roll, Throttle
        DJISampleApplication.getAircraftInstance().getFlightController().sendVirtualStickFlightControlData(
            cmd.toDJIVirtualStickFlightControlData(),
            djiError -> {}
        );
    };

    /* Priavte Methods */
    private boolean isFlightControllerNotAvailiable() {
        final boolean availiable = DJIModuleVerificationUtil.isFlightControllerAvailable();
        if (!availiable) this.log2ListLogger("FlightController Not Availiable");
        return !availiable;
    }

    /* Loggers */
    private void log2ListLogger(String format, Object ...args) {
        log2ListLogger(String.format(format, args));
    }
    private void log2ListLogger(int resid) {
        log2ListLogger(this.getContext().getResources().getString(resid));
    }
    private void log2ListLogger(String log) {
        Timber.d(log);
        mLog.add(0, log);
        mAdapter.notifyDataSetChanged();
    }
}