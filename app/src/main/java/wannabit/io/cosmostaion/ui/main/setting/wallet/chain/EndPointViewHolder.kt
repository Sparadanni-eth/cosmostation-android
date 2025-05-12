package wannabit.io.cosmostaion.ui.main.setting.wallet.chain

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.cosmos.base.tendermint.v1beta1.QueryProto.GetNodeInfoRequest
import com.cosmos.base.tendermint.v1beta1.ServiceGrpc.newBlockingStub
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import wannabit.io.cosmostaion.R
import wannabit.io.cosmostaion.chain.BaseChain
import wannabit.io.cosmostaion.chain.CosmosEndPointType
import wannabit.io.cosmostaion.chain.majorClass.ChainIota
import wannabit.io.cosmostaion.chain.majorClass.ChainSui
import wannabit.io.cosmostaion.chain.testnetClass.ChainGnoTestnet
import wannabit.io.cosmostaion.common.goneOrVisible
import wannabit.io.cosmostaion.common.jsonRpcResponse
import wannabit.io.cosmostaion.common.setView
import wannabit.io.cosmostaion.data.model.req.JsonRpcRequest
import wannabit.io.cosmostaion.databinding.ItemEndpointBinding
import java.io.IOException

class EndPointViewHolder(
    private val context: Context, private val binding: ItemEndpointBinding
) : RecyclerView.ViewHolder(binding.root) {

    private var gapTime: Double? = null

    fun evmBind(
        fromChain: BaseChain?, endpoint: JsonObject, listener: EndpointAdapter.EndpointListener?
    ) {
        binding.apply {
            provider.text = endpoint.get("provider").asString
            providerUrl.text = endpoint.get("url").asString.replace("https://", "")

            val checkTime = System.currentTimeMillis() / 1000.0
            val url = endpoint.get("url").asString

            chainView.goneOrVisible(fromChain?.evmRpcFetcher()?.getEvmRpc() != url)
            endpointView.setView(fromChain?.evmRpcFetcher()?.getEvmRpc() != url)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val web3j = Web3j.build(HttpService(url))
                    web3j.web3ClientVersion()?.sendAsync()?.get()?.web3ClientVersion
                    gapTime = (System.currentTimeMillis() / 1000.0 - checkTime)
                    withContext(Dispatchers.Main) {
                        gapTime?.let {
                            provider.setTextColor(
                                ContextCompat.getColorStateList(
                                    context, R.color.color_base01
                                )
                            )
                            providerUrl.setTextColor(
                                ContextCompat.getColorStateList(
                                    context, R.color.color_base02
                                )
                            )

                            loading.visibility = View.GONE
                            apiStatus.visibility = View.VISIBLE
                            apiStatus.setTextColor(
                                ContextCompat.getColorStateList(
                                    context, R.color.color_base01
                                )
                            )

                            if (it <= 1.2) {
                                apiStatus.setBackgroundResource(R.drawable.round_box_faster)
                                apiStatus.text = "Faster"

                            } else if (it <= 3) {
                                apiStatus.setBackgroundResource(R.drawable.round_box_normal)
                                apiStatus.text = "Normal"

                            } else {
                                apiStatus.setBackgroundResource(R.drawable.round_box_slow)
                                apiStatus.text = "Slower"
                            }
                        }
                    }

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        provider.setTextColor(
                            ContextCompat.getColorStateList(
                                context, R.color.color_base04
                            )
                        )
                        providerUrl.setTextColor(
                            ContextCompat.getColorStateList(
                                context, R.color.color_base04
                            )
                        )

                        loading.visibility = View.GONE
                        apiStatus.text = "Closed"
                        apiStatus.visibility = View.VISIBLE
                        apiStatus.setBackgroundResource(R.drawable.round_box_closed)
                        apiStatus.setTextColor(
                            ContextCompat.getColorStateList(
                                context, R.color.color_base04
                            )
                        )
                    }
                }
            }

            endpointView.setOnClickListener {
                listener?.rpcSelect(endpoint.get("url").asString, gapTime)
            }
        }
    }

    fun bind(
        fromChain: BaseChain?, endpoint: JsonObject, listener: EndpointAdapter.EndpointListener?
    ) {
        binding.apply {
            provider.text = endpoint.get("provider").asString
            providerUrl.text = endpoint.get("url").asString

            val checkTime = System.currentTimeMillis() / 1000.0
            val host = endpoint.get("url").asString.split(":")[0].trim()
            val port =
                endpoint.get("url").asString.split(":").getOrNull(1)?.trim()?.toIntOrNull() ?: 443

            if (fromChain?.cosmosFetcher()
                    ?.endPointType(fromChain) == CosmosEndPointType.USE_GRPC && fromChain.cosmosFetcher()
                    ?.getGrpc()?.first == host
            ) {
                chainView.visibility = View.VISIBLE
                endpointView.setBackgroundColor(
                    ContextCompat.getColor(
                        context, R.color.color_base08
                    )
                )

            } else {
                chainView.visibility = View.GONE
                endpointView.setBackgroundColor(
                    ContextCompat.getColor(
                        context, R.color.color_transparent
                    )
                )
            }

            CoroutineScope(Dispatchers.IO).launch {
                val channel = getChannel(host, port)
                try {
                    val stub = newBlockingStub(channel)
                    val request = GetNodeInfoRequest.newBuilder().build()
                    val response = stub.getNodeInfo(request)

                    if (response.defaultNodeInfo.network == fromChain?.chainIdCosmos) {
                        gapTime = (System.currentTimeMillis() / 1000.0 - checkTime)
                        withContext(Dispatchers.Main) {
                            gapTime?.let {
                                provider.setTextColor(
                                    ContextCompat.getColorStateList(
                                        context, R.color.color_base01
                                    )
                                )
                                providerUrl.setTextColor(
                                    ContextCompat.getColorStateList(
                                        context, R.color.color_base02
                                    )
                                )

                                loading.visibility = View.GONE
                                apiStatus.visibility = View.VISIBLE
                                apiStatus.setTextColor(
                                    ContextCompat.getColorStateList(
                                        context, R.color.color_base01
                                    )
                                )

                                if (it <= 1.2) {
                                    apiStatus.setBackgroundResource(R.drawable.round_box_faster)
                                    apiStatus.text = "Faster"

                                } else if (it <= 3) {
                                    apiStatus.setBackgroundResource(R.drawable.round_box_normal)
                                    apiStatus.text = "Normal"

                                } else {
                                    apiStatus.setBackgroundResource(R.drawable.round_box_slow)
                                    apiStatus.text = "Slower"
                                }
                            }
                        }

                    } else {
                        channel.shutdown()
                        withContext(Dispatchers.Main) {
                            provider.setTextColor(
                                ContextCompat.getColorStateList(
                                    context, R.color.color_base04
                                )
                            )
                            providerUrl.setTextColor(
                                ContextCompat.getColorStateList(
                                    context, R.color.color_base04
                                )
                            )

                            loading.visibility = View.GONE
                            apiStatus.text = "Closed"
                            apiStatus.visibility = View.VISIBLE
                            apiStatus.setBackgroundResource(R.drawable.round_box_closed)
                            apiStatus.setTextColor(
                                ContextCompat.getColorStateList(
                                    context, R.color.color_base04
                                )
                            )
                        }
                    }

                } catch (e: Exception) {
                    channel.shutdown()
                    withContext(Dispatchers.Main) {
                        provider.setTextColor(
                            ContextCompat.getColorStateList(
                                context, R.color.color_base04
                            )
                        )
                        providerUrl.setTextColor(
                            ContextCompat.getColorStateList(
                                context, R.color.color_base04
                            )
                        )

                        loading.visibility = View.GONE
                        apiStatus.text = "Closed"
                        apiStatus.visibility = View.VISIBLE
                        apiStatus.setBackgroundResource(R.drawable.round_box_closed)
                        apiStatus.setTextColor(
                            ContextCompat.getColorStateList(
                                context, R.color.color_base04
                            )
                        )
                    }
                }
            }

            endpointView.setOnClickListener {
                listener?.select(endpoint.get("url").asString, gapTime)
            }
        }
    }

    fun lcdBind(
        fromChain: BaseChain?, endpoint: JsonObject, listener: EndpointAdapter.EndpointListener?
    ) {
        binding.apply {
            fromChain?.let { chain ->
                provider.text = endpoint.get("provider").asString
                providerUrl.text = endpoint.get("url").asString

                val checkTime = System.currentTimeMillis() / 1000.0
                var url = endpoint.get("url").asString
                if (!url.endsWith("/")) {
                    url += "/"
                }
                url += "cosmos/base/tendermint/v1beta1/node_info"

                if (chain.cosmosFetcher?.endPointType(chain) == CosmosEndPointType.USE_LCD && chain.cosmosFetcher?.getLcd()
                        ?.contains(endpoint["url"].asString) == true
                ) {
                    chainView.visibility = View.VISIBLE
                    endpointView.setBackgroundColor(
                        ContextCompat.getColor(
                            context, R.color.color_base08
                        )
                    )

                } else {
                    chainView.visibility = View.GONE
                    endpointView.setBackgroundColor(
                        ContextCompat.getColor(
                            context, R.color.color_transparent
                        )
                    )
                }

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val request = Request.Builder().url(url).build()
                        OkHttpClient().newCall(request).execute().use { response ->
                            if (!response.isSuccessful) throw IOException("Unexpected code $response")
                            val body = response.body?.string()
                            val json = Gson().fromJson(body, JsonObject::class.java)

                            val network = try {
                                json["default_node_info"].asJsonObject["network"].asString
                            } catch (e: Exception) {
                                json["node_info"].asJsonObject["network"].asString
                            }

                            if (network == chain.chainIdCosmos) {
                                gapTime = (System.currentTimeMillis() / 1000.0 - checkTime)
                                withContext(Dispatchers.Main) {
                                    gapTime?.let {
                                        provider.setTextColor(
                                            ContextCompat.getColorStateList(
                                                context, R.color.color_base01
                                            )
                                        )
                                        providerUrl.setTextColor(
                                            ContextCompat.getColorStateList(
                                                context, R.color.color_base02
                                            )
                                        )

                                        loading.visibility = View.GONE
                                        apiStatus.visibility = View.VISIBLE
                                        apiStatus.setTextColor(
                                            ContextCompat.getColorStateList(
                                                context, R.color.color_base01
                                            )
                                        )

                                        if (it <= 1.2) {
                                            apiStatus.setBackgroundResource(R.drawable.round_box_faster)
                                            apiStatus.text = "Faster"

                                        } else if (it <= 3) {
                                            apiStatus.setBackgroundResource(R.drawable.round_box_normal)
                                            apiStatus.text = "Normal"

                                        } else {
                                            apiStatus.setBackgroundResource(R.drawable.round_box_slow)
                                            apiStatus.text = "Slower"
                                        }
                                    }
                                }

                            } else {
                                withContext(Dispatchers.Main) {
                                    provider.setTextColor(
                                        ContextCompat.getColorStateList(
                                            context, R.color.color_base04
                                        )
                                    )
                                    providerUrl.setTextColor(
                                        ContextCompat.getColorStateList(
                                            context, R.color.color_base04
                                        )
                                    )

                                    loading.visibility = View.GONE
                                    apiStatus.text = "Closed"
                                    apiStatus.visibility = View.VISIBLE
                                    apiStatus.setBackgroundResource(R.drawable.round_box_closed)
                                    apiStatus.setTextColor(
                                        ContextCompat.getColorStateList(
                                            context, R.color.color_base04
                                        )
                                    )
                                }
                            }
                        }

                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            provider.setTextColor(
                                ContextCompat.getColorStateList(
                                    context, R.color.color_base04
                                )
                            )
                            providerUrl.setTextColor(
                                ContextCompat.getColorStateList(
                                    context, R.color.color_base04
                                )
                            )

                            loading.visibility = View.GONE
                            apiStatus.text = "Closed"
                            apiStatus.visibility = View.VISIBLE
                            apiStatus.setBackgroundResource(R.drawable.round_box_closed)
                            apiStatus.setTextColor(
                                ContextCompat.getColorStateList(
                                    context, R.color.color_base04
                                )
                            )
                        }
                    }
                }

                endpointView.setOnClickListener {
                    listener?.lcdSelect(endpoint.get("url").asString, gapTime)
                }
            }
        }
    }

    fun suiBind(
        fromChain: BaseChain?, endpoint: JsonObject, listener: EndpointAdapter.EndpointListener?
    ) {
        binding.apply {
            (fromChain as ChainSui).suiFetcher()?.let { fetcher ->
                provider.text = endpoint.get("provider").asString
                providerUrl.text = endpoint.get("url").asString.replace("https://", "")

                val checkTime = System.currentTimeMillis() / 1000.0
                val url = endpoint.get("url").asString
                if (fetcher.suiRpc() != url) {
                    chainView.visibility = View.GONE
                    endpointView.setBackgroundColor(
                        ContextCompat.getColor(
                            context, R.color.color_transparent
                        )
                    )

                } else {
                    chainView.visibility = View.VISIBLE
                    endpointView.setBackgroundColor(
                        ContextCompat.getColor(
                            context, R.color.color_base08
                        )
                    )
                }

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val suiChainIdRequest = JsonRpcRequest(
                            method = "sui_getChainIdentifier", params = listOf()
                        )
                        val suiChainIdResponse =
                            jsonRpcResponse(fetcher.suiRpc(), suiChainIdRequest)
                        if (suiChainIdResponse.isSuccessful) {
                            gapTime = (System.currentTimeMillis() / 1000.0 - checkTime)
                            withContext(Dispatchers.Main) {
                                gapTime?.let {
                                    provider.setTextColor(
                                        ContextCompat.getColorStateList(
                                            context, R.color.color_base01
                                        )
                                    )
                                    providerUrl.setTextColor(
                                        ContextCompat.getColorStateList(
                                            context, R.color.color_base02
                                        )
                                    )

                                    loading.visibility = View.GONE
                                    apiStatus.visibility = View.VISIBLE
                                    apiStatus.setTextColor(
                                        ContextCompat.getColorStateList(
                                            context, R.color.color_base01
                                        )
                                    )

                                    if (it <= 1.2) {
                                        apiStatus.setBackgroundResource(R.drawable.round_box_faster)
                                        apiStatus.text = "Faster"

                                    } else if (it <= 3) {
                                        apiStatus.setBackgroundResource(R.drawable.round_box_normal)
                                        apiStatus.text = "Normal"

                                    } else {
                                        apiStatus.setBackgroundResource(R.drawable.round_box_slow)
                                        apiStatus.text = "Slower"
                                    }
                                }
                            }

                        } else {
                            withContext(Dispatchers.Main) {
                                provider.setTextColor(
                                    ContextCompat.getColorStateList(
                                        context, R.color.color_base04
                                    )
                                )
                                providerUrl.setTextColor(
                                    ContextCompat.getColorStateList(
                                        context, R.color.color_base04
                                    )
                                )

                                loading.visibility = View.GONE
                                apiStatus.text = "Closed"
                                apiStatus.visibility = View.VISIBLE
                                apiStatus.setBackgroundResource(R.drawable.round_box_closed)
                                apiStatus.setTextColor(
                                    ContextCompat.getColorStateList(
                                        context, R.color.color_base04
                                    )
                                )
                            }
                        }

                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            provider.setTextColor(
                                ContextCompat.getColorStateList(
                                    context, R.color.color_base04
                                )
                            )
                            providerUrl.setTextColor(
                                ContextCompat.getColorStateList(
                                    context, R.color.color_base04
                                )
                            )

                            loading.visibility = View.GONE
                            apiStatus.text = "Closed"
                            apiStatus.visibility = View.VISIBLE
                            apiStatus.setBackgroundResource(R.drawable.round_box_closed)
                            apiStatus.setTextColor(
                                ContextCompat.getColorStateList(
                                    context, R.color.color_base04
                                )
                            )
                        }
                    }
                }

                endpointView.setOnClickListener {
                    listener?.rpcSelect(endpoint.get("url").asString, gapTime)
                }
            }
        }
    }

    fun iotaBind(
        fromChain: BaseChain?, endpoint: JsonObject, listener: EndpointAdapter.EndpointListener?
    ) {
        binding.apply {
            (fromChain as ChainIota).iotaFetcher()?.let { fetcher ->
                provider.text = endpoint.get("provider").asString
                providerUrl.text = endpoint.get("url").asString.replace("https://", "")

                val checkTime = System.currentTimeMillis() / 1000.0
                val url = endpoint.get("url").asString
                if (fetcher.iotaRpc() != url) {
                    chainView.visibility = View.GONE
                    endpointView.setBackgroundColor(
                        ContextCompat.getColor(
                            context, R.color.color_transparent
                        )
                    )

                } else {
                    chainView.visibility = View.VISIBLE
                    endpointView.setBackgroundColor(
                        ContextCompat.getColor(
                            context, R.color.color_base08
                        )
                    )
                }

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val suiChainIdRequest = JsonRpcRequest(
                            method = "iota_getChainIdentifier", params = listOf()
                        )
                        val suiChainIdResponse =
                            jsonRpcResponse(fetcher.iotaRpc(), suiChainIdRequest)
                        if (suiChainIdResponse.isSuccessful) {
                            gapTime = (System.currentTimeMillis() / 1000.0 - checkTime)
                            withContext(Dispatchers.Main) {
                                gapTime?.let {
                                    provider.setTextColor(
                                        ContextCompat.getColorStateList(
                                            context, R.color.color_base01
                                        )
                                    )
                                    providerUrl.setTextColor(
                                        ContextCompat.getColorStateList(
                                            context, R.color.color_base02
                                        )
                                    )

                                    loading.visibility = View.GONE
                                    apiStatus.visibility = View.VISIBLE
                                    apiStatus.setTextColor(
                                        ContextCompat.getColorStateList(
                                            context, R.color.color_base01
                                        )
                                    )

                                    if (it <= 1.2) {
                                        apiStatus.setBackgroundResource(R.drawable.round_box_faster)
                                        apiStatus.text = "Faster"

                                    } else if (it <= 3) {
                                        apiStatus.setBackgroundResource(R.drawable.round_box_normal)
                                        apiStatus.text = "Normal"

                                    } else {
                                        apiStatus.setBackgroundResource(R.drawable.round_box_slow)
                                        apiStatus.text = "Slower"
                                    }
                                }
                            }

                        } else {
                            withContext(Dispatchers.Main) {
                                provider.setTextColor(
                                    ContextCompat.getColorStateList(
                                        context, R.color.color_base04
                                    )
                                )
                                providerUrl.setTextColor(
                                    ContextCompat.getColorStateList(
                                        context, R.color.color_base04
                                    )
                                )

                                loading.visibility = View.GONE
                                apiStatus.text = "Closed"
                                apiStatus.visibility = View.VISIBLE
                                apiStatus.setBackgroundResource(R.drawable.round_box_closed)
                                apiStatus.setTextColor(
                                    ContextCompat.getColorStateList(
                                        context, R.color.color_base04
                                    )
                                )
                            }
                        }

                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            provider.setTextColor(
                                ContextCompat.getColorStateList(
                                    context, R.color.color_base04
                                )
                            )
                            providerUrl.setTextColor(
                                ContextCompat.getColorStateList(
                                    context, R.color.color_base04
                                )
                            )

                            loading.visibility = View.GONE
                            apiStatus.text = "Closed"
                            apiStatus.visibility = View.VISIBLE
                            apiStatus.setBackgroundResource(R.drawable.round_box_closed)
                            apiStatus.setTextColor(
                                ContextCompat.getColorStateList(
                                    context, R.color.color_base04
                                )
                            )
                        }
                    }
                }

                endpointView.setOnClickListener {
                    listener?.rpcSelect(endpoint.get("url").asString, gapTime)
                }
            }
        }
    }

    fun rpcBind(
        fromChain: BaseChain?, endpoint: JsonObject, listener: EndpointAdapter.EndpointListener?
    ) {
        binding.apply {
            (fromChain as ChainGnoTestnet).gnoRpcFetcher()?.let { fetcher ->
                provider.text = endpoint.get("provider").asString
                providerUrl.text = endpoint.get("url").asString.replace("https://", "")

                val checkTime = System.currentTimeMillis() / 1000.0
                var url = endpoint.get("url").asString
                if (fetcher.gnoRpc() != url) {
                    chainView.visibility = View.GONE
                    endpointView.setBackgroundColor(
                        ContextCompat.getColor(
                            context, R.color.color_transparent
                        )
                    )

                } else {
                    chainView.visibility = View.VISIBLE
                    endpointView.setBackgroundColor(
                        ContextCompat.getColor(
                            context, R.color.color_base08
                        )
                    )
                }
                url += "/health"

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val request = Request.Builder().url(url).build()
                        OkHttpClient().newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                gapTime = (System.currentTimeMillis() / 1000.0 - checkTime)
                                withContext(Dispatchers.Main) {
                                    gapTime?.let {
                                        provider.setTextColor(
                                            ContextCompat.getColorStateList(
                                                context, R.color.color_base01
                                            )
                                        )
                                        providerUrl.setTextColor(
                                            ContextCompat.getColorStateList(
                                                context, R.color.color_base02
                                            )
                                        )

                                        loading.visibility = View.GONE
                                        apiStatus.visibility = View.VISIBLE
                                        apiStatus.setTextColor(
                                            ContextCompat.getColorStateList(
                                                context, R.color.color_base01
                                            )
                                        )

                                        if (it <= 1.2) {
                                            apiStatus.setBackgroundResource(R.drawable.round_box_faster)
                                            apiStatus.text = "Faster"

                                        } else if (it <= 3) {
                                            apiStatus.setBackgroundResource(R.drawable.round_box_normal)
                                            apiStatus.text = "Normal"

                                        } else {
                                            apiStatus.setBackgroundResource(R.drawable.round_box_slow)
                                            apiStatus.text = "Slower"
                                        }
                                    }
                                }

                            } else {
                                withContext(Dispatchers.Main) {
                                    provider.setTextColor(
                                        ContextCompat.getColorStateList(
                                            context, R.color.color_base04
                                        )
                                    )
                                    providerUrl.setTextColor(
                                        ContextCompat.getColorStateList(
                                            context, R.color.color_base04
                                        )
                                    )

                                    loading.visibility = View.GONE
                                    apiStatus.text = "Closed"
                                    apiStatus.visibility = View.VISIBLE
                                    apiStatus.setBackgroundResource(R.drawable.round_box_closed)
                                    apiStatus.setTextColor(
                                        ContextCompat.getColorStateList(
                                            context, R.color.color_base04
                                        )
                                    )
                                }
                            }
                        }

                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            provider.setTextColor(
                                ContextCompat.getColorStateList(
                                    context, R.color.color_base04
                                )
                            )
                            providerUrl.setTextColor(
                                ContextCompat.getColorStateList(
                                    context, R.color.color_base04
                                )
                            )

                            loading.visibility = View.GONE
                            apiStatus.text = "Closed"
                            apiStatus.visibility = View.VISIBLE
                            apiStatus.setBackgroundResource(R.drawable.round_box_closed)
                            apiStatus.setTextColor(
                                ContextCompat.getColorStateList(
                                    context, R.color.color_base04
                                )
                            )
                        }
                    }
                }

                endpointView.setOnClickListener {
                    listener?.rpcSelect(endpoint.get("url").asString, gapTime)
                }
            }
        }
    }

    fun getChannel(host: String, port: Int): ManagedChannel {
        return ManagedChannelBuilder.forAddress(host, port).useTransportSecurity().build()
    }
}