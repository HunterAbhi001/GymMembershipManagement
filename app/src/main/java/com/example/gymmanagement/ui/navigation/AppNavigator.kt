package com.example.gymmanagement.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.gymmanagement.GymManagementApplication
import com.example.gymmanagement.ui.screens.*
import com.example.gymmanagement.viewmodel.MainViewModel
import com.example.gymmanagement.viewmodel.MainViewModelFactory

@Composable
fun AppNavigator(application: GymManagementApplication) {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(application.database.memberDao())
    )

    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            val allMembers by viewModel.allMembers.collectAsState()
            val membersExpiringSoon by viewModel.membersExpiringSoon.collectAsState()
            val expiredMembers by viewModel.expiredMembers.collectAsState()
            DashboardScreen(
                navController = navController,
                allMembers = allMembers,
                membersExpiringSoon = membersExpiringSoon,
                expiredMembers = expiredMembers,
                onDeleteMember = { member -> viewModel.deleteMember(member) }
            )
        }

        // --- NEW: Route for the Search Screen ---
        composable("search_members") {
            val searchQuery by viewModel.searchQuery.collectAsState()
            // The `allMembers` flow is already filtered by the search query in your ViewModel
            val searchedMembers by viewModel.allMembers.collectAsState()

            SearchScreen(
                navController = navController,
                searchedMembers = searchedMembers,
                searchQuery = searchQuery,
                onSearchQueryChange = { query -> viewModel.onSearchQueryChange(query) }
            )
        }

        // ... other routes ...

        composable("all_members_list") {
            val allMembers by viewModel.allMembers.collectAsState()
            AllMembersListScreen(
                navController = navController,
                allMembers = allMembers,
                onImport = { uri -> viewModel.importMembersFromCsv(application, uri) },
                onExport = { uri -> viewModel.exportMembersToCsv(application, uri) }
            )
        }

        composable("active_members_list") {
            val members by viewModel.activeMembers.collectAsState()
            ActiveMembersListScreen(
                navController = navController,
                members = members
            )
        }

        composable("expiring_members") {
            val members by viewModel.membersExpiringSoon.collectAsState()
            ExpiringMembersScreen(
                navController = navController,
                members = members
            )
        }

        composable("expired_members_list") {
            val members by viewModel.expiredMembers.collectAsState()
            ExpiredMembersListScreen(
                navController = navController,
                members = members
            )
        }

        composable(
            route = "member_details/{memberId}",
            arguments = listOf(navArgument("memberId") { type = NavType.IntType })
        ) { backStackEntry ->
            val memberId = backStackEntry.arguments?.getInt("memberId") ?: 0
            val member by viewModel.getMemberById(memberId).collectAsState(initial = null)

            MemberDetailScreen(
                navController = navController,
                member = member,
                onDelete = { if (member != null) viewModel.deleteMember(member!!) }
            )
        }

        composable(
            route = "add_edit_member/{memberId}?isRenewal={isRenewal}",
            arguments = listOf(
                navArgument("memberId") {
                    type = NavType.IntType
                    defaultValue = -1
                },
                navArgument("isRenewal") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val memberId = backStackEntry.arguments?.getInt("memberId") ?: -1
            val isRenewal = backStackEntry.arguments?.getBoolean("isRenewal") ?: false

            val member: com.example.gymmanagement.data.database.Member? =
                if (memberId == -1) {
                    null
                } else {
                    val m by viewModel.getMemberById(memberId).collectAsState(initial = null)
                    m
                }

            AddEditMemberScreen(
                navController = navController,
                member = member,
                onSave = { memberToSave -> viewModel.addOrUpdateMember(memberToSave) },
                isRenewal = isRenewal
            )
        }
    }
}
