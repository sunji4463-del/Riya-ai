package com.lia.assistant.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lia.assistant.LiaAccessibilityService
import com.lia.assistant.PhoneControl
import com.lia.assistant.data.KeyStore
import com.lia.assistant.ui.theme.LiaColors

private fun ok(b: Boolean, name: String) = "$name ${if (b) "✓" else "✗"}"

@Composable
fun PhoneControlScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val main = remember { Handler(Looper.getMainLooper()) }
    val store = remember { KeyStore(ctx) }

    var tick by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val ob = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) tick++
        }
        owner.lifecycle.addObserver(ob)
        onDispose { owner.lifecycle.removeObserver(ob) }
    }

    fun granted(p: String) =
        ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { tick++ }

    val accOn = remember(tick) {
        LiaAccessibilityService.instance != null || PhoneControl.accessibilityEnabled(ctx)
    }
    val overlayOn = remember(tick) { Settings.canDrawOverlays(ctx) }
    val callOn = remember(tick) {
        granted(Manifest.permission.CALL_PHONE) && granted(Manifest.permission.READ_CONTACTS)
    }

    var appName by remember { mutableStateOf("") }
    var tapText by remember { mutableStateOf("") }
    var typeText by remember { mutableStateOf("") }
    var cc by remember { mutableStateOf(store.countryCode) }
    var confirm by remember { mutableStateOf(store.confirmActions) }

    fun toast(s: String) = Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show()

    fun act(delayMs: Long = 0, block: (LiaAccessibilityService) -> String) {
        val go = {
            val s = LiaAccessibilityService.instance
            PhoneControl.log =
                if (s == null) "Accessibility চালু নেই। উপরের Enable চাপো।" else block(s)
        }
        if (delayMs > 0) {
            toast("${delayMs / 1000} সেকেন্ডের মধ্যে অন্য অ্যাপে যাও")
            main.postDelayed({ go() }, delayMs)
        } else {
            go()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = LiaColors.TextMain)
            }
            Text("Phone control", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = LiaColors.TextMain)
        }
        Text(
            "Lia-কে ফোন চালানোর অনুমতি দাও। সব কাজ শুধু তোমার কমান্ডে হবে।",
            color = LiaColors.TextSub, fontSize = 13.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        StatusCard(
            "Accessibility service",
            "অ্যাপ ট্যাপ, টাইপ, স্ক্রল, স্ক্রিন লক ও স্ক্রিন পড়ার জন্য।",
            accOn, "Open Accessibility settings"
        ) { PhoneControl.openAccessibilitySettings(ctx) }

        Spacer(Modifier.height(12.dp))
        StatusCard(
            "Display over other apps",
            "ব্যাকগ্রাউন্ড থেকে অ্যাপ খুলতে লাগতে পারে (Android 10+)।",
            overlayOn, "Allow display over apps"
        ) { PhoneControl.openOverlaySettings(ctx) }

        Spacer(Modifier.height(12.dp))
        StatusCard(
            "Calls & contacts",
            "নাম বলে কল করতে ও WhatsApp মেসেজ পাঠাতে লাগে। কন্টাক্ট শুধু ফোনেই থাকে।",
            callOn, "Allow calls & contacts"
        ) {
            permLauncher.launch(
                arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_CONTACTS)
            )
        }

        Spacer(Modifier.height(12.dp))
        LiaCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Accessibility-র সুইচ ধূসর হলে", fontWeight = FontWeight.SemiBold, color = LiaColors.TextMain)
                Text(
                    "Android 13+ এ APK ফাইল থেকে ইনস্টল করা অ্যাপে সুইচ বন্ধ থাকে। App info খুলে ওপরে ডানদিকের ⋮ মেনু → \"Allow restricted settings\" চাপো। তারপর Accessibility-তে গিয়ে LIA চালু করো।",
                    color = LiaColors.TextSub, fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
                GradientButton("Open LIA app info") { PhoneControl.openAppInfo(ctx) }
            }
        }

        Text(
            "AGENT SETTINGS", fontSize = 12.sp, color = LiaColors.PurpleLight, letterSpacing = 2.sp,
            modifier = Modifier.padding(top = 22.dp, bottom = 8.dp)
        )
        LiaCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("কল/মেসেজের আগে জিজ্ঞেস করো", fontWeight = FontWeight.SemiBold, color = LiaColors.TextMain)
                    Text(
                        "চালু থাকলে Lia \"হ্যাঁ\" শুনে তবেই কল বা মেসেজ করবে।",
                        fontSize = 12.sp, color = LiaColors.TextSub
                    )
                }
                Switch(
                    checked = confirm,
                    onCheckedChange = { confirm = it; store.confirmActions = it },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = LiaColors.Purple,
                        checkedThumbColor = Color.White
                    )
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        LiaCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("দেশের কোড (WhatsApp নম্বরের জন্য)", fontWeight = FontWeight.SemiBold, color = LiaColors.TextMain)
                Text(
                    "01712... নম্বর থেকে 8801712... বানাতে। বাংলাদেশ = 880, ভারত = 91।",
                    fontSize = 12.sp, color = LiaColors.TextSub,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                Field(cc, {
                    cc = it.filter { c -> c.isDigit() }.take(4)
                    store.countryCode = cc
                }, "880")
            }
        }

        Text(
            "TEST ACTIONS", fontSize = 12.sp, color = LiaColors.PurpleLight, letterSpacing = 2.sp,
            modifier = Modifier.padding(top = 22.dp, bottom = 8.dp)
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TestBtn("Home", Modifier.weight(1f)) { act { ok(it.home(), "Home") } }
            TestBtn("Back", Modifier.weight(1f)) { act { ok(it.back(), "Back") } }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TestBtn("Recents", Modifier.weight(1f)) { act { ok(it.recents(), "Recents") } }
            TestBtn("Notifications", Modifier.weight(1f)) { act { ok(it.notifications(), "Notifications") } }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TestBtn("Lock screen", Modifier.weight(1f)) { act { ok(it.lockScreen(), "Lock") } }
            TestBtn("Screenshot", Modifier.weight(1f)) { act { ok(it.screenshot(), "Screenshot") } }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TestBtn("Power menu", Modifier.weight(1f)) { act { ok(it.powerMenu(), "Power menu") } }
            TestBtn("Scroll down (4s)", Modifier.weight(1f)) {
                act(4000) { ok(it.scroll(true), "Scroll") }
            }
        }
        Spacer(Modifier.height(10.dp))
        TestBtn("Read screen (4s)", Modifier.fillMaxWidth()) { act(4000) { it.readScreen() } }

        Spacer(Modifier.height(16.dp))
        Field(appName, { appName = it }, "App name (e.g. YouTube)")
        Spacer(Modifier.height(8.dp))
        TestBtn("Open app", Modifier.fillMaxWidth()) {
            PhoneControl.log = PhoneControl.openApp(ctx, appName)
        }

        Spacer(Modifier.height(16.dp))
        Field(tapText, { tapText = it }, "Text to tap (e.g. Search)")
        Spacer(Modifier.height(8.dp))
        TestBtn("Tap text (4s)", Modifier.fillMaxWidth()) {
            act(4000) { ok(it.clickText(tapText), "Tap \"$tapText\"") }
        }

        Spacer(Modifier.height(16.dp))
        Field(typeText, { typeText = it }, "Text to type")
        Spacer(Modifier.height(8.dp))
        TestBtn("Type into focused box (4s)", Modifier.fillMaxWidth()) {
            act(4000) { ok(it.typeText(typeText), "Type") }
        }

        Text(
            "RESULT", fontSize = 12.sp, color = LiaColors.PurpleLight, letterSpacing = 2.sp,
            modifier = Modifier.padding(top = 22.dp, bottom = 8.dp)
        )
        LiaCard(Modifier.fillMaxWidth()) {
            SelectionContainer {
                Text(
                    PhoneControl.log.ifEmpty { "ফলাফল এখানে দেখাবে" },
                    color = LiaColors.TextMain, fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp, modifier = Modifier.padding(14.dp)
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatusCard(title: String, sub: String, on: Boolean, btn: String, onClick: () -> Unit) {
    LiaCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title, fontWeight = FontWeight.SemiBold, color = LiaColors.TextMain,
                    modifier = Modifier.weight(1f)
                )
                val c = if (on) LiaColors.Green else LiaColors.Gold
                Text(
                    if (on) "On" else "Off", color = c, fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(c.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Text(
                sub, color = LiaColors.TextSub, fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            GradientButton(btn) { onClick() }
        }
    }
}

@Composable
private fun TestBtn(text: String, modifier: Modifier, onClick: () -> Unit) {
    LiaCard(modifier.height(48.dp), onClick = onClick) {
        Text(
            text, color = LiaColors.TextMain, fontSize = 13.sp,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, hint: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = liaFieldColors(),
        placeholder = { Text(hint, color = LiaColors.TextSub) }
    )
}
