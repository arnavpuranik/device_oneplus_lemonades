/*
* Copyright (C) 2016 The OmniROM Project
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*
*/
package org.aosp.device.OnePlusLab;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandle;
import android.os.VibrationEffect;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.widget.Toast;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import org.aosp.device.OnePlusLab.ModeSwitch.DCModeSwitch;
import org.aosp.device.OnePlusLab.ModeSwitch.HBMModeSwitch;
import org.aosp.device.OnePlusLab.Preferences.CustomSeekBarPreference;
import org.aosp.device.OnePlusLab.Preferences.SwitchPreference;
import org.aosp.device.OnePlusLab.Preferences.VibratorStrengthPreference;
import org.aosp.device.OnePlusLab.Services.HBMModeService;
import org.aosp.device.OnePlusLab.Services.VolumeService;
import org.aosp.device.OnePlusLab.Utils.FileUtils;
import org.aosp.device.OnePlusLab.Utils.Protocol;
import org.aosp.device.OnePlusLab.Utils.Utils;
import org.aosp.device.OnePlusLab.Utils.VibrationUtils;
import org.aosp.device.OnePlusLab.doze.DozeSettingsActivity;

import com.qualcomm.qcrilmsgtunnel.IQcrilMsgTunnel;

public class OnePlusLab extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    public static final String CATEGORY_DISPLAY = "display";
    public static final String KEY_DC_SWITCH = "dc_dim";
    public static final String KEY_HBM_SWITCH = "hbm";
    public static final String KEY_AUTO_HBM_SWITCH = "auto_hbm";
    public static final String KEY_AUTO_HBM_THRESHOLD = "auto_hbm_threshold";
    public static final String KEY_MUTE_MEDIA = "mute_media";
    public static final String KEY_NR_MODE_SWITCHER = "nr_mode_switcher";
    public static final String KEY_VIBSTRENGTH = "vib_strength";
    public static final String KEY_TOUCHPANEL = "touchpanel";

    private static final String PREF_DOZE = "advanced_doze_settings";

    private static final String FILE_LEVEL = "/sys/devices/platform/soc/a8c000.i2c/i2c-3/3-005a/leds/vibrator/level";
    public static final String DEFAULT = "3";

    private static ListPreference mNrModeSwitcher;
    private static Preference mDozeSettings;
    private static SwitchPreference mDCModeSwitch;
    private static SwitchPreference mHBMModeSwitch;
    private static SwitchPreference mAutoHBMSwitch;
    private static SwitchPreference mMuteMedia;

    private static CustomSeekBarPreference mFpsInfoTextSizePreference;
    private static VibratorStrengthPreference mVibratorStrengthPreference;

    private Protocol mProtocol;
    private Runnable unbindService;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        addPreferencesFromResource(R.xml.main);

        mDCModeSwitch = (SwitchPreference) findPreference(KEY_DC_SWITCH);
        mDCModeSwitch.setEnabled(DCModeSwitch.isSupported());
        mDCModeSwitch.setChecked(DCModeSwitch.isCurrentlyEnabled(getContext()));
        mDCModeSwitch.setOnPreferenceChangeListener(new DCModeSwitch());

        Intent mIntent = new Intent();
        mIntent.setClassName("com.qualcomm.qcrilmsgtunnel", "com.qualcomm.qcrilmsgtunnel.QcrilMsgTunnelService");
        getContext().bindServiceAsUser(mIntent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                IQcrilMsgTunnel tunnel = IQcrilMsgTunnel.Stub.asInterface(service);
                if (tunnel != null)
                    mProtocol = new Protocol(tunnel);

                ServiceConnection serviceConnection = this;

                unbindService = () -> getContext().unbindService(serviceConnection);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mProtocol = null;
            }
        }, getContext().BIND_AUTO_CREATE, UserHandle.CURRENT);

        mHBMModeSwitch = (SwitchPreference) findPreference(KEY_HBM_SWITCH);
        mHBMModeSwitch.setEnabled(HBMModeSwitch.isSupported());
        mHBMModeSwitch.setChecked(PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(OnePlusLab.KEY_HBM_SWITCH, false));
        mHBMModeSwitch.setOnPreferenceChangeListener(this);

        mAutoHBMSwitch = (SwitchPreference) findPreference(KEY_AUTO_HBM_SWITCH);
        mAutoHBMSwitch.setChecked(PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(OnePlusLab.KEY_AUTO_HBM_SWITCH, false));
        mAutoHBMSwitch.setOnPreferenceChangeListener(this);

        mDozeSettings = (Preference) findPreference(PREF_DOZE);
        mDozeSettings.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity().getApplicationContext(), DozeSettingsActivity.class);
            startActivity(intent);
            return true;
        });

        mMuteMedia = (SwitchPreference) findPreference(KEY_MUTE_MEDIA);
        mMuteMedia.setChecked(PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(OnePlusLab.KEY_MUTE_MEDIA, false));
        mMuteMedia.setOnPreferenceChangeListener(this);

        mNrModeSwitcher = (ListPreference) findPreference(KEY_NR_MODE_SWITCHER);
        mNrModeSwitcher.setOnPreferenceChangeListener(this);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        mVibratorStrengthPreference =  (VibratorStrengthPreference) findPreference(KEY_VIBSTRENGTH);
        if (FileUtils.isFileWritable(FILE_LEVEL)) {
            mVibratorStrengthPreference.setValue(sharedPrefs.getInt(KEY_VIBSTRENGTH,
                Integer.parseInt(FileUtils.getFileValue(FILE_LEVEL, DEFAULT))));
            mVibratorStrengthPreference.setOnPreferenceChangeListener(this);
        } else {
            mVibratorStrengthPreference.setEnabled(false);
            mVibratorStrengthPreference.setSummary(getString(R.string.unsupported_feature));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (unbindService != null) {
            unbindService.run();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        mHBMModeSwitch.setChecked(HBMModeSwitch.isCurrentlyEnabled(getContext()));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
          if (preference == mAutoHBMSwitch) {
            Boolean enabled = (Boolean) newValue;
            SharedPreferences.Editor prefChange = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
            prefChange.putBoolean(KEY_AUTO_HBM_SWITCH, enabled).commit();
            Utils.enableService(getContext());
        } else if (preference == mHBMModeSwitch) {
            Boolean enabled = (Boolean) newValue;
            FileUtils.writeLine(HBMModeSwitch.getFile(), enabled ? "5" : "0");
            Intent hbmIntent = new Intent(getContext(), HBMModeService.class);
            if (enabled) {
                getContext().startServiceAsUser(hbmIntent, UserHandle.CURRENT);
            } else {
                getContext().stopServiceAsUser(hbmIntent, UserHandle.CURRENT);
            }
        } else if (preference == mMuteMedia) {
            Boolean enabled = (Boolean) newValue;
            VolumeService.setEnabled(getContext(), enabled);
        } else if (preference == mNrModeSwitcher) {
            int mode = Integer.parseInt(newValue.toString());
            return setNrModeChecked(mode);
        } else if (preference == mVibratorStrengthPreference) {
            int value = Integer.parseInt(newValue.toString());
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            sharedPrefs.edit().putInt(KEY_VIBSTRENGTH, value).commit();
            FileUtils.writeLine(FILE_LEVEL, String.valueOf(value));
            VibrationUtils.doHapticFeedback(getContext(), VibrationEffect.EFFECT_CLICK, true);
        }
        return true;
    }

    public static boolean isHBMModeService(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(OnePlusLab.KEY_HBM_SWITCH, false);
    }

    public static boolean isAUTOHBMEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(OnePlusLab.KEY_AUTO_HBM_SWITCH, false);
    }

    public static void restoreVibStrengthSetting(Context context) {
        if (FileUtils.isFileWritable(FILE_LEVEL)) {
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            int value = sharedPrefs.getInt(KEY_VIBSTRENGTH,
                    Integer.parseInt(FileUtils.getFileValue(FILE_LEVEL, DEFAULT)));
            FileUtils.writeLine(FILE_LEVEL, String.valueOf(value));
        }
    }

    private boolean setNrModeChecked(int mode) {
        if (mode == 0) {
            return setNrModeChecked(Protocol.NR_5G_DISABLE_MODE_TYPE.NAS_NR5G_DISABLE_MODE_SA);
        } else if (mode == 1) {
            return setNrModeChecked(Protocol.NR_5G_DISABLE_MODE_TYPE.NAS_NR5G_DISABLE_MODE_NSA);
        } else {
            return setNrModeChecked(Protocol.NR_5G_DISABLE_MODE_TYPE.NAS_NR5G_DISABLE_MODE_NONE);
        }
    }

    private boolean setNrModeChecked(Protocol.NR_5G_DISABLE_MODE_TYPE mode) {
        if (mProtocol == null) {
            Toast.makeText(getContext(), R.string.service_not_ready, Toast.LENGTH_LONG).show();
            return false;
        }
        int index = SubscriptionManager.getSlotIndex(SubscriptionManager.getDefaultDataSubscriptionId());
        if (index == SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
            Toast.makeText(getContext(), R.string.unavailable_sim_slot, Toast.LENGTH_LONG).show();
            return false;
        }
        new Thread(() -> mProtocol.setNrMode(index, mode)).start();
        return true;
    }
}
