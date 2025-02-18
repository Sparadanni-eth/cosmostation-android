package wannabit.io.cosmostaion.ui.main

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.webkit.WebView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.google.common.collect.Lists
import com.google.firebase.FirebaseApp
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.android.relay.ConnectionType
import com.reown.sign.client.Sign
import com.reown.sign.client.SignClient
import net.sqlcipher.database.SQLiteDatabase
import wannabit.io.cosmostaion.BuildConfig
import wannabit.io.cosmostaion.R
import wannabit.io.cosmostaion.data.repository.wallet.WalletRepositoryImpl
import wannabit.io.cosmostaion.data.viewmodel.ApplicationViewModel
import wannabit.io.cosmostaion.data.viewmodel.ApplicationViewModelProviderFactory
import wannabit.io.cosmostaion.database.AppDatabase
import wannabit.io.cosmostaion.database.CipherHelper
import wannabit.io.cosmostaion.database.Prefs
import java.util.UUID

class CosmostationApp : Application(), ViewModelStoreOwner {
    companion object {
        lateinit var instance: CosmostationApp
            private set
    }

    init {
        instance = this
    }

    private val mViewModelStore = ViewModelStore()
    lateinit var applicationViewModel: ApplicationViewModel
    var appStatus: AppStatus? = null

    override val viewModelStore: ViewModelStore
        get() = mViewModelStore

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(ApplicationLifecycleStatus())
        val walletRepository = WalletRepositoryImpl()
        applicationViewModel = ViewModelProvider(
            this, ApplicationViewModelProviderFactory(Application(), walletRepository)
        )[ApplicationViewModel::class.java]

        initialize()
        FirebaseApp.initializeApp(this)
        SQLiteDatabase.loadLibs(this)
        setRandomBackgroundImage()

        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
        initWalletConnectV2()
    }

    fun setRandomBackgroundImage() {
        val imageArray = intArrayOf(
            R.drawable.bg_00,
            R.drawable.bg_01,
            R.drawable.bg_02,
            R.drawable.bg_03,
            R.drawable.bg_04,
            R.drawable.bg_05
        )

        val randomIndex = (Math.random() * imageArray.size).toInt()
        Prefs.background = imageArray[randomIndex]
    }

    private fun initWalletConnectV2() {
        val projectId = BuildConfig.WALLETCONNECT_API_KEY
        val relayUrl = "relay.walletconnect.com"
        val serverUrl = "wss://$relayUrl?projectId=$projectId"
        val connectionType = ConnectionType.AUTOMATIC
        val metaData = Core.Model.AppMetaData(
            getString(R.string.str_wc_peer_name),
            getString(R.string.str_wc_peer_url),
            getString(R.string.str_wc_peer_desc),
            Lists.newArrayList<String>(),
            "cosmostation://wc"
        )
        CoreClient.initialize(metaData, serverUrl, connectionType, this, null, null, null) { }
        SignClient.initialize(Sign.Params.Init(CoreClient)) { }
    }

    private fun initialize() {
        if (Prefs.passphrase.isEmpty()) {
            Prefs.passphrase = CipherHelper.encrypt(UUID.randomUUID().toString())
        }
    }

    fun isReturnedForeground(): Boolean {
        return appStatus?.ordinal == AppStatus.RETURNED_TO_FOREGROUND.ordinal
    }

    fun needShowLockScreen(): Boolean {
        if (!isReturnedForeground() || AppDatabase.getInstance().passwordDao().selectAll()
                .isEmpty() || !Prefs.appLock || AppDatabase.getInstance().baseAccountDao()
                .selectAll().isEmpty()
        ) return false
        return true
    }

    class ApplicationLifecycleStatus : ActivityLifecycleCallbacks {
        private var running = 0

        override fun onActivityStarted(activity: Activity) {
            if (++running == 1) {
                instance.appStatus = AppStatus.RETURNED_TO_FOREGROUND
            } else if (running > 1) {
                instance.appStatus = AppStatus.FOREGROUND
            }
        }

        override fun onActivityStopped(activity: Activity) {
            if (--running == 0) {
                instance.appStatus = AppStatus.BACKGROUND
            }
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

        override fun onActivityResumed(activity: Activity) {}

        override fun onActivityPaused(activity: Activity) {}

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {}
    }

    enum class AppStatus {
        BACKGROUND, RETURNED_TO_FOREGROUND, FOREGROUND
    }
}