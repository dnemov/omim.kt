package com.mapswithme.util

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.mapswithme.util.ConnectionState
import com.mapswithme.util.Graphics
import com.mapswithme.util.HttpClient

import com.mapswithme.util.Language
import com.mapswithme.util.Utils.isIntentSupported
import com.mapswithme.util.concurrency.UiThread

object InputUtils {
    private var mVoiceInputSupported: Boolean? = null
    fun isVoiceInputSupported(context: Context?): Boolean {
        if (mVoiceInputSupported == null) mVoiceInputSupported =
            isIntentSupported(
                context!!,
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            )
        return mVoiceInputSupported!!
    }

    fun createIntentForVoiceRecognition(promptText: String?): Intent {
        val vrIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        vrIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH
        )
            .putExtra(RecognizerIntent.EXTRA_PROMPT, promptText)
        return vrIntent
    }

    /**
     * Get most confident recognition result or null if nothing is available
     */
    fun getBestRecognitionResult(vrIntentResult: Intent): String? {
        val recognizedStrings =
            vrIntentResult.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?: return null
        return if (recognizedStrings.isEmpty()) null else recognizedStrings[0]
    }

    private fun showKeyboardSync(input: View?) {
        if (input != null) (input.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun showKeyboardDelayed(input: View?, delay: Int) {
        if (input == null) return
        UiThread.runLater(Runnable { showKeyboardSync(input) }, delay.toLong())
    }

    fun showKeyboard(input: View?) {
        showKeyboardDelayed(input, 100)
    }

    fun hideKeyboard(view: View?) {
        val imm =
            view?.context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    /*
   Hacky method to remove focus from the only EditText at activity
   */
    fun removeFocusEditTextHack(editText: EditText) {
        editText.isFocusableInTouchMode = false
        editText.isFocusable = false
        editText.isFocusableInTouchMode = true
        editText.isFocusable = true
    }
}