package com.lj0011977.update.interfaces

import java.io.File

/**
 * @author : created by archerLj
 * date: 2019/4/3 10
 * usage:
 */
interface IUpdateDownloader {

    fun download(agent: IDownloadAgent, url: String, temp: File)
}