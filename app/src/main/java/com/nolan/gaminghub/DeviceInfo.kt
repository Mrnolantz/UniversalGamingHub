package com.nolan.gaminghub

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build

data class DeviceInfo(val model: String, val android: String, val chip: String, val cores: Int, val abi: String)
data class LiveStats(val usedMb: Long, val totalMb: Long, val batteryPct: Int, val tempC: Float)

fun readDeviceInfo(): DeviceInfo = DeviceInfo(
    model = "${Build.MANUFACTURER} ${Build.MODEL}",
    android = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
    chip = if (Build.VERSION.SDK_INT >= 31 && Build.SOC_MODEL.isNotBlank()) Build.SOC_MODEL else Build.HARDWARE,
    cores = Runtime.getRuntime().availableProcessors(),
    abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
)

fun readLiveStats(ctx: Context): LiveStats {
    val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
    val total = mi.totalMem / 1048576
    val used = total - mi.availMem / 1048576
    val bat: Intent? = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = bat?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = bat?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    val temp = (bat?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
    return LiveStats(used, total, if (level >= 0) level * 100 / scale else -1, temp)
}
