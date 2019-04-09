package com.lj0011977.update.interfaces

import com.lj0011977.update.UpdateError
import com.lj0011977.update.UpdateInfo

/**
 * @author : created by archerLj
 * date: 2019/4/3 09
 * usage:
 */
interface IDownloadAgent: OnDownloadListener {

    fun getInfo(): UpdateInfo?

    fun setError(error: UpdateError)
}