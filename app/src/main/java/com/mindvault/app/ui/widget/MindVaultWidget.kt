package com.mindvault.app.ui.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.unit.ColorProvider
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.SizeMode
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

class MindVaultWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(120.dp, 48.dp),
            DpSize(250.dp, 48.dp),
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                MindVaultWidgetContent()
            }
        }
    }
}
