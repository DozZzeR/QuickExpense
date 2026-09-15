package dev.keslorod.quickexpense.voice

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.keslorod.quickexpense.BuildConfig
import dev.keslorod.quickexpense.R

class VoiceRecognizerHandle(val start: () -> Unit)

// Some devices resolve the implicit RECOGNIZE_SPEECH intent to this on-device fallback
// ("Speech Services by Google") instead of the full Google app's cloud recognizer, with no
// chooser shown — and it's noticeably worse at rare proper nouns/brand names. Prefer the real
// Google app explicitly when it's installed.
private const val PREFERRED_RECOGNIZER_PACKAGE = "com.google.android.googlequicksearchbox"

/** Maps the app's stored language code ("", "en", "ru", "sr") to a locale tag the system speech
 * recognizer understands. Blank (the "system default" setting) leaves EXTRA_LANGUAGE unset so
 * the recognizer just follows the device's own locale. */
fun languageCodeToRecognizerTag(languageCode: String): String? = when (languageCode) {
    "en" -> "en-US"
    "ru" -> "ru-RU"
    "sr" -> "sr-RS"
    else -> null
}

/**
 * Wraps the system speech-recognition dialog — the same one behind e.g. Google Search's mic.
 * No RECORD_AUDIO permission needed here: the assistant app that handles the intent holds it and
 * only ever hands this app back the transcribed text.
 */
@Composable
fun rememberVoiceRecognizerLauncher(languageTag: String?, onResult: (List<String>) -> Unit): VoiceRecognizerHandle {
    val context = LocalContext.current
    val notAvailableMessage = stringResource(R.string.voice_not_available)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            if (BuildConfig.DEBUG) Log.d("VoiceInput", "recognizer returned no result, resultCode=${result.resultCode}")
            onResult(emptyList())
            return@rememberLauncherForActivityResult
        }
        val alternatives = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).orEmpty()
        if (BuildConfig.DEBUG) Log.d("VoiceInput", "recognized alternatives: $alternatives")
        onResult(alternatives)
    }
    return VoiceRecognizerHandle {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            // WEB_SEARCH is tuned for short queries with proper nouns/brand names — a much
            // closer fit for "amount + merchant" than FREE_FORM's general dictation model.
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
            // Ask for several guesses: a lower-ranked alternative sometimes spells a brand
            // name the way it's actually stored even when the top guess mangles it (see
            // VoiceExpenseParser.parseBest, which tries all of them).
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            if (!languageTag.isNullOrBlank()) {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            }
            setPackage(PREFERRED_RECOGNIZER_PACKAGE)
        }
        if (intent.resolveActivity(context.packageManager) == null) {
            // Preferred package isn't installed or doesn't handle this intent — fall back to
            // whatever's actually available instead of failing outright.
            intent.setPackage(null)
        }
        try {
            launcher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            // No app on the device handles speech recognition at all (rare, but some ROMs/
            // regions ship without one) — tell the user instead of silently doing nothing.
            Toast.makeText(context, notAvailableMessage, Toast.LENGTH_LONG).show()
            onResult(emptyList())
        }
    }
}
