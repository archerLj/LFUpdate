package com.lj0011977.update.interfaces

/**
 * @author : created by archerLj
 * date: 2019/4/3 08
 * usage:
 */
interface OnDownloadListener {

    fun onStart()

    fun onProgress(progress: Int)

    fun onFinish()
}