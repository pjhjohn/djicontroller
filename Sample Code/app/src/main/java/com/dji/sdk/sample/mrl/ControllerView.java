package com.dji.sdk.sample.mrl;

import android.app.Service;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.common.DJISampleApplication;
import com.dji.sdk.sample.common.Utils;
import com.dji.sdk.sample.utils.DJIModuleVerificationUtil;
import com.dji.sdk.sample.utils.OnScreenJoystick;
import com.dji.sdk.sample.utils.OnScreenJoystickListener;

import java.util.Timer;
import java.util.TimerTask;

import dji.common.error.DJIError;
import dji.common.flightcontroller.DJIFlightControllerDataType;
import dji.common.flightcontroller.DJISimulatorInitializationData;
import dji.common.flightcontroller.DJISimulatorStateData;
import dji.common.flightcontroller.DJIVirtualStickFlightControlData;
import dji.common.flightcontroller.DJIVirtualStickFlightCoordinateSystem;
import dji.common.flightcontroller.DJIVirtualStickRollPitchControlMode;
import dji.common.flightcontroller.DJIVirtualStickVerticalControlMode;
import dji.common.flightcontroller.DJIVirtualStickYawControlMode;
import dji.common.util.DJICommonCallbacks.DJICompletionCallback;
import dji.sdk.flightcontroller.DJISimulator;

public class ControllerView extends RelativeLayout implements View.OnClickListener {

    private boolean mYawControlModeFlag = true;
    private boolean mRollPitchControlModeFlag = true;
    private boolean mVerticalControlModeFlag = true;
    private boolean mHorizontalCoordinateFlag = true;
    private boolean mStartSimulatorFlag = false;

    private Button mBtnEnableVirtualStick;
    private Button mBtnDisableVirtualStick;
    private Button mBtnHorizontalCoordinate;
    private Button mBtnSetYawControlMode;
    private Button mBtnSetVerticalControlMode;
    private Button mBtnSetRollPitchControlMode;
    private ToggleButton mBtnSimulator;
    private Button mBtnTakeOff;

    private TextView mTextView;

    private OnScreenJoystick mScreenJoystickRight;
    private OnScreenJoystick mScreenJoystickLeft;

    private Timer mSendVirtualStickDataTimer;
    private SendVirtualStickDataTask mSendVirtualStickDataTask;

    private float mPitch;
    private float mRoll;
    private float mYaw;
    private float mThrottle;

