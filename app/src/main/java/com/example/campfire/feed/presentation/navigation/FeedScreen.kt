package com.example.campfire.feed.presentation.navigation

import com.example.campfire.core.presentation.navigation.NavigationDestination


sealed class FeedScreen(override val route: String) : NavigationDestination {
    object FeedOverview : FeedScreen("feed_overview")
    object PostDetail : FeedScreen("feed_post_detail/{postId}") {
        fun createRoute(postId: String) = "feed_post_detail/$postId"
    }
}