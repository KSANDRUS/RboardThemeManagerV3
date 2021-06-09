package de.dertyp7214.rboardthememanager.utils

import android.annotation.SuppressLint
import android.content.Intent
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileInputStream
import de.Maxr1998.modernpreferences.Preference
import de.Maxr1998.modernpreferences.PreferenceScreen
import de.Maxr1998.modernpreferences.helpers.*
import de.dertyp7214.rboardthememanager.Application
import de.dertyp7214.rboardthememanager.Config
import de.dertyp7214.rboardthememanager.R
import de.dertyp7214.rboardthememanager.core.*
import de.dertyp7214.rboardthememanager.screens.PreferencesActivity
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

class Flags {
    enum class FILES(val fileName: String) {
        FLAGS("flag_value.xml"),
        GBOARD_PREFERENCES("${Config.GBOARD_PACKAGE_NAME}_preferences.xml")
    }

    enum class TYPE {
        BOOLEAN,
        STRING,
        INT,
        LONG,
        FLOAT,
        GROUP,
        JUST_CLICK
    }

    enum class FLAGS(
        val key: String,
        @StringRes val title: Int,
        @StringRes val summary: Int,
        @DrawableRes val icon: Int,
        val defaultValue: Any,
        val type: TYPE,
        val file: FILES,
        val valueMap: Map<Any?, Any?>? = null,
        val visible: Boolean = true,
        val onClick: () -> Unit = {}
    ) {
        EMOJI_COMPAT_APP_WHITELIST(
            "emoji_compat_app_whitelist",
            R.string.emoji_compat_app_whitelist,
            -1,
            R.drawable.ic_emoji_compat,
            false,
            TYPE.BOOLEAN,
            FILES.FLAGS,
            mapOf(
                Pair(true, "*"),
                Pair(false, "disabled"),
                Pair(null, "disabled")
            )
        ),
        ENABLE_JOYSTICK_DELETE(
            "enable_joystick_delete",
            R.string.enable_joystick_delete,
            -1,
            R.drawable.ic_backspace,
            false,
            TYPE.BOOLEAN,
            FILES.FLAGS
        ),
        DEPRECATE_SEARCH(
            "deprecate_search",
            R.string.deprecate_search,
            -1,
            R.drawable.ic_google_logo,
            false,
            TYPE.BOOLEAN,
            FILES.FLAGS,
            mapOf(
                Pair(false, second = true),
                Pair(null, false),
                Pair(true, second = false)
            ),
            false
        ),
        ENABLE_SHARING(
            "enable_sharing",
            R.string.enable_sharing,
            -1,
            R.drawable.ic_baseline_share_24,
            false,
            TYPE.BOOLEAN,
            FILES.FLAGS
        ),
        THEMED_NAVBAR_STYLE(
            "themed_navbar_style",
            R.string.themed_navbar_style,
            -1,
            R.drawable.ic_navbar,
            false,
            TYPE.BOOLEAN,
            FILES.FLAGS,
            mapOf(
                Pair(true, 2),
                Pair(false, 1),
                Pair(null, 1)
            )
        ),
        ENABLE_EMAIL_PROVIDER_COMPLETION(
            "enable_email_provider_completion",
            R.string.enable_email_provider_completion,
            -1,
            R.drawable.ic_email,
            false,
            TYPE.BOOLEAN,
            FILES.FLAGS
        ),
        ENABLE_POPUP_VIEW_V2(
            "enable_popup_view_v2",
            R.string.enable_popup_view_v2,
            -1,
            R.drawable.ic_popup_v2,
            false,
            TYPE.BOOLEAN,
            FILES.FLAGS
        ),
        ENABLE_KEY_BORDER(
            "enable_key_border",
            R.string.enable_key_border,
            -1,
            R.drawable.ic_key_boarder,
            false,
            TYPE.BOOLEAN,
            FILES.GBOARD_PREFERENCES
        ),
        ENABLE_SECONDARY_SYMBOLS(
            "enable_secondary_symbols",
            R.string.enable_secondary_symbols,
            -1,
            R.drawable.ic_numeric,
            false,
            TYPE.BOOLEAN,
            FILES.GBOARD_PREFERENCES
        ),
        SHOW_SUGGESTIONS(
            "show_suggestions",
            R.string.show_suggestions,
            -1,
            R.drawable.ic_alphabetical,
            false,
            TYPE.BOOLEAN,
            FILES.GBOARD_PREFERENCES
        ),
        SHOW_ALL_FLAGS(
            "show_all_flags",
            R.string.show_all_flags,
            R.string.show_all_flags_long,
            -1,
            "",
            TYPE.JUST_CLICK,
            FILES.FLAGS,
            onClick = {
                Application.context?.let {
                    PreferencesActivity::class.java.start(
                        it
                    ) {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        putExtra("type", "all_flags")
                    }
                }
            }
        );

        inline fun <reified T> getValue(defaultValue: T? = null): T? {
            return if (valueMap != null) valueMap.filter {
                it.value == values[key]
            }.entries.firstOrNull()?.key as? T ?: defaultValue else values[key] as? T
                ?: defaultValue
        }

        @SuppressLint("SdCardPath")
        fun <T> setValue(v: T): Boolean {
            val value = if (valueMap != null) valueMap[v] else v
            val fileName = "/data/data/${Config.GBOARD_PACKAGE_NAME}/shared_prefs/${file.fileName}"
            val content = SuFileInputStream.open(SuFile(fileName)).use {
                it.bufferedReader().readText()
            }.let { fileText ->
                val type = when (value) {
                    is Boolean -> "boolean"
                    is Int -> "integer"
                    is Float -> "float"
                    else -> "string"
                }
                if (type != "string") {
                    when {
                        "<$type name=\"$key\"" in fileText -> fileText.replace(
                            """<$type name="$key" value=".*" />""".toRegex(),
                            """<$type name="$key" value="$value" />"""
                        )
                        Regex("<map[ |]/>") in fileText -> fileText.replace(
                            Regex("<map[ |]/>"),
                            """<map><$type name="$key" value="$value" /></map>"""
                        )
                        else -> fileText.replace(
                            "<map>",
                            """<map><$type name="$key" value="$value" />"""
                        )
                    }
                } else {
                    when {
                        "<$type name\"$key\"" in fileText -> fileText.replace(
                            """<$type name="$key">.*</$type>""".toRegex(),
                            """<$type name="$key">$value</$type>"""
                        )
                        Regex("<map[ |]>") in fileText -> fileText.replace(
                            Regex("<map[ |]>"),
                            """<map><$type name="$key">$value</$type></map>"""
                        )
                        else -> fileText.replace(
                            "<map>",
                            """<map><$type name="$key">$value</$type>"""
                        )
                    }
                }
            }

            SuFile(fileName).writeFile(content)

            return "am force-stop ${Config.GBOARD_PACKAGE_NAME}".runAsCommand()
        }
    }

