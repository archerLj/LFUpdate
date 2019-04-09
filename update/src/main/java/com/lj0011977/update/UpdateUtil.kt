package com.lj0011977.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.support.v4.content.FileProvider
import android.text.TextUtils
import java.io.*
import java.math.BigInteger
import java.security.MessageDigest

/**
 * @author : created by archerLj
 * date: 2019/4/2 17
 * usage:
 */
class UpdateUtil {
    companion object {

        private val TAG = "com.mhd"
        private val PREFS = "com.mhd.prefs"
        private val KEY_IGNORE = "com.mhd.prefs.ignore"
        private val KEY_UPDATE = "com.mhd.prefs.update"

        fun install(context: Context, file: File, force: Boolean) {
            val intent = Intent(Intent.ACTION_VIEW)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
            } else {
                val uri = FileProvider.getUriForFile(context, context.packageName + ".updatefileprovider", file)
                intent.setDataAndType(uri, "application/vnd.android.package-archive")
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            if (force) {
                System.exit(0)
            }
        }

        fun toFormatUrl(context: Context, url: String, channel: String): String {
            val builder = StringBuilder()
            builder.append(url)
            builder.append(if (url.indexOf("?") < 0) "?" else "&")
            builder.append("package=")
            builder.append(context.packageName)
            builder.append("&version=")
            builder.append(getVersionCode(context))
            builder.append("&channel=")
            builder.append(channel)
            return builder.toString()
        }

        /**
         * 获取版本号
         * */
        fun getVersionCode(context: Context): Int {
            try {
                val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_META_DATA)
                return info.versionCode
            } catch (e: Exception) {
                return 0
            }
        }

        /**
         * 确保缓存目录存在
         * */
        fun ensureExternalCacheDir(context: Context) {
            var file = context.externalCacheDir
            if (file == null) {
                file = File(context.filesDir.parentFile, "cache")
            }
            if (file != null) {
                file.mkdirs()
            }
        }

        fun isIgnore(context: Context, md5: String): Boolean {
            return !TextUtils.isEmpty(md5) && md5.equals(context.getSharedPreferences(PREFS, 0).getString(KEY_IGNORE, ""))
        }

        fun setUpdate(context: Context, md5: String) {
            if (TextUtils.isEmpty(md5)) {
                return
            }
            val sp = context.getSharedPreferences(PREFS, 0)
            val old = sp.getString(KEY_UPDATE, "")
            if (md5.equals(old)) {
                return
            }
            val oldFile = File(context.externalCacheDir, old)
            if (oldFile.exists()) {
                oldFile.delete()
            }
            sp.edit().putString(KEY_UPDATE, md5).apply()
            val file = File(context.externalCacheDir, md5)
            if (!file.exists()) {
                file.createNewFile()
            }
        }

        fun md5(file: File): String {
            var digest: MessageDigest? = null
            var fis: FileInputStream? = null
            val buffer = ByteArray(1024)

            try {
                if (!file.isFile) {
                    return ""
                }

                digest = MessageDigest.getInstance("MD5")
                fis = FileInputStream(file)

                while (true) {
                    val len = fis.read(buffer, 0, 1024)
                    if (len == -1) {
                        fis.close()
                        break
                    }

                    digest!!.update(buffer, 0, len)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return ""
            }


            val var5 = BigInteger(1, digest!!.digest())
            return String.format("%1$032x", *arrayOf<Any>(var5))
        }

        fun verify(apk: File, md5: String): Boolean {
            if (!apk.exists()) {
                return false
            }
            val _md5 = md5(apk)
            if (TextUtils.isEmpty(_md5)) {
                return false
            }

            val result = _md5.equals(md5, true)
            if (!result) {
                apk.delete()
            }
            return result
        }

        /**
         * 将InputStream中的数据转换成String
         */
        @Throws(IOException::class)
        fun readString(input: InputStream): String {
            val output = ByteArrayOutputStream()
            try {
                val buffer = ByteArray(4096)
                var n = 0
                while (-1 != n) {
                    output.write(buffer, 0, n)
                    n = input.read(buffer)
                }
                output.flush()
            } finally {
                close(input)
                close(output)
            }
            return output.toString("UTF-8")
        }

        fun close(closeable: Closeable?) {
            if (closeable != null) {
                try {
                    closeable.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        /**
         * 检查wifi
         */
        fun checkWifi(context: Context): Boolean {
            val connectivity =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager ?: return false
            val info = connectivity.activeNetworkInfo
            return info != null && info.isConnected && info.type == ConnectivityManager.TYPE_WIFI
        }

        /**
         * 检查网络连接
         */
        fun checkNetwork(context: Context): Boolean {
            val connectivity =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager ?: return false
            val info = connectivity.activeNetworkInfo
            return info != null && info.isConnected
        }

        fun setIgnore(context: Context, md5: String) {
            context.getSharedPreferences(PREFS, 0).edit().putString(KEY_IGNORE, md5).apply()
        }
    }
}