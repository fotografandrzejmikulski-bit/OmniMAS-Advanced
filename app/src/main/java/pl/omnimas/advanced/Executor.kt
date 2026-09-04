package pl.omnimas.advanced

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo

class ExecutorAgent(private val service: AccessibilityService) {
    fun execute(action: Action, nodes: List<AccessibilityNodeInfo>): Boolean = when (action.type.uppercase()) {
        "CLICK" -> action.nodeId?.let { index ->
            nodes.getOrNull(index)?.takeIf { it.isEnabled }?.performAction(
                AccessibilityNodeInfo.ACTION_CLICK
            ) ?: false
        } ?: false

        "TYPE" -> {
            val target = service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                ?: nodes.firstOrNull { it.isEditable && it.isEnabled }
            if (target == null || action.text == null) false
            else Bundle().let { args ->
                args.putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    action.text
                )
                target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            }
        }

        "BACK" -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        "HOME" -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
        "SCROLL" -> scroll(action.direction ?: "DOWN")
        "DONE" -> true
        else -> false
    }

    private fun scroll(direction: String): Boolean {
        val display = service.resources.displayMetrics
        val x = display.widthPixels / 2f
        val y = display.heightPixels / 2f
        val dy = display.heightPixels * 0.30f
        val path = Path()
        if (direction.uppercase() == "DOWN") {
            path.moveTo(x, y + dy)
            path.lineTo(x, y - dy)
        } else {
            path.moveTo(x, y - dy)
            path.lineTo(x, y + dy)
        }
        return service.dispatchGesture(
            GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 450))
                .build(),
            null,
            null
        )
    }
}
