package com.anwesh.uiprojects.stepaltrotlineview

/**
 * Created by anweshmishra on 13/04/19.
 */

import android.view.View
import android.app.Activity
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Color
import android.content.Context
import android.view.MotionEvent

val nodes : Int = 5
val lines : Int = 4
val scGap : Float = 0.05f
val scDiv : Double = 0.51
val strokeFactor : Int = 90
val sizeFactor : Float = 2.9f
val foreColor : Int = Color.parseColor("#673AB7")
val backColor : Int = Color.parseColor("#BDBDBD")
val lineDeg : Float = 180f
val parts : Int = 2
val rotDeg : Float = 90f

fun Int.inverse() : Float = 1f / this
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.scaleFactor() : Float = Math.floor(this / scDiv).toFloat()
fun Float.mirrorValue(a : Int, b : Int) : Float {
    val k : Float = scaleFactor()
    return (1 - k) * a.inverse() + k * b.inverse()
}
fun Float.updateValue(dir : Float, a : Int, b : Int) : Float = mirrorValue(a, b) * dir * scGap
fun Int.sjf() : Float = 1f - 2 * (this % 2)

fun Canvas.drawRotatingLine(i : Int, sc : Float, xGap : Float, paint : Paint) {
    save()
    translate(0f, -xGap * i - xGap * sc.divideScale(1, 2))
    rotate(180f * i.sjf() * sc.divideScale(0, 2))
    drawLine(0f, 0f, -xGap * i.sjf(), 0f, paint)
    restore()
}

fun Canvas.drawSARLNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = h / (nodes + 1)
    val size : Float = gap / sizeFactor
    val sc1 : Float = scale.divideScale(0, 2)
    val sc2 : Float = scale.divideScale(1, 2)
    val xGap : Float = (2 * size) / lines
    paint.color = foreColor
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    paint.strokeCap = Paint.Cap.ROUND
    save()
    translate(w / 2, gap * (i + 1))
    rotate(rotDeg * sc2)
    save()
    translate(0f, size)
    for (j in 0..(lines - 1)) {
        drawRotatingLine(j, sc1.divideScale(j, lines), xGap, paint)
    }
    restore()
    restore()
}

class StepAltRotLineView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas : Canvas) {

    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {

            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var prevScale : Float = 0f, var dir : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scale.updateValue(dir, lines * parts, 1)
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View) {

        private var animated : Boolean = false

        fun animate(cb : () -> Unit) {
            cb()
            try {
                Thread.sleep(50)
                view.invalidate()
            } catch(ex : Exception) {

            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class SARLNode(var i : Int, val state : State = State()) {

        private var next : SARLNode? = null
        private var prev : SARLNode? = null

        init {

        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = SARLNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawSARLNode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : SARLNode {
            var curr : SARLNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class StepAltRotLine(var i : Int) {

        private val root : SARLNode = SARLNode(0)
        private var curr : SARLNode = root
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, scl)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : StepAltRotLineView) {

        private val animator : Animator = Animator(view)
        private var sarl : StepAltRotLine = StepAltRotLine(0)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(backColor)
            sarl.draw(canvas, paint)
            animator.animate {
                sarl.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            sarl.startUpdating {
                animator.start()
            }
        }
    }
}