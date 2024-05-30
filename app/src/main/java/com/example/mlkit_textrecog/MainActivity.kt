package com.example.mlkit_textrecog

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.mlkit_textrecog.ui.theme.MLKit_TextRecogTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var lastProcessedBitmap: Bitmap? = null  // Add this variable to hold the last processed bitmap


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        setContent {
            MLKit_TextRecogTheme {
                AppNavigator()
            }
        }
    }

    fun signInWithEmailAndPassword(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    firebaseAnalytics.setUserProperty("favorite_language", "Korean")
                    logUserActivity("login", "User logged in with email: $email")
                    onSuccess()
                } else {
                    task.exception?.let { onFailure(it) }
                }
            }
    }

    fun logUserActivity(activity: String, details: String) {
        val logData = hashMapOf(
            "activity" to activity,
            "details" to details,
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("user_logs")
            .add(logData)
            .addOnSuccessListener { documentReference ->
                Log.d("Firestore", "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error adding document", e)
            }
    }

    fun logSelectedLanguage(userId: String, language: String) {
        val logData = hashMapOf(
            "userId" to userId,
            "selectedLanguage" to language,
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("language_selections")
            .add(logData)
            .addOnSuccessListener { documentReference ->
                Log.d("Firestore", "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error adding document", e)
            }
        val bundle = Bundle().apply {
            putString("user_id", userId)
            putString("selected_language", language)
        }
        firebaseAnalytics.logEvent("select_language", bundle)
    }


    suspend fun uploadImageAndLog(uri: Uri, context: Context): String {
        return try {
            val storageRef = storage.reference
            val imageRef = storageRef.child("images/${uri.lastPathSegment}")
            val uploadTask = imageRef.putFile(uri).await()

            if (uploadTask.task.isSuccessful) {
                val downloadUri = imageRef.downloadUrl.await()
                val imageUrl = downloadUri.toString()
                logUserActivity("image_upload", "Image uploaded: $imageUrl")

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Image uploaded successfully!", Toast.LENGTH_SHORT)
                        .show()
                }


                imageUrl
            } else {
                throw IOException("Upload task was not successful.")
            }
        } catch (e: Exception) {
            Log.e("Storage", "Image upload failed", e)
            throw e
        }
    }

    suspend fun recognizeTextFromImage(context: Context, uri: Uri): Text? {
        return try {
            val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
            val image = InputImage.fromFilePath(context, uri)
            recognizer.process(image).await()
        } catch (e: Exception) {
            Log.e("TextRecognition", "Text recognition failed", e)
            null
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

    fun saveHistory(userId: String, imageUrl: String, translatedText: String) {
        val historyData = hashMapOf(
            "userId" to userId,
            "imageUrl" to imageUrl,
            "translatedText" to translatedText,
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("translation_history")
            .add(historyData)
            .addOnSuccessListener { documentReference ->
                Log.d("Firestore", "History saved with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error saving history", e)
            }
    }

    fun getHistory(userId: String, onSuccess: (List<HistoryItem>) -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("translation_history")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(5)
            .get()
            .addOnSuccessListener { result ->
                val historyList = result.map { document ->
                    HistoryItem(
                        imageUrl = document.getString("imageUrl") ?: "",
                        translatedText = document.getString("translatedText") ?: "",
                        timestamp = document.getLong("timestamp") ?: 0L
                    )
                }
                onSuccess(historyList)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    data class HistoryItem(val imageUrl: String, val translatedText: String, val timestamp: Long)

    suspend fun drawBoundingBoxesAndText(context: Context, uri: Uri, recognizedText: Text): Bitmap? {
        val originalBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        val rotatedBitmap = rotateBitmapIfRequired(context, originalBitmap, uri)
        val mutableBitmap = rotatedBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val paint = Paint().apply {
            color = android.graphics.Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }
        val textPaint = Paint().apply {
            color = android.graphics.Color.BLUE
            textSize = 40f
        }

        recognizedText.textBlocks.forEach { block ->
            block.boundingBox?.let { rect ->
                canvas.drawRect(rect, paint)
                canvas.drawText(block.text, rect.left.toFloat(), rect.bottom.toFloat(), textPaint)
            }
        }

        // Store the last processed bitmap
        lastProcessedBitmap = mutableBitmap

        return mutableBitmap
    }

    fun rotateBitmapIfRequired(context: Context, img: Bitmap, selectedImage: Uri): Bitmap {
        val input = context.contentResolver.openInputStream(selectedImage)
        val ei = input?.let { ExifInterface(it) }
        val orientation = ei?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270f)
            else -> img
        }
    }

    fun rotateImage(img: Bitmap, degree: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree)
        return Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
    }

    fun getLastProcessedBitmap(): Bitmap? {
        return lastProcessedBitmap
    }
}
@Composable
fun AppNavigator() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "login") {
        composable("login") { LoginScreen(navController) }
        composable("language_selection") { LanguageSelectionScreen(navController) }
        composable("image_translation/{selectedLanguage}") { backStackEntry ->
            val selectedLanguage = backStackEntry.arguments?.getString("selectedLanguage")
            selectedLanguage?.let { ImageTranslationScreen(navController, it) }
        }
        composable("transformable_image") {
            TransformableImageScreen(navController)
        }
        composable("history") { HistoryScreen(navController) }
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
fun LoginScreen(navController: NavHostController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    val activity = context as? MainActivity

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            activity?.signInWithEmailAndPassword(email, password, {
                navController.navigate("language_selection")
            }, { exception ->
                Log.e("LoginScreen", "Login failed", exception)
            })
        }) {
            Text("Login")
        }
    }
}

