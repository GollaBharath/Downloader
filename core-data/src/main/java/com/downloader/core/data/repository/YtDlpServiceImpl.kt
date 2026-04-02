package com.downloader.core.data.repository

import android.content.Context
import com.downloader.core.domain.model.MediaInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YtDlpServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : YtDlpService {

    private val json = Json { ignoreUnknownKeys = true }

    private fun parseMediaInfo(obj: JsonObject, fallbackUrl: String): MediaInfo {
        val isPlaylist = obj["_type"]?.jsonPrimitive?.contentOrNull == "playlist"
        val resolvedUrl =
            obj["webpage_url"]?.jsonPrimitive?.contentOrNull
                ?: obj["url"]?.jsonPrimitive?.contentOrNull
                ?: fallbackUrl

        val playlistEntries = if (isPlaylist) {
            obj["entries"]
                ?.jsonArray
                ?.mapNotNull { element ->
                    element as? JsonObject
                }
                ?.map { entryObj ->
                    val entryFallbackUrl =
                        entryObj["webpage_url"]?.jsonPrimitive?.contentOrNull
                            ?: entryObj["url"]?.jsonPrimitive?.contentOrNull
                            ?: resolvedUrl
                    parseMediaInfo(entryObj, entryFallbackUrl)
                }
                ?.filter { it.url.isNotBlank() }
                ?: emptyList()
        } else {
            null
        }

        return MediaInfo(
            url = resolvedUrl,
            title = obj["title"]?.jsonPrimitive?.contentOrNull,
            description = obj["description"]?.jsonPrimitive?.contentOrNull,
            duration = obj["duration"]?.jsonPrimitive?.contentOrNull,
            thumbnail = obj["thumbnail"]?.jsonPrimitive?.contentOrNull,
            uploader = obj["uploader"]?.jsonPrimitive?.contentOrNull,
            uploadDate = obj["upload_date"]?.jsonPrimitive?.contentOrNull,
            viewCount = obj["view_count"]?.jsonPrimitive?.longOrNull,
            formats = emptyList(),
            isPlaylist = isPlaylist,
            playlistEntries = playlistEntries
        )
    }

    override suspend fun fetchMediaInfo(url: String): MediaInfo {
        val pythonClass = Class.forName("com.chaquo.python.Python")
        val isStarted = pythonClass.getMethod("isStarted").invoke(null) as Boolean
        if (!isStarted) {
            val androidPlatformClass = Class.forName("com.chaquo.python.android.AndroidPlatform")
            val platform = androidPlatformClass.getConstructor(Context::class.java).newInstance(context)
            pythonClass.getMethod("start", Class.forName("com.chaquo.python.Python\$Platform"))
                .invoke(null, platform)
        }

        val python = pythonClass.getMethod("getInstance").invoke(null)
        val module = pythonClass.getMethod("getModule", String::class.java)
            .invoke(python, "yt_dlp_bridge")
        val callAttr = module.javaClass.methods.first {
            it.name == "callAttr" && it.parameterTypes.size == 2
        }
        val payload = callAttr.invoke(module, "fetch_info", arrayOf<Any>(url))?.toString()
            ?: throw IllegalStateException("yt-dlp did not return metadata JSON")
        val obj = json.parseToJsonElement(payload).jsonObject

        return parseMediaInfo(obj, url)
    }
}
