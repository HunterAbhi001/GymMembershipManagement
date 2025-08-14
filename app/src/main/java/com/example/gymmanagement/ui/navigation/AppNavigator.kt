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
        factory = MainViewModelFactory(application.database.memberDao(), application.database.checkInDao())
    )

    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            val membersExpiringSoon by viewModel.membersExpiringSoon.collectAsState()
            val allMembers by viewModel.allMembers.collectAsState()
            DashboardScreen(
                navController = navController,
                activeMemberCount = allMembers.count { it.expiryDate >= System.currentTimeMillis() },
                membersExpiringSoon = membersExpiringSoon
            )
        }
        composable("members_list") {
            val members by viewModel.allMembers.collectAsState()
            val searchQuery by viewModel.searchQuery.collectAsState()
            MembersListScreen(
                navController = navController,
                members = members,
                searchQuery = searchQuery,
                onSearchQueryChange = { newQuery -> viewModel.onSearchQueryChange(newQuery) }
            )
        }
        composable("expiring_members") {
            val members by viewModel.membersExpiringSoon.collectAsState()
            ExpiringMembersScreen(
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
            val checkIns by viewModel.getCheckInsForMember(memberId).collectAsState(initial = emptyList())

            MemberDetailScreen(
                navController = navController,
                member = member,
                checkIns = checkIns,
                onCheckIn = { viewModel.recordCheckIn(memberId) },
                onDelete = { if(member != null) viewModel.deleteMember(member!!) }
            )
        }
        composable(
            route = "add_edit_member?memberId={memberId}",
            arguments = listOf(navArgument("memberId") {
                type = NavType.IntType
                defaultValue = -1
            })
        ) { backStackEntry ->
            val memberId = backStackEntry.arguments?.getInt("memberId") ?: -1
            val member by viewModel.getMemberById(memberId).collectAsState(initial = null)

            AddEditMemberScreen(
                navController = navController,
                member = if (memberId == -1) null else member,
                onSave = { memberToSave -> viewModel.addOrUpdateMember(memberToSave) }
            )
        }
    }
}