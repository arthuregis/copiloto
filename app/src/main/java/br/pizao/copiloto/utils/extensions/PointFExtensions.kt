package br.pizao.copiloto.utils.extensions

import android.graphics.PointF
import kotlin.math.pow
import kotlin.math.sqrt

fun PointF.distance(other: PointF): Float {
    return sqrt((x - other.x).pow(2) + (y - other.y).pow(2))
}