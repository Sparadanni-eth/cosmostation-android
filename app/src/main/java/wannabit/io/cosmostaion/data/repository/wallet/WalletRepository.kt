package wannabit.io.cosmostaion.data.repository.wallet

import com.babylon.btcstaking.v1.ParamsProto.Params
import com.babylon.epoching.v1.QueryProto.QueuedMessageResponse
import com.cosmos.base.v1beta1.CoinProto
import com.cosmos.distribution.v1beta1.DistributionProto
import com.cosmos.staking.v1beta1.StakingProto
import com.cosmwasm.wasm.v1.QueryProto.QuerySmartContractStateResponse
import com.google.gson.JsonObject
import io.grpc.ManagedChannel
import retrofit2.Response
import wannabit.io.cosmostaion.chain.BaseChain
import wannabit.io.cosmostaion.chain.cosmosClass.ChainInitia
import wannabit.io.cosmostaion.chain.cosmosClass.ChainZenrock
import wannabit.io.cosmostaion.chain.fetcher.BabylonFetcher
import wannabit.io.cosmostaion.chain.fetcher.IotaFetcher
import wannabit.io.cosmostaion.chain.fetcher.SuiFetcher
import wannabit.io.cosmostaion.chain.majorClass.ChainBitCoin86
import wannabit.io.cosmostaion.chain.majorClass.ChainIota
import wannabit.io.cosmostaion.chain.majorClass.ChainSui
import wannabit.io.cosmostaion.data.model.req.MoonPayReq
import wannabit.io.cosmostaion.data.model.res.AppVersion
import wannabit.io.cosmostaion.data.model.res.AssetResponse
import wannabit.io.cosmostaion.data.model.res.Cw20TokenResponse
import wannabit.io.cosmostaion.data.model.res.Cw721
import wannabit.io.cosmostaion.data.model.res.Cw721Response
import wannabit.io.cosmostaion.data.model.res.Erc20TokenResponse
import wannabit.io.cosmostaion.data.model.res.Grc20TokenResponse
import wannabit.io.cosmostaion.data.model.res.MoonPay
import wannabit.io.cosmostaion.data.model.res.NetworkResult
import wannabit.io.cosmostaion.data.model.res.NoticeResponse
import wannabit.io.cosmostaion.data.model.res.Price
import wannabit.io.cosmostaion.data.model.res.Token
import wannabit.io.cosmostaion.database.model.Password
import java.math.BigDecimal

interface WalletRepository {
    suspend fun selectPassword(): NetworkResult<MutableList<Password>>

    suspend fun insertPassword(password: Password)

    suspend fun version(): NetworkResult<Response<AppVersion>>

    suspend fun price(currency: String): NetworkResult<List<Price>>

    suspend fun usdPrice(): NetworkResult<List<Price>>

    suspend fun asset(): NetworkResult<AssetResponse>

    suspend fun param(): NetworkResult<JsonObject?>

    suspend fun cw20(): NetworkResult<Cw20TokenResponse>

    suspend fun erc20(): NetworkResult<Erc20TokenResponse>

    suspend fun grc20(): NetworkResult<Grc20TokenResponse>

    suspend fun cw721(): NetworkResult<Cw721Response>

    suspend fun auth(
        channel: ManagedChannel?, chain: BaseChain
    ): NetworkResult<Unit>

    suspend fun balance(
        channel: ManagedChannel?, chain: BaseChain
    ): NetworkResult<MutableList<CoinProto.Coin>>

    suspend fun spendableBalance(
        channel: ManagedChannel?, chain: BaseChain
    ): NetworkResult<MutableList<CoinProto.Coin>>

    suspend fun delegation(
        channel: ManagedChannel?, chain: BaseChain
    ): NetworkResult<MutableList<StakingProto.DelegationResponse>>

    suspend fun unBonding(
        channel: ManagedChannel?, chain: BaseChain
    ): NetworkResult<MutableList<StakingProto.UnbondingDelegation>>

    suspend fun reward(
        channel: ManagedChannel?, chain: BaseChain
    ): NetworkResult<MutableList<DistributionProto.DelegationDelegatorReward>>

    suspend fun rewardAddress(channel: ManagedChannel?, chain: BaseChain): NetworkResult<String>

    suspend fun baseFee(
        channel: ManagedChannel?, chain: BaseChain
    ): NetworkResult<MutableList<CoinProto.DecCoin>>?

