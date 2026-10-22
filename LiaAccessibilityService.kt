package com.lia.assistant

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class LiaAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val e = event ?: return
        if (e.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            e.packageName?.toString()?.let { currentPackage = it }
        }
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        if (instance === this) instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    // ---------- সিস্টেম বাটন ----------

    fun home() = performGlobalAction(GLOBAL_ACTION_HOME)
    fun back() = performGlobalAction(GLOBAL_ACTION_BACK)
    fun recents() = performGlobalAction(GLOBAL_ACTION_RECENTS)
    fun notifications() = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    fun quickSettings() = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
    fun powerMenu() = performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)

    /** স্ক্রিন লক (Android 9+) */
    fun lockScreen(): Boolean =
        if (Build.VERSION.SDK_INT >= 28) performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) else false

    fun screenshot(): Boolean =
        if (Build.VERSION.SDK_INT >= 28) performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT) else false

    // ---------- স্ক্রিন পড়া ----------

    /**
     * স্ক্রিনের দৃশ্যমান লেখা লিস্ট করে।
     * ফ্ল্যাগ: C=ক্লিক করা যায়, E=লেখা যায়, S=স্ক্রল হয়, X=চেক করা
     * @x,y = এলিমেন্টের মাঝের স্থানাঙ্ক
     */
    fun readScreen(max: Int = 150): String {
        val root = rootInActiveWindow ?: return "(screen not readable)"
        val sb = StringBuilder()
        sb.append("app: ").append(root.packageName).append('\n')
        var count = 0

        fun walk(n: AccessibilityNodeInfo?) {
            if (n == null || count >= max) return
            if (n.isVisibleToUser) {
                val t = n.text?.toString()?.trim().orEmpty()
                val d = n.contentDescription?.toString()?.trim().orEmpty()
                var label = if (t.isNotEmpty()) t else d
                if (n.isPassword) label = "(password field)"
                if (label.isNotEmpty() || n.isEditable) {
                    val r = Rect()
                    n.getBoundsInScreen(r)
                    val flags = buildString {
                        if (n.isClickable) append('C')
                        if (n.isEditable) append('E')
                        if (n.isScrollable) append('S')
                        if (n.isChecked) append('X')
                    }
                    sb.append("- \"").append(label.take(80).replace('\n', ' ')).append('"')
                    if (flags.isNotEmpty()) sb.append(" [").append(flags).append(']')
                    sb.append(" @").append(r.centerX()).append(',').append(r.centerY()).append('\n')
                    count++
                }
            }
            for (i in 0 until n.childCount) walk(n.getChild(i))
        }

        walk(root)
        return sb.toString().trim()
    }

    // ---------- ট্যাপ ----------

    /** লেখা বা description মিললে সেই বাটনে ক্লিক */
    fun clickText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val matches = root.findAccessibilityNodeInfosByText(text)
        val target = matches.firstOrNull { it.isVisibleToUser }
            ?: matches.firstOrNull()
            ?: findByDesc(root, text)
            ?: return false
        return clickNode(target)
    }

    private fun clickNode(n: AccessibilityNodeInfo): Boolean {
        var cur: AccessibilityNodeInfo? = n
        var hops = 0
        while (cur != null && hops < 8) {
            if (cur.isClickable && cur.isEnabled) {
                return cur.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            cur = cur.parent
            hops++
        }
        val r = Rect()
        n.getBoundsInScreen(r)
        return if (r.width() > 0 && r.height() > 0) tap(r.exactCenterX(), r.exactCenterY()) else false
    }

    private fun findByDesc(n: AccessibilityNodeInfo?, q: String): AccessibilityNodeInfo? {
        if (n == null) return null
        val d = n.contentDescription?.toString()
        if (d != null && d.contains(q, ignoreCase = true) && n.isVisibleToUser) return n
        for (i in 0 until n.childCount) {
            findByDesc(n.getChild(i), q)?.let { return it }
        }
        return null
    }

    // ---------- টাইপ ----------

    /** ফোকাস করা (না থাকলে প্রথম) লেখার বক্সে টাইপ করে */
    fun typeText(text: String, append: Boolean = false): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.takeIf { it.isEditable }
            ?: findEditable(root)
            ?: return false
        val base = if (append && !node.isShowingHintText) node.text?.toString().orEmpty() else ""
        val args = Bundle()
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, base + text)
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    /** কীবোর্ডের Enter / Search (Android 11+) */
    fun pressEnter(): Boolean {
        if (Build.VERSION.SDK_INT < 30) return false
        val root = rootInActiveWindow ?: return false
        val node = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        return node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id)
    }

    private fun findEditable(n: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (n == null) return null
        if (n.isEditable && n.isVisibleToUser) return n
        for (i in 0 until n.childCount) {
            findEditable(n.getChild(i))?.let { return it }
        }
        return null
    }

    // ---------- স্ক্রল ----------

    fun scroll(down: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = findScrollable(root) ?: return false
        return node.performAction(
            if (down) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        )
    }

    private fun findScrollable(n: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (n == null) return null
        if (n.isScrollable && n.isVisibleToUser) return n
        for (i in 0 until n.childCount) {
            findScrollable(n.getChild(i))?.let { return it }
        }
        return null
    }

    // ---------- জেসচার ----------

    fun tap(x: Float, y: Float): Boolean {
        val p = Path().apply { moveTo(x, y) }
        return gesture(p, 60)
    }

    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, ms: Long = 350): Boolean {
        val p = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
        return gesture(p, ms)
    }

    private fun gesture(p: Path, ms: Long): Boolean {
        val g = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(p, 0, ms))
            .build()
        return dispatchGesture(g, null, null)
    }

    companion object {
        @Volatile var instance: LiaAccessibilityService? = null
        @Volatile var currentPackage: String = ""
    }
}
