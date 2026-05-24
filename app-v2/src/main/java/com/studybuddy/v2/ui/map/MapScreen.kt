package com.studybuddy.v2.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.TextureMapView
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.studybuddy.v2.theme.ClaudeColors
import com.studybuddy.v2.theme.ClaudeRadius
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.ClaudeType
import com.studybuddy.v2.theme.appColors
import com.studybuddy.v2.ui.component.GlassPresets

/**
 * 地图：双人定位 + 共享地标 + 距离。
 *
 * - 我的位置由 AMap.isMyLocationEnabled 处理（蓝点）
 * - 搭档位置用 Marker（coral 圆点）
 * - 共享地标用小图标
 * - 距离卡片显示精确距离 + "在一起"/"几公里外"等语义文字
 */
@SuppressLint("MissingPermission")
@Composable
fun MapScreen(viewModel: MapViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.appColors
    val ctx = LocalContext.current
    var pendingLandmark by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasLocationPermission = grants.values.all { it }
        if (hasLocationPermission) viewModel.tickMyLocation()
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            viewModel.tickMyLocation()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
    ) {
        AMapView(
            modifier = Modifier.fillMaxSize(),
            enableLocation = hasLocationPermission,
            partnerLat = state.partnerLat,
            partnerLng = state.partnerLng,
            myLat = state.myLat,
            myLng = state.myLng,
            landmarks = state.landmarks,
            onLongPress = { lat, lng -> pendingLandmark = lat to lng }
        )

        // 顶部状态卡：双人距离
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ClaudeSpacing.pageHorizontal)
                .padding(top = ClaudeSpacing.xxl)
        ) {
            Text("MAP", style = ClaudeType.CaptionUppercase, color = colors.ink)
            Spacer(Modifier.height(ClaudeSpacing.xxs))
            GlassPresets.Cream(shape = RoundedCornerShape(ClaudeRadius.lg)) {
                Column(modifier = Modifier.padding(ClaudeSpacing.md)) {
                    when {
                        state.partner == null -> Text(
                            "还没绑定搭档",
                            style = ClaudeType.TitleSm, color = colors.ink
                        )
                        !state.partnerFresh -> Text(
                            "${state.partner!!.nickname.ifBlank { "TA" }} 暂时没共享位置",
                            style = ClaudeType.TitleSm, color = colors.ink
                        )
                        state.distanceM == null -> Text(
                            "正在算距离…",
                            style = ClaudeType.TitleSm, color = colors.muted
                        )
                        else -> {
                            Text(
                                distanceLabel(state.distanceM!!),
                                style = ClaudeType.TitleLg, color = colors.ink
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "你和 ${state.partner!!.nickname.ifBlank { "TA" }} 之间",
                                style = ClaudeType.Caption, color = colors.muted
                            )
                        }
                    }
                }
            }
        }

        // 底部状态条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = ClaudeSpacing.pageHorizontal)
                .padding(bottom = ClaudeSpacing.md)
        ) {
            GlassPresets.Cream(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(ClaudeRadius.lg)) {
                Row(
                    modifier = Modifier.padding(ClaudeSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (hasLocationPermission) colors.success else colors.mutedSoft)
                    )
                    Spacer(Modifier.size(ClaudeSpacing.xs))
                    Text(
                        if (!hasLocationPermission) "需要位置权限"
                        else if (state.landmarks.isEmpty()) "去过的地方还没记录"
                        else "${state.landmarks.size} 个共享地标",
                        style = ClaudeType.Caption,
                        color = colors.muted
                    )
                }
            }
        }
    }

    // 长按地图后弹"命名"对话框
    pendingLandmark?.let { (lat, lng) ->
        var nameInput by remember { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingLandmark = null },
            containerColor = colors.surfaceCard,
            title = { Text("在这里加个地标", style = ClaudeType.TitleLg, color = colors.ink) },
            text = {
                androidx.compose.foundation.layout.Column {
                    Text("你们一起去过的地方？给它起个名字。",
                        style = ClaudeType.BodySm, color = colors.body)
                    Spacer(Modifier.height(ClaudeSpacing.sm))
                    androidx.compose.material3.OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it.take(40) },
                        placeholder = { Text("图书馆三楼 / 那家咖啡馆 …", color = colors.muted) },
                        singleLine = true,
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = com.studybuddy.v2.theme.ClaudeColors.Primary,
                            unfocusedBorderColor = colors.hairline
                        )
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        viewModel.createLandmark(nameInput, lat, lng)
                        pendingLandmark = null
                    },
                    enabled = nameInput.trim().isNotEmpty()
                ) {
                    Text("加上", color = com.studybuddy.v2.theme.ClaudeColors.Primary)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pendingLandmark = null }) {
                    Text("取消", color = colors.muted)
                }
            }
        )
    }
}

