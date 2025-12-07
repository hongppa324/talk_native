@file:OptIn(
    androidx.media3.common.util.UnstableApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package com.vmerp.works.ui.chat.content

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.InsertPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.* // remember/LaunchedEffect/derivedStateOf 등 상태 & 효과
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color // 색상 타입
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource // res/drawable에 있는 xml (<-svg) 그려주는 함수
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.vmerp.works.ChatNativeTokenStore
import com.vmerp.works.R // res/drawable에 접근
import com.vmerp.works.ui.theme.BrandOnSurfaceLight

// 영상 메시지
@Composable
fun VideoMessage(
    url: String?, // 동영상 원본 URL
    willPlay: Boolean = false,
    isDownloading: Boolean = false,
    onPress: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    // 톡 토큰 가져오기
    val token = ChatNativeTokenStore.getToken(context)

    val shape = RoundedCornerShape(8.dp)
    val isPreview = LocalInspectionMode.current

    val imageLoader =
        remember {
            ImageLoader.Builder(context)
                .components {
                    add(VideoFrameDecoder.Factory())
                }
                .build()
        }

    var showPlayer by remember { mutableStateOf(false) }

    LaunchedEffect(willPlay) {
        if (willPlay == true && !isDownloading && !url.isNullOrBlank()) {
            showPlayer = true
        } else {
            showPlayer = false
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth(0.6f) // 화면 60%만 사용
                .clip(shape),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f) // 16:9 비율 유지
                    .clip(shape)
                    .combinedClickable(
                        indication = null, // 클릭 시 RippleEffect(회색 음영) 제거
                        interactionSource = remember { MutableInteractionSource() },
                        enabled = !url.isNullOrBlank(),
                        onClick = { onPress?.invoke() },
                        onLongClick = { onLongPress?.invoke() },
                    ),
        ) {
            if (isPreview) {
                ImagePlaceholder(modifier = Modifier.fillMaxSize(), shape = shape)
            } else {
                // 썸네일 URL
                val displayUrl = url ?: "https://picsum.photos/seed/video_fallback/1280/720"
                SubcomposeAsyncImage(
                    imageLoader = imageLoader,
                    model =
                        ImageRequest.Builder(context)
                            .data(displayUrl)
                            .apply {
                                if (!token.isNullOrBlank()) {
                                    addHeader("Authorization", "Bearer $token")
                                }
                            }
                            .videoFrameMillis(0)
                            .crossfade(true)
                            .build(),
                    contentDescription = "video message thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = { ImagePlaceholder(modifier = Modifier.fillMaxSize(), shape = shape) },
                    error = { ImagePlaceholder(modifier = Modifier.fillMaxSize(), shape = shape) },
                )
            }

            // 중앙 재생 버튼
            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(999.dp),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier =
                    Modifier
                        .size(44.dp)
                        .align(Alignment.Center),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.video_play_button),
                    contentDescription = "영상 재생 버튼",
                    tint = Color.Unspecified,
                )
            }
        }
    }

    // 전체화면 비디오 플레이어(Dialog + ExoPlayer)
    if (showPlayer && !url.isNullOrBlank()) {
        VideoPlayerDialog(
            url = url,
            onDismiss = { showPlayer = false },
        )
    }
}

@Composable
private fun VideoPlayerDialog(
    url: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    // ExoPlayer 생성/해제 관리
    val exoPlayer =
        remember(url) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(url))
                prepare()
                playWhenReady = true
            }
        }
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false, // 전체화면
                decorFitsSystemWindows = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
            ),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = androidx.compose.ui.graphics.Color.Black) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                        layoutParams =
                            ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                        // 필요 시 UI 옵션 조정:
                        // this.useController = true
                        // this.controllerShowTimeoutMs = 3000
                    }
                },
                update = { it.player = exoPlayer },
            )
        }
    }
}

@Composable
private fun ImagePlaceholder(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp),
) {
    Box(
        modifier =
            modifier
                .background(BrandOnSurfaceLight.copy(alpha = 0.06f), shape)
                .border(0.5.dp, BrandOnSurfaceLight.copy(alpha = 0.08f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.InsertPhoto,
            contentDescription = null,
            tint = BrandOnSurfaceLight.copy(alpha = 0.6f),
            modifier = Modifier.size(36.dp),
        )
    }
}
