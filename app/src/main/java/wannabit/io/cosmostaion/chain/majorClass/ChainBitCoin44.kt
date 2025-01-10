package wannabit.io.cosmostaion.chain.majorClass

import android.os.Parcelable
import com.google.common.collect.ImmutableList
import kotlinx.parcelize.Parcelize
import org.bitcoinj.crypto.ChildNumber
import wannabit.io.cosmostaion.R
import wannabit.io.cosmostaion.chain.AccountKeyType
import wannabit.io.cosmostaion.chain.PubKeyType
import wannabit.io.cosmostaion.common.BaseKey

@Parcelize
class ChainBitCoin44 : ChainBitCoin84(), Parcelable {

    override var name: String = "Bitcoin"
    override var tag: String = "bitcoin44"
    override var logo: Int = R.drawable.chain_bitcoin
    override var isDefault: Boolean = false
    override var apiName: String = "bitcoin"

    override var accountKeyType = AccountKeyType(PubKeyType.BTC_LEGACY, "m/44'/0'/0'/0/X")
    override var setParentPath: List<ChildNumber> = ImmutableList.of(
        ChildNumber(44, true), ChildNumber(0, true), ChildNumber.ZERO_HARDENED, ChildNumber.ZERO
    )

    override var coinSymbol: String = "BTC"
    override var coinGeckoId: String = "bitcoin"
    override var coinLogo: Int = R.drawable.token_btc

    override var mainUrl: String = "https://rpc-office.cosmostation.io/bitcoin-mainnet"

    override fun setInfoWithPrivateKey(privateKey: ByteArray?) {
        this.privateKey = privateKey
        publicKey = BaseKey.getPubKeyFromPKey(privateKey, accountKeyType.pubkeyType)
        mainAddress = BaseKey.getAddressFromPubKey(publicKey, accountKeyType.pubkeyType, bech32PrefixPattern, pubKeyHash, scriptHash)
    }
}