    suspend fun bondedValidator(
        channel: ManagedChannel?, chain: BaseChain
    ): NetworkResult<MutableList<StakingProto.Validator>>

    suspend fun unBondedValidator(
        channel: ManagedChannel?, chain: BaseChain
    ): NetworkResult<MutableList<StakingProto.Validator>>

    suspend fun unBondingValidator(
        channel: ManagedChannel?, chain: BaseChain
    ): NetworkResult<MutableList<StakingProto.Validator>>

    suspend fun moonPay(data: MoonPayReq): NetworkResult<Response<MoonPay>>

    suspend fun cw20Balance(
        channel: ManagedChannel?, chain: BaseChain, token: Token
    )

    suspend fun erc20Balance(
        chain: BaseChain, token: Token
    )

    suspend fun grc20Balance(
        chain: BaseChain, grc20Token: Token
    )

    //neutron
    suspend fun vestingData(
        channel: ManagedChannel?, chain: BaseChain
    ): NetworkResult<QuerySmartContractStateResponse>

    suspend fun vaultDeposit(
        channel: ManagedChannel?, chain: BaseChain
    ): NetworkResult<String?>

    suspend fun stakingRewards(
        channel: ManagedChannel?, chain: BaseChain
    ): NetworkResult<BigDecimal?>

    //initia
    suspend fun initiaDelegation(
        channel: ManagedChannel?, chain: ChainInitia
    ): NetworkResult<MutableList<com.initia.mstaking.v1.StakingProto.DelegationResponse>>

    suspend fun initiaUnBonding(
        channel: ManagedChannel?, chain: ChainInitia
    ): NetworkResult<MutableList<com.initia.mstaking.v1.StakingProto.UnbondingDelegation>>

    suspend fun initiaBondedValidator(
        channel: ManagedChannel?, chain: ChainInitia
    ): NetworkResult<MutableList<com.initia.mstaking.v1.StakingProto.Validator>>

    suspend fun initiaUnBondedValidator(
        channel: ManagedChannel?, chain: ChainInitia
    ): NetworkResult<MutableList<com.initia.mstaking.v1.StakingProto.Validator>>

    suspend fun initiaUnBondingValidator(
        channel: ManagedChannel?, chain: ChainInitia
    ): NetworkResult<MutableList<com.initia.mstaking.v1.StakingProto.Validator>>

    //zenrock
    suspend fun zenrockDelegation(
        channel: ManagedChannel?, chain: ChainZenrock
    ): NetworkResult<MutableList<com.zrchain.validation.StakingProto.DelegationResponse>>

    suspend fun zenrockUnBonding(
        channel: ManagedChannel?, chain: ChainZenrock
    ): NetworkResult<MutableList<com.zrchain.validation.StakingProto.UnbondingDelegation>>

    suspend fun zenrockBondedValidator(
        channel: ManagedChannel?, chain: ChainZenrock
    ): NetworkResult<MutableList<com.zrchain.validation.HybridValidationProto.ValidatorHV>>

    suspend fun zenrockUnBondedValidator(
        channel: ManagedChannel?, chain: ChainZenrock
    ): NetworkResult<MutableList<com.zrchain.validation.HybridValidationProto.ValidatorHV>>

    suspend fun zenrockUnBondingValidator(
        channel: ManagedChannel?, chain: ChainZenrock
    ): NetworkResult<MutableList<com.zrchain.validation.HybridValidationProto.ValidatorHV>>

    //okt
    suspend fun oktAccountInfo(
        chain: BaseChain
    ): NetworkResult<JsonObject?>

    suspend fun oktDeposit(
        chain: BaseChain
    ): NetworkResult<JsonObject?>

    suspend fun oktWithdraw(
        chain: BaseChain
    ): NetworkResult<JsonObject?>

    suspend fun evmBalance(chain: BaseChain): NetworkResult<String>

    suspend fun cw721TokenIds(
        channel: ManagedChannel?, chain: BaseChain, list: Cw721
    ): NetworkResult<JsonObject?>

    suspend fun cw721TokenInfo(
        channel: ManagedChannel?, chain: BaseChain, list: Cw721, tokenId: String
    ): NetworkResult<JsonObject?>

    suspend fun cw721TokenDetail(
        chain: BaseChain, contractAddress: String, tokenId: String
    ): NetworkResult<JsonObject>

