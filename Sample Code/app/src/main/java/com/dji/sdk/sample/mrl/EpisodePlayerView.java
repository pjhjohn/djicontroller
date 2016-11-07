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

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.common.DJISampleApplication;
import com.dji.sdk.sample.common.Utils;
import com.dji.sdk.sample.mrl.network.api.Api;
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
import dji.thirdparty.retrofit2.Call;
import dji.thirdparty.retrofit2.Callback;
import dji.thirdparty.retrofit2.Response;
import dji.thirdparty.rx.functions.Action1;

public class EpisodePlayerView extends RelativeLayout {

    private final double SIMULATOR_LATITUDE = 20;
    private final double SIMULATOR_LONGITUDE = 20;
    private final int SIMULATOR_STATE_UPDATE_FREQUENCY = 10;
    private final int SIMULATOR_NUM_OF_SATELLITES = 10;

    @BindView(R.id.episode_list) protected ListView mEpisodeList;
    @BindView(R.id.btn_initialize_flight_config) protected Button mButtonConfigInitializer;
    @BindView(R.id.btn_finalize_flight_config) protected Button mButtonConfigFinalizer;
    @BindView(R.id.btn_take_off) protected Button mButtonTakeOff;
    @BindView(R.id.btn_auto_landing) protected Button mButtonAutoLanding;

    private Unbinder mUnbinder;
    private ArrayList<Episode> mEpisodes;
    private ArrayAdapter<Episode> mEpisodeAdapter;

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

    private void initUI(Context context, AttributeSet attrs) {
        /* Inflate & Initialize View */
        View content = LayoutInflater.from(context).inflate(R.layout.view_episode_player, null, false);
        addView(content, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mUnbinder = ButterKnife.bind(this, content);

        /* Initialize Episode List */
        mEpisodes = new ArrayList<>();
        mEpisodeAdapter = new ArrayAdapter<>(this.getContext(), R.layout.list_logger_item, android.R.id.text1, mEpisodes); // TODO : new layout to inflace with Episode
        mEpisodeList.setAdapter(mEpisodeAdapter);
        mEpisodeList.setOnItemClickListener((parent, view, position, id) -> {
            mEpisodes.get(position).getVirtualStickCommandsObservable().subscribe(EpisodePlayerView.this.sendVirtualStickCommand);
        });

        /* Fetch Episode Data */
        Call<ArrayList<Episode>> call = Api.database().getEpisodes();
        call.enqueue(new Callback<ArrayList<Episode>>() {
            @Override
            public void onResponse(Call<ArrayList<Episode>> call, Response<ArrayList<Episode>> response) {
                mEpisodeAdapter.clear();
                mEpisodeAdapter.addAll(response.body());
                mEpisodeAdapter.notifyDataSetChanged();
                Toast.makeText(EpisodePlayerView.this.getContext(), response.body().size(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<ArrayList<Episode>> call, Throwable throwable) {
                Toast.makeText(EpisodePlayerView.this.getContext(), "call failed : " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        mButtonConfigInitializer.setOnClickListener(unused -> {
            // Check FlightController Accessibility
            if (!DJIModuleVerificationUtil.isFlightControllerAvailable()) return;
            DJIFlightController controller = DJISampleApplication.getAircraftInstance().getFlightController();

            controller.enableVirtualStickControlMode(djiError -> Utils.showDialogBasedOnError(getContext(), djiError));
            controller.getSimulator().startSimulator(
                new DJISimulatorInitializationData(SIMULATOR_LATITUDE, SIMULATOR_LONGITUDE, SIMULATOR_STATE_UPDATE_FREQUENCY, SIMULATOR_NUM_OF_SATELLITES),
                djiError -> Utils.showDialogBasedOnError(getContext(), djiError)
            );
        });

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

            controller.takeOff(djiError -> Utils.showDialogBasedOnError(getContext(), djiError));
        });

        mButtonAutoLanding.setOnClickListener(unused -> {
            // Check FlightController Accessibility
            if (!DJIModuleVerificationUtil.isFlightControllerAvailable()) return;
            DJIFlightController controller = DJISampleApplication.getAircraftInstance().getFlightController();

            controller.autoLanding(djiError -> Utils.showDialogBasedOnError(getContext(), djiError));
        });
    }

    private Action1<VirtualStickCommand> sendVirtualStickCommand = (cmd) -> {
        // Check FlightController Accessibility
        if (!DJIModuleVerificationUtil.isFlightControllerAvailable()) return;
        DJIFlightController controller = DJISampleApplication.getAircraftInstance().getFlightController();

        controller.sendVirtualStickFlightControlData(
            cmd.toDJIVirtualStickFlightControlData(),
            djiError -> Utils.showDialogBasedOnError(getContext(), djiError)
        );
    };
}
