package com.lj0011977.update

import com.lj0011977.update.interfaces.ICheckAgent
import com.lj0011977.update.interfaces.IUpdateChecker
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * @author : created by archerLj
 * date: 2019/4/4 10
 * usage:
 */
class UpdateChecker(var mPostData: ByteArray? = null): IUpdateChecker {

    override fun check(agent: ICheckAgent, url: String) {
        var connection: HttpURLConnection? = null
        try {
            connection = URL(url).openConnection() as HttpURLConnection
            connection.setRequestProperty("Accept", "application/json")

            mPostData?:let {
                connection.requestMethod = "GET"
                connection.connect()
            }
            mPostData?.let {
                connection.requestMethod = "POST"
                connection.doOutput = true // 设置是否向httpUrlConnection输出，应为这个是post请求，参数要放在http正文内，所以设为true，默认是false
                connection.instanceFollowRedirects = false
                connection.useCaches = false // post请求不能使用缓存
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Content-Length", Integer.toString(it.size))
                connection.outputStream.write(mPostData) // getOutputStream会默认调用connect()方法 ， write()向对象输出流写出数据
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                // connection.getInputStream() 获取响应
                agent.setInfo(UpdateUtil.readString(connection.inputStream))
            } else {
                agent.setError(UpdateError(UpdateError.CHECK_HTTP_STATUS, "" + connection.responseCode))
            }
        } catch (e: IOException) {
            e.printStackTrace()
            agent.setError(UpdateError(UpdateError.CHECK_NETWORK_IO))
        } finally {
            connection?.disconnect()
        }
    }
}