    suspend fun ecoSystemInfo(): NetworkResult<MutableList<JsonObject>>

    suspend fun notice(): NetworkResult<NoticeResponse>

    // Sui
    suspend fun suiBalance(
        fetcher: SuiFetcher, chain: ChainSui
    ): NetworkResult<JsonObject?>

    suspend fun suiSystemState(
        fetcher: SuiFetcher, chain: ChainSui
    ): NetworkResult<JsonObject>

    suspend fun suiOwnedObject(
        fetcher: SuiFetcher, chain: ChainSui, cursor: String?
    )

    suspend fun suiStakes(
        fetcher: SuiFetcher, chain: ChainSui
    ): NetworkResult<JsonObject>

    suspend fun suiCoinMetadata(
        fetcher: SuiFetcher, chain: ChainSui, coinType: String?
    ): NetworkResult<JsonObject>

    suspend fun suiApys(
        fetcher: SuiFetcher, chain: ChainSui
    ): NetworkResult<MutableList<JsonObject>>


    //Iota
    suspend fun iotaBalance(
        fetcher: IotaFetcher, chain: ChainIota
    ): NetworkResult<JsonObject?>

    suspend fun iotaSystemState(
        fetcher: IotaFetcher, chain: ChainIota
    ): NetworkResult<JsonObject>

    suspend fun iotaOwnedObject(
        fetcher: IotaFetcher, chain: ChainIota, cursor: String?
    )

    suspend fun iotaStakes(
        fetcher: IotaFetcher, chain: ChainIota
    ): NetworkResult<JsonObject>

    suspend fun iotaCoinMetadata(
        fetcher: IotaFetcher, chain: ChainIota, coinType: String?
    ): NetworkResult<JsonObject>

    suspend fun iotaApys(
        fetcher: IotaFetcher, chain: ChainIota
    ): NetworkResult<MutableList<JsonObject>>


    //Bitcoin
    suspend fun bitBalance(chain: ChainBitCoin86): NetworkResult<JsonObject>

    suspend fun bitStakingBalance(chain: BaseChain): NetworkResult<JsonObject>

    suspend fun rpcAuth(chain: BaseChain): NetworkResult<okhttp3.Response>

    suspend fun btcStakingStatus(chain: BaseChain): NetworkResult<MutableList<JsonObject>?>

    suspend fun btcReward(
        channel: ManagedChannel?, chain: BaseChain
    ): NetworkResult<MutableList<CoinProto.Coin>>

    suspend fun chainHeight(
        channel: ManagedChannel?, chain: BaseChain
    ): NetworkResult<Long>

    suspend fun currentEpoch(
        channel: ManagedChannel?, chain: BaseChain
    ): NetworkResult<com.babylon.epoching.v1.QueryProto.QueryCurrentEpochResponse>

    suspend fun epochMessage(
        channel: ManagedChannel?, chain: BaseChain, epoch: Long
    ): NetworkResult<MutableList<QueuedMessageResponse>>

    suspend fun epochMessageType(
        channel: ManagedChannel?, chain: BaseChain, epochMsgs: MutableList<QueuedMessageResponse>?
    ): NetworkResult<MutableList<BabylonFetcher.BabylonEpochTxType>?>

    suspend fun statusHeight(channel: ManagedChannel?): NetworkResult<Long>

    suspend fun btcFinalityVotingPower(
        chain: BaseChain, height: Long
    ): NetworkResult<MutableList<com.babylon.finality.v1.QueryProto.ActiveFinalityProvidersAtHeightResponse>>

    suspend fun btcFinalityProviders(
        channel: ManagedChannel?
    ): NetworkResult<MutableList<com.babylon.btcstaking.v1.QueryProto.FinalityProviderResponse>>

    suspend fun btcParams(channel: ManagedChannel?): NetworkResult<Params>

    suspend fun btcNetworkInfo(chain: BaseChain): NetworkResult<JsonObject>

    suspend fun btcClientTip(chain: BaseChain): NetworkResult<JsonObject>

    suspend fun btcFee(chain: ChainBitCoin86): NetworkResult<JsonObject>

    suspend fun mempoolUtxo(
        chain: ChainBitCoin86
    ): NetworkResult<MutableList<JsonObject>>

    suspend fun mempoolIsValidAddress(
        chain: ChainBitCoin86
    ): NetworkResult<JsonObject>
}