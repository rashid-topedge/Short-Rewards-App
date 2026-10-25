package com.dramflix.android.navigation

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dramflix.android.ui.catalog.CatalogScreen
import com.dramflix.android.ui.catalog.CatalogViewModel
import com.dramflix.android.ui.detail.DetailScreen
import com.dramflix.android.ui.detail.DetailViewModel
import com.dramflix.android.ui.player.PlayerScreen
import com.dramflix.android.ui.player.PlayerViewModel
import com.dramflix.android.ui.reels.ReelDetailScreen
import com.dramflix.android.ui.reels.ReelDetailViewModel
import com.dramflix.android.ui.reels.ReelsListScreen
import com.dramflix.android.ui.reels.ReelsListViewModel
import com.dramflix.android.ui.settings.RedemptionFormScreen
import com.dramflix.android.ui.settings.RedemptionFormViewModel
import com.dramflix.android.ui.settings.SettingsScreen
import com.dramflix.android.ui.settings.SettingsViewModel
import com.dramflix.android.ui.shell.AppShell
import com.dramflix.android.ui.shell.Tab

object Routes {
    const val Catalog = "catalog"
    const val ReelsList = "reels"
    const val ReelDetail = "reel/{reelId}"
    const val Settings = "settings"
    const val Detail = "drama/{dramaId}"
    const val Player = "player/{dramaId}/{ep}"
    const val RedemptionForm = "redemption/form"

    fun detail(dramaId: Int) = "drama/$dramaId"
    fun player(dramaId: Int, ep: Int) = "player/$dramaId/$ep"
    fun reelDetail(reelId: Int) = "reel/$reelId"

    fun topLevelDestination(tab: Tab): String = when (tab) {
        Tab.Dramas -> Catalog
        Tab.Reels -> ReelsList
        Tab.Settings -> Settings
    }
}

@Composable
fun DramaNavGraph() {
    val navController = rememberNavController()
    val app = LocalContext.current.applicationContext as Application

    AppShell(navController = navController) {
        NavHost(
            navController = navController,
            startDestination = Routes.Catalog,
        ) {
            composable(Routes.Catalog) {
                val vm: CatalogViewModel = viewModel(factory = CatalogViewModel.factory(app))
                CatalogScreen(
                    viewModel = vm,
                    onOpenDrama = { id -> navController.navigate(Routes.detail(id)) },
                )
            }

            composable(Routes.ReelsList) {
                val vm: ReelsListViewModel = viewModel(factory = ReelsListViewModel.factory(app))
                ReelsListScreen(
                    viewModel = vm,
                    onOpenReel = { id -> navController.navigate(Routes.reelDetail(id)) },
                    onOpenDrama = { dramaId -> navController.navigate(Routes.detail(dramaId)) },
                )
            }

            composable(
                route = Routes.ReelDetail,
                arguments = listOf(navArgument("reelId") { type = NavType.IntType }),
            ) { entry ->
                val reelId = entry.arguments?.getInt("reelId") ?: return@composable
                val vm: ReelDetailViewModel = viewModel(factory = ReelDetailViewModel.factory(app, reelId))
                ReelDetailScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onOpenDrama = { dramaId -> navController.navigate(Routes.detail(dramaId)) },
                )
            }

            composable(Routes.Settings) {
                val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(app))
                SettingsScreen(
                    viewModel = vm,
                    onOpenRedemptionForm = {
                        navController.navigate(Routes.RedemptionForm)
                    },
                )
            }

            composable(Routes.RedemptionForm) {
                val vm: RedemptionFormViewModel = viewModel(factory = RedemptionFormViewModel.factory(app))
                RedemptionFormScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onSubmitted = {
                        navController.popBackStack()
                    },
                )
            }

            composable(
                route = Routes.Detail,
                arguments = listOf(navArgument("dramaId") { type = NavType.IntType }),
            ) { entry ->
                val dramaId = entry.arguments?.getInt("dramaId") ?: return@composable
                val vm: DetailViewModel = viewModel(factory = DetailViewModel.factory(app, dramaId))
                DetailScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onPlayEpisode = { ep, _, _ ->
                        navController.navigate(Routes.player(dramaId, ep))
                    },
                )
            }

            composable(
                route = Routes.Player,
                arguments = listOf(
                    navArgument("dramaId") { type = NavType.IntType },
                    navArgument("ep") { type = NavType.IntType },
                ),
            ) { entry ->
                val dramaId = entry.arguments?.getInt("dramaId") ?: return@composable
                val ep = entry.arguments?.getInt("ep") ?: return@composable
                val vm: PlayerViewModel = viewModel(
                    key = "player-$dramaId-$ep",
                    factory = PlayerViewModel.factory(app, dramaId, ep),
                )
                PlayerScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
