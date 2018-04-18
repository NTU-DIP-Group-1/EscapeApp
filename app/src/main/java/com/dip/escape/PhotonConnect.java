package com.dip.escape;

import android.content.Context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;

/**
 * Created by irynashvydchenko on 2018-03-20.
 */

public class PhotonConnect {

    public static final String EMAIL = "email@email.com";
    public static final String PASSWORD = "password";
    public static final String DEVICE_ID = "xxx";

    private static ParticleDevice mCurrDevice;

    public static void authenticate(Context ctx) {
        if (mCurrDevice == null) {
        ParticleDeviceSetupLibrary.init(ctx, ColorPuzzleActivity.class);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ParticleCloudSDK.getCloud().logIn(EMAIL, PASSWORD);
                        mCurrDevice = ParticleCloudSDK.getCloud().getDevice(DEVICE_ID);
                    } catch (ParticleCloudException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    public static void toggleServo(final int servo) {
        if (mCurrDevice == null) {
            return;
        }
        new Thread() {
            @Override
            public void run() {
                List<String> args = new ArrayList<String>();

                args.add(Integer.toString(servo));

                try {
                    mCurrDevice.callFunction("servo", args);
                } catch (ParticleCloudException | ParticleDevice.FunctionDoesNotExistException | IOException e) {
                    e.printStackTrace();
                }

            }
        }.start();


    }
}
