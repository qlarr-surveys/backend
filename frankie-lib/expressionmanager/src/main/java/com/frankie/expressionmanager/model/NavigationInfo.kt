package com.frankie.expressionmanager.model

data class NavigationInfo(
    val navigationIndex: NavigationIndex? = null,
    val navigationDirection: NavigationDirection = NavigationDirection.Start
)