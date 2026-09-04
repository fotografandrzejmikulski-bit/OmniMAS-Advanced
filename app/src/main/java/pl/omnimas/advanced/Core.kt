package pl.omnimas.advanced

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import java.security.MessageDigest

data class UiNode(
    val id: Int,
    val role: String,
    val text: String?,
    val desc: String?,
    val resourceId: String?,
    val clickable: Boolean,
    val editable: Boolean,
    val enabled: Boolean,
    val bounds: String
)

data class UiState(
    val packageName: String,
    val nodes: List<UiNode>,
    val fingerprint: String
)

data class Action(
    val type: String,
    val nodeId: Int? = null,
    val text: String? = null,
    val direction: String? = null,
    val expected: String? = null,
    val reason: String? = null,
    val risk: String = "LOW"
)

data class MissionStep(val id: String, val objective: String, val successCriteria: String)

object Fingerprint {
    fun of(packageName: String, nodes: List<UiNode>): String {
        val raw = packageName + "|" + nodes.joinToString("|") {
            "${it.role}|${it.text}|${it.desc}|${it.resourceId}|${it.bounds}"
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}

class GroundingAgent {
    fun capture(root: AccessibilityNodeInfo?, packageName: String): UiState {
        if (root == null) return UiState(packageName, emptyList(), "")
        val out = mutableListOf<UiNode>()
        var id = 0

        fun walk(n: AccessibilityNodeInfo) {
            val r = Rect()
            n.getBoundsInScreen(r)
            val role = n.className?.toString()?.substringAfterLast('.') ?: "View"
            val text = n.text?.toString()?.take(200)
            val desc = n.contentDescription?.toString()?.take(200)
            if (r.width() > 0 && r.height() > 0 &&
                (n.isClickable || n.isEditable || !text.isNullOrBlank() || !desc.isNullOrBlank())
            ) {
                out += UiNode(
                    id = id++, role = role, text = text, desc = desc,
                    resourceId = n.viewIdResourceName, clickable = n.isClickable,
                    editable = n.isEditable, enabled = n.isEnabled, bounds = r.toString()
                )
            }
            for (i in 0 until n.childCount) {
                n.getChild(i)?.let {
                    walk(it)
                    it.recycle()
                }
            }
        }

        walk(root)
        return UiState(packageName, out, Fingerprint.of(packageName, out))
    }
}
