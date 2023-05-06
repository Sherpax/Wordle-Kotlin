package com.example.wordle

import android.view.animation.Animation
import android.view.animation.RotateAnimation
import java.util.concurrent.CountDownLatch

class AnimationListener(private val latch: CountDownLatch) : Animation.AnimationListener {
        override fun onAnimationStart(animation: Animation?) {
            // No se utiliza
        }

        override fun onAnimationEnd(animation: Animation?) {
            // Se llama cuando la animación termina
            latch.countDown()
        }

        override fun onAnimationRepeat(animation: Animation?) {
            // No se utiliza
        }
    }

    // Crear una instancia de CountDownLatch con un contador inicial de 1
    val latch = CountDownLatch(1)

    // Crear la animación
    val rotateAnimation = RotateAnimation(
        0f, // fromDegrees
        -5f, // toDegrees
        Animation.RELATIVE_TO_SELF, 0.5f, // pivotX
        Animation.RELATIVE_TO_SELF, 0.5f // pivotY
    ).apply {
        duration = 120 // duration
        repeatCount = 0 // repeatCount
        setAnimationListener(AnimationListener(latch)) // Establecer el escucha de animación
    }

