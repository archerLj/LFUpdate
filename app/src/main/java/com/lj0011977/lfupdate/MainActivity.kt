package com.lj0011977.lfupdate

import android.content.DialogInterface
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.alibaba.fastjson.JSON
import com.lj0011977.update.UpdateError
import com.lj0011977.update.UpdateInfo
import com.lj0011977.update.UpdateManager
import com.lj0011977.update.interfaces.IUpdateParser
import com.lj0011977.update.interfaces.OnFailureListener

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        UpdateManager.create(this)
            .setUrl("http://101.132.90.137/HennessyRFID/api/auth/collectionPDA/getSoftVersion")
            .setPostData(JSON.toJSONString(UpdateReqInfo(applicationContext)))
            .setManual(false)
            .setNotifyId(998)
            .setWifiOnly(false)
            .setParser(object : IUpdateParser {
                override fun parse(source: String): UpdateInfo? {
                    return JSON.parseObject(source, UpdateInfo::class.java)
                }
            }).setOnFailureListener(object : OnFailureListener{
                override fun onFailture(error: UpdateError) {
                    if (error.code == UpdateError.UPDATE_NO_NEWER || error.code == UpdateError.UPDATE_IGNORED) {
                        Toast.makeText(this@MainActivity, "no newer || ignored", Toast.LENGTH_SHORT).show()
                    } else {
                        val dialog = AlertDialog.Builder(this@MainActivity).create()
                        dialog.setCancelable(false)
                        dialog.setCanceledOnTouchOutside(false)
                        dialog.setTitle(error.toString())
                        dialog.setButton(
                            DialogInterface.BUTTON_POSITIVE, "好的"
                        ) { dialog, which ->
                            finish()
                        }
                        dialog.show()
                    }
                }
            }).check()

    }
}
