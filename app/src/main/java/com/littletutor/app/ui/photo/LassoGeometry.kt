package com.littletutor.app.ui.photo

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

internal fun isTokenHitByLasso(tokenRect: Rect, polygon: List<Offset>): Boolean {
    if (isPointInPolygon(tokenRect.center, polygon)) return true
    val corners = listOf(tokenRect.topLeft, tokenRect.topRight, tokenRect.bottomLeft, tokenRect.bottomRight)
    return corners.count { isPointInPolygon(it, polygon) } >= 2
}

internal fun distancePointToRect(point: Offset, rect: Rect): Float {
    val dx = when { point.x < rect.left -> rect.left - point.x; point.x > rect.right -> point.x - rect.right; else -> 0f }
    val dy = when { point.y < rect.top -> rect.top - point.y; point.y > rect.bottom -> point.y - rect.bottom; else -> 0f }
    return kotlin.math.sqrt(dx * dx + dy * dy)
}

/** Even-odd ray casting; the polygon is implicitly closed. */
internal fun isPointInPolygon(point: Offset, polygon: List<Offset>): Boolean {
    var inside = false
    var j = polygon.lastIndex
    for (i in polygon.indices) {
        val xi = polygon[i].x; val yi = polygon[i].y
        val xj = polygon[j].x; val yj = polygon[j].y
        // The straddle check guarantees yi != yj, so the division is safe.
        if ((yi > point.y) != (yj > point.y) && point.x < (xj - xi) * (point.y - yi) / (yj - yi) + xi) {
            inside = !inside
        }
        j = i
    }
    return inside
}
