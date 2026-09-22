package com.example.physicswhiteboard

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** A small, platform-independent point used by the simulation and drawing layers. */
data class GeometryPoint(val x: Float, val y: Float)

data class BodyHalfExtents(val halfWidth: Float, val halfHeight: Float)

/**
 * Geometry shared by hit testing, collision/fusion thresholds, and rendering.
 * Keeping these calculations together prevents the canvas and the physics loop
 * from disagreeing about how large a body is.
 */
object BodyGeometry {
    private const val MIN_SYMBOL_HALF_WIDTH = 24f
    private const val MIN_SYMBOL_HALF_HEIGHT = 26f
    private const val HIT_PADDING = 8f
    private const val ROTATION_HANDLE_DISTANCE = 44f

    fun expression(body: SimBody): RenderedExpression =
        body.renderedExpr ?: FormulaTypesetter.buildExpression(body.char, body.char)

    fun visualHalfExtents(body: SimBody): BodyHalfExtents {
        return when {
            body.isFieldSource && body.fieldType == FieldType.ELECTRIC_E ->
                BodyHalfExtents(FieldGeometry.halfLength(body), FieldGeometry.halfWidth(body))

            body.isFieldSource && body.fieldType == FieldType.MAGNETIC_B ->
                BodyHalfExtents(body.fieldRadius, body.fieldRadius)

            body.isRod ->
                BodyHalfExtents(body.rodLength / 2f, 18f)

            else -> {
                val expr = expression(body)
                BodyHalfExtents(
                    max(MIN_SYMBOL_HALF_WIDTH, expr.width / 2f + HIT_PADDING),
                    max(MIN_SYMBOL_HALF_HEIGHT, expr.height / 2f + HIT_PADDING)
                )
            }
        }
    }

    fun collisionHalfExtents(body: SimBody): BodyHalfExtents = visualHalfExtents(body)

    fun collisionRadius(body: SimBody): Float {
        val extents = collisionHalfExtents(body)
        return hypot(extents.halfWidth, extents.halfHeight)
    }

    /**
     * The distance at which a dragged body can fuse with another body. It is
     * based on both rendered bodies instead of a single screen-space constant.
     */
    fun fusionDistance(first: SimBody, second: SimBody): Float {
        val combinedRadius = collisionRadius(first) + collisionRadius(second)
        return min(160f, max(60f, combinedRadius * 0.72f))
    }

    fun isWithinFusionDistance(first: SimBody, second: SimBody): Boolean =
        hypot(first.x - second.x, first.y - second.y) <= fusionDistance(first, second)

    fun maxHalfExtent(body: SimBody): Float {
        val extents = visualHalfExtents(body)
        return max(extents.halfWidth, extents.halfHeight)
    }

    /**
     * Minimum center distance used by the magnetic fusion preview. It grows
     * with big formulas but always stays inside the fusion range, so the snap
     * can never push a pair out of a valid fusion.
     */
    fun fusionPreviewDistance(first: SimBody, second: SimBody): Float {
        val want = (maxHalfExtent(first) + maxHalfExtent(second)) * 0.8f
        return min(want.coerceIn(30f, 110f), fusionDistance(first, second) * 0.8f)
    }

    fun containsPoint(body: SimBody, x: Float, y: Float): Boolean {
        return when {
            body.isFieldSource && body.fieldType == FieldType.ELECTRIC_E ->
                FieldGeometry.contains(body, x, y, HIT_PADDING)

            body.isFieldSource && body.fieldType == FieldType.MAGNETIC_B ->
                hypot(x - body.x, y - body.y) <= body.fieldRadius + HIT_PADDING

            body.isRod -> {
                val local = rotateIntoRodSpace(body, x, y)
                abs(local.x) <= body.rodLength / 2f + HIT_PADDING && abs(local.y) <= 18f + HIT_PADDING
            }

            else -> {
                val extents = visualHalfExtents(body)
                abs(x - body.x) <= extents.halfWidth && abs(y - body.y) <= extents.halfHeight
            }
        }
    }

    fun rotationHandle(body: SimBody): GeometryPoint? {
        val angle = when {
            body.hasThrust -> body.thrustAngle
            body.isFieldSource && body.fieldType == FieldType.ELECTRIC_E -> body.fieldAngle
            body.isRod -> body.rodAngle
            else -> return null
        }
        return GeometryPoint(
            body.x + sin(angle) * ROTATION_HANDLE_DISTANCE,
            body.y - cos(angle) * ROTATION_HANDLE_DISTANCE
        )
    }

    fun isOnRotationHandle(body: SimBody, x: Float, y: Float): Boolean {
        val handle = rotationHandle(body) ?: return false
        return hypot(x - handle.x, y - handle.y) <= 28f
    }

    private fun rotateIntoRodSpace(body: SimBody, x: Float, y: Float): GeometryPoint {
        val dx = x - body.x
        val dy = y - body.y
        val c = cos(body.rodAngle)
        val s = sin(body.rodAngle)
        return GeometryPoint(dx * c + dy * s, -dx * s + dy * c)
    }
}

/**
 * Electric fields are rectangles in their own local coordinate system. The
 * field angle points in the direction of the arrows; screen +Y is downward.
 */
object FieldGeometry {
    fun halfLength(field: SimBody): Float = field.fieldRadius.coerceAtLeast(1f)

    fun halfWidth(field: SimBody): Float =
        field.fieldHalfWidth.coerceIn(1f, halfLength(field))

    fun worldToLocal(field: SimBody, x: Float, y: Float): GeometryPoint {
        val dx = x - field.x
        val dy = y - field.y
        val c = cos(field.fieldAngle)
        val s = sin(field.fieldAngle)
        return GeometryPoint(dx * c + dy * s, -dx * s + dy * c)
    }

    fun localToWorld(field: SimBody, x: Float, y: Float): GeometryPoint {
        val c = cos(field.fieldAngle)
        val s = sin(field.fieldAngle)
        return GeometryPoint(
            field.x + c * x - s * y,
            field.y + s * x + c * y
        )
    }

    fun contains(field: SimBody, x: Float, y: Float, padding: Float = 0f): Boolean {
        val local = worldToLocal(field, x, y)
        return abs(local.x) <= halfLength(field) + padding &&
                abs(local.y) <= halfWidth(field) + padding
    }

    fun corners(field: SimBody): List<GeometryPoint> {
        val length = halfLength(field)
        val width = halfWidth(field)
        return listOf(
            localToWorld(field, -length, -width),
            localToWorld(field, length, -width),
            localToWorld(field, length, width),
            localToWorld(field, -length, width)
        )
    }
}
