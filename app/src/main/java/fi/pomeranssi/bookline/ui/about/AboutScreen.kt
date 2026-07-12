package fi.pomeranssi.bookline.ui.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import fi.pomeranssi.bookline.BuildConfig
import fi.pomeranssi.bookline.R
import fi.pomeranssi.bookline.ui.theme.BooklineTheme

private const val PRIVACY_POLICY_URL = "https://pomeranssi.fi/bookline/privacy-policy.html"
private const val GITHUB_URL = "https://github.com/thaapasa/bookline"
private const val APACHE_LICENSE_URL = "https://www.apache.org/licenses/LICENSE-2.0"

// Shipped (implementation) dependencies, all Apache-2.0. Keep in sync with
// app/build.gradle.kts when dependencies change.
private val OPEN_SOURCE_LIBRARIES =
    listOf(
        "AndroidX Jetpack (Compose, Material 3, Navigation, Room, Lifecycle)",
        "Kotlin & kotlinx.coroutines",
        "Coil 3",
        "OkHttp & Okio",
        "Reorderable",
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        AboutContent(modifier = Modifier.padding(innerPadding))
    }
}

@Composable
private fun AboutContent(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text =
                        stringResource(
                            R.string.about_version,
                            BuildConfig.VERSION_NAME,
                            BuildConfig.VERSION_CODE,
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AppIcon(modifier = Modifier.size(64.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.about_tagline),
            style = MaterialTheme.typography.bodyLarge,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.about_description),
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.about_disclaimer),
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.about_license),
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.about_open_source_title),
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.about_open_source_intro),
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.height(4.dp))

        OPEN_SOURCE_LIBRARIES.forEach { library ->
            Text(
                text = "• $library",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        ExternalLinkButton(
            label = stringResource(R.string.about_apache_license),
            onClick = { uriHandler.openUri(APACHE_LICENSE_URL) },
        )

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        ExternalLinkButton(
            label = stringResource(R.string.about_privacy_policy),
            onClick = { uriHandler.openUri(PRIVACY_POLICY_URL) },
        )
        ExternalLinkButton(
            label = stringResource(R.string.about_source_code),
            onClick = { uriHandler.openUri(GITHUB_URL) },
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AppIcon(modifier: Modifier = Modifier) {
    if (LocalInspectionMode.current) {
        // Launcher icon is an adaptive-icon XML; not renderable in previews.
        Icon(
            imageVector = Icons.Default.AutoStories,
            contentDescription = null,
            modifier = modifier,
            tint = MaterialTheme.colorScheme.primary,
        )
        return
    }
    val context = LocalContext.current
    val appIcon =
        remember(context) {
            context.packageManager
                .getApplicationIcon(context.packageName)
                .toBitmap()
                .asImageBitmap()
        }
    Image(
        bitmap = appIcon,
        contentDescription = null,
        modifier = modifier,
    )
}

@Composable
private fun ExternalLinkButton(
    label: String,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label)
    }
}

@Preview(showBackground = true)
@Composable
private fun AboutScreenPreview() {
    BooklineTheme(dynamicColor = false) {
        AboutScreen(onNavigateBack = {})
    }
}
