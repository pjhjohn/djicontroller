# Phantom4Simulator

Research-purpose Android Application to control [DJI Phantom4](http://www.dji.com/phantom-4) using [DJI Mobile SDK](https://developer.dji.com/mobile-sdk/)

## Getting Started

**NOTE : Can be frequently edited over workaround**

### Prerequisites

- AndroidStudio 2.2
- Android SDK
- [DJI Mobile SDK for Andorid](https://github.com/snumrl/DJIAndroidMobileSDK)

### Download

```sh
git clone --recursive https://github.com/snumrl/DJIControllerSample
```

Open `./DJIControllerSample/Sample Code` using AndroidStudio

## Remote Debugging using wi-fi

We need remote debugger to get logs from the device through logcat.
Both the device and PC should be under same wi-fi router : like `mrl`

1. Connect the device to the PC using USB and establish an ADB connection
2. `adb tcpip 5555` # 5555 is the port number
3. Disconnect your USB cable
4. `adb connect MOBILE_IP:port` => `adb connect 192.IP_REMAINING:5555`
5. Tern on AndroidStudio and check the logcat
* You can get the ip address by **dialing to `*#*#4636#*#*` > Wifi Information > Wifi Status (> Refresh)**

**References** : [Checking Device IP](http://android.stackexchange.com/questions/2984/how-can-i-see-what-ip-address-my-android-phone-has), [Remote Logging](http://forum.dev.dji.com/thread-32434-1-1.html)

## README from [Mobile-SDK-Android](https://github.com/dji-sdk/Mobile-SDK-Android)

### Development Workflow 

From registering as a developer, to deploying an application, the following will take you through the full Mobile SDK Application development process:

- [Prerequisites](https://developer.dji.com/mobile-sdk/documentation/application-development-workflow/workflow-prerequisits.html)
- [Register as DJI Developer & Download SDK](https://developer.dji.com/mobile-sdk/documentation/application-development-workflow/workflow-register.html)
- [Integrate SDK into Application](https://developer.dji.com/mobile-sdk/documentation/application-development-workflow/workflow-integrate.html)
- [Run Application](https://developer.dji.com/mobile-sdk/documentation/application-development-workflow/workflow-run.html)
- [Testing, Profiling & Debugging](https://developer.dji.com/mobile-sdk/documentation/application-development-workflow/workflow-testing.html)
- [Deploy](https://developer.dji.com/mobile-sdk/documentation/application-development-workflow/workflow-deploy.html)

### Sample Projects & Tutorials

Several Android tutorials are provided as examples on how to use different features of the Mobile SDK and debug tools includes:

- [Running DJI SDK Sample Code in Android Studio](https://developer.dji.com/mobile-sdk/documentation/android-tutorials/index.html)
- [Camera Application](https://developer.dji.com/mobile-sdk/documentation/android-tutorials/FPVDemo.html)
- [MapView and Waypoint Application (GaodeMap)](https://developer.dji.com/mobile-sdk/documentation/android-tutorials/GSDemo-Gaode-Map.html)
- [MapView and Waypoint Application (GoogleMap)](https://developer.dji.com/mobile-sdk/documentation/android-tutorials/GSDemo-Google-Map.html)
- [TapFly and ActiveTrack Application](https://developer.dji.com/mobile-sdk/documentation/android-tutorials/P4MissionsDemo.html)

### Learn More about DJI Products and the Mobile SDK

Please visit [DJI Mobile SDK Documentation](https://developer.dji.com/mobile-sdk/documentation/introduction/index.html) for more details.

### SDK API Reference

[**Android SDK API Documentation**](https://developer.dji.com/iframe/mobile-sdk-doc/android/reference/packages.html)

### Support

You can get support from DJI with the following methods:

- [**DJI Forum**](http://forum.dev.dji.com/en)
- Post questions in [**Stackoverflow**](http://stackoverflow.com) using [**dji-sdk**](http://stackoverflow.com/questions/tagged/dji-sdk) tag
- dev@dji.com
