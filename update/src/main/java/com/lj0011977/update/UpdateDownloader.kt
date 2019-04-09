package com.lj0011977.update

import android.content.Context
import android.os.AsyncTask
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.lj0011977.update.interfaces.IDownloadAgent
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

/**
 * @author : created by archerLj
 * date: 2019/4/3 15
 * usage:
 */
class UpdateDownloader(var mAgent: IDownloadAgent, var mContext: Context, var mUrl: String, var mTemp: File)
    : AsyncTask<Void, Int, Long>() {

    ////////////////////////////////////////////////////////////////////////
    //     ---  variables
    ////////////////////////////////////////////////////////////////////////
    private var mBytesLoaded: Long = 0
    private var mBytesTotal: Long = 0
    private var mBytesTemp: Long = 0
    private var mTimeBegin: Long = 0
    private var mTimeUsed: Long = 1
    private var mTimeLast: Long = 0
    private var mSpeed: Long = 0

    private var mConnection: HttpURLConnection? = null


    ////////////////////////////////////////////////////////////////////////
    //     ---
    ////////////////////////////////////////////////////////////////////////
    init {
        mTemp?.let {
            if (it.exists()) {
                mBytesTemp = it.length()
            }
        }
    }

    companion object {

        private val TIME_OUT = 30000
        private val BUFFER_SIZE = 1024 * 100

        private val EVENT_START = 1
        private val EVENT_PROGRESS = 2
        private val EVENT_COMPLETE = 3

        fun getAvaliableStorage(): Long {
            val stat = StatFs(Environment.getExternalStorageDirectory().toString())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                return stat.availableBlocksLong * stat.blockSizeLong
            } else {
                return (stat.availableBlocks * stat.blockSize).toLong()
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////
    //     ---
    ////////////////////////////////////////////////////////////////////////

    fun getBytesLoaded(): Long {
        return mBytesLoaded + mBytesTemp
    }

    override fun doInBackground(vararg params: Void): Long? {
        mTimeBegin = System.currentTimeMillis()
        try {
            val result = download()
            if (isCancelled) {
                mAgent.setError(UpdateError(UpdateError.DOWNLOAD_CANCELLED))
            } else if (result == -1.toLong()) {
                mAgent.setError(UpdateError(UpdateError.DOWNLOAD_UNKNOWN))
            } else if (!UpdateUtil.verify(mTemp, mTemp.name)) {
                mAgent.setError(UpdateError(UpdateError.DOWNLOAD_VERIFY))
            }
        } catch (e: UpdateError) {
            mAgent.setError(e)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            mAgent.setError(UpdateError(UpdateError.DOWNLOAD_DISK_IO))
        } catch (e: IOException) {
            e.printStackTrace()
            mAgent.setError(UpdateError(UpdateError.DOWNLOAD_NETWORK_IO))
        } finally {
            mConnection?.let {
                it.disconnect()
            }
        }
        return null
    }

    override fun onProgressUpdate(vararg values: Int?) {
        values?.let {
            when (it[0]) {
                EVENT_START -> mAgent.onStart()
                EVENT_PROGRESS -> {
                    val now = System.currentTimeMillis()
                    if (now - mTimeLast >= 900) {
                        mTimeLast = now
                        mTimeUsed = now - mTimeBegin
                        mSpeed = mBytesLoaded * 1000 / mTimeUsed
                        mAgent.onProgress((this.getBytesLoaded() * 100 / mBytesTotal).toInt())
                    }
                }
            }
        }
    }

    override fun onPostExecute(result: Long?) {
        mAgent.onFinish()
    }


    @Throws(IOException::class)
    private fun create(url: URL): HttpURLConnection {
        val connection = url.openConnection() as HttpURLConnection
        connection.setRequestProperty("Accept", "application/*")
        connection.connectTimeout = 10000
        return connection
    }


    @Throws(IOException::class, UpdateError::class)
    private fun download(): Long {

        publishProgress(EVENT_START) // 在doInBackground()执行中调用，通知UI线程去更新，每次调用都会触发onProgressUpdate()
        checkNetwork()

        mConnection = create(URL(mUrl))
//        mConnection?.let {
            mConnection!!.connect()
            checkStatus()

            mBytesTotal = mConnection!!.getContentLength().toLong()
            checkSpace(mBytesTemp, mBytesTotal) // 检查可用空间是否够


            if (mBytesTemp == mBytesTotal) {
                publishProgress(EVENT_START)
                return 0
            }

            // 续传
            if (mBytesTemp > 0) {

                mConnection!!.disconnect()
                mConnection = create(mConnection!!.getURL())
                mConnection?.let {
                    it.addRequestProperty("Range", "bytes=$mBytesTemp-")
                    it.connect()

                    checkStatus()
                }
            }

            val bytesCopied = copy(mConnection!!.getInputStream(), LoadingRandomAccessFile(mTemp))

            if (isCancelled) {
            } else if (mBytesTemp + bytesCopied != mBytesTotal && mBytesTotal != -1.toLong()) {
                throw UpdateError(UpdateError.DOWNLOAD_INCOMPLETE)
            }

            return bytesCopied.toLong()
//        }

//        return 0
    }

    @Throws(IOException::class, UpdateError::class)
    private fun copy(input: InputStream, out: RandomAccessFile): Int {

        val buffer = ByteArray(BUFFER_SIZE)
        val bis = BufferedInputStream(input, BUFFER_SIZE)
        try {

            out.seek(out.length())

            var bytes = 0
            var previousBlockTime: Long = -1

            while (!isCancelled) {
                val n = bis.read(buffer, 0, BUFFER_SIZE)
                if (n == -1) {
                    break
                }
                out.write(buffer, 0, n)
                bytes += n

                checkNetwork()

                if (mSpeed != 0L) {
                    previousBlockTime = -1
                } else if (previousBlockTime == -1.toLong()) {
                    previousBlockTime = System.currentTimeMillis()
                } else if (System.currentTimeMillis() - previousBlockTime > TIME_OUT) {
                    throw UpdateError(UpdateError.DOWNLOAD_NETWORK_TIMEOUT)
                }
            }
            return bytes
        } finally {
            out.close()
            bis.close()
            input.close()
        }
    }

    /**
     * RandomAccessFile是Java输入/输出流体系中功能最丰富的文件内容访问类，既可以读取文件内容，也可以向文件输出。和普通的输入/输出流不同的是，RandomAccessFile支持跳到文件的任意
     * 位置读写数据，RandomAccessFile对象包含一个记录指针，用以标识当前读写处的位置，当程序创建一个新的RandomAccessFile对象时，该对象的文件记录指针对于文件头（也就是0处），当读写
     * n个字节后，文件记录指针将会向后移动n个字节。除此之外，RandomAccessFile可以自由移动该记录指针
     *
     * RandomAccessFile包含两个方法来操作文件记录指针：
     *
     * long getFilePointer()：返回文件记录指针的当前位置
     * void seek(long pos)：将文件记录指针定位到pos位置
     * RandomAccessFile类在创建对象时，除了指定文件本身，还需要指定一个mode参数，该参数指定RandomAccessFile的访问模式，该参数有如下四个值：
     *
     * r：以只读方式打开指定文件。如果试图对该RandomAccessFile指定的文件执行写入方法则会抛出IOException
     * rw：以读取、写入方式打开指定文件。如果该文件不存在，则尝试创建文件
     * rws：以读取、写入方式打开指定文件。相对于rw模式，还要求对文件的内容或元数据的每个更新都同步写入到底层存储设备，默认情形下(rw模式下),是使用buffer的,只有cache满的或者使用
     * RandomAccessFile.close()关闭流的时候儿才真正的写到文件
     * rwd：与rws类似，只是仅对文件的内容同步更新到磁盘，而不修改文件的元数据
     */
    private inner class LoadingRandomAccessFile @Throws(FileNotFoundException::class)
    constructor(file: File) : RandomAccessFile(file, "rw") {

        @Throws(IOException::class)
        override fun write(buffer: ByteArray, offset: Int, count: Int) {

            super.write(buffer, offset, count)
            mBytesLoaded += count.toLong()
            publishProgress(EVENT_PROGRESS)
        }
    }

    /**
     * 检查网络
     */
    @Throws(UpdateError::class)
    fun checkNetwork() {
        if (!UpdateUtil.checkNetwork(mContext)) {
            throw UpdateError(UpdateError.DOWNLOAD_NETWORK_BLOCKED)
        }
    }

    fun checkStatus() {
        val statusCode = mConnection?.responseCode
        if (statusCode != 200 && statusCode != 206) {
            throw UpdateError(UpdateError.DOWNLOAD_HTTP_STATUS, "$statusCode")
        }
    }

    fun checkSpace(loaded: Long, total: Long) {
        val storage = getAvaliableStorage()
        if ((total - loaded) > storage) {
            throw UpdateError(UpdateError.DOWNLOAD_DISK_NO_SPACE)
        }
    }
}