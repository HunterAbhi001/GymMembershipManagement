package com.example.gymmanagement.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Butt
import androidx.compose.ui.graphics.StrokeJoin.Companion.Miter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object AppIcons {
    val WhatsApp: ImageVector
        get() {
            if (_whatsapp != null) {
                return _whatsapp!!
            }
            _whatsapp = Builder(
                name = "WhatsApp", defaultWidth = 24.0.dp, defaultHeight = 24.0.dp,
                viewportWidth = 24.0f, viewportHeight = 24.0f
            ).path(
                fill = SolidColor(Color(0xFF4CAF50)), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(12.0f, 2.0f)
                curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
                curveToRelative(0.0f, 1.77f, 0.45f, 3.44f, 1.25f, 4.95f)
                lineTo(2.0f, 22.0f)
                lineToRelative(5.27f, -1.24f)
                curveTo(8.71f, 21.55f, 10.31f, 22.0f, 12.0f, 22.0f)
                curveToRelative(5.52f, 0.0f, 10.0f, -4.48f, 10.0f, -10.0f)
                reflectiveCurveTo(17.52f, 2.0f, 12.0f, 2.0f)
                close()
                moveTo(16.5f, 15.3f)
                curveToRelative(-0.2f, -0.1f, -1.2f, -0.6f, -1.4f, -0.65f)
                curveToRelative(-0.2f, -0.05f, -0.35f, -0.08f, -0.5f, 0.08f)
                curveToRelative(-0.15f, 0.16f, -0.5f, 0.65f, -0.6f, 0.8f)
                curveToRelative(-0.1f, 0.15f, -0.2f, 0.16f, -0.4f, 0.05f)
                curveToRelative(-0.2f, -0.1f, -0.8f, -0.3f, -1.5f, -0.9f)
                curveToRelative(-0.6f, -0.5f, -1.0f, -1.1f, -1.1f, -1.3f)
                curveToRelative(-0.1f, -0.2f, 0.0f, -0.3f, 0.1f, -0.4f)
                curveToRelative(0.1f, -0.1f, 0.2f, -0.25f, 0.3f, -0.35f)
                curveToRelative(0.1f, -0.1f, 0.15f, -0.2f, 0.2f, -0.3f)
                curveToRelative(0.05f, -0.1f, 0.02f, -0.2f, -0.0f, -0.3f)
                curveToRelative(-0.03f, -0.1f, -0.5f, -1.2f, -0.7f, -1.6f)
                curveToRelative(-0.2f, -0.4f, -0.4f, -0.35f, -0.5f, -0.35f)
                horizontalLineToRelative(-0.3f)
                curveToRelative(-0.15f, 0.0f, -0.4f, 0.05f, -0.6f, 0.25f)
                curveToRelative(-0.2f, 0.2f, -0.75f, 0.7f, -0.75f, 1.75f)
                curveToRelative(0.0f, 1.05f, 0.8f, 2.05f, 0.9f, 2.2f)
                curveToRelative(0.1f, 0.15f, 1.4f, 2.1f, 3.4f, 2.9f)
                curveToRelative(2.0f, 0.8f, 2.0f, 0.5f, 2.3f, 0.5f)
                curveToRelative(0.3f, 0.0f, 1.2f, -0.5f, 1.4f, -1.0f)
                curveToRelative(0.2f, -0.5f, 0.2f, -0.9f, 0.1f, -1.0f)
                curveToRelative(-0.1f, -0.1f, -0.2f, -0.15f, -0.4f, -0.25f)
                close()
            }.build()
            return _whatsapp!!
        }
    private var _whatsapp: ImageVector? = null
}