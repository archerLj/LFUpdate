package com.lj0011977.update.interfaces

import com.lj0011977.update.UpdateError

/**
 * @author : created by archerLj
 * date: 2019/4/3 09
 * usage:
 */
interface ICheckAgent {

    fun setInfo(info: String)

    fun setError(error: UpdateError)
}