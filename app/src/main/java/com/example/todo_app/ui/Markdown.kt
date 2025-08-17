package com.example.todo_app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import org.commonmark.node.*
import org.commonmark.parser.Parser

/**
 * Markdown renderer (CommonMark) using Material 3.
 * Supports: headings, paragraphs, blockquotes, hr, fenced/indented code,
 * bullet & ordered lists (nested), links, bold/italic/inline code.
 */
@Composable
fun MarkdownText(
    md: String,
    modifier: Modifier = Modifier,
    blockSpacingDp: Int = 12
) {
    val parser = remember { Parser.builder().build() }
    val root = remember(md) { parser.parse(md) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(blockSpacingDp.dp)
    ) {
        renderBlocks(root, indent = 0)
    }
}

/** Optional alias so you can call Markdown(...) if you prefer */
@Composable
fun Markdown(
    md: String,
    modifier: Modifier = Modifier,
    blockSpacingDp: Int = 12
) = MarkdownText(md, modifier, blockSpacingDp)

/* ------------------------------- Blocks ------------------------------- */

@Composable
private fun renderBlocks(node: Node, indent: Int) {
    var child: Node? = node.firstChild
    while (child != null) {
        when (child) {
            is Heading -> RenderHeading(child)
            is Paragraph -> RenderParagraph(child)
            is BlockQuote -> RenderBlockQuote(child, indent)
            is ThematicBreak -> HorizontalDivider()
            is FencedCodeBlock -> RenderFencedCode(child)
            is IndentedCodeBlock -> RenderIndentedCode(child)
            is BulletList -> RenderBulletList(child, indent)
            is OrderedList -> RenderOrderedList(child, indent)
            is HtmlBlock, is HtmlInline -> RenderParagraph(asParagraphFromHtml(child))
            else -> renderBlocks(child, indent) // descend into unknown containers
        }
        child = child.next
    }
}

@Composable
private fun RenderHeading(h: Heading) {
    val style = when (h.level) {
        1 -> MaterialTheme.typography.headlineSmall
        2 -> MaterialTheme.typography.titleLarge
        3 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        4 -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
        else -> MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
    }
    val text = buildInlineAnnotatedString(
        container = h,
        linkColor = MaterialTheme.colorScheme.primary,
        codeBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    )
    Text(text, style = style)
}

@Composable
private fun RenderParagraph(p: Paragraph) {
    val uriHandler = LocalUriHandler.current
    val annotated = buildInlineAnnotatedString(
        container = p,
        linkColor = MaterialTheme.colorScheme.primary,
        codeBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    )
    @Suppress("DEPRECATION") // ClickableText is deprecated; fine to use here for link taps
    ClickableText(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium,
        onClick = { offset ->
            annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { ann -> uriHandler.openUri(ann.item) }
        }
    )
}

@Composable
private fun RenderBlockQuote(bq: BlockQuote, indent: Int) {
    Row(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .width(4.dp + (indent * 2).dp)
                .heightIn(min = 8.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        )
        Column(
            Modifier
                .padding(start = 12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            renderBlocks(bq, indent + 1)
        }
    }
}

@Composable
private fun RenderFencedCode(code: FencedCodeBlock) {
    val lang = code.info?.trim().orEmpty()
    Surface(
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            if (lang.isNotEmpty()) {
                Text(
                    text = lang,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
            }
            Text(
                (code.literal ?: "").trimEnd(),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun RenderIndentedCode(code: IndentedCodeBlock) {
    Surface(
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    ) {
        Text(
            (code.literal ?: "").trimEnd(),
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun RenderBulletList(list: BulletList, indent: Int) {
    val items = toListItems(list)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (item in items) {
            Row(Modifier.fillMaxWidth()) {
                Text(
                    text = "•",
                    modifier = Modifier.width(((indent + 1) * 16).dp),
                    style = MaterialTheme.typography.bodyMedium
                )
                Column(Modifier.fillMaxWidth()) {
                    renderBlocks(item, indent + 1)
                }
            }
        }
    }
}

@Composable
private fun RenderOrderedList(list: OrderedList, indent: Int) {
    val items = toListItems(list)
    @Suppress("DEPRECATION") var num: Int = list.startNumber
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (item in items) {
            Row(Modifier.fillMaxWidth()) {
                Text(
                    text = "${num}.",
                    modifier = Modifier.width(((indent + 1) * 16).dp),
                    style = MaterialTheme.typography.bodyMedium
                )
                Column(Modifier.fillMaxWidth()) {
                    renderBlocks(item, indent + 1)
                }
            }
            num++
        }
    }
}

/* ------------------------------ Inline ------------------------------- */

private fun buildInlineAnnotatedString(
    container: Node,
    linkColor: Color,
    codeBg: Color
): AnnotatedString {
    val linkStyle = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)
    val bold = SpanStyle(fontWeight = FontWeight.Bold)
    val italic = SpanStyle(fontStyle = FontStyle.Italic)
    val code = SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg)

    return buildAnnotatedString {
        fun appendInline(n: Node) {
            when (n) {
                is Text -> append(n.literal ?: "")
                is SoftLineBreak -> append(" ")
                is HardLineBreak -> append("\n")
                is Emphasis -> { pushStyle(italic); visitChildren(n, ::appendInline); pop() }
                is StrongEmphasis -> { pushStyle(bold); visitChildren(n, ::appendInline); pop() }
                is Code -> { pushStyle(code); append(n.literal ?: ""); pop() }
                is Link -> {
                    val dest = n.destination.orEmpty()
                    pushStringAnnotation(tag = "URL", annotation = dest)
                    pushStyle(linkStyle)
                    visitChildren(n, ::appendInline)
                    pop(); pop()
                }
                is Image -> { append("["); visitChildren(n, ::appendInline); append("]") }
                is Paragraph, is Document -> visitChildren(n, ::appendInline)
                else -> visitChildren(n, ::appendInline)
            }
        }
        visitChildren(container, ::appendInline)
    }
}

/* ----------------------------- Utilities ----------------------------- */

private fun toListItems(list: Node): List<ListItem> {
    val out = mutableListOf<ListItem>()
    var c: Node? = list.firstChild
    while (c != null) {
        (c as? ListItem)?.let { out.add(it) }
        c = c.next
    }
    return out
}

private inline fun visitChildren(parent: Node, fn: (Node) -> Unit) {
    var c: Node? = parent.firstChild
    while (c != null) {
        val cur = c
        fn(cur)
        c = cur.next
    }
}

// Convert HtmlInline/HtmlBlock to a Paragraph with raw literal text
/** * Converts an HtmlInline or HtmlBlock node into a Paragraph node.
 * * The literal content of the HTML node is extracted and set as the text of the new Paragraph.
 * * This is used to render HTML content as plain text within the Markdown structure.
 * * * @param node The HtmlInline or HtmlBlock node to convert.
 * * @return A new Paragraph node containing the literal text of the input HTML node.
 * * If the input node is not an HtmlInline or HtmlBlock, an empty Paragraph is returned.
 * */
private fun asParagraphFromHtml(node : Node) : Paragraph {
    val para = Paragraph()
    val textNode = Text()
    textNode.literal = when (node) {
        is HtmlInline -> node.literal ?: ""
        is HtmlBlock -> node.literal ?: ""
        else -> ""
    }
    para.appendChild (textNode)
    return para
}
