package wannabit.io.cosmostaion.ui.main.setting

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import wannabit.io.cosmostaion.R
import wannabit.io.cosmostaion.chain.BaseChain
import wannabit.io.cosmostaion.chain.CosmosEndPointType
import wannabit.io.cosmostaion.chain.testnetClass.ChainGnoTestnet
import wannabit.io.cosmostaion.common.dpToPx
import wannabit.io.cosmostaion.common.makeToast
import wannabit.io.cosmostaion.data.viewmodel.ApplicationViewModel
import wannabit.io.cosmostaion.database.Prefs
import wannabit.io.cosmostaion.databinding.FragmentCommonBottomBinding
import wannabit.io.cosmostaion.databinding.ItemSegmentedFeeBinding
import wannabit.io.cosmostaion.ui.main.SettingType


class SettingBottomFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentCommonBottomBinding? = null
    private val binding get() = _binding!!

    private var fromChain: BaseChain? = null
    private lateinit var settingType: SettingType

    private lateinit var settingAdapter: SettingBottomAdapter

    companion object {
        @JvmStatic
        fun newInstance(
            fromChain: BaseChain?, settingType: SettingType
        ): SettingBottomFragment {
            val args = Bundle().apply {
                putParcelable("fromChain", fromChain)
                putSerializable("settingType", settingType)
            }
            val fragment = SettingBottomFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommonBottomBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
        initRecyclerView()
        setUpClickAction()
        setUpUpdateData()
    }

    private fun initView() {
        binding.apply {
            arguments?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    getParcelable("fromChain", BaseChain::class.java)?.let {
                        fromChain = it
                    }
                    getSerializable(
                        "settingType", SettingType::class.java
                    )?.let { settingType = it }

                } else {
                    (getParcelable("fromChain") as? BaseChain)?.let {
                        fromChain = it
                    }
                    (getSerializable("settingType") as? SettingType)?.let {
                        settingType = it
                    }
                }
            }
        }
    }

    private fun initRecyclerView() {
        binding.apply {
            when (settingType) {
                SettingType.LANGUAGE -> {
                    selectTitle.text = getString(R.string.str_select_language)
                    val languageList = listOf(
                        getString(R.string.str_system),
                        getString(R.string.title_language_en),
                        getString(R.string.title_language_kr),
                        getString(R.string.title_language_ja)
                    )

                    settingAdapter = SettingBottomAdapter(
                        null, null, mutableListOf(), SettingType.LANGUAGE, null
                    )
                    recycler.setHasFixedSize(true)
                    recycler.layoutManager = LinearLayoutManager(requireActivity())
                    recycler.adapter = settingAdapter
                    settingAdapter.submitList(languageList)

                    settingAdapter.setOnItemClickListener {
                        Prefs.language = it
                        Prefs.foreToBack = false
                        dismiss()
                        requireActivity().recreate()
                    }
                }

                SettingType.CURRENCY -> {
                    selectTitle.text = getString(R.string.str_select_currency)
                    val currencyList = resources.getStringArray(R.array.currency_unit_array)

                    settingAdapter = SettingBottomAdapter(
                        null, null, mutableListOf(), SettingType.CURRENCY, null
                    )
                    recycler.setHasFixedSize(true)
                    recycler.layoutManager = LinearLayoutManager(requireActivity())
                    recycler.adapter = settingAdapter
                    settingAdapter.submitList(currencyList.toList())

                    settingAdapter.setOnItemClickListener {
                        if (Prefs.currency != it) {
                            Prefs.currency = it
                        }
                        val bundle = Bundle()
                        bundle.putInt("currency", it)
                        parentFragmentManager.setFragmentResult("currency", bundle)
                        dismiss()
                    }
                }

                SettingType.PRICE_STATUS -> {
                    selectTitle.text = getString(R.string.title_price_change_color)

                    settingAdapter = SettingBottomAdapter(
                        null, null, mutableListOf(), SettingType.PRICE_STATUS, null
                    )
                    recycler.setHasFixedSize(true)
                    recycler.layoutManager = LinearLayoutManager(requireActivity())
                    recycler.adapter = settingAdapter
                    settingAdapter.submitList(listOf("Style1", "Style2"))

                    settingAdapter.setOnItemClickListener {
                        val bundle = Bundle()
                        bundle.putInt("priceStyle", it)
                        parentFragmentManager.setFragmentResult("priceStyle", bundle)
                        dismiss()
                    }
                }

                SettingType.BUY_CRYPTO -> {
                    selectTitle.text = getString(R.string.title_buy_crypto)
                    val buyCryptoList = listOf("MOONPAY", "KADO", "BINANCE")

                    settingAdapter = SettingBottomAdapter(
                        null, null, mutableListOf(), SettingType.BUY_CRYPTO, null
                    )
                    recycler.setHasFixedSize(true)
                    recycler.layoutManager = LinearLayoutManager(requireActivity())
                    recycler.adapter = settingAdapter
                    settingAdapter.submitList(buyCryptoList)

                    settingAdapter.setOnItemClickListener {
                        val bundle = Bundle()
                        bundle.putInt("crypto", it)
                        parentFragmentManager.setFragmentResult("crypto", bundle)
                        dismiss()
                    }
                }

                SettingType.END_POINT_SUI -> {
                    val rpcEndpoints: MutableList<Any> = ArrayList()
                    if (fromChain is ChainGnoTestnet) {
                        selectTitle.text = getString(R.string.title_select_end_point_grpc)
                        fromChain?.getChainListParam()?.getAsJsonArray("cosmos_rpc_endpoint")?.let {
                            for (jsonElement in it) {
                                rpcEndpoints.add(jsonElement.asJsonObject)
                            }
                        }
                        settingAdapter = SettingBottomAdapter(
                            fromChain,
                            rpcEndpoints,
                            mutableListOf(),
                            SettingType.END_POINT_SUI,
                            endpointClickAction
                        )

                    } else {
                        selectTitle.text = getString(R.string.title_select_end_point_sui_rpc)
                        fromChain?.getChainListParam()?.getAsJsonArray("rpc_endpoint")?.let {
                            for (jsonElement in it) {
                                rpcEndpoints.add(jsonElement.asJsonObject)
                            }
                        }
                        settingAdapter = SettingBottomAdapter(
                            fromChain,
                            rpcEndpoints,
                            mutableListOf(),
                            SettingType.END_POINT_SUI,
                            endpointClickAction
                        )
                    }
                    recycler.setHasFixedSize(true)
                    recycler.layoutManager = LinearLayoutManager(requireActivity())
                    recycler.adapter = settingAdapter
                    settingAdapter.submitList(rpcEndpoints)
                }

                SettingType.END_POINT_EVM -> {
                    selectTitle.text = getString(R.string.title_select_end_point_rpc)
                    val rpcEndpoints: MutableList<Any> = ArrayList()
                    fromChain?.getChainListParam()?.getAsJsonArray("evm_rpc_endpoint")?.let {
                        for (jsonElement in it) {
                            rpcEndpoints.add(jsonElement.asJsonObject)
                        }
                    }

                    settingAdapter = SettingBottomAdapter(
                        fromChain,
                        rpcEndpoints,
                        mutableListOf(),
                        SettingType.END_POINT_EVM,
                        endpointClickAction
                    )
                    recycler.setHasFixedSize(true)
                    recycler.layoutManager = LinearLayoutManager(requireActivity())
                    recycler.adapter = settingAdapter
                    settingAdapter.submitList(rpcEndpoints)
                }

                SettingType.END_POINT_COSMOS -> {
                    if (fromChain?.isEvmCosmos() == true) {
                        selectTitle.text = getString(R.string.title_select_end_point)
                        segmentView.visibility = View.VISIBLE
                        view.visibility = View.GONE

                        val recyclerViewLayoutParams =
                            recycler.layoutParams as RelativeLayout.LayoutParams
                        recyclerViewLayoutParams.addRule(RelativeLayout.BELOW, R.id.segment_view)
                        recycler.layoutParams = recyclerViewLayoutParams
                        initSegmentView()

                    } else {
                        selectTitle.text = getString(R.string.title_select_end_point_grpc)
                        segmentView.visibility = View.GONE
                        view.visibility = View.VISIBLE
                    }

                    val grpcEndpoints: MutableList<Any> = ArrayList()
                    fromChain?.getChainListParam()?.getAsJsonArray("grpc_endpoint")?.let {
                        for (jsonElement in it) {
                            grpcEndpoints.add(jsonElement.asJsonObject)
                        }
                    }

                    val lcdEndpoints: MutableList<Any> = ArrayList()
                    fromChain?.getChainListParam()?.getAsJsonArray("lcd_endpoint")?.let {
                        for (jsonElement in it) {
                            lcdEndpoints.add(jsonElement.asJsonObject)
                        }
                    }

                    settingAdapter = SettingBottomAdapter(
                        fromChain,
                        grpcEndpoints,
                        lcdEndpoints,
                        SettingType.END_POINT_COSMOS,
                        endpointClickAction
                    )
                    recycler.setHasFixedSize(true)
                    recycler.layoutManager = LinearLayoutManager(requireActivity())
                    recycler.adapter = settingAdapter
                    settingAdapter.submitList(grpcEndpoints + lcdEndpoints)
                }
            }
        }
    }

    private val endpointClickAction = object : SettingBottomAdapter.EndpointListener {
        override fun select(endpoint: String, gapTime: Double?) {
            if (gapTime != null) {
                Prefs.setGrpcEndpoint(fromChain, endpoint)
                Prefs.setEndpointType(fromChain, CosmosEndPointType.USE_GRPC)
                dismiss()

                val bundle = Bundle()
                bundle.putString("endpoint", endpoint)
                parentFragmentManager.setFragmentResult("endpoint", bundle)
                dismiss()

            } else {
                requireActivity().makeToast(R.string.error_useless_end_point)
                return
            }
        }

        override fun rpcSelect(endpoint: String, gapTime: Double?) {
            if (gapTime != null) {
                Prefs.setEvmRpcEndpoint(fromChain, endpoint)
                dismiss()

                val bundle = Bundle()
                bundle.putString("endpoint", endpoint)
                parentFragmentManager.setFragmentResult("endpoint", bundle)
                dismiss()

            } else {
                requireActivity().makeToast(R.string.error_useless_end_point)
                return
            }
        }

        override fun lcdSelect(endpoint: String, gapTime: Double?) {
            if (gapTime != null) {
                Prefs.setLcdEndpoint(fromChain, endpoint)
                Prefs.setEndpointType(fromChain, CosmosEndPointType.USE_LCD)
                dismiss()

                val bundle = Bundle()
                bundle.putString("endpoint", endpoint)
                parentFragmentManager.setFragmentResult("endpoint", bundle)
                dismiss()

            } else {
                requireActivity().makeToast(R.string.error_useless_end_point)
                return
            }
        }
    }

    private fun initSegmentView() {
        binding.apply {
            segmentView.setBackgroundResource(R.drawable.cell_search_bg)
            rpcSegment.apply {
                setSelectedBackground(
                    ContextCompat.getColor(
                        requireActivity(), R.color.color_accent_purple
                    )
                )
                setRipple(
                    ContextCompat.getColor(
                        requireActivity(), R.color.color_accent_purple
                    )
                )
            }

            for (i in 0 until 2) {
                val segmentView = ItemSegmentedFeeBinding.inflate(layoutInflater)
                rpcSegment.addView(
                    segmentView.root,
                    i,
                    LinearLayout.LayoutParams(0, dpToPx(requireActivity(), 32), 1f)
                )

                when (i) {
                    0 -> {
                        segmentView.btnTitle.text = "Cosmos Endpoint"
                    }

                    else -> {
                        segmentView.btnTitle.text = "EVM RPC Endpoint"
                    }
                }
            }
        }
    }

    private fun setUpClickAction() {
        binding.apply {
            rpcSegment.setOnPositionChangedListener { position ->
                when (position) {
                    0 -> {
                        val grpcEndpoints: MutableList<Any> = ArrayList()
                        fromChain?.getChainListParam()?.getAsJsonArray("grpc_endpoint")?.let {
                            for (jsonElement in it) {
                                grpcEndpoints.add(jsonElement.asJsonObject)
                            }
                        }
                        val lcdEndpoints: MutableList<Any> = ArrayList()
                        fromChain?.getChainListParam()?.getAsJsonArray("lcd_endpoint")?.let {
                            for (jsonElement in it) {
                                lcdEndpoints.add(jsonElement.asJsonObject)
                            }
                        }

                        settingAdapter = SettingBottomAdapter(
                            fromChain,
                            grpcEndpoints,
                            lcdEndpoints,
                            SettingType.END_POINT_COSMOS,
                            endpointClickAction
                        )
                        if (isAdded) {
                            recycler.setHasFixedSize(true)
                            recycler.layoutManager = LinearLayoutManager(requireActivity())
                            recycler.adapter = settingAdapter
                            settingAdapter.submitList(grpcEndpoints + lcdEndpoints)
                        }
                    }

                    else -> {
                        val rpcEndpoints: MutableList<Any> = ArrayList()
                        fromChain?.getChainListParam()?.getAsJsonArray("evm_rpc_endpoint")?.let {
                            for (jsonElement in it) {
                                rpcEndpoints.add(jsonElement.asJsonObject)
                            }
                        }

                        settingAdapter = SettingBottomAdapter(
                            fromChain,
                            rpcEndpoints,
                            mutableListOf(),
                            SettingType.END_POINT_EVM,
                            endpointClickAction
                        )
                        if (isAdded) {
                            recycler.setHasFixedSize(true)
                            recycler.layoutManager = LinearLayoutManager(requireActivity())
                            recycler.adapter = settingAdapter
                            settingAdapter.submitList(rpcEndpoints)
                        }
                    }
                }
            }
        }
    }

    private fun setUpUpdateData() {
        ApplicationViewModel.shared.updateParamResult.observe(viewLifecycleOwner) {
            if (settingType == SettingType.END_POINT_SUI || settingType == SettingType.END_POINT_EVM || settingType == SettingType.END_POINT_COSMOS) {
                initView()
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}