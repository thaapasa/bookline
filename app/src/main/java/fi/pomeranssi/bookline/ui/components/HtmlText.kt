package fi.pomeranssi.bookline.ui.components

import android.graphics.Typeface
import android.text.Html
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fi.pomeranssi.bookline.ui.theme.BooklineTheme

/**
 * Renders simple HTML (bold, italic, underline, line breaks) as styled Compose text.
 */
@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
) {
    val annotated = remember(html) { htmlToAnnotatedString(html) }
    Text(
        text = annotated,
        modifier = modifier,
        style = style,
    )
}

private fun htmlToAnnotatedString(html: String): AnnotatedString {
    val spanned: Spanned = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
    return buildAnnotatedString {
        append(spanned.toString())
        for (span in spanned.getSpans(0, spanned.length, Any::class.java)) {
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)
            when (span) {
                is StyleSpan -> {
                    when (span.style) {
                        Typeface.BOLD -> {
                            addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                        }

                        Typeface.ITALIC -> {
                            addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                        }

                        Typeface.BOLD_ITALIC -> {
                            addStyle(
                                SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
                                start,
                                end,
                            )
                        }
                    }
                }

                is UnderlineSpan -> {
                    addStyle(
                        SpanStyle(textDecoration = TextDecoration.Underline),
                        start,
                        end,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HtmlTextPreview() {
    BooklineTheme(dynamicColor = false) {
        HtmlText(
            html =
                "A review with <b>bold</b>, <i>italic</i>, <b><i>bold italic</i></b> " +
                    "and <u>underlined</u> text.<br>Second line after a break.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}
