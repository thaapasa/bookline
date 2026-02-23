package fi.pomeranssi.bookline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import fi.pomeranssi.bookline.ui.navigation.BooklineApp
import fi.pomeranssi.bookline.ui.theme.BooklineTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BooklineTheme {
                BooklineApp()
            }
        }
    }
}
