package com.example.mpdam.n_puzzle

import android.view.MotionEvent
import kotlin.math.PI
import kotlin.math.atan2

class FlingDetector {

    companion object {

        private val BOUNDARY_ANGLES =
            doubleArrayOf(0.0, 0.25 * PI, 0.75 * PI, 1.25 * PI, 1.75 * PI, 2 * PI)

        fun getDirection(
            e1: MotionEvent,
            e2: MotionEvent,
        ): FlingDirection {
            val angle: Double = getAngle(e1.x, e1.y, e2.x, e2.y)

            if (isUp(angle)) {
                return FlingDirection.UP
            }

            if (isDown(angle)) {
                return FlingDirection.DOWN
            }

            if (isLeft(angle)) {
                return FlingDirection.LEFT
            }

            if (isRight(angle)) {
                return FlingDirection.RIGHT
            }

            /* Since the code covers all possible angles, this code should be unreachable. */
            return FlingDirection.INVALID
        }


        private fun getAngle(x1: Float, y1: Float, x2: Float, y2: Float): Double {
            /*
             * Add π since it is more convenient to work with values in the range [0, 2π]
             * compared to those in atan2's default [-π, π].
             */
            return atan2((y1 - y2).toDouble(), (x2 - x1).toDouble()) + PI
        }

        private fun isUp(angle: Double): Boolean {
            return BOUNDARY_ANGLES[3] <= angle && angle <= BOUNDARY_ANGLES[4]
        }

        private fun isDown(angle: Double): Boolean {
            return BOUNDARY_ANGLES[1] <= angle && angle <= BOUNDARY_ANGLES[2]
        }

        private fun isLeft(angle: Double): Boolean {
            return BOUNDARY_ANGLES[0] <= angle && angle <= BOUNDARY_ANGLES[1]
                    || BOUNDARY_ANGLES[4] <= angle && angle <= BOUNDARY_ANGLES[5]
        }

        private fun isRight(angle: Double): Boolean {
            return BOUNDARY_ANGLES[2] <= angle && angle <= BOUNDARY_ANGLES[3]
        }
    }
}