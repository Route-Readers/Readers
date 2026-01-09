package com.route.readers.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.route.readers.data.model.AdminLog
import com.route.readers.data.model.Report
import com.route.readers.data.model.User
import com.route.readers.data.remote.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AdminViewModel : ViewModel() {
    private val adminRepository = AdminRepository()

    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    val reports: StateFlow<List<Report>> = _reports

    private val _adminLogs = MutableStateFlow<List<AdminLog>>(emptyList())
    val adminLogs: StateFlow<List<AdminLog>> = _adminLogs

    private val _userSearchQuery = MutableStateFlow("")
    val userSearchQuery: StateFlow<String> = _userSearchQuery

    private val _userSearchResults = MutableStateFlow<List<User>>(emptyList())
    val userSearchResults: StateFlow<List<User>> = _userSearchResults

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

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

    fun loadAdminLogs() {
        viewModelScope.launch {
            _isLoading.value = true
            _adminLogs.value = adminRepository.getAdminLogs()
            _isLoading.value = false
        }
    }

    fun onUserSearchQueryChanged(query: String) {
        _userSearchQuery.value = query
    }

    fun searchUsers() {
        viewModelScope.launch {
            _isSearching.value = true
            _userSearchResults.value = adminRepository.searchUsers(_userSearchQuery.value)
            _isSearching.value = false
        }
    }

    fun clearUserSearch() {
        _userSearchQuery.value = ""
        _userSearchResults.value = emptyList()
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

    fun warnUser(userId: String, reason: String) {
        viewModelScope.launch {
            adminRepository.warnUser(userId, reason)
            searchUsers()
        }
    }

    fun banUser(userId: String, reason: String, durationDays: Int?) {
        viewModelScope.launch {
            adminRepository.banUser(userId, reason, durationDays)
            searchUsers()
        }
    }

    fun unbanUser(userId: String) {
        viewModelScope.launch {
            adminRepository.unbanUser(userId)
            searchUsers()
        }
    }
}
