@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.okcanvas.rnnative.ui.viewer

import android.Manifest
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mxalbert.zoomable.Zoomable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.IOException
import java.net.URL
import java.net.URLConnection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 여러 장 이미지 전체 화면 뷰어 (mxalbert Zoomable + Pager + 다운로드) */
@Composable
fun ImageGalleryViewer(
    images: List<String>,
    startIndex: Int = 0,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, (images.size - 1).coerceAtLeast(0)),
        pageCount = { images.size }
    )
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }

    // Android 9 이하 권한 런처 (Q+에선 보통 불필요)
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted -> 필요 시 결과 처리 */ }

    BackHandler { onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = true
        ) { page ->
            Zoomable {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(images[page])
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 닫기 버튼
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp)
                .background(Color(0x66000000), CircleShape)
        ) {
            Icon(Icons.Filled.Close, contentDescription = "닫기", tint = Color.White)
        }

        // 다운로드 버튼
        IconButton(
            onClick = {
                val url = images.getOrNull(pagerState.currentPage) ?: return@IconButton
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
                if (!isDownloading) {
                    isDownloading = true
                    scope.launch {
                        val ok = saveImageToGallery(context, url) // suspend OK (코루틴 안)
                        isDownloading = false
                        Toast.makeText(
                            context,
                            if (ok) "갤러리에 저장되었습니다." else "저장 실패",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 18.dp, end = 18.dp)
                .background(Color(0x66000000), CircleShape),
            enabled = !isDownloading
        ) {
            if (isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(10.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Icon(Icons.Outlined.Download, contentDescription = "다운로드", tint = Color.White)
            }
        }

        // 페이지 인디케이터
        Text(
            text = "${pagerState.currentPage + 1}/${images.size}",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
                .background(Color(0x66000000), CircleShape)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

/* ================== 저장 로직 ================== */
private suspend fun saveImageToGallery(context: android.content.Context, urlStr: String): Boolean {
    return try {
        // 1) 네트워크로 바이트 가져오기 (IO 스레드)
        val (bytes, mime) = downloadBytes(urlStr) ?: return false

        // 2) 파일명 생성
        val ext = when {
            mime.endsWith("png") -> "png"
            mime.endsWith("webp") -> "webp"
            mime.endsWith("gif") -> "gif"
            else -> "jpg"
        }
        val name = "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.$ext"

        // 3) MediaStore로 저장
        val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/KakaoStyle"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val itemUri = resolver.insert(collection, values) ?: return false

        resolver.openOutputStream(itemUri)?.use { out ->
            out.write(bytes)
            out.flush()
        } ?: return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(itemUri, values, null, null)
        }

        true
    } catch (_: Exception) {
        false
    }
}

private suspend fun downloadBytes(urlStr: String): Pair<ByteArray, String>? = withContext(Dispatchers.IO) {
    try {
        val url = URL(urlStr)
        val conn: URLConnection = url.openConnection().apply {
            connectTimeout = 15000
            readTimeout = 30000
        }
        val mime = conn.contentType ?: guessMime(urlStr)
        BufferedInputStream(conn.getInputStream()).use { input ->
            val buffer = ByteArray(8 * 1024)
            val out = java.io.ByteArrayOutputStream()
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                out.write(buffer, 0, read)
            }
            Pair(out.toByteArray(), mime)
        }
    } catch (_: IOException) {
        null
    }
}

private fun guessMime(urlStr: String): String {
    val lower = urlStr.lowercase(Locale.US)
    return when {
        lower.endsWith(".png") -> "image/png"
        lower.endsWith(".webp") -> "image/webp"
        lower.endsWith(".gif") -> "image/gif"
        else -> "image/jpeg"
    }
}
