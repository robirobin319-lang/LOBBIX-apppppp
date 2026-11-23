package com.example.ui.navigation

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.data.LobbixRepository
import com.example.data.authStateFlow
import com.example.data.signOut
import com.example.model.Tournament
import com.example.model.UiState
import com.example.notifications.MatchReminderScheduler
import com.example.ui.components.LobbixTopBar
import com.example.ui.components.TournamentPreviewBottomSheet
import com.example.ui.screens.AuthFlowScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.ModesScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SupportScreen
import com.example.ui.screens.TournamentDetailScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.CrimsonTint
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.MatteBlack
import com.example.ui.viewmodel.LobbixViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

object LobbixRoutes {
    const val HOME = "home"
    const val MODES = "modes"
    const val LEADERBOARD = "leaderboard"
    const val SUPPORT = "support"
    const val PROFILE = "profile"
    const val WALLET = "wallet"
    const val NOTIFICATIONS = "notifications"
    const val TOURNAMENT = "tournament/{tournamentId}"

    fun tournamentRoute(tournamentId: String) = "tournament/$tournamentId"
}

private data class TopLevelTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val title: String
)

private val TOP_LEVEL_TABS = listOf(
    TopLevelTab(LobbixRoutes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home, "LOBBIX"),
    TopLevelTab(LobbixRoutes.MODES, "Modes", Icons.Filled.SportsEsports, Icons.Outlined.SportsEsports, "Game Modes"),
    TopLevelTab(LobbixRoutes.LEADERBOARD, "Ranks", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents, "Leaderboard"),
    TopLevelTab(LobbixRoutes.SUPPORT, "Support", Icons.Filled.SupportAgent, Icons.Outlined.SupportAgent, "Support Desk"),
    TopLevelTab(LobbixRoutes.PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person, "Player Profile")
)

@Composable
fun LobbixRoot(
    auth: FirebaseAuth = Firebase.auth,
    deepLinkTournamentId: String? = null,
    onConsumeDeepLink: () -> Unit = {}
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var pendingFullName by remember { mutableStateOf<String?>(null) }
    var pendingPhone by remember { mutableStateOf<String?>(null) }

    val user = currentUser
    if (user == null) {
        AuthFlowScreen(
            onAuthSuccess = { name, phone ->
                pendingFullName = name
                pendingPhone = phone
            }
        )
    } else {
        val uid = user.uid
        key(uid) {
            AuthenticatedLobbixApp(
                currentUserId = uid,
                pendingFullName = pendingFullName,
                pendingPhone = pendingPhone,
                deepLinkTournamentId = deepLinkTournamentId,
                onConsumeDeepLink = onConsumeDeepLink
            )
        }
    }
}