    public ControllerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initUI(context, attrs);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (null != mSendVirtualStickDataTimer) {
            mSendVirtualStickDataTask.cancel();
            mSendVirtualStickDataTask = null;
            mSendVirtualStickDataTimer.cancel();
            mSendVirtualStickDataTimer.purge();
            mSendVirtualStickDataTimer = null;
        }
    }

    private void initUI(Context context, AttributeSet attrs) {
        LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Service.LAYOUT_INFLATER_SERVICE);

        View content = layoutInflater.inflate(R.layout.view_virtual_stick, null, false);
        addView(content, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mBtnEnableVirtualStick = (Button) findViewById(R.id.btn_enable_virtual_stick);
        mBtnDisableVirtualStick = (Button) findViewById(R.id.btn_disable_virtual_stick);
        mBtnHorizontalCoordinate = (Button) findViewById(R.id.btn_horizontal_coordinate);
        mBtnSetYawControlMode = (Button) findViewById(R.id.btn_yaw_control_mode);
        mBtnSetVerticalControlMode = (Button) findViewById(R.id.btn_vertical_control_mode);
        mBtnSetRollPitchControlMode = (Button) findViewById(R.id.btn_roll_pitch_control_mode);
        mBtnTakeOff = (Button) findViewById(R.id.btn_take_off);

        mBtnSimulator = (ToggleButton) findViewById(R.id.btn_start_simulator);

        mTextView = (TextView) findViewById(R.id.textview_simulator);

        mScreenJoystickRight = (OnScreenJoystick)findViewById(R.id.directionJoystickRight);
        mScreenJoystickLeft = (OnScreenJoystick)findViewById(R.id.directionJoystickLeft);

        mBtnEnableVirtualStick.setOnClickListener(this);
        mBtnDisableVirtualStick.setOnClickListener(this);
        mBtnHorizontalCoordinate.setOnClickListener(this);
        mBtnSetYawControlMode.setOnClickListener(this);
        mBtnSetVerticalControlMode.setOnClickListener(this);
        mBtnSetRollPitchControlMode.setOnClickListener(this);
        mBtnTakeOff.setOnClickListener(this);

        mBtnSimulator.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                mTextView.setVisibility(VISIBLE);
                DJISampleApplication.getAircraftInstance().getFlightController().getSimulator().startSimulator(
                    new DJISimulatorInitializationData(23, 113, 10, 10),
                    djiError -> {}
                );
            } else {
                mTextView.setVisibility(INVISIBLE);
                DJISampleApplication.getAircraftInstance().getFlightController().getSimulator().stopSimulator(
                    djiError -> {}
                );
            }
        });

        DJISampleApplication.getAircraftInstance().getFlightController().getSimulator().setUpdatedSimulatorStateDataCallback(
            djiSimulatorStateData -> new Handler(Looper.getMainLooper()).post(() -> mTextView.setText(
                "Yaw : " + djiSimulatorStateData.getYaw() + "\n" +
                "X : " + djiSimulatorStateData.getPositionX() + "\n" +
                "Y : " + djiSimulatorStateData.getPositionY() + "\n" +
                "Z : " + djiSimulatorStateData.getPositionZ()
            ))
        );

        mScreenJoystickLeft.setJoystickListener((joystick, pX, pY) -> {
            if(Math.abs(pX) < 0.02 ) {
                pX = 0;
            }

            if(Math.abs(pY) < 0.02 ) {
                pY = 0;
            }
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
            if(Math.abs(pX) < 0.02 ) {
                pX = 0;
            }

            if(Math.abs(pY) < 0.02 ) {
                pY = 0;
            }
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

    @Override
    public void onClick(View v) {
        if (!DJIModuleVerificationUtil.isFlightControllerAvailable()) return;
        switch (v.getId()) {
            case R.id.btn_enable_virtual_stick:
                DJISampleApplication.getAircraftInstance().getFlightController().enableVirtualStickControlMode(
                    djiError -> Utils.showDialogBasedOnError(getContext(), djiError)
                );
                break;

            case R.id.btn_disable_virtual_stick:
                DJISampleApplication.getAircraftInstance().getFlightController().disableVirtualStickControlMode(
                    djiError -> Utils.showDialogBasedOnError(getContext(), djiError)
                );
                break;

            case R.id.btn_roll_pitch_control_mode:
                if (mRollPitchControlModeFlag) {
                    DJISampleApplication.getAircraftInstance().getFlightController().setRollPitchControlMode(
                        DJIVirtualStickRollPitchControlMode.Angle
                    );
                    mRollPitchControlModeFlag = false;
                } else {
                    DJISampleApplication.getAircraftInstance().getFlightController().setRollPitchControlMode(
                        DJIVirtualStickRollPitchControlMode.Velocity
                    );
                    mRollPitchControlModeFlag = true;
                }
                try {
                    Utils.setResultToToast(getContext(), DJISampleApplication.getAircraftInstance().getFlightController().getRollPitchControlMode().name());
                } catch(Exception ex) {}
                break;

            case R.id.btn_yaw_control_mode:
                if (mYawControlModeFlag) {
                    DJISampleApplication.getAircraftInstance().getFlightController().setYawControlMode(
                        DJIVirtualStickYawControlMode.Angle
                    );
                    mYawControlModeFlag = false;
                } else {
                    DJISampleApplication.getAircraftInstance().getFlightController().setYawControlMode(
                        DJIVirtualStickYawControlMode.AngularVelocity
                    );
                    mYawControlModeFlag = true;
                }
                try {
                    Utils.setResultToToast(getContext(), DJISampleApplication.getAircraftInstance().getFlightController().getYawControlMode().name());
                } catch(Exception ex) {}
                break;

            case R.id.btn_vertical_control_mode:
                if (mVerticalControlModeFlag) {
                    DJISampleApplication.getAircraftInstance().getFlightController().setVerticalControlMode(
                        DJIVirtualStickVerticalControlMode.Position
                    );
                    mVerticalControlModeFlag = false;
                } else {
                    DJISampleApplication.getAircraftInstance().getFlightController().setVerticalControlMode(
                        DJIVirtualStickVerticalControlMode.Velocity
                    );
                    mVerticalControlModeFlag = true;
                }
                try {
                    Utils.setResultToToast(getContext(), DJISampleApplication.getAircraftInstance().getFlightController().getVerticalControlMode().name());
                } catch(Exception ex) {}
                break;

            case R.id.btn_horizontal_coordinate:
                if (mHorizontalCoordinateFlag) {
                    DJISampleApplication.getAircraftInstance().getFlightController().setHorizontalCoordinateSystem(
                        DJIVirtualStickFlightCoordinateSystem.Ground
                    );
                    mHorizontalCoordinateFlag = false;
                } else {
                    DJISampleApplication.getAircraftInstance().getFlightController().setHorizontalCoordinateSystem(
                        DJIVirtualStickFlightCoordinateSystem.Body
                    );
                    mHorizontalCoordinateFlag = true;
                }
                try {
                    Utils.setResultToToast(getContext(), DJISampleApplication.getAircraftInstance().getFlightController().getRollPitchCoordinateSystem().name());
                } catch(Exception ex) {}
                break;

            case R.id.btn_take_off:
                DJISampleApplication.getAircraftInstance().getFlightController().takeOff(
                    djiError -> Utils.showDialogBasedOnError(getContext(), djiError)
                );
                break;
            default: break;
        }
    }

    class SendVirtualStickDataTask extends TimerTask {
        @Override
        public void run() {
            if (!DJIModuleVerificationUtil.isFlightControllerAvailable()) return;
            DJISampleApplication.getAircraftInstance().getFlightController().sendVirtualStickFlightControlData(
                new DJIVirtualStickFlightControlData(mPitch, mRoll, mYaw, mThrottle),
                djiError -> {}
            );
        }
    }
}