package com.lj0011977.update.listener

import android.content.DialogInterface
import com.lj0011977.update.interfaces.IUpdateAgent

/**
 * @author : created by archerLj
 * date: 2019/4/2 17
 * usage:
 */
open class DefaultPromptClickListener(var mIsAutoDismiss: Boolean): DialogInterface.OnClickListener {

    private var mAgent: IUpdateAgent? = null

    fun setAgent(agent: IUpdateAgent) {
        mAgent = agent
    }

    fun getAgent(): IUpdateAgent? {
        return mAgent
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        when (which) {
            DialogInterface.BUTTON_POSITIVE -> {
                mAgent?.update()
            }
            DialogInterface.BUTTON_NEUTRAL -> {
                mAgent?.ignore()
            }
            DialogInterface.BUTTON_NEGATIVE -> {
                // not now
            }
        }

        if (mIsAutoDismiss) {
            dialog?.dismiss()
        }
    }
}