package com.citruschat.citrusmobile.ui.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.citruschat.citrusmobile.navigation.Routes
import com.citruschat.citrusmobile.ui.home.HomeScreen
import com.citruschat.citrusmobile.ui.profile.ProfileScreen
import com.citruschat.citrusmobile.ui.shared.component.NavigationComponent
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    navController: NavHostController,
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            NavigationComponent(
                currentRoute =
                    when (pagerState.currentPage) {
                        0 -> Routes.Home
                        1 -> Routes.Profile
                        else -> Routes.Home
                    },
                onHomeClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(0)
                    }
                },
                onProfileClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(1)
                    }
                },
            )
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(padding),
        ) { page ->
            when (page) {
                0 ->
                    HomeScreen(onChatClick = { chat ->
                        navController.navigate(Routes.chat(chat.id))
                    })
                1 -> ProfileScreen()
            }
        }
    }
}
