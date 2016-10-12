package com.dji.sdk.sample.mrl;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.common.DJISampleApplication;
import com.dji.sdk.sample.common.Utils;
import com.dji.sdk.sample.utils.DJIModuleVerificationUtil;
import com.dji.sdk.sample.utils.OnScreenJoystick;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dji.common.flightcontroller.DJIFlightControllerDataType;
import dji.common.flightcontroller.DJISimulatorInitializationData;
import dji.common.flightcontroller.DJIVirtualStickFlightControlData;
import dji.thirdparty.rx.Observable;
import dji.thirdparty.rx.Subscriber;
import dji.thirdparty.rx.android.schedulers.AndroidSchedulers;
import dji.thirdparty.rx.schedulers.Schedulers;
import timber.log.Timber;

import static android.R.attr.value;

public class ControllerView extends RelativeLayout {

    private final double SIMULATOR_INITIAL_LATITUDE = 20;
    private final double SIMULATOR_INITIAL_LONGITUDE = 20;
    private final int SIMULATION_UPDATE_FREQUENCY = 10;
    private final int SIMULATOR_INITIAL_NUM_OF_SATELLITES = 10;

    @BindView(R.id.logger_text) protected TextView mTextLogger;
    @BindView(R.id.logger_textlist) protected ListView mListLogger;

    @BindView(R.id.btn_toggle_simulator) protected ToggleButton mToggleSimulator;
    @BindView(R.id.btn_toggle_advanced_flight_mode) protected ToggleButton mToggleAdvFlightMode;
    @BindView(R.id.btn_toggle_virtual_stick) protected ToggleButton mToggleVirtualStick;

    @BindView(R.id.btn_take_off) protected Button mBtnTakeOff;
    @BindView(R.id.btn_custom_action1) protected Button mBtnCustomAction1;
    @BindView(R.id.btn_custom_action2) protected Button mBtnCustomAction2;

    @BindView(R.id.joystic_left) protected OnScreenJoystick mScreenJoystickLeft;
    @BindView(R.id.joystic_right) protected OnScreenJoystick mScreenJoystickRight;

    private Unbinder mUnbinder;

    private ArrayList<String> mLog;
    private ArrayAdapter<String> mAdapter;

    private Timer mSendVirtualStickDataTimer;
    private SendVirtualStickDataTask mSendVirtualStickDataTask;

    private float mPitch;
    private float mRoll;
    private float mYaw;
    private float mThrottle;
    private int mCommandIndex;

