package me.rerere.rikkahub.ui.components.richtext

import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import me.rerere.rikkahub.ui.components.webview.WebView
import me.rerere.rikkahub.ui.components.webview.rememberWebViewState
import org.jsoup.Jsoup

private const val MIN_HTML_BLOCK_HEIGHT = 120
private const val DEFAULT_HTML_BLOCK_HEIGHT = 220

private const val HTML_HELPER_STYLE_ID = "rikkahub-html-style"
private const val HTML_HELPER_SCRIPT_ID = "rikkahub-html-bridge"

private const val HTML_HELPER_STYLE = """
    :root {
      --rikkahub-viewport-height: 100vh;
    }

    *,
    *::before,
    *::after {
      box-sizing: border-box;
    }

    html,
    body {
      margin: 0 !important;
      padding: 0 !important;
      max-width: 100% !important;
      overflow-x: hidden !important;
    }

    img,
    svg,
    video,
    canvas,
    iframe {
      max-width: 100%;
      height: auto;
    }

    pre,
    table {
      max-width: 100%;
    }
"""

private const val HTML_HELPER_SCRIPT = """
    (function() {
      function getDocumentHeight() {
        var body = document.body;
        var doc = document.documentElement;
        if (!body || !doc) {
          return 0;
        }

        var maxHeight = Math.max(
          body.scrollHeight || 0,
          body.offsetHeight || 0,
          body.clientHeight || 0,
          doc.clientHeight || 0,
          doc.scrollHeight || 0,
          doc.offsetHeight || 0
        );

        var elements = body.querySelectorAll('*');
        for (var i = 0; i < elements.length; i++) {
          var el = elements[i];
          var rect = el.getBoundingClientRect();
          var pageTop = rect.top + (window.scrollY || window.pageYOffset || 0);
          var elementBottom = pageTop + Math.max(
            rect.height,
            el.scrollHeight || 0,
            el.offsetHeight || 0,
            el.clientHeight || 0
          );
          if (elementBottom > maxHeight) {
            maxHeight = Math.ceil(elementBottom);
          }
        }

        return maxHeight;
      }

      function updateViewportCssVariable() {
        var viewportHeight = window.innerHeight || 0;
        if (window.visualViewport && window.visualViewport.height) {
          viewportHeight = window.visualViewport.height;
        }

        if (viewportHeight > 0) {
          document.documentElement.style.setProperty('--rikkahub-viewport-height', Math.ceil(viewportHeight) + 'px');
        }
      }

      function normalizeUrl(rawUrl) {
        if (!rawUrl) {
          return '';
        }

        var url = String(rawUrl).trim();
        if (!url) {
          return '';
        }

        if (/^javascript:/i.test(url)) {
          return '';
        }

        try {
          return new URL(url, document.baseURI).toString();
        } catch (error) {
          return url;
        }
      }

      function openExternalUrl(rawUrl) {
        var normalized = normalizeUrl(rawUrl);
        if (!normalized || normalized.charAt(0) === '#') {
          return false;
        }

        var parsedUrl = null;
        try {
          parsedUrl = new URL(normalized, document.baseURI);
        } catch (error) {
          return false;
        }

        var protocol = parsedUrl.protocol ? parsedUrl.protocol.toLowerCase() : '';
        if (protocol !== 'http:' &&
            protocol !== 'https:' &&
            protocol !== 'mailto:' &&
            protocol !== 'tel:' &&
            protocol !== 'sms:' &&
            protocol !== 'geo:') {
          return false;
        }

        if ((protocol === 'http:' || protocol === 'https:') &&
            parsedUrl.hostname &&
            parsedUrl.hostname.toLowerCase() === 'rikkahub.local') {
          return false;
        }

        if (window.AndroidInterface && typeof window.AndroidInterface.openExternalUrl === 'function') {
          window.AndroidInterface.openExternalUrl(parsedUrl.toString());
          return true;
        }

        return false;
      }

      var lastReportedHeight = 0;
      var reportScheduled = false;

      function reportHeight() {
        reportScheduled = false;
        updateViewportCssVariable();

        var height = Math.ceil(getDocumentHeight());
        if (!isFinite(height) || height <= 0) return;
        if (height === lastReportedHeight) return;
        lastReportedHeight = height;

        if (window.AndroidInterface && typeof window.AndroidInterface.updateHeight === 'function') {
          window.AndroidInterface.updateHeight(height);
        }
      }

      function scheduleReport() {
        if (reportScheduled) {
          return;
        }

        reportScheduled = true;
        if (typeof window.requestAnimationFrame === 'function') {
          window.requestAnimationFrame(reportHeight);
        } else {
          setTimeout(reportHeight, 16);
        }
      }

      if (!window.__rikkahubObserverInstalled) {
        window.__rikkahubObserverInstalled = true;

        document.addEventListener('click', function(event) {
          var node = event.target;

          while (node && node !== document) {
            if (node.tagName && node.tagName.toLowerCase() === 'a') {
              break;
            }
            node = node.parentElement;
          }

          if (!node || node === document || !node.getAttribute) {
            return;
          }

          var href = node.getAttribute('href');
          if (!href) {
            return;
          }

          if (openExternalUrl(href)) {
            event.preventDefault();
            event.stopPropagation();
          }
        }, true);

        window.addEventListener('load', function() {
          reportHeight();
          setTimeout(reportHeight, 80);
          setTimeout(reportHeight, 300);
          setTimeout(reportHeight, 1000);
        });

        window.addEventListener('resize', scheduleReport);

        if (window.visualViewport) {
          window.visualViewport.addEventListener('resize', scheduleReport);
        }

        if (typeof ResizeObserver !== 'undefined') {
          var resizeObserver = new ResizeObserver(function() {
            scheduleReport();
          });
          resizeObserver.observe(document.documentElement);
          if (document.body) {
            resizeObserver.observe(document.body);
          }
          window.__rikkahubResizeObserver = resizeObserver;
        }

        var mutationObserver = new MutationObserver(function() {
          scheduleReport();
        });
        mutationObserver.observe(document.documentElement || document, {
          childList: true,
          subtree: true,
          attributes: true,
          characterData: true
        });
        window.__rikkahubMutationObserver = mutationObserver;
      }

      window.__rikkahubReportHeight = reportHeight;
      scheduleReport();
    })();
"""

