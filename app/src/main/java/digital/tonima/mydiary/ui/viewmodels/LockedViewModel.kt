package digital.tonima.mydiary.ui.viewmodels

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import digital.tonima.mydiary.billing.BillingManager
import javax.inject.Inject

@HiltViewModel
class LockedViewModel @Inject constructor(
    billingManager: BillingManager
) : ViewModel() {
    val isProUser = billingManager.isProUser

}