    public ControllerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initUI(context, attrs);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mUnbinder.unbind();
        if (null != mSendVirtualStickDataTimer) {
            mSendVirtualStickDataTask.cancel();
            mSendVirtualStickDataTask = null;
            mSendVirtualStickDataTimer.cancel();
            mSendVirtualStickDataTimer.purge();
            mSendVirtualStickDataTimer = null;
        }
    }

    private void initUI(Context context, AttributeSet attrs) {
        /* Inflate & Initialize View */
        View content = LayoutInflater.from(context).inflate(R.layout.view_controller, null, false);
        addView(content, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mUnbinder = ButterKnife.bind(this, content);

        mCommandIndex = 0;

        /* Initialize Loggers */
        this.setLoggerMode(LOGGER_LIST);

        mLog = new ArrayList<>();
        mAdapter = new ArrayAdapter<>(this.getContext(), R.layout.list_logger_item, android.R.id.text1, mLog);
        mListLogger.setAdapter(mAdapter);

        /* Set Action Toggle Button Handlers */
        DJISampleApplication.getAircraftInstance().getFlightController().getSimulator().setUpdatedSimulatorStateDataCallback(
            djiSimulatorStateData -> new Handler(Looper.getMainLooper()).post(() -> mTextLogger.setText(String.format(
                "Yaw : %f\nPitch : %f\nRoll : %f\nX : %f\nY : %f\nZ : %f",
                djiSimulatorStateData.getYaw(),
                djiSimulatorStateData.getPitch(),
                djiSimulatorStateData.getRoll(),
                djiSimulatorStateData.getPositionX(),
                djiSimulatorStateData.getPositionY(),
                djiSimulatorStateData.getPositionZ()
            )))
        );

        mToggleSimulator.setOnCheckedChangeListener((buttonView, checked) -> {
            if (checked) {
                this.setLoggerMode(LOGGER_TEXT);
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
                this.setLoggerMode(LOGGER_LIST);
                DJISampleApplication.getAircraftInstance().getFlightController().getSimulator().stopSimulator(
                    djiError -> Utils.showDialogBasedOnError(getContext(), djiError)
                );
            }
        });

        mToggleAdvFlightMode.setOnCheckedChangeListener((buttonView, checked) -> {
            this.setLoggerMode(LOGGER_LIST);
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
                this.setLoggerMode(LOGGER_LIST);
                mCommandIndex = 0;
                DJISampleApplication.getAircraftInstance().getFlightController().enableVirtualStickControlMode(
                    djiError -> Utils.showDialogBasedOnError(getContext(), djiError)
                );
            } else {
                this.setLoggerMode(LOGGER_TEXT);
                DJISampleApplication.getAircraftInstance().getFlightController().disableVirtualStickControlMode(
                    djiError -> Utils.showDialogBasedOnError(getContext(), djiError)
                );
            }
        });

        /* Set Action Button Handlers */
        mBtnTakeOff.setOnClickListener(v -> {
            this.setLoggerMode(LOGGER_LIST);
            if (this.isFlightControllerNotAvailiable()) return;
            DJISampleApplication.getAircraftInstance().getFlightController().takeOff(
                djiError -> Utils.showDialogBasedOnError(getContext(), djiError)
            );
        });
        mBtnCustomAction1.setOnClickListener(v -> {
            this.setLoggerMode(LOGGER_LIST);
            Integer[] commandIndexes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
            Observable.from(commandIndexes)
                .concatMap( val -> Observable.just(val).delay(200, TimeUnit.MILLISECONDS))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( cmdIndex -> {
                    if (ControllerView.this.isFlightControllerNotAvailiable()) return;
                    float y = 0.0f, p = 0.0f, r = 0.0f, t = DJIFlightControllerDataType.DJIVirtualStickVerticalControlMaxVelocity * 0.5f;
                    ControllerView.this.log2ListLogger(String.format("CMD#%d : [ %4f | %4f | %4f | %4f ]", cmdIndex, y, p, r, t)); // Yaw, Pitch, Roll, Throttle
                    ControllerView.this.mCommandIndex++;
                    DJISampleApplication.getAircraftInstance().getFlightController().sendVirtualStickFlightControlData(
                        new DJIVirtualStickFlightControlData(p, r, y, t),
                        djiError -> {}
                    );
                }
            );
        });
        mBtnCustomAction2.setOnClickListener(v -> {
            this.setLoggerMode(LOGGER_LIST);
            this.log2ListLogger("Not Implemented");
        });

        /* Set Joystick Handlers */
        mScreenJoystickLeft.setJoystickListener((joystick, pX, pY) -> {
            this.setLoggerMode(LOGGER_TEXT);
            if(Math.abs(pX) < 0.02 ) pX = 0;
            if(Math.abs(pY) < 0.02 ) pY = 0;

            float pitchJoyControlMaxSpeed = DJIFlightControllerDataType.DJIVirtualStickRollPitchControlMaxVelocity;
            float rollJoyControlMaxSpeed = DJIFlightControllerDataType.DJIVirtualStickRollPitchControlMaxVelocity;

            mPitch = pitchJoyControlMaxSpeed * pY;
            mRoll = rollJoyControlMaxSpeed * pX;

            if (null == mSendVirtualStickDataTimer) {
                mSendVirtualStickDataTask = new SendVirtualStickDataTask();
                mSendVirtualStickDataTimer = new Timer();
                mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 100, 200);
            }
        });

        mScreenJoystickRight.setJoystickListener((joystick, pX, pY) -> {
            this.setLoggerMode(LOGGER_TEXT);
            if(Math.abs(pX) < 0.02 ) pX = 0;
            if(Math.abs(pY) < 0.02 ) pY = 0;

            float verticalJoyControlMaxSpeed = DJIFlightControllerDataType.DJIVirtualStickVerticalControlMaxVelocity;
            float yawJoyControlMaxSpeed = DJIFlightControllerDataType.DJIVirtualStickYawControlMaxAngularVelocity;

            mYaw = yawJoyControlMaxSpeed * pX;
            mThrottle = verticalJoyControlMaxSpeed * pY;

            if (null == mSendVirtualStickDataTimer) {
                mSendVirtualStickDataTask = new SendVirtualStickDataTask();
                mSendVirtualStickDataTimer = new Timer();
                mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 0, 200);
            }
        });
    }

    // TODO : Replace with Observables
    class SendVirtualStickDataTask extends TimerTask {
        @Override
        public void run() {
            if (ControllerView.this.isFlightControllerNotAvailiable()) return;
            ControllerView.this.log2ListLogger(String.format("[YPRT] : [ %4f | %4f | %4f | %4f ]", mYaw, mPitch, mRoll, mThrottle));
            ControllerView.this.mCommandIndex++;
            DJISampleApplication.getAircraftInstance().getFlightController().sendVirtualStickFlightControlData(
                new DJIVirtualStickFlightControlData(mPitch, mRoll, mYaw, mThrottle),
                djiError -> {}
            );
        }
    }

    /* Priavte Methods */
    private boolean isFlightControllerNotAvailiable() {
        final boolean availiable = DJIModuleVerificationUtil.isFlightControllerAvailable();
        if (!availiable) this.log2ListLogger("FlightController Not Availiable");
        return !availiable;
    }

    /* Loggers */
    public static boolean LOGGER_TEXT = true;
    public static boolean LOGGER_LIST = false;
    private void setLoggerMode(boolean mode) {
        mTextLogger.setVisibility(mode==LOGGER_TEXT ? VISIBLE : GONE);
        mListLogger.setVisibility(mode==LOGGER_LIST ? VISIBLE : GONE);
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