package wannabit.io.cosmostaion.data.repository.chain

import com.google.protobuf.ByteString
import retrofit2.Response
import wannabit.io.cosmostaion.chain.BaseChain
import wannabit.io.cosmostaion.data.model.res.CosmosProposal
import wannabit.io.cosmostaion.data.model.res.NetworkResult
import wannabit.io.cosmostaion.data.model.res.OnChainVote
import wannabit.io.cosmostaion.data.model.res.ResDaoVoteStatus
import wannabit.io.cosmostaion.data.model.res.VoteStatus

interface ProposalRepository {

    suspend fun cosmosProposal(chain: String): NetworkResult<Response<MutableList<CosmosProposal>>>

    suspend fun voteStatus(chain: String, account: String?): NetworkResult<Response<VoteStatus>>

    suspend fun onChainProposal(
        chain: BaseChain,
        pageKey: ByteString?
    ): NetworkResult<Pair<MutableList<CosmosProposal>, ByteString?>>

    suspend fun onChainVoteStatus(
        chain: BaseChain,
        proposals: MutableList<CosmosProposal>
    ): NetworkResult<MutableList<OnChainVote>>

    suspend fun daoProposals(chain: BaseChain, contAddress: String?): NetworkResult<String?>

    suspend fun daoVoteStatus(
        chain: String,
        address: String?
    ): NetworkResult<Response<MutableList<ResDaoVoteStatus>>>
}