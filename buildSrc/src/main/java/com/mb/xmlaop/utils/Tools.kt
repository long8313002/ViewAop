package com.mb.xmlaop.utils

import com.zhangzheng0601.buildsrc.utils.FileUtil

/**
 * Created by longlong on 2017/4/15.
 */
class Tools {

    companion object {

        fun isLinux(): Boolean {
            val system = System.getProperty("os.name")
            return system.startsWith("Linux")
        }

        fun chmod() {
            outputMessage("chmod 755 -R ${FileUtil.getRootDirPath()}")
        }

        private fun outputMessage(cmd: String) {
            val process = Runtime.getRuntime().exec(cmd)
            process.waitFor()
        }

    }
}
