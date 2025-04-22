package wannabit.io.cosmostaion.ui.main

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import wannabit.io.cosmostaion.R
import wannabit.io.cosmostaion.chain.FetchState
import wannabit.io.cosmostaion.common.BaseActivity
import wannabit.io.cosmostaion.common.BaseData
import wannabit.io.cosmostaion.common.CosmostationConstants
import wannabit.io.cosmostaion.common.goneOrVisible
import wannabit.io.cosmostaion.common.makeToast
import wannabit.io.cosmostaion.data.repository.wallet.WalletRepositoryImpl
import wannabit.io.cosmostaion.data.viewmodel.ApplicationViewModel
import wannabit.io.cosmostaion.data.viewmodel.intro.WalletViewModel
import wannabit.io.cosmostaion.data.viewmodel.intro.WalletViewModelProviderFactory
import wannabit.io.cosmostaion.database.Prefs
import wannabit.io.cosmostaion.databinding.ActivityMainBinding
import wannabit.io.cosmostaion.databinding.ViewTabItemBinding
import wannabit.io.cosmostaion.ui.init.IntroActivity

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var walletViewModel: WalletViewModel

    private var isClickable = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViewModel()
        initView()
        setupViewModels()
        setUpClickAction()
        showPushData()
        setUpBg()
    }

    @SuppressLint("WrongConstant")
    private fun showPushData() {
        intent.apply {
            if (getStringExtra("push_txhash")?.isEmpty() == true) {
                return
            }
            if (getStringExtra("push_type").toString() == "0") {
                getStringExtra("push_txhash")?.let { txHash ->
                    getStringExtra("push_network")?.let { network ->
                        val url = CosmostationConstants.EXPLORER_BASE_TX_URL.replace(
                            "{apiName}", network
                        ).replace("{hash}", txHash)

                        Handler(Looper.getMainLooper()).postDelayed({
                            Intent(this@MainActivity, PushNotificationActivity::class.java).apply {
                                putExtra("url", url)
                                startActivity(this)
                                if (Build.VERSION.SDK_INT >= 34) {
                                    overrideActivityTransition(
                                        Activity.OVERRIDE_TRANSITION_OPEN,
                                        R.anim.anim_slide_in_bottom,
                                        R.anim.anim_fade_out
                                    )
                                } else {
                                    overridePendingTransition(
                                        R.anim.anim_slide_in_bottom, R.anim.anim_fade_out
                                    )
                                }
                            }
                        }, 1000)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (BaseData.baseAccount == null) {
            Intent(this, IntroActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(this)
            }
        }
        recreateView()
    }

    private fun initViewModel() {
        val walletRepository = WalletRepositoryImpl()
        val walletViewModelProviderFactory = WalletViewModelProviderFactory(walletRepository)
        walletViewModel =
            ViewModelProvider(this, walletViewModelProviderFactory)[WalletViewModel::class.java]
    }

    private fun setupViewModels() {
        ApplicationViewModel.shared.currentAccountResult.observe(this) {
            binding.accountName.text = BaseData.baseAccount?.name
            CosmostationApp.instance.setRandomBackgroundImage()
            binding.parentLayout.setBackgroundResource(Prefs.background)
        }

        ApplicationViewModel.shared.changeNameResult.observe(this) { account ->
            if (BaseData.baseAccount?.id == account?.id) {
                binding.accountName.text = account?.name
            }
        }

        ApplicationViewModel.shared.themeOptionResult.observe(this) {
            CosmostationApp.instance.setRandomBackgroundImage()
            binding.parentLayout.setBackgroundResource(Prefs.background)
        }
    }

    private fun initView() {
        binding.apply {
            parentLayout.setBackgroundResource(Prefs.background)
            btnSort.setImageResource(
                if (Prefs.chainFilter) R.drawable.icon_name_sort else R.drawable.icon_value_sort
            )

            accountName.text = BaseData.baseAccount?.name
            val mainViewPagerAdapter = MainViewPageAdapter(this@MainActivity)
            mainViewPager.adapter = mainViewPagerAdapter
            mainViewPager.setCurrentItem(intent.getIntExtra("page", 0), false)
            mainViewPager.offscreenPageLimit = 2
            mainViewPager.isUserInputEnabled = false

            val tabLayoutMediator = TabLayoutMediator(tabLayout, mainViewPager) { tab, position ->
                val tabBinding =
                    ViewTabItemBinding.inflate(LayoutInflater.from(this@MainActivity), null, false)
                val tabView = tabBinding.root
                tabBinding.tabText.visibility = View.GONE

                when (position) {
                    0 -> {
                        tabBinding.tabIcon.setImageResource(R.drawable.icon_wallet)
                        tabBinding.tabText.text = getString(R.string.str_chains)
                    }

                    1 -> {
                        tabBinding.tabIcon.setImageResource(R.drawable.icon_service)
                        tabBinding.tabText.text = getString(R.string.str_service)
                    }

                    2 -> {
                        tabBinding.tabIcon.setImageResource(R.drawable.icon_setting)
                        tabBinding.tabText.text = getString(R.string.str_setting)
                    }
                }
                tab.customView = tabView
            }
            tabLayoutMediator.attach()

            for (i in 0 until tabLayout.tabCount) {
                val tab = tabLayout.getTabAt(i)
                if (i == 0) {
                    tabIconSetColor(tab, true)
                } else {
                    tabIconSetColor(tab, false)
                }
            }

            tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    val position = tab?.position ?: 0
                    mainViewPager.setCurrentItem(position, false)
                    tabIconSetColor(tab, true)

                    btnEdit.goneOrVisible(position == 1 || position == 2)
                    btnSort.goneOrVisible(position == 1 || position == 2)
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {
                    tabIconSetColor(tab, false)
                }

                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })
        }
    }

    private fun tabIconSetColor(tab: TabLayout.Tab?, isSelected: Boolean) {
        tab?.let {
            tab.customView?.let { ViewTabItemBinding.bind(it) }?.apply {
                if (isSelected) {
                    tabIcon.colorFilter = PorterDuffColorFilter(
                        ContextCompat.getColor(
                            this@MainActivity, R.color.color_base01
                        ), PorterDuff.Mode.SRC_IN
                    )
                    tabText.setTextColor(
                        ContextCompat.getColorStateList(
                            this@MainActivity, R.color.color_base01
                        )
                    )
                } else {
                    tabIcon.colorFilter = PorterDuffColorFilter(
                        ContextCompat.getColor(
                            this@MainActivity, R.color.color_base03
                        ), PorterDuff.Mode.SRC_IN
                    )
                    tabText.setTextColor(
                        ContextCompat.getColorStateList(
                            this@MainActivity, R.color.color_base03
                        )
                    )
                }
            }
        }
    }

    private fun setUpClickAction() {
        binding.apply {
            btnEdit.setOnClickListener {
                handleOneClickWithDelay(
                    ChainEditFragment()
                )
            }

            accountImg.setOnClickListener {
                BaseData.baseAccount?.let { account ->
                    if (account.sortedDisplayChains().none { it.fetchState == FetchState.BUSY }) {
                        handleOneClickWithDelay(
                            AccountSelectFragment()
                        )
                    } else {
                        makeToast(R.string.str_data_synchronizing)
                        return@setOnClickListener
                    }
                }
            }

            accountName.setOnClickListener {
                BaseData.baseAccount?.let { account ->
                    if (account.sortedDisplayChains().none { it.fetchState == FetchState.BUSY }) {
                        handleOneClickWithDelay(
                            AccountSelectFragment()
                        )
                    } else {
                        makeToast(R.string.str_data_synchronizing)
                        return@setOnClickListener
                    }
                }
            }

            btnRandom.setOnClickListener {
                CosmostationApp.instance.setRandomBackgroundImage()
                binding.parentLayout.setBackgroundResource(Prefs.background)
            }

            btnSort.setOnClickListener {
                BaseData.baseAccount?.let { account ->
                    if (account.sortedDisplayChains().none { it.fetchState == FetchState.BUSY }) {
                        Prefs.chainFilter = !Prefs.chainFilter
                        ApplicationViewModel.shared.chainFilter(Prefs.chainFilter)
                        btnSort.setImageResource(
                            if (Prefs.chainFilter) R.drawable.icon_name_sort else R.drawable.icon_value_sort
                        )
                    } else {
                        makeToast(R.string.str_data_synchronizing)
                        return@setOnClickListener
                    }
                }
            }
        }
    }

    private fun setUpBg() {
        ApplicationViewModel.shared.changeBgResult.observe(this) {
            binding.parentLayout.setBackgroundResource(Prefs.background)
        }
    }

    private fun handleOneClickWithDelay(bottomSheetDialogFragment: BottomSheetDialogFragment) {
        if (isClickable) {
            isClickable = false

            bottomSheetDialogFragment.show(
                supportFragmentManager, bottomSheetDialogFragment::class.java.name
            )

            Handler(Looper.getMainLooper()).postDelayed({
                isClickable = true
            }, 300)
        }
    }

    private fun recreateView() {
        ApplicationViewModel.shared.txRecreateResult.observe(this) {
            BaseData.baseAccount?.sortedDisplayChains()?.forEach {
                it.fetchState = FetchState.IDLE
            }

            val mainViewPagerAdapter = MainViewPageAdapter(this)
            binding.apply {
                mainViewPager.adapter = mainViewPagerAdapter
                mainViewPager.setCurrentItem(it, false)
                mainViewPager.offscreenPageLimit = 2
                mainViewPager.isUserInputEnabled = false
                mainViewPagerAdapter.notifyDataSetChanged()
            }
        }

        ApplicationViewModel.shared.chainFilterResult.observe(this) {
            binding.btnSort.setImageResource(
                if (Prefs.chainFilter) R.drawable.icon_name_sort else R.drawable.icon_value_sort
            )
        }
    }

    class MainViewPageAdapter(fragmentActivity: FragmentActivity) :
        FragmentStateAdapter(fragmentActivity) {
        private val mainFragments = mutableListOf(
            DashboardFragment.newInstance(BaseData.baseAccount),
            ServiceFragment(),
            SettingFragment()
        )

        override fun getItemCount(): Int {
            return mainFragments.size
        }

        override fun createFragment(position: Int): Fragment {
            return mainFragments[position]
        }
    }

    override fun onBackPressed() {
        val fragmentManager = supportFragmentManager
        if (fragmentManager.backStackEntryCount <= 0) {
            moveTaskToBack(true)
            return
        }
        super.onBackPressed()
    }
}