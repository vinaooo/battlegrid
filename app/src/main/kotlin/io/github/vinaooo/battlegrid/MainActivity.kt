package io.github.vinaooo.battlegrid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dagger.hilt.android.AndroidEntryPoint
import io.github.vinaooo.vinkit.core.ThemeColor
import io.github.vinaooo.vinkit.designsystem.VinkitTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            // ponytail: scaffold screen; the game screen replaces it in milestone 5.
            // Teal is BattleGrid's brand color.
            VinkitTheme(themeColor = ThemeColor.TEAL) {
                Surface(Modifier.fillMaxSize()) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displayMedium)
                    }
                }
            }
        }
    }
}
