package com.example.mlkit_textrecog

import android.net.Uri
import android.os.Bundle
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.mlkit_textrecog.ui.theme.MLKit_TextRecogTheme


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
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally

    ) {
        var imgUri by remember { mutableStateOf<Uri?>(null) }

        val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            imgUri = uri
        }

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
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MLKit_TextRecogTheme {
        MainScreen()
    }
}