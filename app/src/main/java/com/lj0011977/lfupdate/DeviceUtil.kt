package com.lj0011977.lfupdate

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi
import android.telephony.TelephonyManager

/**
 * @author : created by archerLj
 * date: 2019/4/4 14
 * usage:
 */
class DeviceUtil {
    companion object {
        @RequiresApi(Build.VERSION_CODES.O)
        @SuppressLint("MissingPermission")
        fun getDeviceId(context: Context): String {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            return telephonyManager.imei
        }

        fun getVersionCode(context: Context): Int {
            return context.packageManager.getPackageInfo(context.packageName, 0).versionCode
        }
    }
}