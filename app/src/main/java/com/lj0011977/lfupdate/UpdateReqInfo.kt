package com.lj0011977.lfupdate

import android.content.Context

/**
 * @author : created by archerLj
 * date: 2019/4/4 10
 * usage:
 */
class UpdateReqInfo(context: Context) {
    var devId: String? = null
    var versionCode: Int? = null

    init {
        devId = DeviceUtil.getDeviceId(context)
        versionCode = DeviceUtil.getVersionCode(context)
    }
}