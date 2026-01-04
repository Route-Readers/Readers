package com.route.readers.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.route.readers.data.model.AdminLog
import com.route.readers.data.model.Report
import com.route.readers.data.model.User
import com.route.readers.data.remote.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminViewModel : ViewModel() {
    private val repository = AdminRepository()

    private val _pendingReports = MutableStateFlow<List<Report>>(emptyList())
    val pendingReports = _pendingReports.asStateFlow()

    private val _adminLogs = MutableStateFlow<List<AdminLog>>(emptyList())
    val adminLogs = _adminLogs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin = _isAdmin.asStateFlow()

    init {
        checkAdminStatus()
    }

    private fun checkAdminStatus() {
        viewModelScope.launch {
            _isAdmin.value = repository.isAdmin()
        }
    }

    fun loadPendingReports() {
        viewModelScope.launch {
            _isLoading.value = true
            _pendingReports.value = repository.getPendingReports()
            _isLoading.value = false
        }
    }

    fun loadAdminLogs() {
        viewModelScope.launch {
            _adminLogs.value = repository.getAdminLogs(100)
        }
    }

    fun searchUsers(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _isSearching.value = true
            _searchResults.value = repository.searchUsers(query)
            _isSearching.value = false
        }
    }

    fun warnUser(userId: String, reason: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.warnUser(userId, reason)
            loadPendingReports()
            loadAdminLogs()
            onComplete()
        }
    }

    fun banUser(userId: String, reason: String, days: Int?, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.banUser(userId, reason, days)
            loadPendingReports()
            loadAdminLogs()
            onComplete()
        }
    }

    fun unbanUser(userId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.unbanUser(userId)
            searchResults.value.find { it.uid == userId }?.let {
                _searchResults.value = _searchResults.value.map { user ->
                    if (user.uid == userId) user.copy(isBanned = false) else user
                }
            }
            loadAdminLogs()
            onComplete()
        }
    }

    fun deleteFeed(feedId: String, reason: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteFeed(feedId, reason)
            loadAdminLogs()
            onComplete()
        }
    }

    fun resolveReport(reportId: String, action: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.resolveReport(reportId, action)
            loadPendingReports()
            loadAdminLogs()
            onComplete()
        }
    }

    fun dismissReport(reportId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.dismissReport(reportId)
            loadPendingReports()
            loadAdminLogs()
            onComplete()
        }
    }
}
