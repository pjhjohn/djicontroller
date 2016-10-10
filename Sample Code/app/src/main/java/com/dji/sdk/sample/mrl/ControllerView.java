package com.dji.sdk.sample.mrl;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.common.DJISampleApplication;
import com.dji.sdk.sample.common.Utils;
import com.dji.sdk.sample.utils.DJIModuleVerificationUtil;
import com.dji.sdk.sample.utils.OnScreenJoystick;

import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dji.common.flightcontroller.DJIFlightControllerDataType;
import dji.common.flightcontroller.DJISimulatorInitializationData;
import dji.common.flightcontroller.DJIVirtualStickFlightControlData;

public class ControllerView extends RelativeLayout {

    @BindView(R.id.simulator_log) protected TextView mTextView;

    @BindView(R.id.btn_toggle_simulator) protected ToggleButton mToggleSimulator;
    @BindView(R.id.btn_toggle_advanced_flight_mode) protected ToggleButton mToggleAdvFlightMode;
    @BindView(R.id.btn_toggle_virtual_stick) protected ToggleButton mToggleVirtualStick;

    @BindView(R.id.btn_take_off) protected Button mBtnTakeOff;
    @BindView(R.id.btn_custom_action1) protected Button mBtnCustomAction1;
    @BindView(R.id.btn_custom_action2) protected Button mBtnCustomAction2;

    @BindView(R.id.joystic_left) protected OnScreenJoystick mScreenJoystickLeft;
    @BindView(R.id.joystic_right) protected OnScreenJoystick mScreenJoystickRight;

    private Unbinder mUnbinder;

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
        mTextView.setText("Simulator is off.");

        /* Set Action Toggle Button Handlers */
        mToggleSimulator.setOnCheckedChangeListener((buttonView, checked) -> {
            if (checked) {
                mTextView.setVisibility(VISIBLE);
                DJISampleApplication.getAircraftInstance().getFlightController().getSimulator().startSimulator(
                    new DJISimulatorInitializationData(23, 113, 10, 10),
                    djiError -> Utils.showDialogBasedOnError(getContext(), djiError)
                );
            } else {
                mTextView.setVisibility(INVISIBLE);
                DJISampleApplication.getAircraftInstance().getFlightController().getSimulator().stopSimulator(
                    djiError -> Utils.showDialogBasedOnError(getContext(), djiError)
                );
            }
        });
        DJISampleApplication.getAircraftInstance().getFlightController().getSimulator().setUpdatedSimulatorStateDataCallback(
            djiSimulatorStateData -> new Handler(Looper.getMainLooper()).post(() -> mTextView.setText(String.format(
                "Yaw : %f\nPitch : %f\nRoll : %f\nX : %f\nY : %f\nZ : %f",
                djiSimulatorStateData.getYaw(),
                djiSimulatorStateData.getPitch(),
                djiSimulatorStateData.getRoll(),
                djiSimulatorStateData.getPositionX(),
                djiSimulatorStateData.getPositionY(),
                djiSimulatorStateData.getPositionZ()
            )))
        );

        mToggleAdvFlightMode.setOnCheckedChangeListener((buttonView, checked) -> {
            if (!DJIModuleVerificationUtil.isFlightControllerAvailable()) return;
            DJISampleApplication.getAircraftInstance().getFlightController().setVirtualStickAdvancedModeEnabled(checked);
        });

        mToggleVirtualStick.setOnCheckedChangeListener((buttonView, checked) -> {
            if (!DJIModuleVerificationUtil.isFlightControllerAvailable()) return;
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

        /* Set Action Button Handlers */

        mBtnTakeOff.setOnClickListener(v -> {
            if (!DJIModuleVerificationUtil.isFlightControllerAvailable()) return;
            Toast.makeText(this.getContext(), "FlightController is availiable. Sending TakeOff Command", Toast.LENGTH_SHORT).show();
            DJISampleApplication.getAircraftInstance().getFlightController().takeOff(
                djiError -> Utils.showDialogBasedOnError(getContext(), djiError)
            );
        });
        mBtnCustomAction1.setOnClickListener(v -> Toast.makeText(this.getContext(), "Not Implemented", Toast.LENGTH_SHORT).show());
        mBtnCustomAction2.setOnClickListener(v -> Toast.makeText(this.getContext(), "Not Implemented", Toast.LENGTH_SHORT).show());

        /* Set Joystick Handlers */
        mScreenJoystickLeft.setJoystickListener((joystick, pX, pY) -> {
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
