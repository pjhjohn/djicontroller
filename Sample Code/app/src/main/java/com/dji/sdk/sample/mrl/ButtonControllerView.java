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
import android.widget.Toast;
import android.widget.ToggleButton;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.common.DJISampleApplication;
import com.dji.sdk.sample.common.Utils;
import com.dji.sdk.sample.mrl.network.api.Api;
import com.dji.sdk.sample.mrl.network.model.Episode;
import com.dji.sdk.sample.utils.DJIModuleVerificationUtil;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dji.common.flightcontroller.DJISimulatorInitializationData;
import dji.thirdparty.retrofit2.Call;
import dji.thirdparty.retrofit2.Callback;
import dji.thirdparty.retrofit2.Response;
import dji.thirdparty.rx.Observable;
import dji.thirdparty.rx.android.schedulers.AndroidSchedulers;
import dji.thirdparty.rx.functions.Action1;
import dji.thirdparty.rx.schedulers.Schedulers;
import timber.log.Timber;

public class ButtonControllerView extends RelativeLayout {

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
    private static final Integer[] CMD_INDEXES = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    private Observable<VirtualStickCommand> getDummyObservable(float pitch, float roll, float yaw, float throttle) {
        return Observable.from(CMD_INDEXES)
            .concatMap( val -> Observable.just(val).delay(200, TimeUnit.MILLISECONDS))
            .map(index -> new VirtualStickCommand(index, pitch, roll, yaw, throttle))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }

    public ButtonControllerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initUI(context, attrs);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mUnbinder.unbind();
    }

    private void initUI(Context context, AttributeSet attrs) {
        /* Inflate & Initialize View */
        View content = LayoutInflater.from(context).inflate(R.layout.view_button_controller, null, false);
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
            if (checked) {
                Call<ArrayList<Episode>> call = Api.database().getEpisodes();
                call.enqueue(new Callback<ArrayList<Episode>>() {
                    @Override
                    public void onResponse(Call<ArrayList<Episode>> call, Response<ArrayList<Episode>> response) {
                        Toast.makeText(ButtonControllerView.this.getContext(), response.body().size(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Call<ArrayList<Episode>> call, Throwable throwable) {
                        Toast.makeText(ButtonControllerView.this.getContext(), "call failed : " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(ButtonControllerView.this.getContext(), R.string.btn_toggle_episode_stop, Toast.LENGTH_SHORT).show();
            }
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

        /* Set Action Button Handlers */
        mBtnTurnLeft.setOnClickListener     (v -> getDummyObservable( 0.0f, 0.0f,-0.5f, 0.0f).subscribe(this.sendVirtualStickCommand));
        mBtnTurnRight.setOnClickListener    (v -> getDummyObservable( 0.0f, 0.0f, 0.5f, 0.0f).subscribe(this.sendVirtualStickCommand));
        mBtnMoveUp.setOnClickListener       (v -> getDummyObservable( 0.0f, 0.0f, 0.0f, 1.0f).subscribe(this.sendVirtualStickCommand));
        mBtnMoveDown.setOnClickListener     (v -> getDummyObservable( 0.0f, 0.0f, 0.0f, 0.0f).subscribe(this.sendVirtualStickCommand));

        mBtnMoveLeft.setOnClickListener     (v -> getDummyObservable( 0.0f,-0.5f, 0.0f, 0.0f).subscribe(this.sendVirtualStickCommand));
        mBtnMoveRight.setOnClickListener    (v -> getDummyObservable( 0.0f, 0.5f, 0.0f, 0.0f).subscribe(this.sendVirtualStickCommand));
        mBtnMoveForward.setOnClickListener  (v -> getDummyObservable(-0.5f, 0.0f, 0.0f, 0.0f).subscribe(this.sendVirtualStickCommand));
        mBtnMoveBackward.setOnClickListener (v -> getDummyObservable( 0.5f, 0.0f, 0.0f, 0.0f).subscribe(this.sendVirtualStickCommand));
    }

    private Action1<VirtualStickCommand> sendVirtualStickCommand = (cmd) -> {
        if (ButtonControllerView.this.isFlightControllerNotAvailiable()) return;
        ButtonControllerView.this.log2ListLogger(cmd.toString()); // Yaw, Pitch, Roll, Throttle
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
    private void log2ListLogger(int resid) {
        log2ListLogger(this.getContext().getResources().getString(resid));
    }
    private void log2ListLogger(String log) {
        Timber.d(log);
        mLog.add(0, log);
        mAdapter.notifyDataSetChanged();
    }
}