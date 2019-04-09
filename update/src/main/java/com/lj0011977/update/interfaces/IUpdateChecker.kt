package com.lj0011977.update.interfaces

/**
 * @author : created by archerLj
 * date: 2019/4/3 09
 * usage:
 */
interface IUpdateChecker {

    fun check(agent: ICheckAgent, url: String)
}