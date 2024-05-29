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
import coil.compose.rememberAsyncImagePainter
import com.example.mlkit_textrecog.ui.theme.MLKit_TextRecogTheme
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MLKit_TextRecogTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var imgUri by remember { mutableStateOf<Uri?>(null) }
    var recognizedText by remember { mutableStateOf<String>("") }
    var translatedText by remember { mutableStateOf<String>("") }

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imgUri = uri
        uri?.let { processImage(context, it) { recognizedText = it; translateText(context, recognizedText) { translatedText = it } } }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally

    ) {
        imgUri?.let { uri ->
            Image(
                modifier = Modifier
                    .padding(16.dp)
                    .height(200.dp)
                    .fillMaxSize(),
                painter = rememberAsyncImagePainter(model = uri),
                contentDescription = null
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            pickMedia.launch("image/*")
        }) {
            Text(text = "Pick Image")
        }


        Spacer(modifier = Modifier.height(16.dp))

        Text(text = recognizedText)

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = translatedText)


    }
}


fun processImage(context: Context, uri: Uri, onResult: (String) -> Unit) {
    val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
    val image = InputImage.fromFilePath(context, uri)
    recognizer.process(image).addOnSuccessListener { visionText ->
        onResult(visionText.text)
    }.addOnFailureListener { e ->
        Log.e("TextRecognition", "Text recognition failed", e)
        onResult("")
    }
}

fun translateText(context: Context, text: String, onResult: (String) -> Unit) {
    val options = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.KOREAN)
        .setTargetLanguage(TranslateLanguage.ENGLISH)
        .build()
    val translator = Translation.getClient(options)

    val conditions = DownloadConditions.Builder()
        .requireWifi()
        .build()

    translator.downloadModelIfNeeded(conditions).addOnSuccessListener {
        translator.translate(text).addOnSuccessListener { translatedText ->
            onResult(translatedText)
        }.addOnFailureListener { e ->
            Log.e("Translation", "Translation failed", e)
            onResult("")
        }
    }.addOnFailureListener { e ->
        Log.e("Translation", "Model download failed", e)
        onResult("")
    }
}
@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MLKit_TextRecogTheme {
        MainScreen()
    }
}