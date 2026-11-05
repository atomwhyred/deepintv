package com.deepintv.launcher.data

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val launchIntent: Intent,
)

/** Lists the apps a TV home screen can launch: everything exported for the leanback launcher row. */
class AppRepository(private val packageManager: PackageManager) {

    fun listLaunchableApps(): List<AppInfo> {
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val resolved = packageManager.queryIntentActivities(query, PackageManager.MATCH_ALL)
        return resolved
            .mapNotNull { resolveInfo ->
                val activity = resolveInfo.activityInfo ?: return@mapNotNull null
                val launchIntent = Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                    .setClassName(activity.packageName, activity.name)
                AppInfo(
                    packageName = activity.packageName,
                    label = resolveInfo.loadLabel(packageManager).toString(),
                    icon = resolveInfo.loadIcon(packageManager),
                    launchIntent = launchIntent,
                )
            }
            .filter { it.packageName != "com.deepintv.launcher" }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
}
