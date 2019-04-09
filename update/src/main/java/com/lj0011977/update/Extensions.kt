package com.lj0011977.update

/**
 * @author : created by archerLj
 * date: 2019/4/3 10
 * usage:
 */

fun String?.isEmpty(): Boolean {
    this?.let {
        if (this.trim().equals("")) {
            return true
        }
        return false
    }
    return true
}