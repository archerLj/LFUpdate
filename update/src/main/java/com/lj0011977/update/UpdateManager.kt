package com.lj0011977.update

import android.content.Context
import android.content.DialogInterface
import com.lj0011977.update.interfaces.*
import com.lj0011977.update.listener.CustomPromptClickListener

/**
 * @author : created by archerLj
 * date: 2019/4/2 17
 * usage:
 */
class UpdateManager {

    companion object {
        private var sIsWifiOnly: Boolean = false
        private var sUrl: String = ""
        private var sChannel: String = ""

        fun setWifiOnly(wifiOnly: Boolean) {
            sIsWifiOnly = wifiOnly
        }

        fun setUrl(url: String) {
            sUrl = url
        }

        fun setChannel(channel: String) {
            sChannel = channel
        }

        fun check(context: Context) {
            create(context).check()
        }

        fun create(context: Context): Builder {

            UpdateUtil.ensureExternalCacheDir(context)
            return Builder(context).setWifiOnly(sIsWifiOnly)
        }
    }

    class Builder(var mContext: Context) {

        companion object {
            var sLastTime: Long = 0
        }

        private var mUrl: String? = null
        private var mPostData: ByteArray? = null
        private var mIsManual: Boolean = false
        private var mIsWifiOnly: Boolean = false
        private var mNotifyId = 0

        private var mOnNotificationDownloadListener: OnDownloadListener? = null
        private var mOnDownloadListener: OnDownloadListener? = null
        private var mPrompter: IUpdatePrompter? = null
        private var mPrompterClickListener: DialogInterface.OnClickListener? = null

        private var mOnFailureListener: OnFailureListener? = null

        private var mParser: IUpdateParser? = null
        private var mChecker: IUpdateChecker? = null
        private var mDownloader: IUpdateDownloader? = null

        fun setUrl(url: String?): Builder {
            mUrl = url
            return this
        }

        fun setUrlAndChannel(url: String?, channel: String): Builder {
            mUrl = url
            sChannel = channel
            return  this
        }

        fun setPostData(data: ByteArray?): Builder {
            mPostData = data
            return this
        }

        fun setPostData(data: String?): Builder {
            mPostData = data?.let { data.toByteArray(Charsets.UTF_8) }
            return this
        }

        fun setNotifyId(notifyId: Int): Builder {
            mNotifyId = notifyId
            return this
        }

        fun setManual(isManual: Boolean): Builder {
            mIsManual = isManual
            return this
        }

        fun setWifiOnly(wifiOnly: Boolean): Builder {
            mIsWifiOnly = wifiOnly
            return this
        }

        fun setParser(parser: IUpdateParser?): Builder {
            mParser = parser
            return this
        }

        fun setChecker(checker: IUpdateChecker?): Builder {
            mChecker = checker
            return this
        }

        fun setDownloader(downloader: IUpdateDownloader?): Builder {
            mDownloader = downloader
            return this
        }

        fun setPrompter(prompter: IUpdatePrompter?): Builder {
            mPrompter = prompter
            return this
        }

        fun setPrompterClickListener(listener: CustomPromptClickListener?): Builder {
            mPrompterClickListener = listener
            return  this
        }

        fun setOnNotificationDownloadListener(listener: OnDownloadListener?): Builder {
            mOnNotificationDownloadListener = listener
            return this
        }

        fun setOnDownloadListener(listener: OnDownloadListener?): Builder {
            mOnDownloadListener = listener
            return this
        }

        fun setOnFailureListener(listener: OnFailureListener?): Builder {
            mOnFailureListener = listener
            return this
        }

        fun check() {
            val now = System.currentTimeMillis()
            if (now - sLastTime < 3000) {
                return
            }
            sLastTime = now

            if (!mUrl.isEmpty()) {
                mUrl = UpdateUtil.toFormatUrl(mContext, mUrl!!, sChannel)
            }

            mUrl?.let {
                val agent = UpdateAgent(mContext, it, mIsManual, mIsWifiOnly, mNotifyId)
                mOnNotificationDownloadListener?.let {
                    agent.setOnNotificationDownloadListener(it)
                }
                mOnDownloadListener?.let {
                    agent.setOnDownloadListener(it)
                }
                mOnFailureListener?.let {
                    agent.setOnFailureListener(it)
                }
                mChecker?.let {
                    agent.setChecker(it)
                }
                mChecker?:let {
                    agent.setChecker(UpdateChecker(mPostData))
                }
                mParser?.let {
                    agent.setParser(it)
                }
                mDownloader?.let {
                    agent.setDownloader(it)
                }
                mPrompter?.let {
                    agent.setPrompter(it)
                }
                mPrompterClickListener?.let {
                    agent.setPrompterClickListener(it)
                }
                agent.check()
            }
        }
    }
}