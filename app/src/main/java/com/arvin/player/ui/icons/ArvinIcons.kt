package com.arvin.player.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Arvin's own icon set — hand-built [ImageVector]s on a 24×24 grid, so the app has a cohesive,
 * bespoke look instead of stock Material glyphs. Every icon is drawn in solid black and recoloured
 * by `Icon(tint = …)`, so a single icon works in both the light and dark themes automatically.
 *
 * The geometry was authored and visually verified as SVG before porting here 1:1.
 *
 * RTL note: the transport glyphs (Play/Pause/SkipNext/SkipPrevious) are NOT auto-mirrored — media
 * controls must keep their direction in Persian/Arabic. Only [ArrowBack] mirrors (autoMirror=true),
 * because a back chevron *should* flip in RTL.
 */
object ArvinIcons {

    private val Ink = Color(0xFF000000)

    private fun build(name: String, autoMirror: Boolean = false, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = "ArvinIcons.$name",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
            autoMirror = autoMirror
        ).apply(block).build()

    private fun ImageVector.Builder.stroke(block: PathBuilder.() -> Unit) {
        path(
            stroke = SolidColor(Ink),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) { block(this) }
    }

    private fun ImageVector.Builder.solid(block: PathBuilder.() -> Unit) {
        path(fill = SolidColor(Ink), pathFillType = PathFillType.NonZero) { block(this) }
    }

