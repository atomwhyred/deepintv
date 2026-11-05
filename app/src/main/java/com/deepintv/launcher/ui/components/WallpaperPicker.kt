package com.deepintv.launcher.ui.components

import com.deepintv.launcher.R
import androidx.compose.ui.res.stringResource
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.deepintv.launcher.data.BuiltinWallpaper
import com.deepintv.launcher.data.WallpaperSettings
import com.deepintv.launcher.ui.theme.DeepinTextPrimary
import com.deepintv.launcher.ui.theme.DeepinTextSecondary
import com.deepintv.launcher.ui.theme.brushForWallpaper

private const val THUMB_PX = 320

/** Lets the person pick a built-in gradient wallpaper or their own picture, and darken it with a
 * slider — "gestor de fondos... con capa de oscurecimiento". */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WallpaperPicker(
    settings: WallpaperSettings,
    modifier: Modifier = Modifier,
    onPickBuiltin: (String) -> Unit,
    onPickCustomUri: (String) -> Unit,
    onDimChange: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val firstRowFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstRowFocusRequester.requestFocus() }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            onPickCustomUri(uri.toString())
        }
    }

    GlassSurface(modifier = modifier.width(380.dp), cornerRadius = 20) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .focusProperties { exit = { FocusRequester.Cancel } }
                .focusRestorer()
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.wallpaper_title),
                    color = DeepinTextPrimary,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f),
                )
                FocusSurface(onClick = onDismiss, shape = RoundedCornerShape(50)) { isFocused ->
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close), tint = focusContent(isFocused, DeepinTextPrimary), modifier = Modifier.padding(6.dp))
                }
            }

            BuiltinWallpaper.entries.chunked(4).forEachIndexed { rowIndex, rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowItems.forEachIndexed { colIndex, wallpaper ->
                    val index = rowIndex * 4 + colIndex
                    val selected = settings.customUri == null && (settings.builtinId ?: BuiltinWallpaper.MIDNIGHT.id) == wallpaper.id
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FocusSurface(
                            onClick = { onPickBuiltin(wallpaper.id) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .size(64.dp)
                                .then(if (index == 0) Modifier.focusRequester(firstRowFocusRequester) else Modifier),
                        ) { _ ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(brushForWallpaper(wallpaper.id)),
                                contentAlignment = Alignment.Center,
                            ) {
                                wallpaper.imageRes?.let {
                                    // Thumbnail-sized decode: the full 1080p JPG would cost ~8 MB each.
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(it)
                                            .size(THUMB_PX)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                                if (selected) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = DeepinTextPrimary)
                                }
                            }
                        }
                        Text(
                            text = stringResource(wallpaper.labelRes),
                            color = DeepinTextSecondary,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
            }

            FocusSurface(
                onClick = { pickImageLauncher.launch(arrayOf("image/*")) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) { isFocused ->
                Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Image, contentDescription = null, tint = focusContent(isFocused, DeepinTextPrimary), modifier = Modifier.padding(end = 10.dp))
                    Text(text = stringResource(R.string.wallpaper_pick_own), color = focusContent(isFocused, DeepinTextPrimary), fontSize = 14.sp)
                }
            }

            Text(text = stringResource(R.string.wallpaper_credit), color = DeepinTextSecondary, fontSize = 10.sp)

            Column {
                Text(text = stringResource(R.string.wallpaper_dim), color = DeepinTextSecondary, fontSize = 12.sp)
                DPadBar(
                    value = settings.dimPercent,
                    range = 0..100,
                    step = 5,
                    onChange = onDimChange,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}
