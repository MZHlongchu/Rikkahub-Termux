package me.rerere.rikkahub.ui.components.richtext

internal const val FRONTEND_PREVIEW_HEIGHT_BRIDGE = "RikkaHubPreviewHeight"
internal const val FRONTEND_PREVIEW_VIEWPORT_VARIABLE = "--rikkahub-viewport-height"

private val FRONTEND_MARKERS = listOf("html>", "<head>", "<body")
private val VH_VALUE_REGEX = Regex("""(?i)(\d+(?:\.\d+)?)vh\b""")
private val CSS_MIN_HEIGHT_VH_REGEX = Regex(
    """(?i)(min-height\s*:\s*)([^;{}\"'<>]*\d+(?:\.\d+)?vh)"""
)
private val JS_STYLE_MIN_HEIGHT_VH_REGEX = Regex(
    """(?is)(\.style\.minHeight\s*=\s*([\"']))(.*?)(\2)"""
)
private val JS_SET_PROPERTY_MIN_HEIGHT_VH_REGEX = Regex(
    """(?is)(setProperty\s*\(\s*([\"'])min-height\2\s*,\s*([\"']))(.*?)(\3\s*\))"""
)

internal fun isFrontendCodeBlock(code: String): Boolean = FRONTEND_MARKERS.any(code::contains)

internal fun buildFrontendPreviewHtml(
    code: String,
    viewportHeightCssPx: Float,
    autoHeight: Boolean = true,
): String {
    val viewportHeight = viewportHeightCssPx.coerceAtLeast(1f).toCssNumber()
    val content = replaceMinHeightVh(code)
    val overflow = if (autoHeight) "hidden" else "auto"
    val heightObserver = if (autoHeight) buildHeightObserverScript() else ""

    return """
        <!DOCTYPE html>
        <html>
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
        :root{$FRONTEND_PREVIEW_VIEWPORT_VARIABLE:${viewportHeight}px;}
        *,*::before,*::after{box-sizing:border-box;}
        html,body{margin:0!important;padding:0;overflow:$overflow!important;max-width:100%!important;}
        </style>
        {{HEIGHT_OBSERVER}}
        </head>
        <body>
        $content
        </body>
        </html>
    """.trimIndent().replace("{{HEIGHT_OBSERVER}}", heightObserver)
}

private fun buildHeightObserverScript(): String = """
        <script>
        (() => {
          let scheduled = false;
          let lastHeight = 0;

          const reportHeight = () => {
            scheduled = false;
            const body = document.body;
            if (!body) return;

            const height = Math.ceil(body.scrollHeight);
            if (!Number.isFinite(height) || height <= 0 || height === lastHeight) return;
            lastHeight = height;
            window.$FRONTEND_PREVIEW_HEIGHT_BRIDGE?.reportHeight(height);
          };

          const scheduleHeightReport = () => {
            if (scheduled) return;
            scheduled = true;
            requestAnimationFrame(reportHeight);
          };

          const startObserving = () => {
            scheduleHeightReport();
            if (!document.body) return;
            new ResizeObserver(scheduleHeightReport).observe(document.body);
            new MutationObserver(scheduleHeightReport).observe(document.body, {
              childList: true,
              subtree: true,
              attributes: true,
              characterData: true,
            });
            document.fonts?.ready.then(scheduleHeightReport);
          };

          if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', startObserving, { once: true });
          } else {
            startObserving();
          }
          window.addEventListener('load', scheduleHeightReport);
          window.addEventListener('resize', scheduleHeightReport);
        })();
        </script>
""".trimIndent()

private fun replaceMinHeightVh(content: String): String {
    var result = CSS_MIN_HEIGHT_VH_REGEX.replace(content) { match ->
        match.groupValues[1] + convertVhValues(match.groupValues[2])
    }
    result = JS_STYLE_MIN_HEIGHT_VH_REGEX.replace(result) { match ->
        match.groupValues[1] + convertVhValues(match.groupValues[3]) + match.groupValues[4]
    }
    return JS_SET_PROPERTY_MIN_HEIGHT_VH_REGEX.replace(result) { match ->
        match.groupValues[1] + convertVhValues(match.groupValues[4]) + match.groupValues[5]
    }
}

private fun convertVhValues(value: String): String = VH_VALUE_REGEX.replace(value) { match ->
    val vh = match.groupValues[1].toDoubleOrNull() ?: return@replace match.value
    if (vh == 100.0) {
        "var($FRONTEND_PREVIEW_VIEWPORT_VARIABLE)"
    } else {
        "calc(var($FRONTEND_PREVIEW_VIEWPORT_VARIABLE) * ${(vh / 100.0).toCssNumber()})"
    }
}

private fun Number.toCssNumber(): String = toDouble().let { value ->
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}
