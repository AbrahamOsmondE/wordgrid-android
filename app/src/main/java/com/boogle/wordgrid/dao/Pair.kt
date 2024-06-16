package com.boogle.wordgrid.dao

import org.opencv.core.Mat


class Pair(var pos: Double, var contour: Mat) : Comparable<Pair?> {
    override fun compareTo(other: Pair?): Int {
        return java.lang.Double.compare(pos, other!!.pos)
    }

    fun equals(obj: Pair?): Boolean {
        return contour == obj!!.contour
    }

    override fun equals(obj: Any?): Boolean {
        return equals(obj as Pair?)
    }
}