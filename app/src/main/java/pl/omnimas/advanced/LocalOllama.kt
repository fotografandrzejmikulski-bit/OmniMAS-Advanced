package pl.omnimas.advanced

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class LocalOllama(
    private val baseUrl: String = "http://127.0.0.1:11434",
    private val model: String = "deepseek-r1:1.5b"
) {
    private val client = OkHttpClient()
    private val mediaType = "application/json".toMediaType()

    suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("model", model)
            .put("prompt", prompt)
            .put("stream", false)
            .toString()

        val request = Request.Builder()
            .url("$baseUrl/api/generate")
            .post(body.toRequestBody(mediaType))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Ollama HTTP ${response.code}")
            JSONObject(response.body?.string().orEmpty()).optString("response")
        }
    }

    suspend fun plan(mission: String): List<MissionStep> {
        val prompt = """
            Zaprojektuj plan automatyzacji Android dla misji użytkownika.
            Maksymalnie 8 kroków. Każdy krok musi mieć jednoznaczny cel i kryterium sukcesu.
            Operacje finansowe, usuwanie danych, wysyłanie wiadomości i publikacja wymagają później potwierdzenia.
            Zwróć WYŁĄCZNIE JSON: {"steps":[{"id":"s1","objective":"...","successCriteria":"..."}]}.
            MISSION: $mission
        """.trimIndent()

        val json = JSONObject(extractJson(generate(prompt)))
        val array = json.optJSONArray("steps") ?: JSONArray()
        return (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.let { item ->
                MissionStep(
                    id = item.optString("id", "s$index"),
                    objective = item.optString("objective"),
                    successCriteria = item.optString("successCriteria")
                )
            }
        }
    }

    suspend fun decide(
        mission: String,
        step: MissionStep,
        state: UiState,
        memory: MemoryAgent
    ): Action {
        val nodes = JSONArray().apply {
            state.nodes.forEach { node ->
                put(JSONObject().apply {
                    put("id", node.id)
                    put("role", node.role)
                    put("text", node.text ?: "")
                    put("desc", node.desc ?: "")
                    put("resourceId", node.resourceId ?: "")
                    put("clickable", node.clickable)
                    put("editable", node.editable)
                    put("enabled", node.enabled)
                    put("bounds", node.bounds)
                })
            }
        }

        val prompt = """
            Jesteś Decision Agentem Androida. Wybierz dokładnie jedną następną akcję.
            Zwróć WYŁĄCZNIE JSON:
            {"type":"CLICK|TYPE|SCROLL|BACK|HOME|DONE","nodeId":0,"text":"","direction":"UP|DOWN","expected":"","reason":"","risk":"LOW|HIGH"}
            Zasady:
            - nigdy nie wymyślaj nodeId; wybieraj tylko id z NODES,
            - jeśli cel jest już spełniony, zwróć DONE,
            - preferuj najbliższą bezpieczną akcję,
            - nie wykonuj operacji finansowych, wysyłki, publikacji ani usuwania danych bez ustawienia risk=HIGH.

            MISSION=$mission
            STEP=${step.objective}
            SUCCESS=${step.successCriteria}
            STATE_FINGERPRINT=${state.fingerprint}
            LOOP_STATE=${memory.repeatedState()}
            LOOP_ACTION=${memory.repeatedAction()}
            NODES=$nodes
        """.trimIndent()

        val json = JSONObject(extractJson(generate(prompt)))
        return Action(
            type = json.optString("type", "DONE").uppercase(),
            nodeId = if (json.has("nodeId") && !json.isNull("nodeId")) json.optInt("nodeId") else null,
            text = json.optStringOrNull("text"),
            direction = json.optStringOrNull("direction"),
            expected = json.optStringOrNull("expected"),
            reason = json.optStringOrNull("reason"),
            risk = json.optString("risk", "LOW").uppercase()
        )
    }

    private fun extractJson(raw: String): String {
        val cleaned = raw.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        require(start >= 0 && end > start) { "Model did not return JSON" }
        return cleaned.substring(start, end + 1)
    }

    private fun JSONObject.optStringOrNull(name: String): String? =
        if (!has(name) || isNull(name)) null else optString(name).takeIf { it.isNotBlank() }
}