@Composable
fun ImageTranslationScreen(
    navController: NavHostController,
    selectedLanguage: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? MainActivity
    var imgUri by remember { mutableStateOf<Uri?>(null) }
    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var recognizedText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    LaunchedEffect(Unit) {
        activity?.logSelectedLanguage(userId, selectedLanguage)
    }

    val pickImageLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            imgUri = uri
            uri?.let {
                isLoading = true
                CoroutineScope(Dispatchers.IO).launch {
                    activity?.let { mainActivity ->
                        try {
                            val imageUrl = mainActivity.uploadImageAndLog(uri, context)
                            val recognized = mainActivity.recognizeTextFromImage(context, uri)
                            if (recognized != null) {
                                recognizedText = recognized.text
                                val translated = mainActivity.translateText(context, recognizedText, selectedLanguage)
                                translatedText = translated
                                processedBitmap = mainActivity.drawBoundingBoxesAndText(context, uri, recognized)
                                mainActivity.saveHistory(userId, imageUrl, translated)
                            } else {
                                recognizedText = "Failed to recognize text."
                                translatedText = ""
                            }
                        } catch (e: Exception) {
                            Log.e("ImageTranslationScreen", "Error processing image", e)
                        } finally {
                            isLoading = false
                        }
                    }
                }
            }
        }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Processing...")
        } else {
            processedBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(16.dp)
                        .height(200.dp)
                        .fillMaxWidth()
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
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { navController.navigate("history") }) {
                Text("Show My History")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { navController.navigate("transformable_image") }) {
                Text("Zoom Image")
            }
        }
    }
}
@Composable
fun TransformableImageScreen(navController: NavHostController) {
    val context = LocalContext.current
    val activity = context as? MainActivity
    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            processedBitmap = activity?.getLastProcessedBitmap()
        }
    }

    processedBitmap?.let { bitmap ->
        TransformableImage(bitmap = bitmap.asImageBitmap())
    }
}

@Composable
fun TransformableImage(
    bitmap: ImageBitmap,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var rotation by remember { mutableStateOf(0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, rotationChange ->
        scale *= zoomChange
        rotation += rotationChange
        offset += offsetChange
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                rotationZ = rotation,
                translationX = offset.x,
                translationY = offset.y
            )
            .transformable(state = state)
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
    }
}
@Composable
fun HistoryScreen(navController: NavHostController) {
    val context = LocalContext.current
    val activity = context as? MainActivity
    val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    var historyList by remember { mutableStateOf<List<MainActivity.HistoryItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        activity?.getHistory(userId, { history ->
            historyList = history
        }, { e ->
            Log.e("HistoryScreen", "Failed to fetch history", e)
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "My History", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        historyList.forEach { item ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Image(
                    painter = rememberAsyncImagePainter(model = item.imageUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .height(100.dp)
                        .fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = item.translatedText.take(50) + "...")
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Timestamp: ${convertTimestampToReadable(item.timestamp)}")
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        Button(onClick = { navController.navigateUp() }) {
            Text("Back")
        }
    }
}

fun convertTimestampToReadable(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val date = Date(timestamp)
    return sdf.format(date)
}

