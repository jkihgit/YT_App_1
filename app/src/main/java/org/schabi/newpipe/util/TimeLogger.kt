package org.schabi.newpipe.util

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.schabi.newpipe.BuildConfig
import org.schabi.newpipe.R
import java.io.File
import java.io.RandomAccessFile
import java.util.*

/**
 * Created by Kihoon Jung on 26.10.22.
 * Copyright 2022 Kihoon Jung <kh_jung@kaist.ac.kr>
 * Handles log file IO & time keeping for the log file
 * License added per NewPipe's GPL
 * <p>
 * License: GPL-3.0+
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

class TimeLogger() {
    companion object {
        @kotlin.jvm.JvmStatic
        private val DEBUG: Boolean = BuildConfig.BUILD_TYPE != "release"
        @kotlin.jvm.JvmStatic
        private val tlName = "timeLogger"
        @kotlin.jvm.JvmStatic
        private var instance: TimeLogger? = null
        @kotlin.jvm.JvmStatic
        private var file: File? = null
        @kotlin.jvm.JvmStatic
        private var lastWatched: Date? = null
        @kotlin.jvm.JvmStatic
        private var lastFCHeartbeat: Date? = null
        @kotlin.jvm.JvmStatic
        private var ignoreNextAutoplay = 0
        //        2 min
        @kotlin.jvm.JvmStatic
        val timerInterval = 120000
        fun getInstance(context: Context): TimeLogger {
            if (instance == null) {
                instance = TimeLogger()
//            retrieve app storage
//            this file is not accessible via the file system
                val dir = context.getDir("timeLogger", Context.MODE_APPEND)
                file = File(dir, "tl.log")
                file!!.createNewFile()
                if (DEBUG) {
                    val lines = file!!.readLines()
                    var fileStr: String = ""
                    for (line in lines) {
                        fileStr += "$line\n"
                    }
                    Log.d(tlName, "File Contents:\n$fileStr\\EOF")
                    Log.d(tlName, "if you cannot see \\EOF, the file is too big for the logger")
                }
            }
            return instance as TimeLogger
        }
        fun heartbeat() {
            val now = Calendar.getInstance().time
            checkInstanceExists()
            val heartbeatHeader = "heartbeat()"
            if ((lastWatched != null) && (now.time - lastWatched!!.time < timerInterval * 1.1)) {
                removeLastHeartbeat()
            }
            TimeLogger.lastWatched = now
            log(heartbeatHeader, null)
        }
        private fun removeLastHeartbeat() {
            val f = RandomAccessFile(TimeLogger.file!!.path, "rw")
            var l1 = f.length() - 1
            var b1: Byte
            do {
                l1 -= 1
                f.seek(l1)
                b1 = f.readByte()
            } while ((b1.toInt().toChar() != '\n') && (l1 > 0))
            f.setLength(l1 + 1)
        }
        private fun checkInstanceExists() {
            if (TimeLogger.file == null) {
                throw NullPointerException(".file is null. Run .getInstance() before use")
            }
        }
        private fun log(message: String, context: Context?, isHeartbeat: Boolean = false) {
            val currentTime: Date = Calendar.getInstance().time
            val msg: String = "$currentTime\t$message"
            if (DEBUG) {
                Log.d(tlName, "log() $msg")
                if (context != null) {
                    val toast: Toast = Toast.makeText(
                        context,
                        msg, Toast.LENGTH_SHORT
                    )
                    toast.show()
                }
            }
            checkInstanceExists()
            TimeLogger.file!!.appendText(msg + '\n')
            if (isHeartbeat) {
                if (lastFCHeartbeat == null) {
                    lastFCHeartbeat = currentTime
//                    fallthrough
                } else if (currentTime.time - lastFCHeartbeat!!.time > timerInterval * 10) {
//                    fallthrough
                } else {
                    return
                }
            }
            FirebaseCrashlytics.getInstance().log(msg)
        }
        private fun startSession(context: Context?) {
            TimeLogger.log("Start Session", context)
        }
        private fun endSession(context: Context?) {
            removeLastHeartbeat()
            TimeLogger.log("End Session", context)
        }
        private fun markIngress(message: String, fromMenu: String, context: Context?) {
            if (TimeLogger.ignoreNextAutoplay > 0) {
                TimeLogger.ignoreNextAutoplay--
                if (message == "Autoplay") {
                    return
                }
            }
            TimeLogger.log("Ingress\t$message\tfrom\t$fromMenu", context)
        }
        fun pauseSession(context: Context?) {
            removeLastHeartbeat()
            TimeLogger.log("Pause Session", context)
        }
        fun resumeSession(context: Context?) {
            TimeLogger.log("Resume Session", context)
        }
        fun getLogContents(): String {
            val f = RandomAccessFile(TimeLogger.file!!.path, "r")
            var s = ""
            do {
                val line = f.readLine() ?: break
                s += line + "\n"
            } while (true)
            f.close()
            return s
        }
        fun ignoreNextAutoplay() {
            TimeLogger.ignoreNextAutoplay++
        }
        fun rsEnabled(context: Context): Boolean {
            return context.getString(R.string.exprSchedule) == "1"
        }
    }
//    yes these are necessary because I dont want want to type .Companion every time I use them
    fun log(message: String, context: Context? = null) {
        TimeLogger.log(message, context)
    }
    fun startSession(context: Context? = null) {
        TimeLogger.startSession(context)
    }
    fun endSession(context: Context? = null) {
        TimeLogger.endSession(context)
    }
    fun markIngress(message: String, fromMenu: String, context: Context? = null) {
        TimeLogger.markIngress(message, fromMenu, context)
    }
    fun pauseSession(context: Context? = null) {
        TimeLogger.pauseSession(context)
    }
    fun resumeSession(context: Context? = null) {
        TimeLogger.resumeSession(context)
    }
    fun getLogContents(): String {
        return TimeLogger.getLogContents()
    }
    fun ignoreNextAutoplay() {
        TimeLogger.ignoreNextAutoplay()
    }
    fun rsEnabled(context: Context): Boolean {
        return TimeLogger.rsEnabled(context)
    }

    class Heartbeat() : TimerTask() {
        override fun run() {
            TimeLogger.heartbeat()
        }
    }
}
