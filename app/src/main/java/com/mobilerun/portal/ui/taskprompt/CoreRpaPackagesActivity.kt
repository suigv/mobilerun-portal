package com.mobilerun.portal.ui.taskprompt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mobilerun.portal.R
import com.mobilerun.portal.config.ConfigManager
import com.mobilerun.portal.taskprompt.CoreRpaClient
import com.mobilerun.portal.taskprompt.CoreRpaPackage
import com.mobilerun.portal.taskprompt.CoreRpaResult
import com.mobilerun.portal.taskprompt.PortalCloudClient
import org.json.JSONObject

class CoreRpaPackagesActivity : AppCompatActivity() {
    companion object {
        private const val EXTRA_PACKAGE_ID = "package_id"

        fun createIntent(context: Context, packageId: String? = null): Intent =
            Intent(context, CoreRpaPackagesActivity::class.java).apply {
                if (packageId != null) putExtra(EXTRA_PACKAGE_ID, packageId)
            }
    }

    private val client = CoreRpaClient()
    private lateinit var content: LinearLayout
    private var selectedId: String? = null
    private var requestGeneration = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedId = savedInstanceState?.getString(EXTRA_PACKAGE_ID)
            ?: intent.getStringExtra(EXTRA_PACKAGE_ID)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
            setBackgroundColor(ContextCompat.getColor(context, R.color.background_primary))
        }
        setContentView(ScrollView(this).apply { addView(content) })
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(EXTRA_PACKAGE_ID, selectedId)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        requestGeneration++
        super.onDestroy()
    }

    private fun credentials(): Pair<String, String>? {
        val config = ConfigManager.getInstance(this)
        val baseUrl = PortalCloudClient.deriveRestBaseUrl(config.reverseConnectionUrlOrDefault)
        val token = config.reverseConnectionToken.trim()
        return if (baseUrl != null && token.isNotBlank()) baseUrl to token else null
    }

    private fun refresh() {
        val auth = credentials()
        if (auth == null) {
            renderMessage(getString(R.string.task_prompt_missing_api_key))
            return
        }
        val generation = ++requestGeneration
        renderMessage(getString(R.string.corerpa_loading))
        val id = selectedId
        if (id == null) {
            client.list(auth.first, auth.second) { result ->
                runOnUiThread {
                    if (generation != requestGeneration || credentials() != auth || isFinishing) return@runOnUiThread
                    when (result) {
                        is CoreRpaResult.Success -> renderList(result.value)
                        is CoreRpaResult.Failure -> renderMessage(result.message)
                    }
                }
            }
        } else {
            client.get(auth.first, auth.second, id) { result ->
                runOnUiThread {
                    if (generation != requestGeneration || credentials() != auth || isFinishing) return@runOnUiThread
                    when (result) {
                        is CoreRpaResult.Success -> renderDetail(result.value)
                        is CoreRpaResult.Failure -> renderMessage(result.message)
                    }
                }
            }
        }
    }

    private fun renderMessage(message: String) {
        content.removeAllViews()
        addHeading(getString(R.string.corerpa_goals_title))
        addText(message)
        addButton(getString(R.string.corerpa_refresh)) { refresh() }
        if (selectedId != null) addButton(getString(R.string.corerpa_back_to_goals)) {
            selectedId = null
            refresh()
        }
    }

    private fun renderList(packages: List<CoreRpaPackage>) {
        content.removeAllViews()
        addHeading(getString(R.string.corerpa_goals_title))
        addButton(getString(R.string.corerpa_refresh)) { refresh() }
        if (packages.isEmpty()) addText(getString(R.string.corerpa_goals_empty))
        packages.forEach { item ->
            addButton("${item.goal}\n${phaseLabel(item.phase)}") {
                selectedId = item.id
                refresh()
            }
        }
    }

    private fun renderDetail(item: CoreRpaPackage) {
        content.removeAllViews()
        addHeading(item.goal)
        addText(phaseLabel(item.phase))
        item.model?.let { addText(getString(R.string.corerpa_model_label, it)) }
        val agreement = agreementText(item)
        if (agreement.isNotBlank()) {
            addHeading(getString(R.string.corerpa_contract_label))
            addText(agreement)
        }
        if (item.phase == "awaiting_confirmation" &&
            item.contractStatus == "proposed" && item.contractId != null
        ) {
            addButton(getString(R.string.corerpa_confirm_contract)) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.corerpa_confirm_contract)
                    .setMessage("${getString(R.string.corerpa_confirm_description)}\n\n$agreement")
                    .setPositiveButton(R.string.corerpa_confirm_button) { _, _ -> confirm(item) }
                    .setNegativeButton(R.string.corerpa_cancel_button, null)
                    .show()
            }
        }
        addButton(getString(R.string.corerpa_refresh)) { refresh() }
        addButton(getString(R.string.corerpa_back_to_goals)) {
            selectedId = null
            refresh()
        }
    }

    private fun confirm(item: CoreRpaPackage) {
        val auth = credentials() ?: return
        val contractId = item.contractId ?: return
        val generation = ++requestGeneration
        renderMessage(getString(R.string.corerpa_loading))
        client.confirm(auth.first, auth.second, item.id, contractId) { result ->
            runOnUiThread {
                if (generation != requestGeneration || credentials() != auth || isFinishing) return@runOnUiThread
                when (result) {
                    is CoreRpaResult.Success -> renderDetail(result.value)
                    is CoreRpaResult.Failure -> renderMessage(
                        if (result.outcomeUnknown) getString(R.string.corerpa_confirm_unknown) else result.message,
                    )
                }
            }
        }
    }

    private fun agreementText(item: CoreRpaPackage): String {
        val sections = mutableListOf<String>()
        item.contractAlignment?.let { sections.add(it) }
        item.contractDetails?.let { raw ->
            try {
                val json = JSONObject(raw)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val label = when (key) {
                        "goal" -> getString(R.string.corerpa_goal_label)
                        "output" -> getString(R.string.corerpa_output_label)
                        "effects" -> getString(R.string.corerpa_effects_label)
                        else -> key
                    }
                    sections.add("$label: ${json.get(key)}")
                }
            } catch (_: Exception) {
                sections.add(raw)
            }
        }
        return sections.joinToString("\n\n")
    }

    private fun phaseLabel(phase: String): String {
        val label = when (phase) {
            "intake" -> R.string.corerpa_phase_intake
            "awaiting_confirmation" -> R.string.corerpa_phase_confirmation
            "explore" -> R.string.corerpa_phase_explore
            "distill" -> R.string.corerpa_phase_distill
            "judge" -> R.string.corerpa_phase_judge
            "awaiting_release_approval" -> R.string.corerpa_phase_release_approval
            "ready" -> R.string.corerpa_phase_ready
            "blocked" -> R.string.corerpa_phase_blocked
            "unknown_effect" -> R.string.corerpa_phase_unknown
            else -> return phase
        }
        return getString(label)
    }

    private fun addHeading(text: String) {
        addText(text, 20f)
    }

    private fun addText(text: String, size: Float = 15f) {
        content.addView(TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(ContextCompat.getColor(context, R.color.text_white))
            setPadding(0, dp(12), 0, dp(8))
        })
    }

    private fun addButton(label: String, action: () -> Unit) {
        content.addView(Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { action() }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
