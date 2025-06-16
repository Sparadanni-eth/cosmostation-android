package wannabit.io.cosmostaion.ui.main.dapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.cosmos.auth.v1beta1.QueryGrpc
import com.cosmos.auth.v1beta1.QueryProto
import com.cosmos.base.query.v1beta1.PaginationProto
import com.cosmos.base.v1beta1.CoinProto
import com.cosmos.tx.v1beta1.TxProto
import com.cosmos.tx.v1beta1.TxProto.Fee
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.grpc.ManagedChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.i2p.crypto.eddsa.Utils
import org.bouncycastle.util.encoders.Base64.toBase64String
import wannabit.io.cosmostaion.R
import wannabit.io.cosmostaion.chain.BaseChain
import wannabit.io.cosmostaion.common.BaseConstant
import wannabit.io.cosmostaion.common.BaseData
import wannabit.io.cosmostaion.common.BaseUtils
import wannabit.io.cosmostaion.common.dpToPx
import wannabit.io.cosmostaion.common.formatAmount
import wannabit.io.cosmostaion.common.formatAssetValue
import wannabit.io.cosmostaion.common.makeToast
import wannabit.io.cosmostaion.common.setTokenImg
import wannabit.io.cosmostaion.common.updateButtonView
import wannabit.io.cosmostaion.common.updateButtonViewEnabled
import wannabit.io.cosmostaion.data.model.res.FeeInfo
import wannabit.io.cosmostaion.databinding.FragmentWcSignBinding
import wannabit.io.cosmostaion.databinding.ItemDappSegmentedFeeBinding
import wannabit.io.cosmostaion.sign.Signer
import wannabit.io.cosmostaion.ui.tx.genTx.BaseTxFragment
import wannabit.io.cosmostaion.ui.tx.option.general.AssetFragment
import wannabit.io.cosmostaion.ui.tx.option.general.AssetSelectListener
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.TimeUnit

