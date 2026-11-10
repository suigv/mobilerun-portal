package com.mobilerun.portal.taskprompt

import android.content.Context
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID

data class CoreRpaPackage(
    val id: String,
    val goal: String,
    val phase: String,
    val model: String?,
    val contractId: String?,
    val contractStatus: String?,
    val contractAlignment: String?,
    val contractDetails: String?,
)

sealed class CoreRpaResult<out T> {
    data class Success<T>(val value: T) : CoreRpaResult<T>()
    data class Failure(val message: String, val outcomeUnknown: Boolean = false) : CoreRpaResult<Nothing>()
}

class CoreRpaClient(private val http: OkHttpClient = OkHttpClient()) {
    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun parsePackage(body: JSONObject): CoreRpaPackage {
            val pkg = body.getJSONObject("package")
            val progress = body.getJSONObject("progress")
            val contract = body.optJSONObject("contract")
            return CoreRpaPackage(
                id = pkg.getString("package_id"),
                goal = pkg.getString("goal"),
                phase = progress.getString("phase"),
                model = body.optString("model").takeIf { it.isNotBlank() && it != "null" },
                contractId = contract?.optString("contract_id")?.takeIf { it.isNotBlank() },
                contractStatus = contract?.optString("status")?.takeIf { it.isNotBlank() },
                contractAlignment = contract?.optString("alignment")?.takeIf { it.isNotBlank() },
                contractDetails = contract?.optString("contract_json")?.takeIf { it.isNotBlank() },
            )
        }
    }

    fun create(
        baseUrl: String,
        token: String,
        deviceId: String,
        goal: String,
        model: String,
        requestKey: String,
        callback: (CoreRpaResult<CoreRpaPackage>) -> Unit,
    ) {
        val payload = JSONObject()
            .put("device_id", deviceId)
            .put("goal", goal)
            .put("model", model)
            .put("request_key", requestKey)
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/portal/packages")
            .header("Authorization", "Bearer $token")
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        execute(request, true, callback, ::parsePackage)
    }

    fun get(
        baseUrl: String,
        token: String,
        packageId: String,
        callback: (CoreRpaResult<CoreRpaPackage>) -> Unit,
    ) {
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/portal/packages/$packageId")
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        execute(request, false, callback, ::parsePackage)
    }

    fun list(
        baseUrl: String,
        token: String,
        callback: (CoreRpaResult<List<CoreRpaPackage>>) -> Unit,
    ) {
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/portal/packages")
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        execute(request, false, callback) { body ->
            val array = body.getJSONArray("packages")
            (0 until array.length()).map { parsePackage(array.getJSONObject(it)) }
        }
    }

    fun confirm(
        baseUrl: String,
        token: String,
        packageId: String,
        contractId: String,
        callback: (CoreRpaResult<CoreRpaPackage>) -> Unit,
    ) {
        val payload = JSONObject().put("contract_id", contractId).toString()
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/portal/packages/$packageId/contract/confirm")
            .header("Authorization", "Bearer $token")
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()
        execute(request, true, callback, ::parsePackage)
    }

    private fun <T> execute(
        request: Request,
        mutation: Boolean,
        callback: (CoreRpaResult<T>) -> Unit,
        parse: (JSONObject) -> T,
    ) {
        http.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(CoreRpaResult.Failure("无法连接 CoreRPA", outcomeUnknown = mutation))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = try {
                        response.body?.string().orEmpty()
                    } catch (_: IOException) {
                        callback(CoreRpaResult.Failure("无法读取服务响应", outcomeUnknown = mutation))
                        return
                    }
                    if (!response.isSuccessful) {
                        val message = try {
                            JSONObject(body).optJSONObject("error")?.optString("message")
                        } catch (_: Exception) {
                            null
                        }
                        callback(
                            CoreRpaResult.Failure(
                                message?.takeIf { it.isNotBlank() } ?: "请求失败 (${response.code})",
                                outcomeUnknown = mutation && response.code >= 500,
                            ),
                        )
                        return
                    }
                    val value = try {
                        parse(JSONObject(body))
                    } catch (_: Exception) {
                        callback(CoreRpaResult.Failure("服务响应不完整", outcomeUnknown = mutation))
                        return
                    }
                    callback(CoreRpaResult.Success(value))
                }
            }
        })
    }
}

/** Retain the same idempotency key if the outcome of a submission is unknown. */
class CoreRpaRequestKeys(context: Context) {
    private val prefs = context.getSharedPreferences("corerpa_goal_requests", Context.MODE_PRIVATE)

    fun keyFor(baseUrl: String, deviceId: String, goal: String, model: String): String {
        val fingerprint = fingerprint(baseUrl, deviceId, goal, model)
        val existing = prefs.getString(fingerprint, null)
        if (existing != null) return existing
        val key = UUID.randomUUID().toString()
        check(prefs.edit().putString(fingerprint, key).commit()) { "无法保存请求身份" }
        return key
    }

    fun completed(baseUrl: String, deviceId: String, goal: String, model: String, key: String) {
        val fingerprint = fingerprint(baseUrl, deviceId, goal, model)
        if (prefs.getString(fingerprint, null) == key) {
            prefs.edit().remove(fingerprint).commit()
        }
    }

    private fun fingerprint(vararg values: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        values.forEach { value ->
            val bytes = value.toByteArray(Charsets.UTF_8)
            digest.update("${bytes.size}:".toByteArray(Charsets.UTF_8))
            digest.update(bytes)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
