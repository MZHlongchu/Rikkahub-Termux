package me.rerere.rikkahub.ui.components.richtext

import android.webkit.JavascriptInterface
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import me.rerere.rikkahub.ui.components.webview.WebView
import me.rerere.rikkahub.ui.components.webview.rememberWebViewState

private const val MIN_HTML_BLOCK_HEIGHT = 120
private const val DEFAULT_HTML_BLOCK_HEIGHT = 220

private const val HTML_HEIGHT_MEASURE_JS = """
    (function() {
      function documentHeight() {
        var body = document.body;
        var doc = document.documentElement;
        if (!body || !doc) return 0;
        return Math.max(
          body.scrollHeight,
          body.offsetHeight,
          doc.clientHeight,
          doc.scrollHeight,
          doc.offsetHeight
        );
      }

      function reportHeight() {
        var height = Math.ceil(documentHeight());
        if (window.AndroidInterface && typeof window.AndroidInterface.updateHeight === 'function') {
          window.AndroidInterface.updateHeight(height);
        }
      }

      if (!window.__rikkahubObserverInstalled) {
        window.__rikkahubObserverInstalled = true;

        window.addEventListener('load', function() {
          reportHeight();
          setTimeout(reportHeight, 50);
          setTimeout(reportHeight, 300);
          setTimeout(reportHeight, 1000);
        });

        if (typeof ResizeObserver !== 'undefined') {
          var resizeObserver = new ResizeObserver(function() {
            reportHeight();
          });
          resizeObserver.observe(document.documentElement);
          if (document.body) {
            resizeObserver.observe(document.body);
          }
          window.__rikkahubResizeObserver = resizeObserver;
        } else {
          window.addEventListener('resize', reportHeight);
        }

        var mutationObserver = new MutationObserver(function() {
          reportHeight();
        });
        mutationObserver.observe(document.documentElement || document, {
          childList: true,
          subtree: true,
          attributes: true,
          characterData: true
        });
        window.__rikkahubMutationObserver = mutationObserver;
      }

      reportHeight();
    })();
"""

@Composable
fun BrowserHtmlBlock(
    html: String,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val minHeightPx = with(density) { MIN_HTML_BLOCK_HEIGHT.dp.toPx().toInt() }
    var contentHeightPx by remember(html, density.density) {
        mutableIntStateOf(with(density) { DEFAULT_HTML_BLOCK_HEIGHT.dp.toPx().toInt() })
    }

    val htmlBridge = remember(density) {
        HtmlHeightBridge { cssHeight ->
            if (cssHeight <= 0) return@HtmlHeightBridge
            val height = (cssHeight * density.density).toInt()
            contentHeightPx = height.coerceAtLeast(minHeightPx)
        }
    }

    val contentHeight = with(density) { contentHeightPx.toDp() }

    val webViewState = rememberWebViewState(
        data = html,
        baseUrl = "https://rikkahub.local",
        mimeType = "text/html",
        encoding = "UTF-8",
        interfaces = mapOf("AndroidInterface" to htmlBridge),
        settings = {
            builtInZoomControls = true
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
        }
    )

    WebView(
        state = webViewState,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .animateContentSize()
            .height(contentHeight),
        onUpdated = { webView ->
            webView.evaluateJavascript(HTML_HEIGHT_MEASURE_JS, null)
        }
    )
}

private class HtmlHeightBridge(
    private val onHeightChanged: (Int) -> Unit
) {
    @JavascriptInterface
    fun updateHeight(height: Int) {
        onHeightChanged(height)
    }
}
