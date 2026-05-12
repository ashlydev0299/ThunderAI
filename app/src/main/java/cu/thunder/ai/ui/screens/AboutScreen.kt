package cu.thunder.ai.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import cu.thunder.ai.R
import cu.thunder.ai.ui.components.PersianText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val sourceUri = stringResource(R.string.github_source)
    val licenseUri = stringResource(R.string.license_link)

    Scaffold(
        topBar = {
            Surface(shadowElevation = 8.dp) {
                TopAppBar(
                    title = { PersianText(stringResource(R.string.about), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Outlined.ArrowBack, "Volver")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "\u26A1",
                style = MaterialTheme.typography.displayMedium
            )

            PersianText(
                text = "ThunderAI",
                fontSize = androidx.compose.ui.unit.TextUnit.Unspecified,
                fontWeight = FontWeight.Bold
            )

            PersianText(
                text = "Versión 1.0",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            PersianText(
                text = stringResource(R.string.license_header),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = sourceUri,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { uriHandler.openUri(sourceUri) }
            )

            Text(
                text = licenseUri,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { uriHandler.openUri(licenseUri) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            PersianText(
                text = stringResource(R.string.about_app),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}