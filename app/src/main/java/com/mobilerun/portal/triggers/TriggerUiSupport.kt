package com.mobilerun.portal.triggers

import android.content.Context
import com.mobilerun.portal.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TriggerUiSupport {
    private val timestampFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    fun sourceLabel(source: TriggerSource): String {
        return when (source) {
            TriggerSource.TIME_DELAY -> "After a delay"
            TriggerSource.TIME_ABSOLUTE -> "At a specific time"
            TriggerSource.TIME_DAILY -> "Repeat every day"
            TriggerSource.TIME_WEEKLY -> "Repeat on selected weekdays"
            else -> source.name
                .lowercase(Locale.US)
                .split('_')
                .joinToString(" ") { it.replaceFirstChar { char -> char.titlecase(Locale.US) } }
        }
    }

    fun sourceDescription(source: TriggerSource): String {
        return when (source) {
            TriggerSource.TIME_DELAY -> "Run once after the selected delay."
            TriggerSource.TIME_ABSOLUTE -> "Run once at the selected date and time."
            TriggerSource.TIME_DAILY -> "Run every day at the selected time."
            TriggerSource.TIME_WEEKLY -> "Run on the selected weekdays at the selected time."
            TriggerSource.NOTIFICATION_POSTED -> "Run when a matching notification appears."
            TriggerSource.NOTIFICATION_REMOVED -> "Run when a matching notification is dismissed or disappears."
            TriggerSource.APP_ENTERED -> "Run when the selected app comes to the foreground."
            TriggerSource.APP_EXITED -> "Run when the selected app leaves the foreground."
            TriggerSource.BATTERY_LOW -> "Run when Android reports that the battery is low."
            TriggerSource.BATTERY_OKAY -> "Run when Android reports that the battery is okay again."
            TriggerSource.BATTERY_LEVEL_CHANGED -> "Run when the battery level crosses the selected threshold."
            TriggerSource.POWER_CONNECTED -> "Run when charging starts."
            TriggerSource.POWER_DISCONNECTED -> "Run when charging stops."
            TriggerSource.USER_PRESENT -> "Run when the device is unlocked and the user becomes active."
            TriggerSource.NETWORK_CONNECTED -> "Run when the device gains network connectivity."
            TriggerSource.NETWORK_TYPE_CHANGED -> "Run when the connection type changes, such as Wi-Fi to cellular."
            TriggerSource.SMS_RECEIVED -> "Run when a matching SMS arrives."
        }
    }

    fun sourceLabel(context: Context, source: TriggerSource): String = context.getString(
        when (source) {
            TriggerSource.TIME_DELAY -> R.string.trigger_source_time_delay
            TriggerSource.TIME_ABSOLUTE -> R.string.trigger_source_time_absolute
            TriggerSource.TIME_DAILY -> R.string.trigger_source_time_daily
            TriggerSource.TIME_WEEKLY -> R.string.trigger_source_time_weekly
            TriggerSource.NOTIFICATION_POSTED -> R.string.trigger_source_notification_posted
            TriggerSource.NOTIFICATION_REMOVED -> R.string.trigger_source_notification_removed
            TriggerSource.APP_ENTERED -> R.string.trigger_source_app_entered
            TriggerSource.APP_EXITED -> R.string.trigger_source_app_exited
            TriggerSource.BATTERY_LOW -> R.string.trigger_source_battery_low
            TriggerSource.BATTERY_OKAY -> R.string.trigger_source_battery_okay
            TriggerSource.BATTERY_LEVEL_CHANGED -> R.string.trigger_source_battery_level_changed
            TriggerSource.POWER_CONNECTED -> R.string.trigger_source_power_connected
            TriggerSource.POWER_DISCONNECTED -> R.string.trigger_source_power_disconnected
            TriggerSource.USER_PRESENT -> R.string.trigger_source_user_present
            TriggerSource.NETWORK_CONNECTED -> R.string.trigger_source_network_connected
            TriggerSource.NETWORK_TYPE_CHANGED -> R.string.trigger_source_network_type_changed
            TriggerSource.SMS_RECEIVED -> R.string.trigger_source_sms_received
        },
    )

    fun sourceDescription(context: Context, source: TriggerSource): String = context.getString(
        when (source) {
            TriggerSource.TIME_DELAY -> R.string.trigger_description_time_delay
            TriggerSource.TIME_ABSOLUTE -> R.string.trigger_description_time_absolute
            TriggerSource.TIME_DAILY -> R.string.trigger_description_time_daily
            TriggerSource.TIME_WEEKLY -> R.string.trigger_description_time_weekly
            TriggerSource.NOTIFICATION_POSTED -> R.string.trigger_description_notification_posted
            TriggerSource.NOTIFICATION_REMOVED -> R.string.trigger_description_notification_removed
            TriggerSource.APP_ENTERED -> R.string.trigger_description_app_entered
            TriggerSource.APP_EXITED -> R.string.trigger_description_app_exited
            TriggerSource.BATTERY_LOW -> R.string.trigger_description_battery_low
            TriggerSource.BATTERY_OKAY -> R.string.trigger_description_battery_okay
            TriggerSource.BATTERY_LEVEL_CHANGED -> R.string.trigger_description_battery_level_changed
            TriggerSource.POWER_CONNECTED -> R.string.trigger_description_power_connected
            TriggerSource.POWER_DISCONNECTED -> R.string.trigger_description_power_disconnected
            TriggerSource.USER_PRESENT -> R.string.trigger_description_user_present
            TriggerSource.NETWORK_CONNECTED -> R.string.trigger_description_network_connected
            TriggerSource.NETWORK_TYPE_CHANGED -> R.string.trigger_description_network_type_changed
            TriggerSource.SMS_RECEIVED -> R.string.trigger_description_sms_received
        },
    )

    fun summary(rule: TriggerRule, context: Context? = null): String {
        return buildList {
            add(if (context == null) sourceLabel(rule.source) else sourceLabel(context, rule.source))
            rule.packageName?.takeIf { it.isNotBlank() }?.let { pkg ->
                val label = resolveAppLabel(context, pkg)
                add(
                    if (context == null) {
                        if (label != null) "app=$label" else "pkg=$pkg"
                    } else {
                        context.getString(if (label != null) R.string.trigger_summary_app else R.string.trigger_summary_package, label ?: pkg)
                    },
                )
            }
            rule.titleFilter?.takeIf { it.isNotBlank() }?.let {
                add(context?.getString(R.string.trigger_summary_title, it) ?: "title=$it")
            }
            rule.textFilter?.takeIf { it.isNotBlank() }?.let {
                add(context?.getString(R.string.trigger_summary_text, it) ?: "text=$it")
            }
            rule.networkType?.let {
                val label = if (context == null) it.name else context.getString(when (it) {
                    TriggerNetworkType.NONE -> R.string.trigger_network_none
                    TriggerNetworkType.WIFI -> R.string.trigger_network_wifi
                    TriggerNetworkType.CELLULAR -> R.string.trigger_network_cellular
                    TriggerNetworkType.ETHERNET -> R.string.trigger_network_ethernet
                    TriggerNetworkType.OTHER -> R.string.trigger_network_other
                })
                add(context?.getString(R.string.trigger_summary_network, label) ?: "network=$label")
            }
            rule.thresholdValue?.let {
                val comparison = if (context == null) rule.thresholdComparison.name.lowercase() else context.getString(
                    when (rule.thresholdComparison) {
                        TriggerThresholdComparison.AT_OR_BELOW -> R.string.trigger_option_at_or_below
                        TriggerThresholdComparison.AT_OR_ABOVE -> R.string.trigger_option_at_or_above
                    },
                )
                add(context?.getString(R.string.trigger_summary_threshold, comparison, it) ?: "threshold=$comparison $it")
            }
            rule.phoneNumberFilter?.takeIf { it.isNotBlank() }?.let {
                add(context?.getString(R.string.trigger_summary_phone, it) ?: "phone=$it")
            }
            rule.delayMinutes?.let {
                add(context?.getString(R.string.trigger_summary_delay, it) ?: "delay=${it}m")
            }
            if (rule.source == TriggerSource.TIME_ABSOLUTE) {
                rule.absoluteTimeMillis?.let {
                    val time = formatTimestamp(it)
                    add(context?.getString(R.string.trigger_summary_at, time) ?: "at=$time")
                }
            }
            if (rule.source == TriggerSource.TIME_DAILY || rule.source == TriggerSource.TIME_WEEKLY) {
                val hour = rule.dailyHour ?: 0
                val minute = rule.dailyMinute ?: 0
                add(String.format(Locale.US, "%02d:%02d", hour, minute))
                if (rule.source == TriggerSource.TIME_WEEKLY) {
                    val labels = rule.resolvedWeeklyDaysOfWeek()
                        .mapNotNull { if (context == null) dayOfWeekLabel(it) else dayOfWeekLabel(context, it) }
                    if (labels.isNotEmpty()) {
                        add(labels.joinToString(", "))
                    }
                }
            }
        }.joinToString(" • ")
    }

    fun dispositionLabel(disposition: TriggerRunDisposition): String {
        return disposition.name
            .lowercase(Locale.US)
            .split('_')
            .joinToString(" ") { it.replaceFirstChar { char -> char.titlecase(Locale.US) } }
    }

    fun dispositionLabel(context: Context, disposition: TriggerRunDisposition): String = context.getString(
        when (disposition) {
            TriggerRunDisposition.MATCHED -> R.string.trigger_disposition_matched
            TriggerRunDisposition.LAUNCHED -> R.string.trigger_disposition_launched
            TriggerRunDisposition.SKIPPED_BUSY -> R.string.trigger_disposition_skipped_busy
            TriggerRunDisposition.BUFFERED -> R.string.trigger_disposition_buffered
            TriggerRunDisposition.DEBOUNCED -> R.string.trigger_disposition_debounced
            TriggerRunDisposition.PERMISSION_MISSING -> R.string.trigger_disposition_permission_missing
            TriggerRunDisposition.LAUNCH_FAILED -> R.string.trigger_disposition_launch_failed
            TriggerRunDisposition.RULE_DISABLED -> R.string.trigger_disposition_rule_disabled
            TriggerRunDisposition.TEST_LAUNCHED -> R.string.trigger_disposition_test_launched
        },
    )

    fun formatTimestamp(timestampMs: Long): String {
        return timestampFormatter.format(Date(timestampMs))
    }

    fun dayOfWeekEntries(): List<Pair<String, Int>> {
        return listOf(
            "Sunday" to java.util.Calendar.SUNDAY,
            "Monday" to java.util.Calendar.MONDAY,
            "Tuesday" to java.util.Calendar.TUESDAY,
            "Wednesday" to java.util.Calendar.WEDNESDAY,
            "Thursday" to java.util.Calendar.THURSDAY,
            "Friday" to java.util.Calendar.FRIDAY,
            "Saturday" to java.util.Calendar.SATURDAY,
        )
    }

    fun dayOfWeekLabel(dayOfWeek: Int): String? {
        return dayOfWeekEntries().firstOrNull { it.second == dayOfWeek }?.first
    }

    fun dayOfWeekLabel(context: Context, dayOfWeek: Int): String? {
        val res = when (dayOfWeek) {
            java.util.Calendar.SUNDAY -> R.string.portal_ui_sun
            java.util.Calendar.MONDAY -> R.string.portal_ui_mon
            java.util.Calendar.TUESDAY -> R.string.portal_ui_tue
            java.util.Calendar.WEDNESDAY -> R.string.portal_ui_wed
            java.util.Calendar.THURSDAY -> R.string.portal_ui_thu
            java.util.Calendar.FRIDAY -> R.string.portal_ui_fri
            java.util.Calendar.SATURDAY -> R.string.portal_ui_sat
            else -> return null
        }
        return context.getString(res)
    }

    fun resolveAppLabel(context: Context?, packageName: String?): String? {
        if (context == null || packageName.isNullOrBlank()) return null
        return try {
            val pm = context.packageManager
            val appInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, android.content.pm.PackageManager.ApplicationInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            null
        }
    }
}
