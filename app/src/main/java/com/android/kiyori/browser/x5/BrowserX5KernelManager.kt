package com.android.kiyori.browser.x5

import android.content.Context
import android.util.Log
import com.android.kiyori.browser.data.BrowserPreferencesRepository
import com.tencent.smtt.sdk.QbSdk
import com.tencent.smtt.sdk.TbsDownloadConfig
import com.tencent.smtt.sdk.TbsListener
import com.tencent.smtt.sdk.TbsPVConfig

data class BrowserX5KernelState(
    val sdkVersion: Int,
    val coreVersion: Int,
    val crashCoreVersion: Int,
    val tmpCoreVersion: Int,
    val disabledCoreVersion: Int,
    val preloadDisableVersion: Int,
    val canLoadX5: Boolean,
    val isCoreInited: Boolean,
    val isEnabled: Boolean,
    val isSysWebViewForced: Boolean,
    val isInitEnvironmentStarted: Boolean,
    val isInstalling: Boolean,
    val installInterruptCode: Int,
    val currentProcessName: String,
    val tbsFolderPath: String,
    val lastDownloadProgress: Int,
    val lastDownloadCode: Int?,
    val lastInstallCode: Int?,
    val lastInitFinished: Boolean?
)

object BrowserX5KernelManager {
    private const val TAG = "KiyoriX5"

    @Volatile
    private var initialized = false

    @Volatile
    private var lastDownloadProgress: Int = -1

    @Volatile
    private var lastDownloadCode: Int? = null

    @Volatile
    private var lastInstallCode: Int? = null

    @Volatile
    private var lastInitFinished: Boolean? = null

    @Volatile
    private var netLogEnabled: Boolean = false

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        val preferencesRepository = BrowserPreferencesRepository(appContext)
        applyKernelSwitch(appContext, preferencesRepository.isX5Enabled())
        if (initialized) {
            return
        }
        initialized = true