private fun distanceLabel(meters: Double): String = when {
    meters < 100 -> "在一起"
    meters < 500 -> "${meters.toInt()} m"
    meters < 1000 -> "几百米外"
    meters < 10_000 -> "${"%.1f".format(meters / 1000)} km"
    meters < 100_000 -> "${(meters / 1000).toInt()} km"
    else -> "几百公里外"
}

@Composable
private fun AMapView(
    modifier: Modifier,
    enableLocation: Boolean,
    partnerLat: Double?,
    partnerLng: Double?,
    myLat: Double?,
    myLng: Double?,
    landmarks: List<com.studybuddy.v2.data.model.PairLandmark> = emptyList(),
    onLongPress: (lat: Double, lng: Double) -> Unit = { _, _ -> }
) {
    val ctx = LocalContext.current
    val mapView = remember { TextureMapView(ctx) }
    var partnerMarker by remember { mutableStateOf<com.amap.api.maps.model.Marker?>(null) }
    val landmarkMarkers = remember { mutableStateOf<List<com.amap.api.maps.model.Marker>>(emptyList()) }

    DisposableEffect(Unit) {
        mapView.onCreate(null)
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDestroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { view ->
            val map = view.map
            if (enableLocation) {
                map.isMyLocationEnabled = true
                map.uiSettings.isMyLocationButtonEnabled = false
                map.uiSettings.isZoomControlsEnabled = false
                map.uiSettings.isScaleControlsEnabled = false
                map.uiSettings.isCompassEnabled = false
            }
            // 长按地图触发"创建共享地标"
            map.setOnMapLongClickListener { ll -> onLongPress(ll.latitude, ll.longitude) }

            // 同步共享地标 markers（diff：先全部清掉再加）
            landmarkMarkers.value.forEach { it.remove() }
            landmarkMarkers.value = landmarks.map { lm ->
                map.addMarker(
                    MarkerOptions()
                        .position(LatLng(lm.lat, lm.lng))
                        .title(lm.name.ifBlank { "共享地标" })
                        .snippet("到过 ${lm.visitCount} 次")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                )
            }

            // 搭档 marker（coral 色）
            if (partnerLat != null && partnerLng != null) {
                val pos = LatLng(partnerLat, partnerLng)
                if (partnerMarker == null) {
                    val opts = MarkerOptions()
                        .position(pos)
                        .title("TA 在这里")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                    partnerMarker = map.addMarker(opts)
                } else {
                    partnerMarker?.position = pos
                }

                // 自动居中：让我和搭档都在视野里
                if (myLat != null && myLng != null) {
                    val midLat = (myLat + partnerLat) / 2
                    val midLng = (myLng + partnerLng) / 2
                    val dLat = kotlin.math.abs(myLat - partnerLat)
                    val dLng = kotlin.math.abs(myLng - partnerLng)
                    val span = maxOf(dLat, dLng)
                    val zoom = when {
                        span > 1.0 -> 6f
                        span > 0.1 -> 10f
                        span > 0.01 -> 13f
                        else -> 15f
                    }
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(midLat, midLng), zoom))
                } else {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 13f))
                }
            } else if (myLat != null && myLng != null) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(myLat, myLng), 14f))
            } else {
                // 默认成都
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(30.6595, 104.0657), 13f))
            }
        }
    )
}
