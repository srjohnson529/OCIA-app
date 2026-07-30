package com.illumined.app.data

import android.content.Context
import com.illumined.app.R
import org.json.JSONObject

object CommonPrayerCatalog {
    fun namesById(context: Context): Map<String, String> = runCatching {
        val text = context.resources.openRawResource(R.raw.spiritual_formation).bufferedReader().use { it.readText() }
        val array = JSONObject(text).getJSONArray("commonPrayers")
        buildMap {
            repeat(array.length()) { index ->
                val prayer = array.getJSONObject(index)
                put(prayer.getString("id"), prayer.getString("title"))
            }
        }
    }.getOrDefault(emptyMap())
}
