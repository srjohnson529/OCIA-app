package com.illumined.app.data

import android.content.Context
import com.illumined.app.R
import org.json.JSONObject
import java.util.Locale

object CommonPrayerCatalog {
    fun namesById(context: Context): Map<String, String> = runCatching {
        val text = context.resources.openRawResource(R.raw.spiritual_formation).bufferedReader().use { it.readText() }
        val array = JSONObject(text).getJSONArray("commonPrayers")
        buildMap {
            repeat(array.length()) { index ->
                val prayer = array.getJSONObject(index)
                val title = prayer.getString("title")
                val localizedTitle = prayer.optString("titleEs").takeIf {
                    Locale.getDefault().language == "es" && it.isNotBlank()
                } ?: title
                put(prayer.getString("id"), localizedTitle)
            }
        }
    }.getOrDefault(emptyMap())
}