@Composable
private fun AuthenticatedLobbixApp(
    currentUserId: String,
    pendingFullName: String?,
    pendingPhone: String?,
    deepLinkTournamentId: String? = null,
    onConsumeDeepLink: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: LobbixViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                LobbixViewModel(
                    appContext = app,
                    repository = LobbixRepository(db),
                    currentUserId = currentUserId,
                    pendingFullName = pendingFullName,
                    pendingPhone = pendingPhone
                )
            }
        }
    )

    val profileState by viewModel.playerProfile.collectAsStateWithLifecycle()
    val paymentConfigState by viewModel.paymentConfig.collectAsStateWithLifecycle()
    val depositRequestsState by viewModel.depositRequests.collectAsStateWithLifecycle()
    val withdrawalRequestsState by viewModel.withdrawalRequests.collectAsStateWithLifecycle()
    val tournamentsState by viewModel.tournaments.collectAsStateWithLifecycle()
    val joinedState by viewModel.joinedTournaments.collectAsStateWithLifecycle()
    val leaderboardState by viewModel.leaderboard.collectAsStateWithLifecycle()
    val transactionsState by viewModel.transactions.collectAsStateWithLifecycle()
    val notificationsState by viewModel.notifications.collectAsStateWithLifecycle()
    val ticketsState by viewModel.tickets.collectAsStateWithLifecycle()
    val selectedTournamentState by viewModel.selectedTournament.collectAsStateWithLifecycle()
    val isJoinedSelected by viewModel.isJoinedSelectedTournament.collectAsStateWithLifecycle()
    val roomCredsState by viewModel.selectedRoomCredentials.collectAsStateWithLifecycle()
    val ticketMessagesState by viewModel.selectedTicketMessages.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    val walletBalance = (profileState as? UiState.Success)?.data?.walletBalance ?: 0L
    val joinedIds = (joinedState as? UiState.Success)?.data.orEmpty().map { it.tournamentId }.toSet()
    val unreadCount = (notificationsState as? UiState.Success)?.data?.count { !it.readStatus } ?: 0
    var previewTournament by remember { mutableStateOf<Tournament?>(null) }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentTopTab = TOP_LEVEL_TABS.firstOrNull { it.route == currentRoute }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionMessage) {
        val msg = actionMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionMessage()
        }
    }

    fun navigateTopTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openTournament(tournamentId: String) {
        viewModel.selectTournament(tournamentId)
        navController.navigate(LobbixRoutes.tournamentRoute(tournamentId))
    }

    LaunchedEffect(deepLinkTournamentId) {
        val targetId = deepLinkTournamentId
        if (!targetId.isNullOrBlank()) {
            openTournament(targetId)
            onConsumeDeepLink()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MatteBlack)
    ) {
        val useNavRail = maxWidth >= 680.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MatteBlack,
            contentWindowInsets = WindowInsets.navigationBars,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                if (currentTopTab != null) {
                    LobbixTopBar(
                        title = currentTopTab.title,
                        walletBalance = walletBalance,
                        unreadNotificationCount = unreadCount,
                        onWalletClick = { navController.navigate(LobbixRoutes.WALLET) },
                        onNotificationsClick = { navController.navigate(LobbixRoutes.NOTIFICATIONS) }
                    )
                }
            },
            bottomBar = {
                if (!useNavRail && currentTopTab != null) {
                    NavigationBar(
                        containerColor = CarbonSurface,
                        contentColor = CrispWhite
                    ) {
                        TOP_LEVEL_TABS.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            letterSpacing = 0.sp
                                        ),
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Visible
                                    )
                                },
                                alwaysShowLabel = true,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CrimsonRed,
                                    selectedTextColor = CrispWhite,
                                    unselectedIconColor = CoolSlate,
                                    unselectedTextColor = CoolSlate,
                                    indicatorColor = CrimsonTint
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail && currentTopTab != null) {
                    NavigationRail(
                        containerColor = CarbonSurface,
                        contentColor = CrispWhite,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        TOP_LEVEL_TABS.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = CrimsonRed,
                                    selectedTextColor = CrispWhite,
                                    unselectedIconColor = CoolSlate,
                                    unselectedTextColor = CoolSlate,
                                    indicatorColor = CrimsonTint
                                )
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    NavHost(
                        navController = navController,
                        startDestination = LobbixRoutes.HOME
                    ) {
                        composable(LobbixRoutes.HOME) {
                            HomeScreen(
                                profileState = profileState,
                                tournamentsState = tournamentsState,
                                joinedState = joinedState,
                                onPreviewTournament = { previewTournament = it },
                                onOpenTournament = ::openTournament,
                                onOpenModes = { navigateTopTab(LobbixRoutes.MODES) },
                                onOpenProfile = { navigateTopTab(LobbixRoutes.PROFILE) },
                                onOpenSupport = { navigateTopTab(LobbixRoutes.SUPPORT) }
                            )
                        }

                        composable(LobbixRoutes.MODES) {
                            ModesScreen(
                                tournamentsState = tournamentsState,
                                joinedState = joinedState,
                                onPreviewTournament = { previewTournament = it }
                            )
                        }

                        composable(LobbixRoutes.LEADERBOARD) {
                            LeaderboardScreen(leaderboardState = leaderboardState)
                        }

                        composable(LobbixRoutes.SUPPORT) {
                            SupportScreen(
                                ticketsState = ticketsState,
                                selectedTicketMessagesState = ticketMessagesState,
                                isBusy = isBusy,
                                onSelectTicket = viewModel::selectTicket,
                                onCreateTicket = viewModel::createSupportTicket,
                                onSendMessage = viewModel::sendSupportMessage,
                                onResolveTicket = viewModel::resolveTicket
                            )
                        }

                        composable(LobbixRoutes.PROFILE) {
                            ProfileScreen(
                                profileState = profileState,
                                joinedState = joinedState,
                                isBusy = isBusy,
                                onUpdateProfile = viewModel::updateProfile,
                                onOpenTournament = ::openTournament,
                                onOpenSupport = { navigateTopTab(LobbixRoutes.SUPPORT) },
                                onOpenNotifications = { navController.navigate(LobbixRoutes.NOTIFICATIONS) },
                                onSignOut = {
                                    MatchReminderScheduler.cancelAllReminders(context)
                                    signOut(credentialManager = credentialManager, scope = scope)
                                }
                            )
                        }

                        composable(
                            route = LobbixRoutes.TOURNAMENT,
                            arguments = listOf(navArgument("tournamentId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val tid = backStackEntry.arguments?.getString("tournamentId").orEmpty()
                            LaunchedEffect(tid) {
                                if (tid.isNotBlank()) viewModel.selectTournament(tid)
                            }
                            TournamentDetailScreen(
                                tournamentState = selectedTournamentState,
                                profile = (profileState as? UiState.Success)?.data,
                                isJoined = isJoinedSelected,
                                roomCredentialsState = roomCredsState,
                                isBusy = isBusy,
                                onBack = { navController.popBackStack() },
                                onJoinClick = viewModel::joinTournament,
                                onSendRoomAlertNow = viewModel::sendRoomReminderNotificationNow,
                                onOpenSupport = { navigateTopTab(LobbixRoutes.SUPPORT) }
                            )
                        }

                        composable(LobbixRoutes.WALLET) {
                            WalletScreen(
                                profileState = profileState,
                                paymentConfigState = paymentConfigState,
                                depositRequestsState = depositRequestsState,
                                withdrawalRequestsState = withdrawalRequestsState,
                                transactionsState = transactionsState,
                                isBusy = isBusy,
                                onBack = { navController.popBackStack() },
                                onSubmitDeposit = viewModel::submitDepositRequest,
                                onSubmitWithdrawal = viewModel::submitWithdrawalRequest
                            )
                        }

                        composable(LobbixRoutes.NOTIFICATIONS) {
                            NotificationsScreen(
                                notificationsState = notificationsState,
                                onMarkRead = viewModel::markNotificationRead,
                                onOpenTournament = { tid ->
                                    openTournament(tid)
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }

        val activePreview = previewTournament
        if (activePreview != null) {
            TournamentPreviewBottomSheet(
                tournament = activePreview,
                isJoined = activePreview.id in joinedIds,
                walletBalance = walletBalance,
                onDismiss = { previewTournament = null },
                onOpenFullDetails = { tid ->
                    previewTournament = null
                    openTournament(tid)
                },
                onOpenWallet = {
                    previewTournament = null
                    navController.navigate(LobbixRoutes.WALLET)
                }
            )
        }
    }
}
