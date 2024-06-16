package com.boogle.wordgrid.dao

import android.util.Log
import java.util.Stack
import kotlin.math.max
import kotlin.math.min


internal class IntervalNode<T>(var start: Double, var end: Double, var data: T?) :
    Comparable<IntervalNode<*>?> {
    private var lChild: IntervalNode<T>? = null
    private var rChild: IntervalNode<T>? = null
    private var parent: IntervalNode<T>? = null
    operator fun contains(node: IntervalNode<*>): Boolean {
        return if (node.start >= start && node.end <= end) {
            true
        } else false
    }

    fun overlaps(node: IntervalNode<*>): Boolean {
        return if (node.end < start || node.start > end) {
            false
        } else true
    }

    fun overlapsPoint(point: Double): Boolean {
        return if (point >= start && point <= end) {
            true
        } else false
    }

    fun add(node: IntervalNode<T>) {
        if (node.start <= start) {
            if (lChild == null) {
                lChild = node
                node.parent = this
            } else {
                lChild!!.add(node)
            }
        } else {
            if (rChild == null) {
                rChild = node
                node.parent = this
            } else {
                rChild!!.add(node)
            }
        }
    }

    fun remove(node: IntervalNode<T?>) { // lazy removal. sets data to null.
        if (node.data == null) {
            return
        }
        if (data != null) {
            if (this.equals(node)) { // same data
                data = null
            }
        } else if (node.start <= start) {
            if (lChild == null) {
                return
            }
            lChild!!.remove(node)
        } else {
            if (rChild == null) {
                return
            }
            rChild!!.remove(node)
        }
    }

    fun removeByData(datum: T?) { // preorder traversal
        if (datum == null) {
            Log.d("Open CV", "NULL data to remove")
            return
        }
        val stack = Stack<IntervalNode<T>?>()
        var curr: IntervalNode<T>? = this
        stack.push(curr)
        while (!stack.empty()) {
            curr = stack.pop()
            if (curr!!.data != null) { // hasn't been removed
                if (curr.data == datum) { // found it
                    Log.d("Open CV", "Found node to remove. Nullifying...")
                    curr.data = null
                    return
                }
            }
            if (curr.lChild != null) {
                stack.push(curr.lChild)
            }
            if (curr.rChild != null) {
                stack.push(curr.rChild)
            }
        }
        Log.d("Open CV", "Data couldn't be found for removal...")
    }

    fun equals(node: IntervalNode<T?>): Boolean {
        return data == node.data
    }

    fun merge(): ArrayList<IntervalNode<ArrayList<T?>>> {
        val stack = Stack<IntervalNode<T>>()
        val merged = ArrayList<IntervalNode<ArrayList<T?>>>()
        var curr: IntervalNode<T>? = this
        while (curr != null || !stack.empty()) {
            while (curr != null) {
                stack.push(curr)
                curr = curr.lChild
            }

            // Gone all the way down L subtree so curr is null
            curr = stack.pop()
            if (curr.data != null) {
                // has not been removed, so add to merged list
                if (merged.size == 0) { // Create new element
                    val newData = ArrayList<T?>()
                    newData.add(curr.data)
                    val newInterval = IntervalNode(curr.start, curr.end, newData)
                    merged.add(newInterval)
                } else {
                    val lastEl = merged[merged.size - 1]
                    if (lastEl.overlaps(curr)) { // Expand last element
                        lastEl.data!!.add(curr.data)
                        lastEl.start = min(lastEl.start, curr.start)
                        lastEl.end = max(lastEl.end, curr.end)
                    } else { // Create new element
                        val newData = ArrayList<T?>()
                        newData.add(curr.data)
                        val newInterval = IntervalNode(curr.start, curr.end, newData)
                        merged.add(newInterval)
                    }
                }
            }


            // Now visit right subtree
            curr = curr.rChild
        }
        return merged
    }

    override fun compareTo(other: IntervalNode<*>?): Int {
        return java.lang.Double.compare(start, other!!.start)

    }
}