        QbSdk.setDownloadWithoutWifi(true)
        QbSdk.setTbsListener(object : TbsListener {
            override fun onDownloadFinish(code: Int) {
                lastDownloadCode = code
                Log.i(TAG, "TBS download finished: $code")
            }

            override fun onInstallFinish(code: Int) {
                lastInstallCode = code
                Log.i(TAG, "TBS install finished: $code")
            }

            override fun onDownloadProgress(progress: Int) {
                lastDownloadProgress = progress
            }
        })
        QbSdk.initX5Environment(appContext, object : QbSdk.PreInitCallback {
            override fun onCoreInitFinished() {
                Log.i(TAG, "TBS core init finished")
            }

            override fun onViewInitFinished(success: Boolean) {
                lastInitFinished = success
                Log.i(TAG, "TBS view init finished: $success")
            }
        })
    }

    fun applyKernelSwitch(context: Context, enabled: Boolean) {
        if (enabled) {
            QbSdk.unForceSysWebView()
        } else {
            QbSdk.forceSysWebView()
        }
        BrowserPreferencesRepository(context.applicationContext).setX5Enabled(enabled)
        Log.i(TAG, "X5 enabled switch = $enabled")
    }

    fun refresh(context: Context) {
        initialized = false
        initialize(context)
    }

    fun reset(context: Context) {
        QbSdk.reset(context.applicationContext)
    }

    fun clearAllCache(context: Context) {
        QbSdk.clearAllWebViewCache(context.applicationContext, true)
    }

    fun clearDebugArtifacts(context: Context) {
        val appContext = context.applicationContext
        QbSdk.clear(appContext)
        QbSdk.clearAllWebViewCache(appContext, true)
        lastDownloadProgress = -1
        lastDownloadCode = null
        lastInstallCode = null
        lastInitFinished = null
        Log.i(TAG, "Cleared X5 debug artifacts")
    }

    fun clearCrashDisableMark(context: Context) {
        val appContext = context.applicationContext

        runCatching {
            val pvConfig = TbsPVConfig.getInstance(appContext)
            pvConfig.putData(KEY_DISABLED_CORE_VERSION, "0")
            pvConfig.commit()
            TbsPVConfig.releaseInstance()
        }.onFailure {
            Log.w(TAG, "Failed to clear TBS disabled_core_version", it)
        }

        runCatching {
            appContext.getSharedPreferences(PREF_PRELOAD_X5_CHECK, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_PRECHECK_DISABLE_VERSION)
                .remove(KEY_PRELOAD_X5_COUNTER)
                .remove(KEY_PRELOAD_X5_RECORDER)
                .remove(KEY_PRELOAD_X5_VERSION)
                .commit()
        }.onFailure {
            Log.w(TAG, "Failed to clear preload crash-disable markers", it)
        }

        Log.i(TAG, "Cleared X5 crash-disable markers")
    }

    fun clearLocalInstallMark(context: Context) {
        val appContext = context.applicationContext

        runCatching {
            QbSdk.setTBSInstallingStatus(false)
        }.onFailure {
            Log.w(TAG, "Failed to reset in-memory TBS installing flag", it)
        }

        runCatching {
            TbsDownloadConfig.getInstance(appContext).setInstallInterruptCode(-1)
        }.onFailure {
            Log.w(TAG, "Failed to reset install interrupt code", it)
        }

        runCatching {
            TbsDownloadConfig.getInstance(appContext).mPreferences.edit()
                .remove(KEY_INSTALL_INTERRUPT_CODE)
                .commit()
        }.onFailure {
            Log.w(TAG, "Failed to clear persisted install interrupt code", it)
        }

        Log.i(TAG, "Cleared X5 local install markers")
    }

    fun isNetLogEnabled(): Boolean = netLogEnabled

    fun setNetLogEnabled(enabled: Boolean) {
        if (enabled == netLogEnabled) {
            return
        }
        if (enabled) {
            QbSdk.openNetLog("kiyori_x5")
        } else {
            runCatching {
                QbSdk.closeNetLogAndSavaToLocal()
            }.onFailure {
                Log.w(TAG, "Failed to close X5 net log", it)
            }
        }
        netLogEnabled = enabled
        Log.i(TAG, "X5 net log enabled = $enabled")
    }

    fun exportNetLog(): String {
        val exportedPath = runCatching {
            QbSdk.closeNetLogAndSavaToLocal().orEmpty()
        }.onFailure {
            Log.w(TAG, "Failed to export X5 net log", it)
        }.getOrDefault("")
        if (netLogEnabled) {
            runCatching {
                QbSdk.openNetLog("kiyori_x5")
            }.onFailure {
                Log.w(TAG, "Failed to resume X5 net log", it)
            }
        }
        return exportedPath
    }

    fun uploadNetLog() {
        QbSdk.uploadNetLog("kiyori_x5")
        Log.i(TAG, "Requested X5 net log upload")
    }

    fun buildDiagnosticReport(context: Context): String {
        val state = getState(context)
        return buildString {
            appendLine("Kiyori X5 Diagnostic")
            appendLine("sdkVersion=${state.sdkVersion}")
            appendLine("coreVersion=${state.coreVersion}")
            appendLine("crashCoreVersion=${state.crashCoreVersion}")
            appendLine("tmpCoreVersion=${state.tmpCoreVersion}")
            appendLine("disabledCoreVersion=${state.disabledCoreVersion}")
            appendLine("preloadDisableVersion=${state.preloadDisableVersion}")
            appendLine("canLoadX5=${state.canLoadX5}")
            appendLine("isCoreInited=${state.isCoreInited}")
            appendLine("isEnabled=${state.isEnabled}")
            appendLine("isSysWebViewForced=${state.isSysWebViewForced}")
            appendLine("isInitEnvironmentStarted=${state.isInitEnvironmentStarted}")
            appendLine("isInstalling=${state.isInstalling}")
            appendLine("installInterruptCode=${state.installInterruptCode}")
            appendLine("netLogEnabled=${isNetLogEnabled()}")
            appendLine("lastDownloadProgress=${state.lastDownloadProgress}")
            appendLine("lastDownloadCode=${state.lastDownloadCode}")
            appendLine("lastInstallCode=${state.lastInstallCode}")
            appendLine("lastInitFinished=${state.lastInitFinished}")
            appendLine("currentProcessName=${state.currentProcessName}")
            appendLine("tbsFolderPath=${state.tbsFolderPath}")
        }.trimEnd()
    }

    fun getState(context: Context): BrowserX5KernelState {
        val appContext = context.applicationContext
        val preferencesRepository = BrowserPreferencesRepository(appContext)
        val downloadConfig = TbsDownloadConfig.getInstance(appContext)
        val preloadCheckPreferences =
            appContext.getSharedPreferences(PREF_PRELOAD_X5_CHECK, Context.MODE_PRIVATE)
        val disabledCoreVersion = runCatching {
            TbsPVConfig.getInstance(appContext).getDisabledCoreVersion()
        }.getOrDefault(0)
        val preloadDisableVersion = preloadCheckPreferences.getInt(KEY_PRECHECK_DISABLE_VERSION, -1)
        val installInterruptCode = downloadConfig.mPreferences.getInt(KEY_INSTALL_INTERRUPT_CODE, -1)

        return BrowserX5KernelState(
            sdkVersion = QbSdk.getTbsSdkVersion(),
            coreVersion = QbSdk.getTbsVersion(appContext),
            crashCoreVersion = QbSdk.getTbsVersionForCrash(appContext),
            tmpCoreVersion = QbSdk.getTmpDirTbsVersion(appContext),
            disabledCoreVersion = disabledCoreVersion,
            preloadDisableVersion = preloadDisableVersion,
            canLoadX5 = QbSdk.canLoadX5(appContext),
            isCoreInited = QbSdk.isTbsCoreInited(),
            isEnabled = preferencesRepository.isX5Enabled(),
            isSysWebViewForced = QbSdk.getIsSysWebViewForcedByOuter(),
            isInitEnvironmentStarted = QbSdk.getIsInitX5Environment(),
            isInstalling = QbSdk.getTBSInstalling(),
            installInterruptCode = installInterruptCode,
            currentProcessName = QbSdk.getCurrentProcessName(appContext).orEmpty(),
            tbsFolderPath = QbSdk.getTbsFolderDir(appContext)?.absolutePath.orEmpty(),
            lastDownloadProgress = lastDownloadProgress,
            lastDownloadCode = lastDownloadCode,
            lastInstallCode = lastInstallCode,
            lastInitFinished = lastInitFinished
        )
    }

    private const val PREF_PRELOAD_X5_CHECK = "tbs_preloadx5_check_cfg_file"
    private const val KEY_DISABLED_CORE_VERSION = "disabled_core_version"
    private const val KEY_PRECHECK_DISABLE_VERSION = "tbs_precheck_disable_version"
    private const val KEY_PRELOAD_X5_COUNTER = "tbs_preload_x5_counter"
    private const val KEY_PRELOAD_X5_RECORDER = "tbs_preload_x5_recorder"
    private const val KEY_PRELOAD_X5_VERSION = "tbs_preload_x5_version"
    private const val KEY_INSTALL_INTERRUPT_CODE = "tbs_install_interrupt_code"
}
