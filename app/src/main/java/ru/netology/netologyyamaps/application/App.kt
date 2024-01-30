package ru.netology.netologyyamaps.application

import android.app.Application
import com.yandex.mapkit.MapKitFactory
import ru.netology.netologyyamaps.BuildConfig


class App: Application() {
    override fun onCreate() {
        super.onCreate()
        MapKitFactory.setApiKey(BuildConfig.MAPKIT_API_KEY)
    }
}