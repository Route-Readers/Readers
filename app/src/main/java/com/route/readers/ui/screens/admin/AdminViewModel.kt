package com.route.readers.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.route.readers.data.model.Report
import com.route.readers.data.remote.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AdminViewModel : ViewModel() {
    private val adminRepository = AdminRepository()
    
    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    val reports: StateFlow<List<Report>> = _reports
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadReports()
    }

    fun loadReports() {
        viewModelScope.launch {
            _isLoading.value = true
            _reports.value = adminRepository.getAllReports()
            _isLoading.value = false
        }
    }

    fun resolveReport(reportId: String, actionTaken: String) {
        viewModelScope.launch {
            adminRepository.resolveReport(reportId, actionTaken)
            loadReports()
        }
    }

    fun dismissReport(reportId: String) {
        viewModelScope.launch {
            adminRepository.dismissReport(reportId)
            loadReports()
        }
    }
}