    fun preferences(builder: PreferenceScreen.Builder) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(Application.context)
        FLAGS.values().forEach { item ->
            if (item.visible) {
                prefs.edit { remove(item.key) }
                val pref: Preference = when (item.type) {
                    TYPE.BOOLEAN -> builder.switch(item.key) {
                        defaultValue = item.getValue(item.defaultValue) as? Boolean ?: false
                        onCheckedChange {
                            if (!item.setValue(it)) Application.context?.let { context ->
                                Toast.makeText(
                                    context,
                                    R.string.error,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            true
                        }
                    }
                    TYPE.INT, TYPE.LONG, TYPE.FLOAT -> builder.pref(item.key) {}
                    TYPE.GROUP -> builder.categoryHeader(item.key) {
                        titleRes = item.title
                        summaryRes = item.summary
                    }.let { Preference("") }
                    TYPE.STRING -> builder.pref(item.key) {}
                    TYPE.JUST_CLICK -> builder.pref(item.key) {
                        onClick { item.onClick(); false }
                    }
                }
                pref.apply {
                    titleRes = item.title
                    summaryRes = item.summary
                    iconRes = item.icon
                }
            }
        }
    }

    companion object {
        val values: Map<String, Any>
            get() {
                return getCurrentXmlValues(FILES.FLAGS.fileName) + getCurrentXmlValues(FILES.GBOARD_PREFERENCES.fileName)
            }

        @SuppressLint("SdCardPath")
        private fun getCurrentXmlValues(file: String): Map<String, Any> {
            val output = HashMap<String, Any>()

            val fileName = "/data/data/${Config.GBOARD_PACKAGE_NAME}/shared_prefs/$file"
            val xmlFile = SuFile(fileName)
            if (!xmlFile.exists()) return output

            val map = try {
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    InputSource(
                        StringReader(
                            SuFileInputStream.open(xmlFile).bufferedReader().readText()
                        )
                    )
                ).getElementsByTagName("map")
            } catch (e: Exception) {
                return output
            }

            for (item in map.item(0).childNodes) {
                val name = item.attributes?.getNamedItem("name")?.nodeValue
                val value = item.attributes?.getNamedItem("value")?.nodeValue
                if (name != null) output[name] =
                    (value?.booleanOrNull() ?: value) ?: item.textContent ?: ""
            }

            return output
        }
    }
}