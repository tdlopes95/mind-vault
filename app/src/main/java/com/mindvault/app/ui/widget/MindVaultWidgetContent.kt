package com.mindvault.app.ui.widget

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentHeight
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.mindvault.app.MainActivity
import com.mindvault.app.R

private val COMPACT_SIZE = DpSize(120.dp, 48.dp)
private val EXPANDED_SIZE = DpSize(250.dp, 48.dp)

@Composable
fun MindVaultWidgetContent() {
    val context = LocalContext.current
    val size = LocalSize.current
    val isExpanded = size.width >= EXPANDED_SIZE.width

    val newNoteIntent = Intent(context, MainActivity::class.java).apply {
        putExtra("widget_action", "new_note")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val quickIdeaIntent = Intent(context, MainActivity::class.java).apply {
        putExtra("widget_action", "quick_idea")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val searchIntent = Intent(context, MainActivity::class.java).apply {
        putExtra("widget_action", "search")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .cornerRadius(16.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_launcher_foreground),
                contentDescription = "MindVault",
                modifier = GlanceModifier.size(28.dp),
            )
            Spacer(GlanceModifier.width(6.dp))
            Text(
                text = "MindVault",
                style = TextStyle(
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.onBackground,
                ),
                modifier = GlanceModifier.defaultWeight(),
            )
            Spacer(GlanceModifier.width(8.dp))
            Box(
                modifier = GlanceModifier
                    .background(GlanceTheme.colors.primary)
                    .cornerRadius(12.dp)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .clickable(actionStartActivity(newNoteIntent)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+ New",
                    style = TextStyle(
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.onPrimary,
                    ),
                )
            }
            if (isExpanded) {
                Spacer(GlanceModifier.width(6.dp))
                Box(
                    modifier = GlanceModifier
                        .background(GlanceTheme.colors.secondaryContainer)
                        .cornerRadius(12.dp)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .clickable(actionStartActivity(quickIdeaIntent)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Quick Idea",
                        style = TextStyle(
                            fontWeight = FontWeight.Medium,
                            color = GlanceTheme.colors.onSecondaryContainer,
                        ),
                    )
                }
                Spacer(GlanceModifier.width(6.dp))
                Box(
                    modifier = GlanceModifier
                        .background(GlanceTheme.colors.surfaceVariant)
                        .cornerRadius(12.dp)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .clickable(actionStartActivity(searchIntent)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Search",
                        style = TextStyle(
                            fontWeight = FontWeight.Medium,
                            color = GlanceTheme.colors.onSurfaceVariant,
                        ),
                    )
                }
            }
        }
    }
}
