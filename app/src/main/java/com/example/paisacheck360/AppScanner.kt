package com.example.paisacheck360

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppScanner(private val context: Context) {

    /**
     * Scans all installed apps and returns risk analysis
     */
    suspend fun scanAllApps(): List<AppRiskInfo> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        val appRiskList = mutableListOf<AppRiskInfo>()

        installedApps.forEach { appInfo ->
            try {
                // Skip system apps if needed (optional - currently including all)
                val packageInfo = packageManager.getPackageInfo(
                    appInfo.packageName,
                    PackageManager.GET_PERMISSIONS
                )

                // Get all requested permissions
                val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()

                // Analyze permissions
                val (riskScore, dangerousPermissions) = PermissionAnalyzer.analyzePermissions(permissions)
                val riskLevel = PermissionAnalyzer.getRiskLevel(riskScore)

                // Get app details
                val appName = packageManager.getApplicationLabel(appInfo).toString()
                val icon = packageManager.getApplicationIcon(appInfo)
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                // Get install/update dates
                val installDate = packageInfo.firstInstallTime
                val lastUpdateDate = packageInfo.lastUpdateTime

                appRiskList.add(
                    AppRiskInfo(
                        appName = appName,
                        packageName = appInfo.packageName,
                        icon = icon,
                        riskLevel = riskLevel,
                        riskScore = riskScore,
                        dangerousPermissions = dangerousPermissions,
                        totalPermissions = permissions.size,
                        installDate = installDate,
                        lastUpdateDate = lastUpdateDate,
                        isSystemApp = isSystemApp
                    )
                )
            } catch (e: Exception) {
                // Skip apps that can't be analyzed
                e.printStackTrace()
            }
        }

        // Sort by risk score (highest first)
        appRiskList.sortedByDescending { it.riskScore }
    }

    /**
     * Get quick statistics
     */
    suspend fun getStatistics(): AppScanStatistics = withContext(Dispatchers.IO) {
        val apps = scanAllApps()
        AppScanStatistics(
            totalApps = apps.size,
            criticalApps = apps.count { it.riskLevel == RiskLevel.CRITICAL },
            highRiskApps = apps.count { it.riskLevel == RiskLevel.HIGH },
            mediumRiskApps = apps.count { it.riskLevel == RiskLevel.MEDIUM },
            lowRiskApps = apps.count { it.riskLevel == RiskLevel.LOW }
        )
    }
}

data class AppScanStatistics(
    val totalApps: Int,
    val criticalApps: Int,
    val highRiskApps: Int,
    val mediumRiskApps: Int,
    val lowRiskApps: Int
)