package dev.nutting.kexplore

import android.app.Application
import dev.nutting.kexplore.data.connection.ConnectionStore

class KexploreApp : Application() {

    lateinit var connectionStore: ConnectionStore
        private set

    override fun onCreate() {
        super.onCreate()
        connectionStore = ConnectionStore(this)
    }
}
