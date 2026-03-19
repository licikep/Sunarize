package com.banglaread.ui.screens.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banglaread.ui.components.ToneSelector
import com.banglaread.ui.navigation.SettingsViewModel
import com.banglaread.ui.theme.BanglaReadColors
import com.banglaread.ui.theme.BanglaReadTheme
import com.banglaread.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onManageFeeds: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(containerColor=BanglaReadColors.Ink900, topBar={
        TopAppBar(title={Text("সেটিংস",style=MaterialTheme.typography.titleMedium,color=BanglaReadColors.Cream100)},
            navigationIcon={IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back",tint=BanglaReadColors.Cream200)}},
            colors=TopAppBarDefaults.topAppBarColors(containerColor=BanglaReadColors.Ink900))
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            Sec("AI সেটিংস") {
                ApiKeyField(state.geminiApiKey, state.hasApiKey, viewModel::saveApiKey)
                Spacer(Modifier.height(Spacing.md))
                ToneSelector(state.banglaTone, viewModel::setTone, Modifier.padding(horizontal=Spacing.md))
            }
            Div()
            Sec("ফিড সেটিংস") {
                Row_(Icons.Outlined.RssFeed,"ফিড ম্যানেজ করুন","${state.activeFeedCount}টি সক্রিয় ফিড",onClick=onManageFeeds)
                Toggle_(Icons.Outlined.Sync,"ব্যাকগ্রাউন্ড সিঙ্ক","প্রতি ৩০ মিনিটে স্বয়ংক্রিয় আপডেট",state.backgroundSyncEnabled,viewModel::setBackgroundSync)
            }
            Div()
            Sec("পড়ার পছন্দ") {
                Row_(Icons.Outlined.TextFields,"ডিফল্ট ভিউ",if(state.defaultView=="summary") "সারাংশ" else "পূর্ণ আর্টিকেল"){ viewModel.setDefaultView(if(state.defaultView=="summary")"full" else "summary") }
                Toggle_(Icons.Outlined.RecordVoiceOver,"টেক্সট-টু-স্পিচ","আর্টিকেল পড়ে শোনা",state.ttsEnabled,viewModel::setTtsEnabled)
                Column(Modifier.padding(horizontal=Spacing.md, vertical=Spacing.xs)) {
                    Row(verticalAlignment=Alignment.CenterVertically){ Icon(Icons.Outlined.FormatSize,null,tint=BanglaReadColors.Gold400,modifier=Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)); Text("ফন্টের আকার",style=MaterialTheme.typography.bodyMedium,color=BanglaReadColors.Cream100); Spacer(Modifier.weight(1f)); Text("${(state.fontScale*100).toInt()}%",style=MaterialTheme.typography.labelLarge,color=BanglaReadColors.Gold400) }
                    Slider(value=state.fontScale, onValueChange=viewModel::setFontScale, valueRange=0.8f..1.6f, steps=3, colors=SliderDefaults.colors(thumbColor=BanglaReadColors.Gold400, activeTrackColor=BanglaReadColors.Gold400, inactiveTrackColor=BanglaReadColors.Ink600))
                }
            }
            Div()
            Sec("ফিড ইম্পোর্ট") { Row_(Icons.Outlined.FileUpload,"OPML ফাইল আমদানি","আপনার RSS ফিড লিস্ট লোড করুন", onClick=viewModel::triggerOPMLImport) }
            Div()
            Sec("ডেটা") { Row_(Icons.Outlined.DeleteSweep,"পুরনো আর্টিকেল মুছুন","৩০ দিনের বেশি পুরনো",iconTint=BanglaReadColors.ErrorRed, onClick=viewModel::pruneOldArticles) }
            Spacer(Modifier.height(Spacing.xxl))
            Text("BanglaRead v1.0.0 — Gemini 2.0 Flash", style=MaterialTheme.typography.labelMedium, color=BanglaReadColors.Cream400, modifier=Modifier.fillMaxWidth().padding(bottom=Spacing.xl), textAlign=androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable private fun Sec(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column { Text(title, style=MaterialTheme.typography.labelLarge, color=BanglaReadColors.Gold400, modifier=Modifier.padding(horizontal=Spacing.md, vertical=Spacing.sm)); content() }
}
@Composable private fun Div() = HorizontalDivider(color=BanglaReadColors.Ink700, modifier=Modifier.padding(vertical=Spacing.sm))

@Composable private fun Row_(icon: ImageVector, title: String, subtitle: String?=null, iconTint: androidx.compose.ui.graphics.Color=BanglaReadColors.Gold400, onClick: (()->Unit)?=null) {
    val m = if (onClick!=null) Modifier.clickable(onClick=onClick) else Modifier
    Row(modifier=m.fillMaxWidth().padding(horizontal=Spacing.md,vertical=Spacing.sm), verticalAlignment=Alignment.CenterVertically) {
        Icon(icon,null,tint=iconTint,modifier=Modifier.size(20.dp)); Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)){ Text(title,style=MaterialTheme.typography.bodyMedium,color=BanglaReadColors.Cream100); if(subtitle!=null) Text(subtitle,style=MaterialTheme.typography.bodySmall,color=BanglaReadColors.Cream400) }
        if (onClick!=null) Icon(Icons.Filled.ChevronRight,null,tint=BanglaReadColors.Cream400,modifier=Modifier.size(18.dp))
    }
}
@Composable private fun Toggle_(icon: ImageVector, title: String, subtitle: String?=null, checked: Boolean, onToggle: (Boolean)->Unit) {
    Row(modifier=Modifier.fillMaxWidth().padding(horizontal=Spacing.md,vertical=Spacing.xs), verticalAlignment=Alignment.CenterVertically) {
        Icon(icon,null,tint=BanglaReadColors.Gold400,modifier=Modifier.size(20.dp)); Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)){ Text(title,style=MaterialTheme.typography.bodyMedium,color=BanglaReadColors.Cream100); if(subtitle!=null) Text(subtitle,style=MaterialTheme.typography.bodySmall,color=BanglaReadColors.Cream400) }
        Switch(checked=checked, onCheckedChange=onToggle, colors=SwitchDefaults.colors(checkedThumbColor=BanglaReadColors.Ink900,checkedTrackColor=BanglaReadColors.Gold400,uncheckedThumbColor=BanglaReadColors.Cream400,uncheckedTrackColor=BanglaReadColors.Ink600))
    }
}
@Composable private fun ApiKeyField(apiKey: String, hasKey: Boolean, onSave: (String)->Unit) {
    var local by remember { mutableStateOf("") }; var show by remember { mutableStateOf(false) }; var editing by remember { mutableStateOf(!hasKey) }
    Column(Modifier.padding(horizontal=Spacing.md)) {
        Row(verticalAlignment=Alignment.CenterVertically){ Icon(Icons.Outlined.Key,null,tint=BanglaReadColors.Gold400,modifier=Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)); Text("Gemini API Key",style=MaterialTheme.typography.bodyMedium,color=BanglaReadColors.Cream100); Spacer(Modifier.weight(1f)); if(hasKey&&!editing) TextButton(onClick={editing=true}){Text("পরিবর্তন",color=BanglaReadColors.Gold400)} }
        if (hasKey&&!editing) Text("●●●●●●●●  সংরক্ষিত ✓",style=MaterialTheme.typography.bodySmall,color=BanglaReadColors.SuccessGreen)
        else {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value=local, onValueChange={local=it}, placeholder={Text("AIza…",color=BanglaReadColors.Cream400)}, modifier=Modifier.fillMaxWidth(), shape=RoundedCornerShape(10.dp), visualTransformation=if(show) VisualTransformation.None else PasswordVisualTransformation(), singleLine=true,
                trailingIcon={IconButton(onClick={show=!show}){Icon(if(show) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,null,tint=BanglaReadColors.Cream400)}},
                colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=BanglaReadColors.Gold400,unfocusedBorderColor=BanglaReadColors.Ink600,focusedTextColor=BanglaReadColors.Cream100,unfocusedTextColor=BanglaReadColors.Cream100,cursorColor=BanglaReadColors.Gold400,focusedContainerColor=BanglaReadColors.Ink800,unfocusedContainerColor=BanglaReadColors.Ink800))
            Spacer(Modifier.height(8.dp))
            Button(onClick={onSave(local);editing=false}, enabled=local.isNotBlank(), modifier=Modifier.fillMaxWidth(), shape=RoundedCornerShape(10.dp), colors=ButtonDefaults.buttonColors(containerColor=BanglaReadColors.Gold400)){ Text("সংরক্ষণ করুন",color=BanglaReadColors.Ink900) }
        }
    }
}

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    var apiKey by remember { mutableStateOf("") }; var showKey by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(BanglaReadColors.Ink900)) {
        Column(Modifier.fillMaxSize().padding(Spacing.xl), horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.Center) {
            Text("বাংলারিড", style=MaterialTheme.typography.displayLarge, color=BanglaReadColors.Gold400)
            Spacer(Modifier.height(8.dp)); Text("বাংলায় পড়ুন, বিশ্বকে জানুন", style=MaterialTheme.typography.bodyMedium, color=BanglaReadColors.Cream400)
            Spacer(Modifier.height(Spacing.xxl))
            listOf("🌐" to "যেকোনো ভাষার আর্টিকেল বাংলায়","🧠" to "AI সারাংশ — সহজ, সাহিত্যিক বা কথ্য","📱" to "অফলাইনে সেভ করুন, যেকোনো সময় পড়ুন").forEach { (e,t) ->
                Row(Modifier.fillMaxWidth().padding(vertical=6.dp), verticalAlignment=Alignment.CenterVertically){ Text(e, fontSize=22.sp); Spacer(Modifier.width(14.dp)); Text(t,style=MaterialTheme.typography.bodyMedium,color=BanglaReadColors.Cream200) }
            }
            Spacer(Modifier.height(Spacing.xxl))
            Text("Gemini API Key", style=MaterialTheme.typography.titleMedium, color=BanglaReadColors.Cream100)
            Spacer(Modifier.height(4.dp)); Text("Google AI Studio থেকে বিনামূল্যে পান: aistudio.google.com", style=MaterialTheme.typography.bodySmall, color=BanglaReadColors.Cream400)
            Spacer(Modifier.height(Spacing.sm))
            OutlinedTextField(value=apiKey, onValueChange={apiKey=it}, placeholder={Text("AIza…",color=BanglaReadColors.Cream400)}, modifier=Modifier.fillMaxWidth(), shape=RoundedCornerShape(12.dp), singleLine=true,
                visualTransformation=if(showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon={IconButton(onClick={showKey=!showKey}){Icon(if(showKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,null,tint=BanglaReadColors.Cream400)}},
                colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=BanglaReadColors.Gold400,unfocusedBorderColor=BanglaReadColors.Ink600,focusedTextColor=BanglaReadColors.Cream100,unfocusedTextColor=BanglaReadColors.Cream100,cursorColor=BanglaReadColors.Gold400,focusedContainerColor=BanglaReadColors.Ink800,unfocusedContainerColor=BanglaReadColors.Ink800))
            Spacer(Modifier.height(Spacing.lg))
            Button(onClick={ if(apiKey.isNotBlank()) onComplete() }, enabled=apiKey.isNotBlank(), modifier=Modifier.fillMaxWidth().height(52.dp), shape=RoundedCornerShape(12.dp), colors=ButtonDefaults.buttonColors(containerColor=BanglaReadColors.Gold400)){ Text("শুরু করুন",style=MaterialTheme.typography.titleMedium,color=BanglaReadColors.Ink900) }
            Spacer(Modifier.height(Spacing.sm))
            TextButton(onClick=onComplete){ Text("এখনই না, পরে যোগ করব",color=BanglaReadColors.Cream400,style=MaterialTheme.typography.bodySmall) }
        }
    }
}
