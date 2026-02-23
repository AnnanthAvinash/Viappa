package avinash.app.audiocallapp.presentation.call

import androidx.lifecycle.ViewModel
import avinash.app.audiocallapp.call.CallManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    val callManager: CallManager
) : ViewModel()
