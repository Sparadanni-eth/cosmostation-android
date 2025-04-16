package wannabit.io.cosmostaion.data.model.res

import android.content.Context
import android.util.Log
import com.cosmos.base.v1beta1.CoinProto
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.bouncycastle.util.encoders.Base64
import wannabit.io.cosmostaion.R
import wannabit.io.cosmostaion.chain.BaseChain
import wannabit.io.cosmostaion.common.ByteUtils
import wannabit.io.cosmostaion.common.hexToBigDecimal
import wannabit.io.cosmostaion.common.toHex
import java.math.BigDecimal
import java.util.regex.Pattern


@JsonClass(generateAdapter = true)
data class CosmosHistory(
    val header: HistoryHeader?,
    val data: HistoryData?,
    @Json(name = "search_after") val searchAfter: String?
) {
    @JsonClass(generateAdapter = true)
    data class HistoryHeader(
        @Json(name = "chain_id") val chainId: String, val timestamp: String
    )

    @JsonClass(generateAdapter = true)
    data class HistoryData(
        val height: String?, val txhash: String?, val code: Int?, val tx: Any?, val logs: Any?
    )

    fun isSuccess(): Boolean {
        data?.code?.let { rawCode ->
            if (rawCode != 0) {
                return false
            }
        }
        return true
    }

    fun getMsgs(): JsonArray? {
        val json = Gson().toJson(data?.tx)
        val jsonObject = Gson().fromJson(json, JsonObject::class.java)
        if (jsonObject["/cosmos-tx-v1beta1-Tx"] != null) {
            return jsonObject["/cosmos-tx-v1beta1-Tx"].asJsonObject["body"].asJsonObject["messages"].asJsonArray

        } else if (jsonObject["cosmos-sdk/StdTx"] != null) {
            return jsonObject["cosmos-sdk/StdTx"].asJsonObject["value"].asJsonObject["msg"].asJsonArray
        }
        return null
    }

    fun getMsgCnt(): Int {
        return getMsgs()?.size() ?: 0
    }

    fun getMsgType(c: Context, address: String?): String {
        if (getMsgCnt() == 0) {
            c.getString(R.string.tx_unknown)
        }
        getMsgs()?.let { msg ->
            if (getMsgCnt() >= 2) {
                var msgType0 = ""
                var msgType1 = ""
                try {
                    msgType0 = msg.get(0).asJsonObject["@type"].asString
                } catch (_: Exception) {
                }
                try {
                    msgType0 = msg.get(0).asJsonObject["type"].asString
                } catch (_: Exception) {
                }
                try {
                    msgType1 = msg.get(1).asJsonObject["@type"].asString
                } catch (_: Exception) {
                }
                try {
                    msgType1 = msg.get(1).asJsonObject["type"].asString
                } catch (_: Exception) {
                }

                if (msgType0.contains("MsgWithdrawDelegatorReward") && msgType1.contains("MsgDelegate") || msgType0.contains(
                        "MsgWithdrawDelegationReward"
                    ) && msgType1.contains("MsgDelegate")
                ) {
                    return if (getMsgCnt() == 2) c.getString(R.string.tx_reinvest) else c.getString(
                        R.string.tx_reinvest
                    ) + " + " + (getMsgCnt() - 1) / 2
                }
            }

            if (getMsgCnt() > 1) {
                var allSend = true
                msg.forEach {
                    val msgType = try {
                        it.asJsonObject["@type"].asString
                    } catch (e: Exception) {
                        null
                    }
                    if (msgType != null && !msgType.contains("MsgSend")) {
                        allSend = false
                    }
                }

                if (allSend) {
                    msg.forEach {
                        val msgType = try {
                            it.asJsonObject["@type"].asString
                        } catch (e: Exception) {
                            null
                        }
                        if (msgType != null) {
                            val msgValue = it.asJsonObject[msgType.replace(".", "-")]
                            msgValue.asJsonObject["from_address"].asString?.let { senderAddr ->
                                if (address == senderAddr) {
                                    return c.getString(R.string.tx_send) + " + " + (getMsgCnt() - 1)
                                }
                            }
                            msgValue.asJsonObject["to_address"].asString?.let { receiverAddr ->
                                if (address == receiverAddr) {
                                    return c.getString(R.string.tx_receive) + " + " + (getMsgCnt() - 1)
                                }
                            }
                        } else {
                            return c.getString(R.string.tx_transfer) + " + " + (getMsgCnt() - 1)
                        }
                    }
                }
            }

            var result: String
            val firstMsg = msg.get(0)
            var msgType = ""
            try {
                msgType = firstMsg.asJsonObject["@type"].asString
            } catch (_: Exception) {
            }
            try {
                msgType = firstMsg.asJsonObject["type"].asString
            } catch (_: Exception) {
            }
            result = msgType.split(".").last().replace("Msg", "")

            val msgValue = firstMsg.asJsonObject[msgType.replace(".", "-")].asJsonObject

            if (msgType.contains("cosmos.") && msgType.contains("staking")) {
                if (msgType.contains("MsgCreateValidator")) {
                    result = c.getString(R.string.tx_create_validator)
                } else if (msgType.contains("MsgEditValidator")) {
                    result = c.getString(R.string.tx_edit_validator)
                } else if (msgType.contains("MsgDelegate")) {
                    result = c.getString(R.string.tx_delegate)
                } else if (msgType.contains("MsgUndelegate")) {
                    result = c.getString(R.string.tx_undelegate)
                } else if (msgType.contains("MsgBeginRedelegate")) {
                    result = c.getString(R.string.tx_redelegate)
                } else if (msgType.contains("MsgCancelUnbondingDelegation")) {
                    result = c.getString(R.string.tx_cancel_undelegate)
                }

            } else if (msgType.contains("cosmos.") && msgType.contains("bank")) {
                if (msgType.contains("MsgSend")) {
                    val fromAddress = msgValue["from_address"].asString
                    val toAddress = msgValue["to_address"].asString
                    result = if (fromAddress == address) {
                        c.getString(R.string.tx_send)
                    } else if (toAddress == address) {
                        c.getString(R.string.tx_receive)
                    } else {
                        c.getString(R.string.tx_transfer)
                    }

                } else if (msgType.contains("MsgMultiSend")) {
                    result = c.getString(R.string.tx_multi_transfer)
                    for (input in msgValue.asJsonObject["inputs"].asJsonArray) {
                        if (input.asJsonObject["address"].asString.equals(address, true)) {
                            result = c.getString(R.string.tx_multi_send)
                            break
                        }
                    }
                    for (output in msgValue.asJsonObject["outputs"].asJsonArray) {
                        if (output.asJsonObject["address"].asString.equals(address, true)) {
                            result = c.getString(R.string.tx_multi_received)
                            break
                        }
                    }
                }

            } else if (msgType.contains("cosmos.") && msgType.contains("distribution")) {
                if (msgType.contains("MsgSetWithdrawAddress") || msgType.contains("MsgModifyWithdrawAddress")) {
                    result = c.getString(R.string.tx_change_reward_address)
                } else if (msgType.contains("MsgWithdrawDelegatorReward") || msgType.contains(
                        "MsgWithdrawDelegationReward"
                    )
                ) {
                    result = c.getString(R.string.tx_get_reward)
                } else if (msgType.contains("MsgWithdrawValidatorCommission")) {
                    result = c.getString(R.string.tx_get_commission)
                } else if (msgType.contains("MsgFundCommunityPool")) {
                    result = c.getString(R.string.tx_fund_pool)
                }

            } else if (msgType.contains("cosmos.") && msgType.contains("gov")) {
                if (msgType.contains("MsgSubmitProposal")) {
                    result = c.getString(R.string.tx_submit_proposal)
                } else if (msgType.contains("MsgDeposit")) {
                    result = c.getString(R.string.tx_deposit_proposal)
                } else if (msgType.contains("MsgVote")) {
                    result = c.getString(R.string.tx_vote)
                } else if (msgType.contains("MsgVoteWeighted")) {
                    result = c.getString(R.string.tx_vote_weighted)
                }

            } else if (msgType.contains("cosmos.") && msgType.contains("authz")) {
                if (msgType.contains("MsgGrant")) {
                    result = c.getString(R.string.tx_authz_grant)
                } else if (msgType.contains("MsgRevoke")) {
                    result = c.getString(R.string.tx_authz_revoke)
                } else if (msgType.contains("MsgExec")) {
                    result = c.getString(R.string.tx_authz_exe)
                }

            } else if (msgType.contains("cosmos.") && msgType.contains("slashing")) {
                if (msgType.contains("MsgUnjail")) {
                    result = c.getString(R.string.tx_unjail)
                }

            } else if (msgType.contains("cosmos.") && msgType.contains("feegrant")) {
                if (msgType.contains("MsgGrantAllowance")) {
                    result = c.getString(R.string.tx_authz_grant_allow)
                } else if (msgType.contains("MsgRevokeAllowance")) {
                    result = c.getString(R.string.tx_authz_grant_revoke)
                }

            } else if (msgType.contains("stride.") && msgType.contains("stakeibc")) {
                if (msgType.contains("MsgLiquidStake")) {
                    result = c.getString(R.string.tx_stride_liquid_stake)
                } else if (msgType.contains("MsgRedeemStake")) {
                    result = c.getString(R.string.tx_stride_redeem_stake)
                }

            } else if (msgType.contains("crescent.") && msgType.contains("liquidstaking")) {
                if (msgType.contains("MsgLiquidStake")) {
                    result = c.getString(R.string.tx_crescent_liquid_stake)
                } else if (msgType.contains("MsgLiquidUnstake")) {
                    result = c.getString(R.string.tx_crescent_liquid_unstake)
                }

            } else if (msgType.contains("crescent.") && msgType.contains("liquidity")) {
                if (msgType.contains("MsgCreatePair")) {
                    result = c.getString(R.string.tx_crescent_liquidity_create_pair)
                } else if (msgType.contains("MsgCreatePool")) {
                    result = c.getString(R.string.tx_crescent_liquidity_create_pool)
                } else if (msgType.contains("MsgDeposit")) {
                    result = c.getString(R.string.tx_crescent_liquidity_deposit)
                } else if (msgType.contains("MsgWithdraw")) {
                    result = c.getString(R.string.tx_crescent_liquidity_withdraw)
                } else if (msgType.contains("MsgLimitOrder")) {
                    result = c.getString(R.string.tx_crescent_liquidity_limit_order)
                } else if (msgType.contains("MsgMarketOrder")) {
                    result = c.getString(R.string.tx_crescent_liquidity_market_order)
                } else if (msgType.contains("MsgCancelOrder")) {
                    result = c.getString(R.string.tx_crescent_liquidity_cancel_order)
                } else if (msgType.contains("MsgCancelAllOrders")) {
                    result = c.getString(R.string.tx_crescent_liquidity_cancelall_order)
                }

            } else if (msgType.contains("crescent.") && msgType.contains("farming")) {
                if (msgType.contains("MsgStake")) {
                    result = c.getString(R.string.tx_crescent_farming_stake)
                } else if (msgType.contains("MsgUnstake")) {
                    result = c.getString(R.string.tx_crescent_farming_unstake)
                } else if (msgType.contains("MsgHarvest")) {
                    result = c.getString(R.string.tx_crescent_farming_harvest)
                } else if (msgType.contains("MsgCreateFixedAmountPlan")) {
                    result = c.getString(R.string.tx_crescent_farming_create_fixed_amount_plan)
                } else if (msgType.contains("MsgCreateRatioPlan")) {
                    result = c.getString(R.string.tx_crescent_farming_create_ratio_plan)
                } else if (msgType.contains("MsgRemovePlan")) {
                    result = c.getString(R.string.tx_crescent_farming_remove_plan)
                } else if (msgType.contains("MsgAdvanceEpoch")) {
                    result = c.getString(R.string.tx_crescent_farming_advance_epoch)
                }

            } else if (msgType.contains("crescent.") && msgType.contains("claim")) {
                if (msgType.contains("MsgClaim")) {
                    result = c.getString(R.string.tx_crescent_farming_claim)
                }

            } else if (msgType.contains("irismod.") && msgType.contains("nft")) {
                if (msgType.contains("MsgMintNFT")) {
                    result = c.getString(R.string.tx_mint_nft)
                } else if (msgType.contains("MsgTransferNFT")) {
                    val sender = msgValue["sender"].asString
                    val recipient = msgValue["recipient"].asString
                    result = if (sender.equals(address, ignoreCase = true)) {
                        c.getString(R.string.tx_send_nft)
                    } else if (recipient.equals(address, ignoreCase = true)) {
                        c.getString(R.string.tx_receive_nft)
                    } else {
                        c.getString(R.string.tx_transfer_nft)
                    }
                } else if (msgType.contains("MsgEditNFT")) {
                    result = "NFT Edit"
                } else if (msgType.contains("MsgIssueDenom")) {
                    result = c.getString(R.string.tx_issue_denom)
                } else if (msgType.contains("MsgRequestRandom")) {
                    result = "Random Request"
                }

            } else if (msgType.contains("irismod") && msgType.contains("coinswap")) {
                if (msgType.contains("MsgSwapOrder")) {
                    result = c.getString(R.string.tx_osmosis_coin_swap)
                } else if (msgType.contains("MsgAddLiquidity")) {
                    result = c.getString(R.string.tx_osmosis_join_pool)
                } else if (msgType.contains("MsgRemoveLiquidity")) {
                    result = c.getString(R.string.tx_osmosis_exit_pool)
                }

            } else if (msgType.contains("irismod") && msgType.contains("farm")) {
                if (msgType.contains("MsgStake")) {
                    result = c.getString(R.string.tx_irismod_farm_stake)
                } else if (msgType.contains("MsgHarvest")) {
                    result = c.getString(R.string.tx_irismod_farm_harvest)
                }

            } else if (msgType.contains("chainmain.") && msgType.contains("nft")) {
                if (msgType.contains("MsgMintNFT")) {
                    result = c.getString(R.string.tx_mint_nft)
                } else if (msgType.contains("MsgTransferNFT")) {
                    val sender = msgValue["sender"].asString
                    val recipient = msgValue["recipient"].asString
                    result = if (sender.equals(address, ignoreCase = true)) {
                        c.getString(R.string.tx_send_nft)
                    } else if (recipient.equals(address, ignoreCase = true)) {
                        c.getString(R.string.tx_receive_nft)
                    } else {
                        c.getString(R.string.tx_transfer_nft)
                    }
                } else if (msgType.contains("MsgEditNFT")) {
                    result = "NFT Edit"
                } else if (msgType.contains("MsgIssueDenom")) {
                    result = c.getString(R.string.tx_issue_denom)
                } else if (msgType.contains("MsgRequestRandom")) {
                    result = "Random Request"
                }

            } else if (msgType.contains("osmosis.") && msgType.contains("gamm") || msgType.contains(
                    "osmosis."
                ) && msgType.contains("poolmanager")
            ) {
                if (msgType.contains("MsgSwapExactAmountIn")) {
                    result = c.getString(R.string.tx_osmosis_coin_swap)
                } else if (msgType.contains("MsgSwapExactAmountOut")) {
                    result = c.getString(R.string.tx_osmosis_coin_swap)
                } else if (msgType.contains("MsgJoinPool")) {
                    result = c.getString(R.string.tx_osmosis_join_pool)
                } else if (msgType.contains("MsgExitPool")) {
                    result = c.getString(R.string.tx_osmosis_exit_pool)
                } else if (msgType.contains("MsgJoinSwapExternAmountIn")) {
                    result = c.getString(R.string.tx_osmosis_coin_swap)
                } else if (msgType.contains("MsgJoinSwapShareAmountOut")) {
                    result = c.getString(R.string.tx_osmosis_coin_swap)
                } else if (msgType.contains("MsgExitSwapExternAmountOut")) {
                    result = c.getString(R.string.tx_osmosis_coin_swap)
                } else if (msgType.contains("MsgExitSwapShareAmountIn")) {
                    result = c.getString(R.string.tx_osmosis_coin_swap)
                } else if (msgType.contains("MsgCreatePool")) {
                    result = c.getString(R.string.tx_osmosis_create_pool)
                } else if (msgType.contains("MsgCreateBalancerPool")) {
                    result = c.getString(R.string.tx_osmosis_create_pool)
                }

            } else if (msgType.contains("osmosis.") && msgType.contains("lockup")) {
                if (msgType.contains("MsgLockTokens")) {
                    result = c.getString(R.string.tx_osmosis_token_lockup)
                } else if (msgType.contains("MsgBeginUnlockingAll")) {
                    result = c.getString(R.string.tx_osmosis_begin_token_unlock_all)
                } else if (msgType.contains("MsgBeginUnlocking")) {
                    result = c.getString(R.string.tx_osmosis_begin_token_unlock)
                }

            } else if (msgType.contains("osmosis.") && msgType.contains("superfluid")) {
                if (msgType.contains("MsgSuperfluidDelegate")) {
                    result = c.getString(R.string.tx_osmosis_super_fluid_delegate)
                } else if (msgType.contains("MsgSuperfluidUndelegate")) {
                    result = c.getString(R.string.tx_osmosis_super_fluid_undelegate)
                } else if (msgType.contains("MsgSuperfluidUnbondLock")) {
                    result = c.getString(R.string.tx_osmosis_super_fluid_unbondinglock)
                } else if (msgType.contains("MsgLockAndSuperfluidDelegate")) {
                    result = c.getString(R.string.tx_osmosis_super_fluid_lockanddelegate)
                }
            } else if (msgType.contains("panacea.") && msgType.contains("aol")) {
                if (msgType.contains("MsgAddRecord")) {
                    result = c.getString(R.string.tx_add_record)
                } else if (msgType.contains("MsgCreateTopic")) {
                    result = c.getString(R.string.tx_create_topic)
                } else if (msgType.contains("MsgAddWriter")) {
                    result = c.getString(R.string.tx_add_writer)
                }

            } else if (msgType.contains("panacea.") && msgType.contains("did")) {
                if (msgType.contains("MsgCreateDID")) {
                    result = c.getString(R.string.tx_create_did)
                }

            } else if (msgType.contains("tendermint.") && msgType.contains("liquidity")) {
                if (msgType.contains("MsgDepositWithinBatch")) {
                    result = c.getString(R.string.tx_osmosis_join_pool)

                } else if (msgType.contains("MsgSwapWithinBatch")) {
                    result = c.getString(R.string.tx_osmosis_coin_swap)

                } else if (msgType.contains("MsgWithdrawWithinBatch")) {
                    result = c.getString(R.string.tx_osmosis_exit_pool)
                }

            } else if (msgType.contains("desmos.") && msgType.contains("profiles")) {
                if (msgType.contains("MsgSaveProfile")) {
                    result = c.getString(R.string.tx_save_profile)
                } else if (msgType.contains("MsgDeleteProfile")) {
                    result = c.getString(R.string.tx_delete_profile)
                } else if (msgType.contains("MsgCreateRelationship")) {
                    result = c.getString(R.string.tx_create_relation)
                } else if (msgType.contains("MsgDeleteRelationship")) {
                    result = c.getString(R.string.tx_delete_relation)
                } else if (msgType.contains("MsgBlockUser")) {
                    result = c.getString(R.string.tx_block_user)
                } else if (msgType.contains("MsgUnblockUser")) {
                    result = c.getString(R.string.tx_unblock_user)
                } else if (msgType.contains("MsgLinkChainAccount")) {
                    result = c.getString(R.string.tx_link_chain_account)
                }

            } else if (msgType.contains("kava.") && msgType.contains("auction")) {
                if (msgType.contains("MsgPlaceBid")) {
                    result = c.getString(R.string.tx_kava_auction_bid)
                }

            } else if (msgType.contains("kava.") && msgType.contains("cdp")) {
                if (msgType.contains("MsgCreateCDP")) {
                    result = c.getString(R.string.tx_kava_create_cdp)
                } else if (msgType.contains("MsgDeposit")) {
                    result = c.getString(R.string.tx_kava_deposit_cdp)
                } else if (msgType.contains("MsgWithdraw")) {
                    result = c.getString(R.string.tx_kava_withdraw_cdp)
                } else if (msgType.contains("MsgDrawDebt")) {
                    result = c.getString(R.string.tx_kava_drawdebt_cdp)
                } else if (msgType.contains("MsgRepayDebt")) {
                    result = c.getString(R.string.tx_kava_repaydebt_cdp)
                } else if (msgType.contains("MsgLiquidate")) {
                    result = c.getString(R.string.tx_kava_liquidate_cdp)
                }

            } else if (msgType.contains("kava.") && msgType.contains("swap")) {
                if (msgType.contains("MsgDeposit")) {
                    result = c.getString(R.string.tx_kava_swap_deposit)
                } else if (msgType.contains("MsgWithdraw")) {
                    result = c.getString(R.string.tx_kava_swap_withdraw)
                } else if (msgType.contains("MsgSwapExactForTokens")) {
                    result = c.getString(R.string.tx_kava_swap_token)
                } else if (msgType.contains("MsgSwapForExactTokens")) {
                    result = c.getString(R.string.tx_kava_swap_token)
                }

            } else if (msgType.contains("kava.") && msgType.contains("hard")) {
                if (msgType.contains("MsgDeposit")) {
                    result = c.getString(R.string.tx_kava_hard_deposit)
                } else if (msgType.contains("MsgWithdraw")) {
                    result = c.getString(R.string.tx_kava_hard_withdraw)
                } else if (msgType.contains("MsgBorrow")) {
                    result = c.getString(R.string.tx_kava_hard_borrow)
                } else if (msgType.contains("MsgRepay")) {
                    result = c.getString(R.string.tx_kava_hard_repay)
                } else if (msgType.contains("MsgLiquidate")) {
                    result = c.getString(R.string.tx_kava_hard_liquidate)
                }

            } else if (msgType.contains("kava.") && msgType.contains("savings")) {
                if (msgType.contains("MsgDeposit")) {
                    result = c.getString(R.string.tx_kava_save_deposit)
                } else if (msgType.contains("MsgWithdraw")) {
                    result = c.getString(R.string.tx_kava_save_withdraw)
                }

            } else if (msgType.contains("kava.") && msgType.contains("incentive")) {
                if (msgType.contains("MsgClaimUSDXMintingReward")) {
                    result = c.getString(R.string.tx_kava_hard_mint_incentive)
                } else if (msgType.contains("MsgClaimHardReward")) {
                    result = c.getString(R.string.tx_kava_hard_hard_incentive)
                } else if (msgType.contains("MsgClaimDelegatorReward")) {
                    result = c.getString(R.string.tx_kava_hard_hard_incentive)
                } else if (msgType.contains("MsgClaimSwapReward")) {
                    result = c.getString(R.string.tx_kava_save_withdraw)
                } else if (msgType.contains("MsgClaimSavingsReward")) {
                    result = c.getString(R.string.tx_kava_hard_save_incentive)
                } else if (msgType.contains("MsgClaimEarnReward")) {
                    result = c.getString(R.string.tx_kava_hard_earn_incentive)
                }

            } else if (msgType.contains("kava.") && msgType.contains("bep3")) {
                if (msgType.contains("MsgCreateAtomicSwap")) {
                    result = c.getString(R.string.tx_kava_bep3_create)
                } else if (msgType.contains("MsgClaimAtomicSwap")) {
                    result = c.getString(R.string.tx_kava_bep3_claim)
                } else if (msgType.contains("MsgRefundAtomicSwap")) {
                    result = c.getString(R.string.tx_kava_bep3_refund)
                }

            } else if (msgType.contains("kava.") && msgType.contains("pricefeed")) {
                if (msgType.contains("MsgPostPrice")) {
                    result = c.getString(R.string.tx_post_price)
                }

            } else if (msgType.contains("kava.") && msgType.contains("router")) {
                if (msgType.contains("MsgDelegateMintDeposit")) {
                    result = c.getString(R.string.tx_kava_earn_delegate_deposit)
                } else if (msgType.contains("MsgWithdrawBurn")) {
                    result = c.getString(R.string.tx_kava_earn_withdraw)
                } else if (msgType.contains("MsgMintDeposit")) {
                    result = c.getString(R.string.tx_kava_earn_deposit)
                }

            } else if (msgType.contains("axelar.") && msgType.contains("reward")) {
                if (msgType.contains("RefundMsgRequest")) {
                    result = c.getString(R.string.tx_axelar_refund_msg_request)
                }

            } else if (msgType.contains("axelar.") && msgType.contains("axelarnet")) {
                if (msgType.contains("LinkRequest")) {
                    result = c.getString(R.string.tx_axelar_link_request)
                } else if (msgType.contains("ConfirmDepositRequest")) {
                    result = c.getString(R.string.tx_axelar_confirm_deposit_request)
                } else if (msgType.contains("RouteIBCTransfersRequest")) {
                    result = c.getString(R.string.tx_axelar_route_ibc_request)
                } else if (msgType.contains("ExecutePendingTransfersRequest")) {
                } else if (msgType.contains("RegisterIBCPathRequest")) {
                } else if (msgType.contains("AddCosmosBasedChainRequest")) {
                } else if (msgType.contains("RegisterAssetRequest")) {
                } else if (msgType.contains("RegisterFeeCollectorRequest")) {
                } else if (msgType.contains("RetryIBCTransferRequest")) {
                }

            } else if (msgType.contains("injective.") && msgType.contains("exchange")) {
                if (msgType.contains("MsgBatchUpdateOrders")) {
                    result = c.getString(R.string.tx_injective_batch_update_order)
                } else if (msgType.contains("MsgBatchCreateDerivativeLimitOrders") || msgType.contains(
                        "MsgCreateDerivativeLimitOrder"
                    )
                ) {
                    result = c.getString(R.string.tx_injective_create_limit_order)
                } else if (msgType.contains("MsgBatchCreateSpotLimitOrders") || msgType.contains(
                        "MsgCreateSpotLimitOrder"
                    )
                ) {
                    result = c.getString(R.string.tx_injective_create_spot_order)
                } else if (msgType.contains("MsgBatchCancelDerivativeOrders") || msgType.contains(
                        "MsgCancelDerivativeOrder"
                    )
                ) {
                    result = c.getString(R.string.tx_injective_cancel_limit_order)
                } else if (msgType.contains("MsgBatchCancelSpotOrder") || msgType.contains("MsgCancelSpotOrder")) {
                    result = c.getString(R.string.tx_injective_cancel_spot_order)
                }

            } else if (msgType.contains("akash.") && msgType.contains("market")) {
                if (msgType.contains("MsgCreateBid")) {
                    result = c.getString(R.string.tx_create_bid)
                } else if (msgType.contains("MsgCloseBid")) {
                    result = c.getString(R.string.tx_close_bid)
                } else if (msgType.contains("MsgCreateLease")) {
                    result = c.getString(R.string.tx_create_lease)
                } else if (msgType.contains("MsgWithdrawLease")) {
                    result = c.getString(R.string.tx_withdraw_lease)
                } else if (msgType.contains("MsgCloseLease")) {
                    result = c.getString(R.string.tx_close_lease)
                }
            } else if (msgType.contains("akash.") && msgType.contains("deployment")) {
                if (msgType.contains("MsgCreateDeployment")) {
                    result = c.getString(R.string.tx_create_deployment)
                } else if (msgType.contains("MsgDepositDeployment")) {
                    result = c.getString(R.string.tx_deposit_deployment)
                } else if (msgType.contains("MsgUpdateDeployment")) {
                    result = c.getString(R.string.tx_update_deployment)
                } else if (msgType.contains("MsgCloseDeployment")) {
                    result = c.getString(R.string.tx_close_deployment)
                } else if (msgType.contains("MsgCloseGroup")) {
                    result = c.getString(R.string.tx_close_group)
                } else if (msgType.contains("MsgPauseGroup")) {
                    result = c.getString(R.string.tx_pause_group)
                } else if (msgType.contains("MsgStartGroup")) {
                    result = c.getString(R.string.tx_start_group)
                }

            } else if (msgType.contains("akash.") && msgType.contains("cert")) {
                if (msgType.contains("MsgCreateCertificate")) {
                    result = c.getString(R.string.tx_create_certificate)
                } else if (msgType.contains("MsgRevokeCertificate")) {
                    result = c.getString(R.string.tx_revoke_certificate)
                }

            } else if (msgType.contains("shentu.") && msgType.contains("oracle")) {
                if (msgType.contains("MsgTaskResponse")) {
                    result = c.getString(R.string.tx_task_response)
                } else if (msgType.contains("MsgCreateTask")) {
                    result = c.getString(R.string.tx_create_task)
                }

            } else if (msgType.contains("pstake.") && msgType.contains("lscosmos")) {
                if (msgType.contains("MsgLiquidStake")) {
                    result = c.getString(R.string.tx_persis_liquid_stake)
                } else if (msgType.contains("MsgLiquidUnstake")) {
                    result = c.getString(R.string.tx_persis_liquid_unstake)
                } else if (msgType.contains("MsgRedeem")) {
                    result = c.getString(R.string.tx_persis_liquid_redeem)
                } else if (msgType.contains("MsgClaim")) {
                    result = c.getString(R.string.tx_persis_liquid_claim)
                }

            } else if (msgType.contains("ibc.")) {
                if (msgType.contains("MsgTransfer")) {
                    result = c.getString(R.string.tx_ibc_send)
                } else if (msgType.contains("MsgUpdateClient")) {
                    result = c.getString(R.string.tx_ibc_update_client)
                } else if (msgType.contains("v2.MsgSendPacket")) {
                    result = c.getString(R.string.tx_ibc_eureka_send)
                } else if (msgType.contains("MsgAcknowledgement")) {
                    result = c.getString(R.string.tx_ibc_acknowledgement)
                } else if (msgType.contains("v2.MsgRecvPacket")) {
                    result = c.getString(R.string.tx_ibc_eureka_receive)
                } else if (msgType.contains("MsgRecvPacket")) {
                    result = c.getString(R.string.tx_ibc_receive)
                }

                if (getMsgCnt() >= 2) {
                    msg.forEach { secondMsg ->
                        if (secondMsg.asJsonObject["@type"].asString.contains("MsgAcknowledgement")) {
                            result = c.getString(R.string.tx_ibc_acknowledgement)
                        }
                    }
                    msg.forEach { secondMsg ->
                        if (secondMsg.asJsonObject["@type"].asString.contains("MsgRecvPacket")) {
                            result = c.getString(R.string.tx_ibc_receive)
                        }
                    }
                    msg.forEach { secondMsg ->
                        if (secondMsg.asJsonObject["@type"].asString.contains("v2.MsgRecvPacket")) {
                            result = c.getString(R.string.tx_ibc_eureka_receive)
                        }
                    }
                }

            } else if (msgType.contains("cosmwasm.")) {
                if (msgType.contains("MsgStoreCode")) {
                    result = c.getString(R.string.tx_cosmwasm_store_code)
                } else if (msgType.contains("MsgInstantiateContract")) {
                    result = c.getString(R.string.tx_cosmwasm_instantiate)
                } else if (msgType.contains("MsgExecuteContract")) {
                    val msgStringify: JsonElement? = msgValue.get("msg__@stringify")
                    when {
                        msgStringify == null -> {
                            result = c.getString(R.string.tx_cosmwasm_execontract)
                        }

                        msgStringify.isJsonPrimitive && msgStringify.asJsonPrimitive.isString -> {
                            val wasmMsg = msgStringify.asString
                            val wasmFunc = Gson().fromJson(wasmMsg, JsonObject::class.java)
                            val recipient = try {
                                wasmFunc.asJsonObject["transfer"].asJsonObject["recipient"].asString
                            } catch (e: Exception) {
                                null
                            }
                            if (recipient != null) {
                                result = if (recipient.equals(address, true)) {
                                    c.getString(R.string.tx_cosmwasm_token_receive)
                                } else {
                                    c.getString(R.string.tx_cosmwasm_token_send)
                                }

                            } else {
                                val description = if (wasmFunc.entrySet().isNotEmpty()) {
                                    wasmFunc.entrySet().first().key
                                } else {
                                    ""
                                }
                                result = c.getString(R.string.tx_wasm) + "_" + description
                                result = result.split('_').joinToString(" ") { des ->
                                    des.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                                }
                            }
                        }

                        else -> {
                            result = c.getString(R.string.tx_cosmwasm_execontract)
                        }
                    }
                }

            } else if (msgType.contains("ethermint.evm") && msgType.contains("MsgEthereumTx")) {
                result = c.getString(R.string.tx_ethermint_evm)
                msgValue.evmDataValue()?.let { dataValue ->
                    val amount = dataValue.asJsonObject["value"].asString
                    val data = try {
                        dataValue.asJsonObject["data"].asString
                    } catch (e: Exception) {
                        null
                    }
                    if (data == null && amount.isNotEmpty() && amount != "0") {
                        result =
                            if (dataValue.asJsonObject["to"].asString.lowercase() == ByteUtils.convertBech32ToEvm(
                                    address
                                ).lowercase()
                            ) {
                                c.getString(R.string.tx_evm_coin_receive)
                            } else {
                                c.getString(R.string.tx_evm_coin_send)
                            }

                    } else if (data?.isNotEmpty() == true) {
                        Base64.decode(data).toHex().let { hexData ->
                            if (hexData.startsWith("a9059cbb")) {
                                result = if (hexData.lowercase().contains(
                                        ByteUtils.convertBech32ToEvm(address).replace("0x", "")
                                            .lowercase()
                                    )
                                ) {
                                    c.getString(R.string.tx_evm_token_receive)
                                } else {
                                    c.getString(R.string.tx_evm_token_send)
                                }
                            }
                        }
                    }
                }
            }

            if (getMsgCnt() > 1) {
                result = result + " + " + (getMsgCnt() - 1)
            }
            if (result.contains("/")) {
                result = result.split("/").last()
            }
            return result
        }
        return c.getString(R.string.tx_unknown)
    }

    fun getDpCoin(chain: BaseChain): MutableList<CoinProto.Coin> {
        val result = mutableListOf<CoinProto.Coin>()
        val json = Gson().toJson(data?.logs)
        val jsonArray = Gson().fromJson(json, JsonArray::class.java)

        if (getMsgCnt() > 0) {
            var allReward = true
            getMsgs()?.let { msgs ->
                msgs.forEach { msg ->
                    var msgType = ""
                    try {
                        msgType = msg.asJsonObject["@type"].asString
                    } catch (_: Exception) {
                    }
                    try {
                        msgType = msg.asJsonObject["type"].asString
                    } catch (_: Exception) {
                    }

                    if (!msgType.contains("MsgWithdrawDelegatorReward")) {
                        allReward = false
                    }
                }
                if (allReward) {
                    jsonArray.forEach { log ->
                        log.asJsonObject["events"].asJsonArray.firstOrNull { it.asJsonObject["type"].asString == "transfer" }
                            ?.let { event ->
                                event.asJsonObject["attributes"].asJsonArray.firstOrNull { it.asJsonObject["key"].asString == "amount" }
                                    ?.let { attribute ->
                                        val value = attribute.asJsonObject["value"].asString
                                        value.split(",").forEach { rawCoin ->
                                            val p = Pattern.compile("([0-9])+")
                                            val m = p.matcher(rawCoin)
                                            if (m.find()) {
                                                val amount = m.group()
                                                val denom: String = rawCoin.substring(m.end())
                                                val coin =
                                                    CoinProto.Coin.newBuilder().setDenom(denom)
                                                        .setAmount(amount).build()
                                                result.add(coin)
                                            }
                                        }
                                    }
                            }
                    }
                    return sortedCoins(chain, result)
                }

                var ibcReceived = false
                msgs.forEach { msg ->
                    var msgType = ""
                    try {
                        msgType = msg.asJsonObject["@type"].asString
                    } catch (_: Exception) {
                    }
                    try {
                        msgType = msg.asJsonObject["type"].asString
                    } catch (_: Exception) {
                    }

                    if (msgType.contains("ibc") && msgType.contains(
                            "MsgRecvPacket"
                        )
                    ) {
                        ibcReceived = true
                    }
                }
                if (ibcReceived) {
                    try {
                        jsonArray.forEach { log ->
                            log.asJsonObject["events"].asJsonArray.firstOrNull { it.asJsonObject["type"].asString == "transfer" }
                                ?.let { event ->
                                    event.asJsonObject["attributes"].asJsonArray.forEach { attribute ->
                                        if (attribute.asJsonObject["value"].asString == chain.address) {
                                            event.asJsonObject["attributes"].asJsonArray.firstOrNull { it.asJsonObject["key"].asString == "amount" }
                                                ?.let { attributeFiltered ->
                                                    attributeFiltered.asJsonObject["value"].asString?.let { rawAmount ->
                                                        rawAmount.split(",").forEach { rawCoin ->
                                                            val p = Pattern.compile("([0-9])+")
                                                            val m = p.matcher(rawCoin)
                                                            if (m.find()) {
                                                                val amount = m.group()
                                                                val denom: String =
                                                                    rawCoin.substring(m.end())
                                                                val coin =
                                                                    CoinProto.Coin.newBuilder()
                                                                        .setDenom(denom)
                                                                        .setAmount(amount)
                                                                        .build()
                                                                result.add(coin)
                                                            }
                                                        }
                                                    }
                                                }
                                        }
                                    }
                                }
                        }
                        return sortedCoins(chain, result)

                    } catch (e: Exception) {

                    }
                }

                var ibcEurekaSend = false
                msgs.forEach { msg ->
                    var msgEurekaType = ""
                    try {
                        msgEurekaType = msg.asJsonObject["@type"].asString
                    } catch (_: Exception) {
                    }
                    try {
                        msgEurekaType = msg.asJsonObject["type"].asString
                    } catch (_: Exception) {
                    }

                    if (msgEurekaType.contains("ibc") && msgEurekaType.contains(
                            "v2.MsgSendPacket"
                        )
                    ) {
                        ibcEurekaSend = true
                    }
                }

                if (ibcEurekaSend) {
                    try {
                        jsonArray.forEach { log ->
                            log.asJsonObject["events"].asJsonArray.firstOrNull { it.asJsonObject["type"].asString == "ibc_transfer" }
                                ?.let { event ->
                                    event.asJsonObject["attributes"].asJsonArray.forEach { attribute ->
                                        if (attribute.asJsonObject["value"].asString == chain.address) {
                                            val amount =
                                                event.asJsonObject["attributes"].asJsonArray.firstOrNull { it.asJsonObject["key"].asString == "amount" }?.asJsonObject?.get(
                                                    "value"
                                                )?.asString
                                            val denom =
                                                event.asJsonObject["attributes"].asJsonArray.firstOrNull { it.asJsonObject["key"].asString == "denom" }?.asJsonObject?.get(
                                                    "value"
                                                )?.asString
                                            val coin = CoinProto.Coin.newBuilder().setDenom(denom)
                                                .setAmount(amount).build()
                                            result.add(coin)
                                        }
                                    }
                                }
                        }
                        return sortedCoins(chain, result)

                    } catch (e: Exception) {

                    }
                }

                var allSend = true
                msgs.forEach { msg ->
                    val msgType = try {
                        msg.asJsonObject["@type"].asString
                    } catch (e: Exception) {
                        null
                    }
                    if (msgType != null && !msgType.contains("MsgSend")) {
                        allSend = false
                    }
                }

                if (allSend) {
                    var totalAmount: BigDecimal = BigDecimal.ZERO
                    var denom = ""
                    for (msg in msgs) {
                        val msgType = try {
                            msg.asJsonObject["@type"].asString
                        } catch (e: Exception) {
                            null
                        }

                        if (msgType != null) {
                            val msgValue = msg.asJsonObject[msgType.replace(".", "-")]
                            try {
                                val senderAddr = msgValue.asJsonObject["from_address"].asString
                                val receiverAddr = msgValue.asJsonObject["to_address"].asString

                                if (senderAddr == chain.address || receiverAddr == chain.address) {
                                    val amount =
                                        msgValue.asJsonObject["amount"].asJsonArray[0].asJsonObject["amount"].asString.toBigDecimal()
                                    totalAmount = totalAmount.add(amount)
                                    denom =
                                        msgValue.asJsonObject["amount"].asJsonArray[0].asJsonObject["denom"].asString
                                }

                            } catch (e: Exception) {

                            }
                        }
                    }
                    val value = CoinProto.Coin.newBuilder().setDenom(denom)
                        .setAmount(totalAmount.toString()).build()
                    result.add(value)
                    return sortedCoins(chain, result)
                }
            }
        }

        if (getMsgCnt() == 2) {
            getMsgs()?.let { msgs ->
                var msgType0 = ""
                var msgType1 = ""
                try {
                    msgType0 = msgs.get(0).asJsonObject["@type"].asString
                } catch (_: Exception) {
                }
                try {
                    msgType0 = msgs.get(0).asJsonObject["type"].asString
                } catch (_: Exception) {
                }
                try {
                    msgType1 = msgs.get(1).asJsonObject["@type"].asString
                } catch (_: Exception) {
                }
                try {
                    msgType1 = msgs.get(1).asJsonObject["type"].asString
                } catch (_: Exception) {
                }

                if (msgType0.contains("MsgWithdrawDelegatorReward") && msgType1.contains("MsgDelegate")) {
                    try {
                        msgs.get(1).asJsonObject[msgType1.replace(".", "-")]?.let { msgValue1 ->
                            val rawAmount = msgValue1.asJsonObject["amount"].asJsonObject
                            CoinProto.Coin.newBuilder().setDenom(rawAmount["denom"].asString)
                                .setAmount(rawAmount["amount"].asString).build()?.let { rawCoin ->
                                    result.add(rawCoin)
                                }
                        }
                        return sortedCoins(chain, result)

                    } catch (e: Exception) {

                    }
                }
            }
        }

        if (getMsgCnt() == 0 || getMsgCnt() > 1) {
            return mutableListOf()
        }

        getMsgs()?.let { msgs ->
            val firstMsg = msgs.get(0)
            var msgType = ""
            try {
                msgType = firstMsg.asJsonObject["@type"].asString
            } catch (_: Exception) {
            }
            try {
                msgType = firstMsg.asJsonObject["type"].asString
            } catch (_: Exception) {
            }
            val msgValue = firstMsg.asJsonObject[msgType.replace(".", "-")].asJsonObject

            if (msgType.contains("MsgSend")) {
                val rawAmounts = try {
                    msgValue["amount"].asJsonArray
                } catch (e: Exception) {
                    null
                }
                if (rawAmounts?.isJsonNull == false) {
                    val coin = CoinProto.Coin.newBuilder()
                        .setDenom(rawAmounts.get(0).asJsonObject["denom"].asString)
                        .setAmount(rawAmounts.get(0).asJsonObject["amount"].asString).build()
                    result.add(coin)
                } else {
                    val rawValue = try {
                        msgValue["value"].asJsonObject
                    } catch (e: Exception) {
                        null
                    }

                    if (rawValue?.isJsonNull == false) {
                        val rawValueAmounts = try {
                            rawValue["amount"].asJsonArray
                        } catch (e: Exception) {
                            null
                        }

                        if (rawValueAmounts?.isJsonNull == false) {
                            val coin = CoinProto.Coin.newBuilder()
                                .setDenom(rawValueAmounts[0].asJsonObject.get("denom").asString)
                                .setAmount(rawValueAmounts[0].asJsonObject.get("amount").asString)
                                .build()
                            result.add(coin)
                        } else {
                            result.add(CoinProto.Coin.newBuilder().build())
                        }
                    } else {
                        result.add(CoinProto.Coin.newBuilder().build())
                    }
                }

            } else if (msgType.contains("MsgDelegate") || msgType.contains("MsgUndelegate") || msgType.contains(
                    "MsgBeginRedelegate"
                ) || msgType.contains("MsgCancelUnbondingDelegation")
            ) {
                val rawAmount = try {
                    msgValue["amount"].asJsonObject
                } catch (e: Exception) {
                    null
                }
                if (rawAmount?.isJsonNull == false) {
                    val coin = CoinProto.Coin.newBuilder().setDenom(rawAmount["denom"].asString)
                        .setAmount(rawAmount["amount"].asString).build()
                    result.add(coin)
                } else {
                    result.add(CoinProto.Coin.newBuilder().build())
                }

            } else if (msgType.contains("ibc") && msgType.contains("MsgTransfer")) {
                val rawAmount = msgValue["token"].asJsonObject
                if (!rawAmount.isJsonNull) {
                    val coin = CoinProto.Coin.newBuilder().setDenom(rawAmount["denom"].asString)
                        .setAmount(rawAmount["amount"].asString).build()
                    result.add(coin)
                } else {
                    result.add(CoinProto.Coin.newBuilder().build())
                }

            } else if (msgType.contains("bank") && msgType.contains("MsgMultiSend")) {
                for (input in msgValue["inputs"].asJsonArray) {
                    if (!input.isJsonNull && chain.address == input.asJsonObject["address"].asString) {
                        val coin = CoinProto.Coin.newBuilder()
                            .setDenom(input.asJsonObject["coins"].asJsonArray[0].asJsonObject["denom"].asString)
                            .setAmount(input.asJsonObject["coins"].asJsonArray[0].asJsonObject["amount"].asString)
                            .build()
                        result.add(coin)
                        break
                    }
                }

                for (output in msgValue["outputs"].asJsonArray) {
                    if (!output.isJsonNull && chain.address == output.asJsonObject["address"].asString) {
                        val coin = CoinProto.Coin.newBuilder()
                            .setDenom(output.asJsonObject["coins"].asJsonArray[0].asJsonObject["denom"].asString)
                            .setAmount(output.asJsonObject["coins"].asJsonArray[0].asJsonObject["amount"].asString)
                            .build()
                        result.add(coin)
                        break
                    }
                }

            } else if (msgType.contains("ethermint.evm") && msgType.contains("MsgEthereumTx")) {
                msgValue.asJsonObject.evmDataValue()?.let { dataValue ->
                    val amount = dataValue.asJsonObject["value"].asString
                    val data = try {
                        dataValue.asJsonObject["data"].asString
                    } catch (e: Exception) {
                        null
                    }
                    if (data == null && amount.isNotEmpty() && amount != "0") {
                        val denom = if (chain.tag == "kava60") {
                            "akava"
                        } else {
                            chain.stakeDenom
                        }
                        val value =
                            CoinProto.Coin.newBuilder().setDenom(denom).setAmount(amount).build()
                        result.add(value)
                    }
                }

            } else {
                return sortedCoins(chain, result)
            }
        }
        return sortedCoins(chain, result)
    }

    fun getDpToken(chain: BaseChain): Pair<Token, BigDecimal>? {
        getMsgs()?.get(0)?.let { firstMsg ->
            firstMsg.asJsonObject["@type"].asString?.let { msgType ->
                val msgValue = firstMsg.asJsonObject[msgType.replace(".", "-")]

                if (msgType.contains("cosmwasm.") && msgType.contains("MsgExecuteContract")) {
                    if (msgValue.asJsonObject["contract"] != null) {
                        val contractAddress = msgValue.asJsonObject["contract"].asString
                        val msgStringify: JsonElement? =
                            msgValue.asJsonObject.get("msg__@stringify")
                        when {
                            msgStringify?.isJsonPrimitive == true && msgStringify.asJsonPrimitive?.isString == true -> {
                                val wasmMsg = msgStringify.asString
                                val wasmFunc = Gson().fromJson(wasmMsg, JsonObject::class.java)
                                val amount = try {
                                    wasmFunc.asJsonObject["transfer"].asJsonObject["amount"].asString
                                } catch (e: Exception) {
                                    null
                                }
                                if (amount != null) {
                                    chain.cosmosFetcher?.tokens?.firstOrNull { it.contract == contractAddress }
                                        ?.let { cw20 ->
                                            return Pair(cw20, amount.toBigDecimal())
                                        }
                                } else {
                                    return null
                                }
                            }

                            else -> {
                                null
                            }
                        }

                    } else {
                        null
                    }

                } else if (msgType.contains("ethermint.evm") && msgType.contains("MsgEthereumTx")) {
                    msgValue.asJsonObject.evmDataValue()?.let { dataValue ->
                        val data = try {
                            dataValue.asJsonObject["data"].asString
                        } catch (e: Exception) {
                            null
                        }
                        if (data != null) {
                            val hexData = Base64.decode(data).toHex()
                            val contractAddress = dataValue.asJsonObject["to"].asString
                            if (hexData.startsWith("a9059cbb")) {
                                chain.evmRpcFetcher?.evmTokens?.firstOrNull { it.contract == contractAddress }
                                    ?.let { erc20 ->
                                        return Pair(erc20, hexData.takeLast(64).hexToBigDecimal())
                                    }
                            }
                        }
                    }

                } else {
                    return null
                }
            }
        }
        return null
    }

    fun getVoteOption(): String {
        var result = ""
        getMsgs()?.let { msgs ->
            val firstMsg = msgs.get(0)
            var msgType = ""
            try {
                msgType = firstMsg.asJsonObject["@type"].asString
            } catch (_: Exception) {
            }
            try {
                msgType = firstMsg.asJsonObject["type"].asString
            } catch (_: Exception) {
            }

            if (msgType.contains("MsgVote")) {
                val msgValue = firstMsg.asJsonObject[msgType.replace(".", "-")].asJsonObject
                when (msgValue["option"].asString) {
                    "VOTE_OPTION_YES", "Yes" -> result = "YES"
                    "VOTE_OPTION_NO", "No" -> result = "NO"
                    "VOTE_OPTION_ABSTAIN", "Abstain" -> result = "ABSTAIN"
                    "VOTE_OPTION_NO_WITH_VETO", "NoWithVeto" -> result = "VETO"
                }
            }
        }
        return result
    }

    private fun sortedCoins(
        chain: BaseChain, inputs: MutableList<CoinProto.Coin>?
    ): MutableList<CoinProto.Coin> {
        var sorted = mutableListOf<CoinProto.Coin>()

        inputs?.forEach { coin ->
            val index = sorted.indexOfFirst { it.denom == coin.denom }

            if (index != -1) {
                val existing = sorted[index].amount.toBigDecimal()
                val added = existing.add(coin.amount.toBigDecimal())

                val tempSorted = sorted.toMutableList()
                tempSorted[index] =
                    CoinProto.Coin.newBuilder().setDenom(coin.denom).setAmount(added.toString())
                        .build()
                sorted = tempSorted
            } else {
                sorted.add(coin)
            }
        }

        sorted.sortWith { coin1, coin2 ->
            if (coin1.denom == chain.stakeDenom && coin2.denom != chain.stakeDenom) {
                -1
            } else {
                0
            }
        }
        return sorted
    }
}

fun JsonObject.evmDataValue(): JsonObject? {
    val dataType = this.asJsonObject["data"].asJsonObject["@type"].asString
    return this.asJsonObject["data"].asJsonObject[dataType.replace(".", "-")].asJsonObject
}






