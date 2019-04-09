package com.lj0011977.update

import org.json.JSONObject

/**
 * @author : created by archerLj
 * date: 2019/4/3 08
 * usage:
 */
data class UpdateInfo(var hasUpdate: Boolean = false, // 是否有新版本
                      var isSilent: Boolean = false, // 是否静默下载：有新版本时不提示直接下载
                      var isForce: Boolean = false, // 是否强制安装：不安装无法使用app
                      var isAutoInstall: Boolean = false, // 是否下载完成后自动安装
                      var isIgnorable: Boolean = false, // 是否可忽略该版本
                      var versionCode: Int = 0,
                      var versionName: String = "",
                      var updateContent: String = "",
                      var url: String = "",
                      var md5: String = "",
                      var size: Long = 0) {

    companion object {
        fun parse(s: String): UpdateInfo {
            var o = JSONObject(s)
            return parse(if (o.has("data")) o.getJSONObject("data") else o)
        }

        private fun parse(o: JSONObject): UpdateInfo {
            var info = UpdateInfo()
            if (o == null) {
                return info
            }

            info.hasUpdate = o.optBoolean("hasUpdate", false)
            info.isSilent = o.optBoolean("isSilent", false)
            info.isForce = o.optBoolean("isForce", false)
            info.isAutoInstall = o.optBoolean("isAutoInstall", !info.isSilent)
            info.isIgnorable = o.optBoolean("isIgnorable", true)

            info.versionCode = o.optInt("versionCode", 0)
            info.versionName = o.optString("versionName", "")
            info.updateContent = o.optString("updateContent", "")

            info.url = o.optString("url", "")
            info.md5 = o.optString("md5", "")
            info.size = o.optLong("size", 0)

            return info
        }
    }
}