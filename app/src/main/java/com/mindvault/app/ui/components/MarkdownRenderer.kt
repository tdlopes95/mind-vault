package com.mindvault.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownRenderer(
    content: String,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lines = content.split("\n")
    var i = 0

    Column(modifier = modifier.fillMaxWidth()) {
        while (i < lines.size) {
            val line = lines[i]

            when {
                // Code blocks
                line.trimStart().startsWith("```") -> {
                    val codeLines = mutableListOf<String>()
                    i++
                    while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                        codeLines.add(lines[i])
                        i++
                    }
                    CodeBlock(code = codeLines.joinToString("\n"))
                }
                // Horizontal rule
                line.trim() == "---" || line.trim() == "***" || line.trim() == "___" -> {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
                // Headings
                line.startsWith("### ") -> {
                    Text(
                        text = parseInline(line.removePrefix("### ")),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = parseInline(line.removePrefix("## ")),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
                line.startsWith("# ") -> {
                    Text(
                        text = parseInline(line.removePrefix("# ")),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                }
                // Checklists
                line.startsWith("- [ ] ") || line.startsWith("- [x] ") -> {
                    val checked = line.startsWith("- [x] ")
                    val label = if (checked) line.removePrefix("- [x] ") else line.removePrefix("- [ ] ")
                    val lineIndex = i
                    ChecklistItem(
                        label = label,
                        checked = checked,
                        onCheckedChange = { newChecked ->
                            val newLines = lines.toMutableList()
                            newLines[lineIndex] = if (newChecked) "- [x] $label" else "- [ ] $label"
                            onContentChange(newLines.joinToString("\n"))
                        },
                    )
                }
                // Bullet lists
                line.startsWith("- ") || line.startsWith("* ") -> {
                    val prefix = if (line.startsWith("- ")) "- " else "* "
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(end = 8.dp, top = 2.dp),
                        )
                        Text(
                            text = parseInline(line.removePrefix(prefix)),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                // Numbered lists
                line.matches(Regex("^\\d+\\.\\s.*")) -> {
                    val dotIndex = line.indexOf('.')
                    val number = line.substring(0, dotIndex)
                    val text = line.substring(dotIndex + 2)
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = "$number.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(28.dp).padding(top = 2.dp),
                        )
                        Text(
                            text = parseInline(text),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                // Blank line
                line.isBlank() -> {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                // Regular paragraph
                else -> {
                    Text(
                        text = parseInline(line),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
            i++
        }
    }
}

@Composable
private fun ChecklistItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
        Text(
            text = parseInline(label),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun CodeBlock(code: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
            )
            .padding(12.dp),
    ) {
        Text(
            text = code,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}

private fun parseInline(text: String): AnnotatedString = buildAnnotatedString {
    val codeStyle = SpanStyle(fontFamily = FontFamily.Monospace, background = androidx.compose.ui.graphics.Color(0x22888888))
    val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
    val italicStyle = SpanStyle(fontStyle = FontStyle.Italic)

    var i = 0
    while (i < text.length) {
        when {
            // Inline code
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end > i) {
                    withStyle(codeStyle) { append(text.substring(i + 1, end)) }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            // Bold **text**
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end > i) {
                    withStyle(boldStyle) { append(text.substring(i + 2, end)) }
                    i = end + 2
                } else {
                    append(text[i])
                    i++
                }
            }
            // Italic *text*
            text[i] == '*' -> {
                val end = text.indexOf('*', i + 1)
                if (end > i) {
                    withStyle(italicStyle) { append(text.substring(i + 1, end)) }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            else -> {
                append(text[i])
                i++
            }
        }
    }
}
