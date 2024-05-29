package com.example.mlkit_textrecog

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.mlkit_textrecog.ui.theme.MLKit_TextRecogTheme
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MLKit_TextRecogTheme {
                AppNavigator()
            }
        }
    }
}

@Composable
fun AppNavigator() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "language_selection") {
        composable("language_selection") { LanguageSelectionScreen(navController) }
        composable("image_translation/{selectedLanguage}") { backStackEntry ->
            val selectedLanguage = backStackEntry.arguments?.getString("selectedLanguage")
            selectedLanguage?.let { ImageTranslationScreen(navController, it) }
        }
    }
}

@Composable
fun LanguageSelectionScreen(navController: NavHostController, modifier: Modifier = Modifier) {
    val languages = listOf(
        "Chinese" to TranslateLanguage.CHINESE,
        "Devanagari" to TranslateLanguage.HINDI,
        "Japanese" to TranslateLanguage.JAPANESE,
        "Korean" to TranslateLanguage.KOREAN,
        "English" to TranslateLanguage.ENGLISH
    )

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Select Language")
        languages.forEach { (name, lang) ->
            Button(onClick = {
                navController.navigate("image_translation/$lang")
            }) {
                Text(name)
            }
        }
    }
}

@Composable
fun ImageTranslationScreen(navController: NavHostController, selectedLanguage: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var imgUri by remember { mutableStateOf<Uri?>(null) }
    var recognizedText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imgUri = uri
        uri?.let {
            CoroutineScope(Dispatchers.IO).launch {
                val recognized = recognizeTextFromImage(context, it)
                val translated = translateText(context, recognized, selectedLanguage)
                recognizedText = recognized
                translatedText = translated
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        imgUri?.let {
            Image(
                painter = rememberAsyncImagePainter(model = it),
                contentDescription = null,
                modifier = Modifier
                    .padding(16.dp)
                    .height(200.dp)
                    .fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { pickImageLauncher.launch("image/*") }) {
            Text("Pick Image")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = recognizedText)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = translatedText)
    }
}

suspend fun recognizeTextFromImage(context: Context, uri: Uri): String {
    return try {
        val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
        val image = InputImage.fromFilePath(context, uri)
        val result = recognizer.process(image).await()
        result.text
    } catch (e: Exception) {
        Log.e("TextRecognition", "Text recognition failed", e)
        ""
    }
}

suspend fun translateText(context: Context, text: String, targetLanguage: String): String {
    return try {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.KOREAN)
            .setTargetLanguage(targetLanguage)
            .build()
        val translator = Translation.getClient(options)
        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()
        translator.downloadModelIfNeeded(conditions).await()
        translator.translate(text).await()
    } catch (e: Exception) {
        Log.e("Translation", "Translation failed", e)
        ""
    }
}

@Preview(showBackground = true)
@Composable
fun LanguageSelectionScreenPreview() {
    MLKit_TextRecogTheme {
        LanguageSelectionScreen(rememberNavController())
    }
}

@Preview(showBackground = true)
@Composable
fun ImageTranslationScreenPreview() {
    MLKit_TextRecogTheme {
        ImageTranslationScreen(rememberNavController(), TranslateLanguage.ENGLISH)
    }
}
