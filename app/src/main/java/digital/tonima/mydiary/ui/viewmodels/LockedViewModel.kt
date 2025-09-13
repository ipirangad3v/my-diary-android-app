package digital.tonima.mydiary.ui.viewmodels

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import digital.tonima.mydiary.delegates.ProUserProvider
import javax.inject.Inject

@HiltViewModel
class LockedViewModel @Inject constructor(
    proUserProvider: ProUserProvider
) : ViewModel() , ProUserProvider by proUserProvider
