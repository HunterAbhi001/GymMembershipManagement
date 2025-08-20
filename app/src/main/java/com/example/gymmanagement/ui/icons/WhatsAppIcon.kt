package com.example.gymmanagement.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Butt
import androidx.compose.ui.graphics.StrokeJoin.Companion.Miter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// This is a custom ImageVector for the official WhatsApp icon.
val WhatsAppIcon: ImageVector
    get() {
        if (_whatsapp != null) {
            return _whatsapp!!
        }
        _whatsapp = Builder(name = "Whatsapp", defaultWidth = 24.0.dp, defaultHeight = 24.0.dp,
            viewportWidth = 24.0f, viewportHeight = 24.0f).apply {
            path(fill = SolidColor(Color(0xFF4CAF50)), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero) {
                moveTo(12.04f, 2.0f)
                curveTo(6.58f, 2.0f, 2.13f, 6.45f, 2.13f, 11.91f)
                curveTo(2.13f, 13.66f, 2.61f, 15.31f, 3.45f, 16.73f)
                lineTo(2.0f, 22.0f)
                lineTo(7.42f, 20.5f)
                curveTo(8.8f, 21.29f, 10.37f, 21.71f, 12.04f, 21.71f)
                curveTo(17.5f, 21.71f, 21.95f, 17.26f, 21.95f, 11.8f)
                curveTo(21.95f, 6.34f, 17.5f, 2.0f, 12.04f, 2.0f)
                moveTo(12.04f, 20.26f)
                curveTo(10.57f, 20.26f, 9.15f, 19.88f, 7.9f, 19.17f)
                lineTo(7.53f, 18.96f)
                lineTo(4.34f, 19.81f)
                lineTo(5.21f, 16.71f)
                lineTo(4.97f, 16.32f)
                curveTo(4.17f, 15.03f, 3.68f, 13.5f, 3.68f, 11.91f)
                curveTo(3.68f, 7.27f, 7.45f, 3.5f, 12.04f, 3.5f)
                curveTo(16.63f, 3.5f, 20.4f, 7.27f, 20.4f, 11.8f)
                curveTo(20.4f, 16.33f, 16.63f, 20.26f, 12.04f, 20.26f)
                moveTo(17.46f, 14.35f)
                curveTo(17.24f, 14.24f, 16.05f, 13.68f, 15.83f, 13.6f)
                curveTo(15.61f, 13.52f, 15.46f, 13.48f, 15.3f, 13.71f)
                curveTo(15.14f, 13.94f, 14.66f, 14.48f, 14.52f, 14.64f)
                curveTo(14.38f, 14.8f, 14.24f, 14.82f, 13.99f, 14.71f)
                curveTo(13.75f, 14.6f, 12.91f, 14.31f, 11.91f, 13.46f)
                curveTo(11.11f, 12.79f, 10.56f, 12.03f, 10.42f, 11.81f)
                curveTo(10.28f, 11.59f, 10.39f, 11.47f, 10.51f, 11.36f)
                curveTo(10.61f, 11.25f, 10.74f, 11.08f, 10.88f, 10.93f)
                curveTo(11.02f, 10.78f, 11.07f, 10.67f, 11.16f, 10.49f)
                curveTo(11.25f, 10.31f, 11.19f, 10.15f, 11.11f, 10.03f)
                curveTo(11.03f, 9.91f, 10.56f, 8.72f, 10.36f, 8.22f)
                curveTo(10.16f, 7.72f, 9.96f, 7.79f, 9.82f, 7.79f)
                curveTo(9.68f, 7.79f, 9.53f, 7.79f, 9.39f, 7.79f)
                curveTo(9.25f, 7.79f, 9.02f, 7.85f, 8.82f, 8.04f)
                curveTo(8.62f, 8.24f, 8.13f, 8.69f, 8.13f, 9.83f)
                curveTo(8.13f, 10.97f, 9.42f, 12.16f, 9.56f, 12.32f)
                curveTo(9.7f, 12.48f, 10.56f, 13.87f, 11.96f, 14.48f)
                curveTo(13.36f, 15.09f, 13.79f, 14.92f, 14.21f, 14.86f)
                curveTo(14.63f, 14.8f, 15.64f, 14.26f, 15.83f, 14.18f)
                curveTo(16.02f, 14.1f, 16.21f, 14.14f, 16.35f, 14.26f)
                curveTo(16.49f, 14.38f, 17.03f, 14.92f, 17.24f, 15.14f)
                curveTo(17.45f, 15.36f, 17.65f, 15.42f, 17.74f, 15.36f)
                curveTo(17.83f, 15.3f, 17.68f, 15.14f, 17.46f, 14.35f)
                close()
            }
        }
            .build()
        return _whatsapp!!
    }

private var _whatsapp: ImageVector? = null
