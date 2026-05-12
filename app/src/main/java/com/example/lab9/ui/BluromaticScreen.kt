package com.example.lab9.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lab9.R
import com.example.lab9.data.BlurAmount
import com.example.lab9.ui.theme.Lab9Theme
import java.io.File

@Composable
fun BluromaticScreen(blurViewModel: BlurViewModel = viewModel(factory = BlurViewModel.Factory)) {
    val uiState by blurViewModel.blurUiState.collectAsStateWithLifecycle()
    val layoutDirection = LocalLayoutDirection.current
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(
                start = WindowInsets.safeDrawing.asPaddingValues()
                    .calculateStartPadding(layoutDirection),
                end = WindowInsets.safeDrawing.asPaddingValues()
                    .calculateEndPadding(layoutDirection)
            )
    ) {
        BluromaticScreenContent(
            blurUiState = uiState,
            blurAmountOptions = blurViewModel.blurAmount,
            applyBlur = blurViewModel::applyBlur,
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(dimensionResource(R.dimen.padding_medium))
        )
    }
}

@Composable
fun BluromaticScreenContent(
    blurUiState: BlurUiState,
    blurAmountOptions: List<BlurAmount>,
    applyBlur: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedValue by rememberSaveable { mutableIntStateOf(1) }
    val context = LocalContext.current

    Column(modifier = modifier) {
        Image(
            painter = painterResource(R.drawable.android_cupcake),
            contentDescription = stringResource(R.string.description_image),
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            contentScale = ContentScale.Fit,
        )
        BlurAmountContent(
            selectedValue = selectedValue,
            blurAmounts = blurAmountOptions,
            modifier = Modifier.fillMaxWidth(),
            onSelectedValueChange = { selectedValue = it }
        )
        BlurActions(
            blurUiState = blurUiState,
            onStartClick = { applyBlur(selectedValue) },
            onSeeFileClick = { currentUri ->
                showBlurredImage(context, currentUri)
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BlurActions(
    blurUiState: BlurUiState,
    onStartClick: () -> Unit,
    onSeeFileClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small))
    ) {
        // Nút Bắt đầu
        Button(
            onClick = onStartClick,
            modifier = Modifier.weight(1f),
            enabled = blurUiState !is BlurUiState.Loading
        ) {
            Text(stringResource(R.string.start))
        }

        // Nút Xem ảnh (Luôn hiển thị, chỉ bật khi có ảnh)
        Button(
            onClick = {
                if (blurUiState is BlurUiState.Complete) {
                    onSeeFileClick(blurUiState.outputUri)
                }
            },
            modifier = Modifier.weight(1f),
            enabled = blurUiState is BlurUiState.Complete
        ) {
            Text(stringResource(R.string.see_file))
        }
    }
}

@Composable
private fun BlurAmountContent(
    selectedValue: Int,
    blurAmounts: List<BlurAmount>,
    modifier: Modifier = Modifier,
    onSelectedValueChange: (Int) -> Unit
) {
    Column(
        modifier = modifier.selectableGroup()
    ) {
        Text(
            text = stringResource(R.string.blur_title),
            style = MaterialTheme.typography.headlineSmall
        )
        blurAmounts.forEach { amount ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        role = Role.RadioButton,
                        selected = (selectedValue == amount.blurAmount),
                        onClick = { onSelectedValueChange(amount.blurAmount) }
                    )
                    .size(48.dp)
            ) {
                RadioButton(
                    selected = (selectedValue == amount.blurAmount),
                    onClick = null,
                    modifier = Modifier.size(48.dp),
                    colors = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.primary
                    )
                )
                Text(stringResource(amount.blurAmountRes))
            }
        }
    }
}

private fun showBlurredImage(context: Context, currentUri: String) {
    if (currentUri.isEmpty()) return
    val uri = currentUri.toUri()
    
    val intent = Intent(Intent.ACTION_VIEW).apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    if (uri.scheme == "content") {
        intent.setDataAndType(uri, "image/*")
    } else {
        val file = File(uri.path ?: "")
        if (file.exists()) {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            intent.setDataAndType(contentUri, "image/*")
        } else {
            intent.setDataAndType(uri, "image/*")
        }
    }
    
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // Handle error
    }
}

@Preview(showBackground = true)
@Composable
fun BluromaticScreenContentPreview() {
    Lab9Theme {
        BluromaticScreenContent(
            blurUiState = BlurUiState.Default,
            blurAmountOptions = listOf(BlurAmount(R.string.blur_lv_1, 1)),
            {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