    private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        moveTo(cx + r, cy)
        arcToRelative(r, r, 0f, false, true, -2 * r, 0f)
        arcToRelative(r, r, 0f, false, true, 2 * r, 0f)
        close()
    }

    private fun PathBuilder.roundRect(x: Float, y: Float, w: Float, h: Float, r: Float) {
        moveTo(x + r, y)
        lineTo(x + w - r, y)
        arcToRelative(r, r, 0f, false, true, r, r)
        lineTo(x + w, y + h - r)
        arcToRelative(r, r, 0f, false, true, -r, r)
        lineTo(x + r, y + h)
        arcToRelative(r, r, 0f, false, true, -r, -r)
        lineTo(x, y + r)
        arcToRelative(r, r, 0f, false, true, r, -r)
        close()
    }

    // ---------------- Transport (never mirrored) ----------------

    val Play: ImageVector by lazy {
        build("Play") {
            solid { moveTo(8f, 5.5f); lineTo(18.5f, 12f); lineTo(8f, 18.5f); close() }
        }
    }

    val Pause: ImageVector by lazy {
        build("Pause") {
            solid { roundRect(7.3f, 5.5f, 3.4f, 13f, 1.7f); roundRect(13.3f, 5.5f, 3.4f, 13f, 1.7f) }
        }
    }

    val SkipNext: ImageVector by lazy {
        build("SkipNext") {
            solid {
                moveTo(6f, 6.5f); lineTo(14.2f, 12f); lineTo(6f, 17.5f); close()
                roundRect(15.6f, 6f, 2.8f, 12f, 1.4f)
            }
        }
    }

    val SkipPrevious: ImageVector by lazy {
        build("SkipPrevious") {
            solid {
                roundRect(5.6f, 6f, 2.8f, 12f, 1.4f)
                moveTo(18f, 6.5f); lineTo(9.8f, 12f); lineTo(18f, 17.5f); close()
            }
        }
    }

    // ---------------- Player controls ----------------

    val Shuffle: ImageVector by lazy {
        build("Shuffle") {
            stroke {
                moveTo(4f, 7f); horizontalLineTo(8f); lineTo(16f, 17f); horizontalLineTo(20f)
                moveTo(4f, 17f); horizontalLineTo(8f); lineTo(16f, 7f); horizontalLineTo(20f)
                moveTo(17f, 4f); lineTo(20.5f, 7f); lineTo(17f, 10f)
                moveTo(17f, 14f); lineTo(20.5f, 17f); lineTo(17f, 20f)
            }
        }
    }

    val Repeat: ImageVector by lazy {
        build("Repeat") {
            stroke {
                moveTo(17f, 2f); lineTo(21f, 6f); lineTo(17f, 10f)
                moveTo(3f, 12f); verticalLineTo(10f); arcToRelative(4f, 4f, 0f, false, true, 4f, -4f); horizontalLineTo(21f)
                moveTo(7f, 22f); lineTo(3f, 18f); lineTo(7f, 14f)
                moveTo(21f, 12f); verticalLineTo(14f); arcToRelative(4f, 4f, 0f, false, true, -4f, 4f); horizontalLineTo(3f)
            }
        }
    }

    val RepeatOne: ImageVector by lazy {
        build("RepeatOne") {
            stroke {
                moveTo(17f, 2f); lineTo(21f, 6f); lineTo(17f, 10f)
                moveTo(3f, 12f); verticalLineTo(10f); arcToRelative(4f, 4f, 0f, false, true, 4f, -4f); horizontalLineTo(21f)
                moveTo(7f, 22f); lineTo(3f, 18f); lineTo(7f, 14f)
                moveTo(21f, 12f); verticalLineTo(14f); arcToRelative(4f, 4f, 0f, false, true, -4f, 4f); horizontalLineTo(3f)
                moveTo(11.4f, 10.6f); lineTo(12.7f, 9.8f); verticalLineTo(15.2f)
            }
        }
    }

    // ---------------- Favourites ----------------

    val HeartFilled: ImageVector by lazy {
        build("HeartFilled") {
            solid {
                moveTo(12f, 21.35f)
                lineToRelative(-1.45f, -1.32f)
                curveTo(5.4f, 15.36f, 2f, 12.28f, 2f, 8.5f)
                curveTo(2f, 5.42f, 4.42f, 3f, 7.5f, 3f)
                curveToRelative(1.74f, 0f, 3.41f, 0.81f, 4.5f, 2.09f)
                curveTo(13.09f, 3.81f, 14.76f, 3f, 16.5f, 3f)
                curveTo(19.58f, 3f, 22f, 5.42f, 22f, 8.5f)
                curveToRelative(0f, 3.78f, -3.4f, 6.86f, -8.55f, 11.54f)
                lineTo(12f, 21.35f)
                close()
            }
        }
    }

    val HeartOutline: ImageVector by lazy {
        build("HeartOutline") {
            stroke {
                moveTo(12f, 20.5f)
                curveTo(6.5f, 15.8f, 3.5f, 12.8f, 3.5f, 8.9f)
                curveTo(3.5f, 6.2f, 5.5f, 4.3f, 7.9f, 4.3f)
                curveToRelative(1.7f, 0f, 3.2f, 1f, 4.1f, 2.4f)
                curveToRelative(0.9f, -1.4f, 2.4f, -2.4f, 4.1f, -2.4f)
                curveToRelative(2.4f, 0f, 4.4f, 1.9f, 4.4f, 4.6f)
                curveToRelative(0f, 3.9f, -3f, 6.9f, -8.5f, 11.6f)
                close()
            }
        }
    }

    // ---------------- Navigation & actions ----------------

    val Search: ImageVector by lazy {
        build("Search") {
            stroke {
                moveTo(17f, 10.5f)
                arcTo(6.5f, 6.5f, 0f, true, false, 4f, 10.5f)
                arcTo(6.5f, 6.5f, 0f, true, false, 17f, 10.5f)
                close()
                moveTo(15.6f, 15.6f); lineTo(20.5f, 20.5f)
            }
        }
    }

    val Tune: ImageVector by lazy {
        build("Tune") {
            solid {
                roundRect(4.5f, 10f, 3.4f, 10f, 1.7f)
                roundRect(10.3f, 4.5f, 3.4f, 15.5f, 1.7f)
                roundRect(16.1f, 13f, 3.4f, 7f, 1.7f)
            }
        }
    }

    val Bedtime: ImageVector by lazy {
        build("Bedtime") {
            solid {
                moveTo(21f, 12.79f)
                arcTo(9f, 9f, 0f, true, true, 11.21f, 3f)
                arcTo(7f, 7f, 0f, false, false, 21f, 12.79f)
                close()
            }
        }
    }

    val ChevronDown: ImageVector by lazy {
        build("ChevronDown") {
            stroke { moveTo(6f, 9.5f); lineTo(12f, 15.5f); lineTo(18f, 9.5f) }
        }
    }

    val ArrowBack: ImageVector by lazy {
        build("ArrowBack", autoMirror = true) {
            stroke {
                moveTo(19f, 12f); horizontalLineTo(5f)
                moveTo(11f, 6f); lineTo(5f, 12f); lineTo(11f, 18f)
            }
        }
    }

    val Add: ImageVector by lazy {
        build("Add") {
            stroke { moveTo(12f, 5f); verticalLineTo(19f); moveTo(5f, 12f); horizontalLineTo(19f) }
        }
    }

    val MoreVert: ImageVector by lazy {
        build("MoreVert") {
            solid { circle(12f, 5.5f, 1.9f); circle(12f, 12f, 1.9f); circle(12f, 18.5f, 1.9f) }
        }
    }

    val QueueMusic: ImageVector by lazy {
        build("QueueMusic") {
            stroke {
                moveTo(4f, 7f); horizontalLineTo(16f)
                moveTo(4f, 12f); horizontalLineTo(16f)
                moveTo(4f, 17f); horizontalLineTo(11f)
                moveTo(16.5f, 17f); verticalLineTo(8.5f); lineTo(21f, 7f)
            }
            solid { circle(14.7f, 17f, 2f) }
        }
    }

    val Settings: ImageVector by lazy {
        build("Settings") {
            stroke {
                moveTo(4f, 7f); horizontalLineTo(20f)
                moveTo(4f, 12f); horizontalLineTo(20f)
                moveTo(4f, 17f); horizontalLineTo(20f)
            }
            solid { circle(9f, 7f, 2.5f); circle(16f, 12f, 2.5f); circle(8f, 17f, 2.5f) }
        }
    }

    val MusicNote: ImageVector by lazy {
        build("MusicNote") {
            stroke { moveTo(9f, 17.5f); verticalLineTo(6f); lineTo(18.5f, 4f); verticalLineTo(15.5f) }
            solid { circle(6.5f, 17.6f, 2.7f); circle(16f, 15.6f, 2.7f) }
        }
    }

    val Delete: ImageVector by lazy {
        build("Delete") {
            stroke {
                moveTo(4f, 6.5f); horizontalLineTo(20f)
                moveTo(9.5f, 6.5f); verticalLineTo(5.2f); arcTo(1.3f, 1.3f, 0f, false, true, 10.8f, 3.9f); horizontalLineTo(13.2f); arcTo(1.3f, 1.3f, 0f, false, true, 14.5f, 5.2f); verticalLineTo(6.5f)
                moveTo(6.6f, 6.5f); lineTo(7.4f, 19f); arcTo(1.6f, 1.6f, 0f, false, false, 9f, 20.5f); horizontalLineTo(15f); arcTo(1.6f, 1.6f, 0f, false, false, 16.6f, 19f); lineTo(17.4f, 6.5f)
                moveTo(10f, 10f); verticalLineTo(16.5f); moveTo(14f, 10f); verticalLineTo(16.5f)
            }
        }
    }

    val Lock: ImageVector by lazy {
        build("Lock") {
            stroke {
                moveTo(7.5f, 11f); horizontalLineTo(16.5f); arcTo(2f, 2f, 0f, false, true, 18.5f, 13f); verticalLineTo(18.5f); arcTo(2f, 2f, 0f, false, true, 16.5f, 20.5f); horizontalLineTo(7.5f); arcTo(2f, 2f, 0f, false, true, 5.5f, 18.5f); verticalLineTo(13f); arcTo(2f, 2f, 0f, false, true, 7.5f, 11f); close()
                moveTo(8f, 11f); verticalLineTo(7.8f); arcTo(4f, 4f, 0f, false, true, 16f, 7.8f); verticalLineTo(11f)
            }
        }
    }

    val Fingerprint: ImageVector by lazy {
        build("Fingerprint") {
            stroke {
                moveTo(12f, 4.5f); arcTo(7.5f, 7.5f, 0f, false, false, 4.5f, 12f)
                moveTo(19.5f, 12f); arcTo(7.5f, 7.5f, 0f, false, false, 12f, 4.5f)
                moveTo(12f, 8f); arcTo(4f, 4f, 0f, false, false, 8f, 12f); verticalLineTo(15f)
                moveTo(16f, 12f); arcTo(4f, 4f, 0f, false, false, 12f, 8f)
                moveTo(12f, 12f); verticalLineTo(16f)
            }
        }
    }

    val Visibility: ImageVector by lazy {
        build("Visibility") {
            stroke {
                moveTo(2.5f, 12f)
                curveTo(5f, 7.2f, 8.4f, 5.8f, 12f, 5.8f)
                curveTo(15.6f, 5.8f, 19f, 7.2f, 21.5f, 12f)
                curveTo(19f, 16.8f, 15.6f, 18.2f, 12f, 18.2f)
                curveTo(8.4f, 18.2f, 5f, 16.8f, 2.5f, 12f)
                close()
                circle(12f, 12f, 3.1f)
            }
        }
    }

    val VisibilityOff: ImageVector by lazy {
        build("VisibilityOff") {
            stroke {
                moveTo(2.5f, 12f)
                curveTo(5f, 7.2f, 8.4f, 5.8f, 12f, 5.8f)
                curveTo(15.6f, 5.8f, 19f, 7.2f, 21.5f, 12f)
                curveTo(19f, 16.8f, 15.6f, 18.2f, 12f, 18.2f)
                curveTo(8.4f, 18.2f, 5f, 16.8f, 2.5f, 12f)
                close()
                circle(12f, 12f, 3.1f)
                moveTo(4.5f, 4.5f)
                lineTo(19.5f, 19.5f)
            }
        }
    }

    val Folder: ImageVector by lazy {
        build("Folder") {
            stroke {
                moveTo(4f, 8f); arcTo(2f, 2f, 0f, false, true, 6f, 6f); horizontalLineTo(9.5f); lineTo(12f, 8.5f); horizontalLineTo(18f); arcTo(2f, 2f, 0f, false, true, 20f, 10.5f); verticalLineTo(17f); arcTo(2f, 2f, 0f, false, true, 18f, 19f); horizontalLineTo(6f); arcTo(2f, 2f, 0f, false, true, 4f, 17f); close()
                moveTo(12f, 11.5f); verticalLineTo(16f); moveTo(9.5f, 13.5f); horizontalLineTo(14.5f)
            }
        }
    }

    val Speed: ImageVector by lazy {
        build("Speed") {
            stroke {
                moveTo(4.5f, 17f); arcTo(8f, 8f, 0f, true, true, 19.5f, 17f)
                moveTo(12f, 12f); lineTo(15.5f, 9f)
            }
            solid { circle(12f, 12f, 1.6f) }
        }
    }

    val PlaylistAdd: ImageVector by lazy {
        build("PlaylistAdd") {
            stroke {
                moveTo(4f, 7f); horizontalLineTo(16f)
                moveTo(4f, 12f); horizontalLineTo(12f)
                moveTo(4f, 17f); horizontalLineTo(11f)
                moveTo(17f, 13f); verticalLineTo(21f); moveTo(13f, 17f); horizontalLineTo(21f)
            }
        }
    }
}
