package de.dertyp7214.rboardthememanager

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.dertyp7214.logs.helpers.Logger
import com.downloader.PRDownloader
import com.topjohnwu.superuser.Shell
import de.dertyp7214.rboardthememanager.core.isReachable
import de.dertyp7214.rboardthememanager.utils.GboardUtils
import de.dertyp7214.rboardthememanager.utils.MagiskUtils
import de.dertyp7214.rboardthememanager.utils.doInBackground
import java.net.URL

class Application : Application() {

    companion object {
        var context: Application? = null
            private set

        var uiHandler: Handler? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        PRDownloader.initialize(this)
        doInBackground {
            if (!URL("https://bin.utwitch.net").isReachable())
                Logger.customBin = "hastebin.com"
        }
        Logger.init(this)
        Logger.extraData = {
            StringBuilder("Rooted: ")
                .append(if (Shell.rootAccess()) "yes" else "no").append("\n")
                .append("Version-Code: ").append(BuildConfig.VERSION_CODE).append("\n")
                .append("Gboard-Version-Code: ").append(GboardUtils.getGboardVersionCode(this))
                .append("\n")
                .append("Gboard-Version-Name: ").append(GboardUtils.getGboardVersion(this))
                .append("\n")
                .append("Version-Name: ").append(BuildConfig.VERSION_NAME).append("\n\n")
                .apply {
                    val magisk = MagiskUtils.isMagiskInstalled()
                    append("Magisk: ").append(if (magisk) "yes" else "no")
                        .append("\n")
                    if (magisk) {
                        append("Magisk-Version: ")
                            .append(MagiskUtils.getMagiskVersionFullString()).append("\n")
                        append("Magisk Modules:\n")
                        MagiskUtils.getModules().forEach { module ->
                            append("\t\t").append(module.meta.name).append("\n")
                        }
                    }
                }.toString()
        }
        context = this
        uiHandler = Handler(Looper.getMainLooper())
        PreferenceManager.getDefaultSharedPreferences(this).apply {
            when (getString("app_theme", "system_theme")) {
                "dark" -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
                "light" -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
                "system_theme" -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_UNSPECIFIED)
                }
            }
        }
    }
}