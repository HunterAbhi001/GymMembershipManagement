package com.example.gymmanagement.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.gymmanagement.ui.screens.*
import com.example.gymmanagement.viewmodel.MainViewModel
import com.example.gymmanagement.viewmodel.MainViewModelFactory

@Composable
fun AppNavigator(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel(factory = MainViewModelFactory())

    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            val allMembers by viewModel.allMembers.collectAsState()
            val membersExpiringSoon by viewModel.membersExpiringSoon.collectAsState()
            val expiredMembers by viewModel.expiredMembers.collectAsState()
            val todaysRevenue by viewModel.todaysRevenue.collectAsState()
            val totalBalance by viewModel.totalBalance.collectAsState()
            val totalDues by viewModel.totalDues.collectAsState()

            DisposableEffect(Unit) {
                onDispose { viewModel.onSearchQueryChange("") }
            }

            DashboardScreen(
                navController = navController,
                allMembers = allMembers,
                membersExpiringSoon = membersExpiringSoon,
                expiredMembers = expiredMembers,
                onDeleteMember = { member -> viewModel.deleteMember(member) },
                todaysRevenue = todaysRevenue,
                totalBalance = totalBalance,
                totalDues = totalDues,
                isDarkTheme = isDarkTheme,
                onThemeToggle = onThemeToggle
            )
        }

        composable("settings_screen") {
            val allPlans by viewModel.allPlans.collectAsState()
            SettingsScreen(
                navController = navController,
                plans = allPlans,
                onSavePlan = { plan -> viewModel.savePlanPrice(plan) }
            )
        }

        composable("todays_revenue") {
            val todaysMembers by viewModel.todaysRevenueMembers.collectAsState()
            TodaysRevenueScreen(
                navController = navController,
                todaysMembers = todaysMembers
            )
        }

        composable("dues_advance") {
            val membersWithDues by viewModel.membersWithDues.collectAsState()
            DuesScreen(
                navController = navController,
                membersWithDues = membersWithDues,
                onUpdateDues = { member, amountPaid -> viewModel.updateDueAdvance(member, amountPaid) }
            )
        }

        composable("collections") {
            val filteredMembers by viewModel.filteredCollection.collectAsState()
            CollectionScreen(
                navController = navController,
                filteredMembers = filteredMembers,
                onDateFilterChange = { filter, start, end -> viewModel.onCollectionDateFilterChange(filter, start, end) }
            )
        }

        composable("analytics") {
            val monthlySignups by viewModel.monthlySignups.collectAsState()
            val planPopularity by viewModel.planPopularity.collectAsState()
            val activeMembers by viewModel.activeMembers.collectAsState()
            val expiredMembers by viewModel.expiredMembers.collectAsState()

            AnalyticsScreen(
                navController = navController,
                monthlySignups = monthlySignups,
                planPopularity = planPopularity,
                activeCount = activeMembers.size,
                expiredCount = expiredMembers.size
            )
        }

        composable("search_members") {
            val searchQuery by viewModel.searchQuery.collectAsState()
            val searchedMembers by viewModel.allMembers.collectAsState()

            DisposableEffect(Unit) {
                onDispose { viewModel.onSearchQueryChange("") }
            }

            SearchScreen(
                navController = navController,
                searchedMembers = searchedMembers,
                searchQuery = searchQuery,
                onSearchQueryChange = { query -> viewModel.onSearchQueryChange(query) }
            )
        }

        composable("all_members_list") {
            val allMembers by viewModel.allMembers.collectAsState()
            AllMembersListScreen(
                navController = navController,
                allMembers = allMembers,
                viewModel = viewModel
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
                members = members,
                onDeleteMember = { member -> viewModel.deleteMember(member) }
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
            arguments = listOf(navArgument("memberId") { type = NavType.StringType })
        ) { backStackEntry ->
            val memberId = backStackEntry.arguments?.getString("memberId") ?: ""
            val allMembers by viewModel.allMembers.collectAsState()
            val member = allMembers.find { it.idString == memberId }

            MemberDetailScreen(
                navController = navController,
                member = member,
                onDelete = { if (member != null) viewModel.deleteMember(member) },
                viewModel = viewModel
            )
        }

        composable(
            route = "add_edit_member?memberId={memberId}&isRenewal={isRenewal}",
            arguments = listOf(
                navArgument("memberId") {
                    type = NavType.StringType
                    defaultValue = "new_member"
                },
                navArgument("isRenewal") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val memberId = backStackEntry.arguments?.getString("memberId") ?: "new_member"
            val isRenewal = backStackEntry.arguments?.getBoolean("isRenewal") ?: false
            val allMembers by viewModel.allMembers.collectAsState()
            val allPlans by viewModel.allPlans.collectAsState()

            val member = if (memberId == "new_member") {
                null
            } else {
                allMembers.find { it.idString == memberId }
            }

            val context = LocalContext.current
            AddEditMemberScreen(
                navController = navController,
                member = member,
                onSave = { memberToSave, photoUri ->
                    viewModel.addOrUpdateMember(memberToSave, photoUri, context, isRenewal)
                },
                isRenewal = isRenewal,
                plans = allPlans,
                isDarkTheme = isDarkTheme
            )
        }
    }
}