class PopUpCosmosSignFragment(
    private val allChains: MutableList<BaseChain>?,
    var selectedChain: BaseChain?,
    private val id: Long,
    private val data: String,
    private val method: String?,
    val listener: WcSignRawDataListener
) : BaseTxFragment() {

    private var _binding: FragmentWcSignBinding? = null
    private val binding get() = _binding!!

    private lateinit var updateJsonData: JsonObject
    private var txFee: Fee? = null
    private var dAppTxFee: Fee? = null

    private var feeInfos: MutableList<FeeInfo> = mutableListOf()

    private var updateData: String? = null

    private var selectFeePosition = 0

    private var isChanged = false
    private var isClickable = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWcSignBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewResource()
        parsingRequest()
        setUpClickAction()
    }

    private fun initViewResource() {
        binding.apply {
            signView.setBackgroundResource(R.drawable.cell_bg)
            feeView.setBackgroundResource(R.drawable.cell_bg)
            segmentView.setBackgroundResource(R.drawable.segment_fee_bg)
        }
    }

    private fun parsingRequest() {
        lifecycleScope.launch(Dispatchers.IO) {
            val txJsonObject = JsonParser.parseString(data).asJsonObject
            val txJsonSignDoc =
                txJsonObject.getAsJsonObject("signDoc") ?: txJsonObject.getAsJsonObject("doc")
            val chainId = if (txJsonSignDoc != null) {
                val chainIdJson = txJsonSignDoc.asJsonObject["chain_id"]
                    ?: txJsonSignDoc.asJsonObject["chainName"]
                chainIdJson.asString
            } else {
                txJsonObject["chainName"].asString ?: txJsonObject["chain_id"].asString
            }

            allChains?.filter { it.supportCosmos() }
                ?.firstOrNull { it.chainIdCosmos.lowercase() == chainId.lowercase() }
                ?.let { chain ->
                    selectedChain = chain
                }

            when (method) {
                "sign_amino" -> {
                    updateJsonData = updateFeeInfoInAminoMessage(txJsonObject)
                    updateJsonData["fee"].asJsonObject?.let { fee ->
                        val feeCoin = CoinProto.Coin.newBuilder()
                            .setDenom(fee["amount"].asJsonArray[0].asJsonObject["denom"].asString)
                            .setAmount(fee["amount"].asJsonArray[0].asJsonObject["amount"].asString)
                            .build()
                        dAppTxFee = Fee.newBuilder().setGasLimit(fee["gas"].asString.toLong())
                            .addAmount(feeCoin).build()
                    }
                }

                "sign_direct" -> {
                    updateJsonData = txJsonObject["doc"].asJsonObject
                    val authInfo =
                        TxProto.AuthInfo.parseFrom(Utils.hexToBytes(updateJsonData["auth_info_bytes"].asString))
                    if (authInfo.fee.gasLimit > 0 && authInfo.fee.amountList.isNotEmpty()) {
                        dAppTxFee = authInfo.fee
                    }
                }

                else -> {
                    updateJsonData = JsonParser.parseString(data).asJsonObject
                    withContext(Dispatchers.Main) {
                        binding.btnConfirm.isEnabled = true
                    }
                }
            }
            initData()
        }
    }

    private fun initData() {
        lifecycleScope.launch(Dispatchers.IO) {
            selectedChain?.let { chain ->
                if (method == "sign_direct") {
                    chain.cosmosFetcher()?.let { fetcher ->
                        try {
                            fetcher.getChannel()?.let { channel ->
                                val loadInputAuthDeferred =
                                    async { loadAuth(channel, chain.address) }
                                val loadInputBalanceDeferred =
                                    async { loadSpendAbleBalance(channel, chain.address) }

                                chain.cosmosFetcher?.cosmosAuth =
                                    loadInputAuthDeferred.await()?.account
                                chain.cosmosFetcher?.cosmosAvailable =
                                    loadInputBalanceDeferred.await().balancesList
                            }
                        } catch (e: Exception) {
                            if (isAdded) {
                                activity?.makeToast(R.string.str_unknown_error)
                            } else {
                                return@launch
                            }
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                binding.apply {
                    loading.visibility = View.GONE
                    viewLayout.visibility = View.VISIBLE
                    initView()
                }
            }
        }
    }

    private fun initView() {
        binding.apply {
            when (method) {
                "sign_message" -> {
                    dialogTitle.text = getString(R.string.str_permit_request)
                    feeView.visibility = View.INVISIBLE
                }

                else -> {
                    dialogTitle.text = getString(R.string.str_tx_request)
                    feeView.visibility = View.VISIBLE
                }
            }
            initFee()
        }
        isCancelable = false
    }

    private fun initFee() {
        binding.apply {
            val gasTitle: MutableList<String>
            selectedChain?.let { chain ->
                when (method) {
                    "sign_amino" -> {
                        gasTitle = mutableListOf(
                            getString(R.string.str_fixed)
                        )
                        val segmentView = ItemDappSegmentedFeeBinding.inflate(layoutInflater)
                        feeSegment.addView(
                            segmentView.root,
                            0,
                            LinearLayout.LayoutParams(0, dpToPx(requireContext(), 32), 1f)
                        )
                        segmentView.btnTitle.text = gasTitle[0]
                        selectFeePosition = 0
                        btnConfirm.isEnabled = true
                        txFee = dAppTxFee
                        updateSegmentView()
                    }

                    "sign_direct" -> {
                        feeInfos = chain.getFeeInfos(requireContext())
                        for (i in feeInfos.indices) {
                            val segmentView = ItemDappSegmentedFeeBinding.inflate(layoutInflater)
                            feeSegment.addView(
                                segmentView.root, i, LinearLayout.LayoutParams(
                                    0, dpToPx(requireContext(), 32), 1f
                                )
                            )
                            segmentView.btnTitle.text = feeInfos[i].title
                        }
                        selectFeePosition = chain.getFeeBasePosition()
                        feeSegment.setPosition(selectFeePosition, false)
                        txFee = chain.getInitPayableFee(requireContext())
                        if (dAppTxFee != null) {
                            btnFromDapp.visibility = View.VISIBLE
                            btnFromDapp.updateButtonViewEnabled(false)
                        }
                        updateSegmentView()
                        txSimulate()
                    }

                    else -> {
                        initSignMessageDataView(data)
                        feeView.visibility = View.INVISIBLE
                    }
                }
                updateFeeView()
            }
        }
    }

    private fun updateSegmentView() {
        binding.apply {
            if (selectFeePosition == -1) {
                feeSegment.setSelectedBackground(
                    ContextCompat.getColor(
                        requireContext(), R.color.color_transparent
                    )
                )
                feeSegment.setRipple(
                    ContextCompat.getColor(
                        requireContext(), R.color.color_transparent
                    )
                )
                feeSegment.setDivider(
                    ContextCompat.getColor(
                        requireContext(), R.color.color_base04
                    ), 4, 8, 2
                )

            } else {
                feeSegment.setSelectedBackground(
                    ContextCompat.getColor(
                        requireContext(), R.color.color_accent_purple
                    )
                )
                feeSegment.setRipple(
                    ContextCompat.getColor(
                        requireContext(), R.color.color_accent_purple
                    )
                )
                feeSegment.setDivider(
                    ContextCompat.getColor(
                        requireContext(), R.color.color_transparent
                    ), 0, 0, 0
                )
            }
        }
    }

    private fun updateFeeView() {
        binding.apply {
            selectedChain?.let { chain ->
                lifecycleScope.launch(Dispatchers.IO) {
                    txFee?.let { fee ->
                        BaseData.getAsset(chain.apiName, fee.getAmount(0).denom)?.let { asset ->
                            val dpFeeAmount = fee.getAmount(0).amount.toBigDecimal()
                                .movePointLeft(asset.decimals ?: 6)
                                .setScale(asset.decimals ?: 6, RoundingMode.DOWN)
                            val price = BaseData.getPrice(asset.coinGeckoId)
                            val value = price.multiply(dpFeeAmount)

                            withContext(Dispatchers.Main) {
                                if (method == "sign_message") {
                                    val message = updateJsonData["message"].asString
                                    signData.text =
                                        GsonBuilder().setPrettyPrinting().disableHtmlEscaping()
                                            .create().toJson(message).replace("\\n", "\n")
                                } else {
                                    signData.text = GsonBuilder().setPrettyPrinting().create()
                                        .toJson(updateJsonData)
                                    dappFeeAmount.text = formatAmount(
                                        dpFeeAmount.toPlainString(), asset.decimals ?: 6
                                    )
                                    dappFeeToken.text = asset.symbol
                                    dappFeeTokenImg.setTokenImg(asset)
                                    feeValue.text = formatAssetValue(value)
                                    isChanged = true
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun txSimulate() {
        binding.apply {
            btnConfirm.updateButtonView(false)
            btnFromDapp.isEnabled = false
            loading.visibility = View.VISIBLE
            if (selectedChain?.isSimulable() == false) {
                return
            }

            lifecycleScope.launch(Dispatchers.IO) {
                if (selectFeePosition >= 0) {
                    val txJsonObject = JsonParser.parseString(data).asJsonObject
                    updateJsonData = updateFeeInfoInDirectMessage(txJsonObject)
                    val authInfo =
                        TxProto.AuthInfo.parseFrom(Utils.hexToBytes(updateJsonData["auth_info_bytes"].asString))
                    txFee = authInfo.fee
                }

                withContext(Dispatchers.Main) {
                    updateFeeView()
                    btnConfirm.updateButtonView(true)
                    btnFromDapp.isEnabled = true
                    loading.visibility = View.GONE
                }
            }
        }
    }

    private fun initSignMessageDataView(data: String) {
        binding.apply {
            lifecycleScope.launch(Dispatchers.IO) {
                val txJsonObject = JsonParser.parseString(data).asJsonObject
                val message = txJsonObject["message"].asString
                withContext(Dispatchers.Main) {
                    feeView.visibility = View.INVISIBLE
                    signData.text = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
                        .toJson(message).replace("\\n", "\n")
                }
                updateData = toBase64String(message.toByteArray())
            }
        }
    }

    private fun setUpClickAction() {
        binding.apply {
            btnFromDapp.setOnClickListener {
                if (selectFeePosition == -1) {
                    return@setOnClickListener
                }
                val txJsonObject = JsonParser.parseString(data).asJsonObject
                updateJsonData = txJsonObject["doc"].asJsonObject
                selectFeePosition = -1
                btnFromDapp.updateButtonViewEnabled(true)
                txFee = dAppTxFee
                txSimulate()
                updateSegmentView()
            }

            feeTokenLayout.setOnClickListener {
                selectedChain?.let { chain ->
                    if (method == "sign_amino") {
                        return@setOnClickListener
                    }

                    if (selectFeePosition < 0) return@setOnClickListener
                    if (feeInfos.isNotEmpty()) {
                        handleOneClickWithDelay(
                            AssetFragment.newInstance(chain,
                                feeInfos[selectFeePosition].feeDatas.toMutableList(),
                                object : AssetSelectListener {
                                    override fun select(denom: String) {
                                        chain.getDefaultFeeCoins(requireContext())
                                            .firstOrNull { it.denom == denom }?.let { feeCoin ->
                                                val updateFeeCoin =
                                                    CoinProto.Coin.newBuilder().setDenom(denom)
                                                        .setAmount(feeCoin.amount).build()

                                                val updateTxFee = Fee.newBuilder()
                                                    .setGasLimit(BaseConstant.BASE_GAS_AMOUNT.toLong())
                                                    .addAmount(updateFeeCoin).build()

                                                txFee = updateTxFee
                                                txSimulate()
                                            }
                                    }
                                })
                        )
                    }
                }
            }

            feeSegment.setOnPositionChangedListener { position ->
                if (isChanged) {
                    if (selectFeePosition == position) {
                        return@setOnPositionChangedListener
                    }
                    selectFeePosition = position
                    btnFromDapp.updateButtonViewEnabled(false)
                    txSimulate()
                    updateSegmentView()
                }
            }

            btnCancel.setOnClickListener {
                if (!loading.isVisible) {
                    listener.cancel(id)
                    dismiss()
                }
            }

            btnConfirm.setOnClickListener {
                if (method == "sign_amino") {
                    updateData = updateJsonData.toString()
                } else if (method == "sign_direct") {
                    updateData = updateJsonData.toString()
                }

                if (!loading.isVisible && btnConfirm.isEnabled) {
                    listener.sign(id, updateData.toString())
                    dismiss()
                }
            }
        }
    }

    private fun updateFeeInfoInAminoMessage(txJsonObject: JsonObject): JsonObject {
        val txJsonSignDoc =
            txJsonObject.getAsJsonObject("signDoc") ?: txJsonObject.getAsJsonObject("doc")
        val isEditFee: Boolean = txJsonObject.getAsJsonPrimitive("isEditFee")?.asBoolean ?: true
        val fee = txJsonSignDoc.get("fee").asJsonObject
        val gas = fee.get("gas").asString
        val amounts = fee.get("amount").asJsonArray

        if (isEditFee || amounts.size() <= 0) {
            val chainId =
                txJsonSignDoc.get("chain_id").asString ?: txJsonObject.get("chainName").asString
            BaseData.baseAccount?.let { account ->
                account.allChains.filter { it.supportCosmos() }
                    .firstOrNull { it.chainIdCosmos.lowercase() == chainId.lowercase() }
                    ?.let { chain ->
                        chain.getFeeInfos(requireContext())
                            .first().feeDatas.firstOrNull { it.denom == chain.stakeDenom }
                            ?.let { gasRate ->
                                val gasLimit =
                                    (gas.toDouble() * chain.simulatedGasMultiply()).toLong().toBigDecimal()
                                val feeCoinAmount = gasRate.gasRate?.multiply(gasLimit)
                                    ?.setScale(0, RoundingMode.UP)

                                if (amounts.size() == 0) {
                                    val jsonObject = JsonObject()
                                    jsonObject.addProperty(
                                        "amount", feeCoinAmount.toString()
                                    )
                                    jsonObject.addProperty("denom", chain.stakeDenom)
                                    amounts.add(jsonObject)

                                } else {
                                    val mainDenomFee =
                                        amounts.firstOrNull { it.asJsonObject["denom"].asString == chain.stakeDenom }
                                    mainDenomFee?.asJsonObject?.addProperty(
                                        "amount", feeCoinAmount.toString()
                                    )
                                }
                            }
                    }
            }
        }
        return txJsonSignDoc
    }

    private fun updateFeeInfoInDirectMessage(txJsonObject: JsonObject): JsonObject {
        val doc = txJsonObject["doc"].asJsonObject
        var authInfo = TxProto.AuthInfo.parseFrom(Utils.hexToBytes(doc["auth_info_bytes"].asString))
        if (authInfo.fee.payer.isEmpty() || authInfo.fee.payer == null) {
            authInfo = authInfo.toBuilder().setFee(txFee).build()
        } else {
            txFee?.let { fee ->
                val dpFee = Fee.newBuilder().setGasLimit(fee.gasLimit).addAmount(
                    CoinProto.Coin.newBuilder().setDenom(fee.getAmount(0).denom)
                        .setAmount(fee.getAmount(0).amount)
                ).setPayer(authInfo.fee.payer).build()
                authInfo = authInfo.toBuilder().setFee(dpFee).build()
            }
        }
        val txBody = TxProto.TxBody.parseFrom(Utils.hexToBytes(doc["body_bytes"].asString))
        val fee = authInfo.fee

        val chainId = doc["chain_id"].asString
        BaseData.baseAccount?.let { account ->
            account.allChains.filter { it.supportCosmos() }
                .firstOrNull { it.chainIdCosmos.lowercase() == chainId.lowercase() }?.let { chain ->
                    selectedChain = chain
                    val simulateGas = Signer.dAppSimulateGas(chain, txBody, authInfo)
                    val simulateGasLimit =
                        (simulateGas.gasUsed.toDouble() * chain.simulatedGasMultiply()).toLong()
                    val updateFee = updateFeeWithSimulate(
                        simulateGasLimit.toBigDecimal(), fee
                    )
                    authInfo = authInfo.toBuilder().setFee(updateFee).build()
                }
        }
        return txJsonObject["doc"].asJsonObject.apply {
            addProperty("auth_info_bytes", Utils.bytesToHex(authInfo.toByteArray()))
        }
    }

    private fun updateFeeWithSimulate(
        simulateGasLimit: BigDecimal, fee: Fee
    ): Fee? {
        val denom = fee.getAmount(0).denom ?: ""
        feeInfos[selectFeePosition].feeDatas.firstOrNull { it.denom == denom }?.let { gasRate ->
            val feeCoinAmount =
                gasRate.gasRate?.multiply(simulateGasLimit)?.setScale(0, RoundingMode.UP)
            val updateFeeCoin =
                CoinProto.Coin.newBuilder().setDenom(denom).setAmount(feeCoinAmount.toString())
                    .build()
            return Fee.newBuilder().setGasLimit(simulateGasLimit.toLong())
                .addAmount(updateFeeCoin).setPayer(fee.payer).build()
        }
        return null
    }

    private fun handleOneClickWithDelay(bottomSheetDialogFragment: BottomSheetDialogFragment) {
        if (isClickable) {
            isClickable = false

            bottomSheetDialogFragment.show(
                requireActivity().supportFragmentManager, bottomSheetDialogFragment::class.java.name
            )

            Handler(Looper.getMainLooper()).postDelayed({
                isClickable = true
            }, 300)
        }
    }

    private fun loadAuth(
        managedChannel: ManagedChannel, address: String?
    ): QueryProto.QueryAccountResponse? {
        val stub = QueryGrpc.newBlockingStub(managedChannel).withDeadlineAfter(8L, TimeUnit.SECONDS)
        val request = QueryProto.QueryAccountRequest.newBuilder().setAddress(address).build()
        return try {
            stub.account(request)
        } catch (e: Exception) {
            null
        }
    }

    private fun loadSpendAbleBalance(
        managedChannel: ManagedChannel, address: String?
    ): com.cosmos.bank.v1beta1.QueryProto.QuerySpendableBalancesResponse {
        val pageRequest = PaginationProto.PageRequest.newBuilder().setLimit(2000).build()
        val stub = com.cosmos.bank.v1beta1.QueryGrpc.newBlockingStub(managedChannel)
            .withDeadlineAfter(8L, TimeUnit.SECONDS)
        val request = com.cosmos.bank.v1beta1.QueryProto.QuerySpendableBalancesRequest.newBuilder()
            .setPagination(pageRequest).setAddress(address).build()
        return stub.spendableBalances(request)
    }

    interface WcSignRawDataListener {
        fun sign(id: Long, data: String)
        fun cancel(id: Long)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}