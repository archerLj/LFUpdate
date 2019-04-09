package com.lj0011977.update

import android.util.SparseArray

/**
 * @author : created by archerLj
 * date: 2019/4/3 09
 * usage:
 */
class UpdateError(var code: Int, message: String?): Throwable(make(code, message)) {
    companion object {
        val UPDATE_IGNORED = 1001
        val UPDATE_NO_NEWER = 2007
        val CHECK_UNKNOWN = 2001
        val CHECK_NO_WIFI = 2002
        val CHECK_NO_NETWORK = 2003
        val CHECK_NETWORK_IO = 2004
        val CHECK_HTTP_STATUS = 2005
        val CHECK_PARSE = 2006
        val DOWNLOAD_UNKNOWN = 3001
        val DOWNLOAD_CANCELLED = 3002
        val DOWNLOAD_DISK_NO_SPACE = 3003
        val DOWNLOAD_DISK_IO = 3004
        val DOWNLOAD_NETWORK_IO = 3005
        val DOWNLOAD_NETWORK_BLOCKED = 3006
        val DOWNLOAD_NETWORK_TIMEOUT = 3007
        val DOWNLOAD_HTTP_STATUS = 3008
        val DOWNLOAD_INCOMPLETE = 3009
        val DOWNLOAD_VERIFY = 3010
        val messages = SparseArray<String>()

        init {
            messages.append(UPDATE_IGNORED, "该版本已经忽略")
            messages.append(UPDATE_NO_NEWER, "已经是最新版了")

            messages.append(CHECK_UNKNOWN, "查询更新失败：未知错误")
            messages.append(CHECK_NO_WIFI, "查询更新失败：没有 WIFI")
            messages.append(CHECK_NO_NETWORK, "查询更新失败：没有网络")
            messages.append(CHECK_NETWORK_IO, "查询更新失败：网络异常")
            messages.append(CHECK_HTTP_STATUS, "查询更新失败：错误的HTTP状态")
            messages.append(CHECK_PARSE, "查询更新失败：解析错误")

            messages.append(DOWNLOAD_UNKNOWN, "下载失败：未知错误")
            messages.append(DOWNLOAD_CANCELLED, "下载失败：下载被取消")
            messages.append(DOWNLOAD_DISK_NO_SPACE, "下载失败：磁盘空间不足")
            messages.append(DOWNLOAD_DISK_IO, "下载失败：磁盘读写错误")
            messages.append(DOWNLOAD_NETWORK_IO, "下载失败：网络异常")
            messages.append(DOWNLOAD_NETWORK_BLOCKED, "下载失败：网络中断")
            messages.append(DOWNLOAD_NETWORK_TIMEOUT, "下载失败：网络超时")
            messages.append(DOWNLOAD_HTTP_STATUS, "下载失败：错误的HTTP状态")
            messages.append(DOWNLOAD_INCOMPLETE, "下载失败：下载不完整")
            messages.append(DOWNLOAD_VERIFY, "下载失败：校验错误")
        }

        fun make(code: Int, message: String?): String? {
            val m = messages.get(code)
            m ?: return message
            message ?: return m
            return "($m$message)"
        }
    }

    constructor(code: Int): this(code, null)
    fun isError() = code >= 2000
    override fun toString(): String = if (isError()) "[$code $message]" else message ?: ""
}