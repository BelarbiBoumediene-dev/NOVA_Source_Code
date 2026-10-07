package com.example.quizz

import android.app.Application
import android.app.Activity
import android.os.Bundle
import android.content.Context

class QuizzApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext

        registerActivityLifecycleCallbacks(
            object : ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) {
                    if (
                        activity !is WelcomeActivity &&
                        activity !is MainActivity0 &&
                        activity !is NovaIntroActivity
                    ) {
                        CosmeticThemeManager.apply(activity)
                    }
                }

                override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
                override fun onActivityStarted(activity: Activity) = Unit
                override fun onActivityPaused(activity: Activity) = Unit
                override fun onActivityStopped(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
                override fun onActivityDestroyed(activity: Activity) = Unit
            }
        )
    }

    companion object {
        lateinit var appContext: Context
            private set
    }
}