internal fun buildBrowserHtmlDocument(html: String): String {
    val document = Jsoup.parse(html)
    document.outputSettings().prettyPrint(false)

    val head = document.head()
    if (head.selectFirst("meta[name=viewport]") == null) {
        head.prependElement("meta")
            .attr("name", "viewport")
            .attr("content", "width=device-width, initial-scale=1.0")
    }

    if (head.getElementById(HTML_HELPER_STYLE_ID) == null) {
        head.appendElement("style")
            .attr("id", HTML_HELPER_STYLE_ID)
            .appendText(HTML_HELPER_STYLE)
    }

    val body = document.body()
    if (body.getElementById(HTML_HELPER_SCRIPT_ID) == null) {
        body.appendElement("script")
            .attr("id", HTML_HELPER_SCRIPT_ID)
            .attr("type", "text/javascript")
            .append(HTML_HELPER_SCRIPT)
    }

    return document.outerHtml()
}

@Composable
fun BrowserHtmlBlock(
    html: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val handler = remember { Handler(Looper.getMainLooper()) }
    val density = LocalDensity.current
    val renderedHtml = remember(html) { buildBrowserHtmlDocument(html) }
    val minHeightPx = with(density) { MIN_HTML_BLOCK_HEIGHT.dp.toPx().toInt() }
    var contentHeightPx by remember(html, density.density) {
        mutableIntStateOf(with(density) { DEFAULT_HTML_BLOCK_HEIGHT.dp.toPx().toInt() })
    }

    val htmlBridge = remember(density.density, context, minHeightPx, handler) {
        HtmlBridge(
            onHeightChanged = { cssHeight ->
                if (cssHeight <= 0) return@HtmlBridge
                val height = (cssHeight * density.density).toInt()
                contentHeightPx = height.coerceAtLeast(minHeightPx)
            },
            onOpenExternalUrl = { rawUrl ->
                val uri = runCatching { Uri.parse(rawUrl.trim()) }.getOrNull() ?: return@HtmlBridge
                val scheme = uri.scheme?.lowercase()
                if (scheme !in setOf("http", "https", "mailto", "tel", "sms", "geo")) {
                    return@HtmlBridge
                }

                handler.post {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, uri).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    }
                }
            }
        )
    }

    val contentHeight = with(density) { contentHeightPx.toDp() }

    val webViewState = rememberWebViewState(
        data = renderedHtml,
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
            webView.evaluateJavascript(
                "if (window.__rikkahubReportHeight) { window.__rikkahubReportHeight(); }",
                null
            )
        }
    )
}

private class HtmlBridge(
    private val onHeightChanged: (Int) -> Unit,
    private val onOpenExternalUrl: (String) -> Unit
) {
    @JavascriptInterface
    fun updateHeight(height: Int) {
        onHeightChanged(height)
    }

    @JavascriptInterface
    fun openExternalUrl(url: String) {
        onOpenExternalUrl(url)
    }
}
