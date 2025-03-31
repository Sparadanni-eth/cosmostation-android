package wannabit.io.cosmostaion.data.viewmodel.chain

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.protobuf.ByteString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import wannabit.io.cosmostaion.chain.BaseChain
import wannabit.io.cosmostaion.chain.cosmosClass.NEUTRON_MULTI_MODULE
import wannabit.io.cosmostaion.chain.cosmosClass.NEUTRON_OVERRULE_MODULE
import wannabit.io.cosmostaion.chain.cosmosClass.NEUTRON_SINGLE_MODULE
import wannabit.io.cosmostaion.data.model.res.CosmosProposal
import wannabit.io.cosmostaion.data.model.res.NetworkResult
import wannabit.io.cosmostaion.data.model.res.OnChainVote
import wannabit.io.cosmostaion.data.model.res.ProposalData
import wannabit.io.cosmostaion.data.model.res.ResDaoVoteStatus
import wannabit.io.cosmostaion.data.model.res.ResProposalData
import wannabit.io.cosmostaion.data.model.res.VoteStatus
import wannabit.io.cosmostaion.data.repository.chain.ProposalRepository
import wannabit.io.cosmostaion.data.viewmodel.event.SingleLiveEvent

class ProposalViewModel(private val proposalRepository: ProposalRepository) : ViewModel() {

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    private var _proposalResult = MutableLiveData<MutableList<CosmosProposal>?>()
    val proposalResult: LiveData<MutableList<CosmosProposal>?> get() = _proposalResult

    fun proposalList(chain: String) = viewModelScope.launch(Dispatchers.IO) {
        when (val response = proposalRepository.cosmosProposal(chain)) {
            is NetworkResult.Success -> {
                response.data.let { data ->
                    if (data.isSuccessful) {
                        _proposalResult.postValue(data.body())
                    } else {
                        _errorMessage.postValue("Error")
                    }
                }
            }

            is NetworkResult.Error -> {
                _errorMessage.postValue("error type : ${response.errorType}  error message : ${response.errorMessage}")
            }
        }
    }

    fun onChainProposalList(chain: BaseChain) = viewModelScope.launch(Dispatchers.IO) {
        val result: MutableList<CosmosProposal> = mutableListOf()
        var nextKey: ByteString? = null

        do {
            when (val response = proposalRepository.onChainProposal(chain, nextKey)) {
                is NetworkResult.Success -> {
                    nextKey = response.data.second
                    if (nextKey?.isEmpty == true) {
                        result.addAll(response.data.first)
                        nextKey = null
                    } else {
                        result.addAll(response.data.first)
                    }
                }

                is NetworkResult.Error -> {
                    _errorMessage.postValue("error type : ${response.errorType}  error message : ${response.errorMessage}")
                }
            }
        } while (nextKey != null)

        _proposalResult.postValue(result)
    }

    private var _voteStatusResult = MutableLiveData<VoteStatus?>()
    val voteStatusResult: LiveData<VoteStatus?> get() = _voteStatusResult

    fun voteStatus(chain: String, account: String?) = viewModelScope.launch(Dispatchers.IO) {
        when (val response = proposalRepository.voteStatus(chain, account)) {
            is NetworkResult.Success -> {
                response.data.let { data ->
                    if (data.isSuccessful) {
                        _voteStatusResult.postValue(data.body())
                    } else {
                        _errorMessage.postValue("Error")
                    }
                }
            }

            is NetworkResult.Error -> {
                _errorMessage.postValue("error type : ${response.errorType}  error message : ${response.errorMessage}")
            }
        }
    }

    private var _onChainvoteStatusResult = MutableLiveData<MutableList<OnChainVote>?>()
    val onChainvoteStatusResult: LiveData<MutableList<OnChainVote>?> get() = _onChainvoteStatusResult

    fun onChainVoteStatus(chain: BaseChain, proposals: MutableList<CosmosProposal>) =
        viewModelScope.launch(Dispatchers.IO) {
            when (val response = proposalRepository.onChainVoteStatus(chain, proposals)) {
                is NetworkResult.Success -> {
                    _onChainvoteStatusResult.postValue(response.data)
                }

                is NetworkResult.Error -> {
                    _errorMessage.postValue("error type : ${response.errorType}  error message : ${response.errorMessage}")
                }
            }
        }

    private var _daoSingleProposalsResult = MutableLiveData<MutableList<ProposalData?>>()
    val daoSingleProposalsResult: LiveData<MutableList<ProposalData?>> get() = _daoSingleProposalsResult

    private var _daoMultipleProposalsResult = MutableLiveData<MutableList<ProposalData?>>()
    val daoMultipleProposalsResult: LiveData<MutableList<ProposalData?>> get() = _daoMultipleProposalsResult

    private var _daoOverruleProposalsResult = MutableLiveData<MutableList<ProposalData?>>()
    val daoOverruleProposalsResult: LiveData<MutableList<ProposalData?>> get() = _daoOverruleProposalsResult

    fun daoProposals(chain: BaseChain, contAddress: String, type: Int) =
        viewModelScope.launch(Dispatchers.IO) {
            when (val response = proposalRepository.daoProposals(chain, contAddress)) {
                is NetworkResult.Success -> {
                    when (type) {
                        NEUTRON_SINGLE_MODULE -> {
                            _daoSingleProposalsResult.postValue(
                                Gson().fromJson(
                                    response.data, ResProposalData::class.java
                                )?.proposals
                            )
                        }

                        NEUTRON_MULTI_MODULE -> {
                            _daoMultipleProposalsResult.postValue(
                                Gson().fromJson(
                                    response.data, ResProposalData::class.java
                                )?.proposals
                            )
                        }

                        NEUTRON_OVERRULE_MODULE -> {
                            _daoOverruleProposalsResult.postValue(
                                Gson().fromJson(
                                    response.data, ResProposalData::class.java
                                )?.proposals
                            )
                        }
                    }
                }

                is NetworkResult.Error -> {
                    _errorMessage.postValue("error type : ${response.errorType}  error message : ${response.errorMessage}")
                }
            }
        }

    var voteDaoStatusResult = SingleLiveEvent<MutableList<ResDaoVoteStatus>?>()
    fun daoMyVoteStatus(chain: String, address: String?) = viewModelScope.launch(Dispatchers.IO) {
        when (val response = proposalRepository.daoVoteStatus(chain, address)) {
            is NetworkResult.Success -> {
                response.data.let { data ->
                    if (data.isSuccessful) {
                        voteDaoStatusResult.postValue(data.body())
                    } else {
                        _errorMessage.postValue("Error")
                    }
                }
            }

            is NetworkResult.Error -> {
                _errorMessage.postValue("error type : ${response.errorType}  error message : ${response.errorMessage}")
            }
        }
    }
}