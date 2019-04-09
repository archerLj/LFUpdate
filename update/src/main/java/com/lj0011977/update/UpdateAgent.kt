package com.lj0011977.update

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.DialogInterface
import android.os.AsyncTask
import android.support.v4.app.NotificationCompat
import android.text.format.Formatter
import android.text.method.ScrollingMovementMethod
import android.widget.TextView
import android.widget.Toast
import com.lj0011977.update.interfaces.*
import com.lj0011977.update.listener.DefaultDownloadListener
import com.lj0011977.update.listener.DefaultPromptClickListener
import java.io.File

/**
 * @author : created by archerLj
 * date: 2019/4/3 10
 * usage:
 */
class UpdateAgent(var mContext: Context, var mUrl: String, var mIsManual: Boolean, var mIsWifiOnly: Boolean, notifyId: Int)
    : ICheckAgent, IUpdateAgent, IDownloadAgent {

    private var mTmpFile: File? = null
    private var mApkFile: File? = null

    private var mInfo: UpdateInfo? = null
    private var mError: UpdateError? = null

    private var mParser: IUpdateParser = DefaultUpdateParser()
    private var mChecker: IUpdateChecker? = null
    private var mDownloader: IUpdateDownloader? = null
    private var mPrompter: IUpdatePrompter? = null

    private var mOnFailureListener: OnFailureListener? = null

    private var mOnDownloadListener: OnDownloadListener? = null
    private var mOnNotificationDownloadListener: OnDownloadListener? = null

    init {
        mDownloader = DefaultUpdateDownloader(mContext)
        mPrompter = DefaultUpdatePrompter(mContext)
        mOnFailureListener = DefaultFailureListener(mContext)
        mOnDownloadListener = DefaultDialogDownloadListener(mContext)
        if (notifyId > 0) {
            mOnNotificationDownloadListener = DefaultNotificationDownloadListener(mContext, notifyId)
        } else {
            mOnNotificationDownloadListener = DefaultDownloadListener()
        }
    }

    fun setParser(parser: IUpdateParser) {
        mParser = parser
    }

    fun setChecker(checker: IUpdateChecker) {
        mChecker = checker
    }

    fun setDownloader(downloader: IUpdateDownloader) {
        mDownloader = downloader
    }

    fun setPrompter(prompter: IUpdatePrompter) {
        mPrompter = prompter
    }

    fun setPrompterClickListener(listener: DialogInterface.OnClickListener) {
        if (mPrompter is DefaultUpdatePrompter) {
            (mPrompter as DefaultUpdatePrompter).setOnClickListener(listener)
        }
    }

    fun setOnNotificationDownloadListener(listener: OnDownloadListener) {
        mOnNotificationDownloadListener = listener
    }

    fun setOnDownloadListener(listener: OnDownloadListener) {
        mOnDownloadListener = listener
    }

    fun setOnFailureListener(listener: OnFailureListener) {
        mOnFailureListener = listener
    }


    fun setInfo(info: UpdateInfo) {
        mInfo = info
    }


    ////////////////////////////////////////////////////////////////////////
    //     ---  private method
    ////////////////////////////////////////////////////////////////////////
    fun check() {
        if (mIsWifiOnly) {
            if (UpdateUtil.checkWifi(mContext)) {
                doCheck()
            } else {
                doFailture(UpdateError(UpdateError.CHECK_NO_WIFI))
            }
        } else {
            if (UpdateUtil.checkNetwork(mContext)) {
                doCheck()
            } else {
                doFailture(UpdateError(UpdateError.CHECK_NO_NETWORK))
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    fun doCheck() {
        object : AsyncTask<String, Void, Void>() {
            override fun doInBackground(vararg params: String?): Void? {
                if (mChecker == null) {
                    mChecker = UpdateChecker()
                }
                mChecker?.check(this@UpdateAgent, mUrl)
                return null
            }

            override fun onPostExecute(result: Void?) {
                doCheckFinish()
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    fun doCheckFinish() {
        var error = mError
        error?.let {
            doFailture(it)
        }
        error?:let {
            val info = getInfo()
            info?:let {
                doFailture(UpdateError(UpdateError.CHECK_UNKNOWN))
            }
            info?.let {
                if (it.versionCode <= UpdateUtil.getVersionCode(mContext)) {
                    doFailture(UpdateError(UpdateError.UPDATE_NO_NEWER))
                } else if (UpdateUtil.isIgnore(mContext, it.md5)) {
                    doFailture(UpdateError(UpdateError.UPDATE_IGNORED))
                } else {
                    UpdateUtil.ensureExternalCacheDir(mContext)
                    UpdateUtil.setUpdate(mContext, it.md5)
                    mTmpFile = File(mContext.externalCacheDir, it.md5)
                    mApkFile = File(mContext.externalCacheDir, it.md5 + ".apk")

                    if (UpdateUtil.verify(mApkFile!!, it.md5)) {
                        doInstall()
                    } else if (it.isSilent) {
                        doDownload()
                    } else {
                        doPrompt()
                    }
                }
            }
        }
    }

    fun doPrompt() {
        mPrompter?.prompt(this)
    }

    fun doDownload() {
        mDownloader?.download(this, mInfo!!.url, mTmpFile!!)
    }

    fun doInstall() {
        UpdateUtil.install(mContext, mApkFile!!, mInfo!!.isForce)
    }

    fun doFailture(error: UpdateError) {
        if (mIsManual || error.isError()) {
            mOnFailureListener?.onFailture(error)
        }
    }



    ////////////////////////////////////////////////////////////////////////
    //     ---  IDownloadAgent
    ////////////////////////////////////////////////////////////////////////
    override fun onStart() {
        mInfo?.let {
            if (it.isSilent) {
                mOnNotificationDownloadListener?.onStart()
            } else {
                mOnDownloadListener?.onStart()
            }
        }
    }

    override fun onProgress(progress: Int) {
        mInfo?.let {
            if (it.isSilent) {
                mOnNotificationDownloadListener?.onProgress(progress)
            } else {
                mOnDownloadListener?.onProgress(progress)
            }
        }
    }

    override fun onFinish() {
        mInfo?.let {
            if (it.isSilent) {
                mOnNotificationDownloadListener?.onFinish()
            } else {
                mOnDownloadListener?.onFinish()
            }

            mError?.let {
                mOnFailureListener?.onFailture(it)
            }
            mError?:let {
                mTmpFile?.renameTo(mApkFile)
                if (mInfo!!.isAutoInstall) {
                    doInstall()
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////
    //     ---  IUpdateAgent
    ////////////////////////////////////////////////////////////////////////
    override fun getInfo(): UpdateInfo? {
        return mInfo
    }

    override fun update() {
        mInfo?.let {
            mApkFile = File(mContext.externalCacheDir, it.md5 + ".apk")
            mApkFile?.let {
                if (UpdateUtil.verify(it, mInfo!!.md5)) {
                    doInstall()
                } else {
                    doDownload()
                }
            }
        }
    }

    override fun ignore() {
        getInfo()?.let {
            UpdateUtil.setIgnore(mContext, it.md5)
        }
    }

    ////////////////////////////////////////////////////////////////////////
    //     ---  ICheckAgent
    ////////////////////////////////////////////////////////////////////////
    override fun setInfo(info: String) {
        mInfo = mParser.parse(info)
    }

    override fun setError(error: UpdateError) {
        mError = error
    }


    ////////////////////////////////////////////////////////////////////////
    //     ---  inner class
    ////////////////////////////////////////////////////////////////////////
    private class DefaultUpdateParser: IUpdateParser {
        override fun parse(source: String): UpdateInfo? {
            return UpdateInfo.parse(source)
        }

    }

    private class DefaultUpdateDownloader(var context: Context): IUpdateDownloader {

        override fun download(agent: IDownloadAgent, url: String, temp: File) {
            UpdateDownloader(agent, context, url, temp).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    private class DefaultUpdatePrompter(var mContext: Context): IUpdatePrompter {

        private var mOnClickListener: DialogInterface.OnClickListener? = null

        fun setOnClickListener(listener: DialogInterface.OnClickListener) {
            mOnClickListener = listener
        }

        override fun prompt(agent: IUpdateAgent) {
            (mContext as? Activity)?.let {
                if (it.isFinishing) {
                    return
                }
            }

            val info = agent.getInfo()
            info?.let {
                val size = Formatter.formatShortFileSize(mContext, info.size)
                val content = "最新版本：" + info.versionName + "\n新版本大小：" + size + "\n\n更新内容\n" + info.updateContent

                val dialog = AlertDialog.Builder(mContext).create()
                dialog.setTitle("应用更新")
                dialog.setCancelable(false)
                dialog.setCanceledOnTouchOutside(false)

                val density = mContext.resources.displayMetrics.density
                val tv = TextView(mContext)
                tv.movementMethod = ScrollingMovementMethod()
                tv.isVerticalScrollBarEnabled = true
                tv.textSize = 14f
                tv.maxHeight = (250 * density).toInt()

                dialog.setView(tv, (25 * density).toInt(), (15 * density).toInt(), (25 * density).toInt(), 0)

                var listener: DialogInterface.OnClickListener = DefaultPromptClickListener(true)
                mOnClickListener?.let {
                    listener = it
                }

                (listener as? DefaultPromptClickListener)?.let {
                    it.getAgent() ?: it.setAgent(agent)
                }

                if (info.isForce) {
                    tv.setText("您需要更新应用才能继续使用\n\n$content")
                    dialog.setButton(DialogInterface.BUTTON_POSITIVE, "确定", listener)
                } else {
                    tv.setText(content)
                    dialog.setButton(DialogInterface.BUTTON_POSITIVE, "立即更新", listener)
                    dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "以后再说", listener)
                    if (info.isIgnorable) {
                        dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "忽略该版", listener)
                    }
                }

                dialog.show()
            }
        }
    }

    private class DefaultFailureListener(var mContext: Context): OnFailureListener {

        override fun onFailture(error: UpdateError) {
            Toast.makeText(mContext, error.toString(), Toast.LENGTH_LONG).show()
        }
    }

    private class DefaultDialogDownloadListener(var mContext: Context): OnDownloadListener {

        private var mDialog: ProgressDialog? = null

        override fun onStart() {
            (mContext as? Activity)?.let {
                if (!it.isFinishing) {
                    val dialog = ProgressDialog(mContext)
                    dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
                    dialog.setMessage("下载中...")
                    dialog.isIndeterminate = false
                    dialog.setCancelable(false)
                    dialog.show()
                    mDialog = dialog
                }
            }
        }

        override fun onProgress(progress: Int) {
            mDialog?.let {
                it.progress = progress
            }
        }

        override fun onFinish() {
            mDialog?.let {
                it.dismiss()
                mDialog = null
            }
        }
    }

    private class DefaultNotificationDownloadListener(var mContext: Context, var mNotifyId: Int): OnDownloadListener {

        var mBuilder: NotificationCompat.Builder? = null

        override fun onStart() {
            mBuilder?:let {
                val title = "下载中 - ${mContext.getString(mContext.applicationInfo.labelRes)}"
                mBuilder = NotificationCompat.Builder(mContext)
                mBuilder?.let {
                    it.setOngoing(true)
                        .setAutoCancel(false)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setDefaults(Notification.DEFAULT_VIBRATE)
                        .setSmallIcon(mContext.applicationInfo.icon)
                        .setTicker(title)
                        .setContentTitle(title)
                }
                onProgress(0)
            }
        }

        override fun onProgress(progress: Int) {
            mBuilder?.let {
                if (progress > 0) {
                    mBuilder?.setPriority(Notification.PRIORITY_DEFAULT)
                    mBuilder?.setDefaults(0)
                }
                mBuilder?.setProgress(100, progress, false)
                val nm: NotificationManager = mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(mNotifyId, mBuilder?.build())
            }
        }

        override fun onFinish() {
            val nm: NotificationManager = mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(mNotifyId)
        }

    }
}