package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.MarketRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOut
import com.example.ui.screens.CartTab
import com.example.ui.screens.CheckoutSheet
import com.example.ui.screens.FavoritesTab
import com.example.ui.screens.MarketHomeTab
import com.example.ui.screens.OrdersTab
import com.example.ui.screens.ProductDetailSheet
import com.example.ui.screens.ProfileSheet
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AyasMarketApp() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

        DisposableEffect(Unit) {
            val listener = FirebaseAuth.AuthStateListener { auth ->
                currentUser = auth.currentUser
            }
            Firebase.auth.addAuthStateListener(listener)
            onDispose { Firebase.auth.removeAuthStateListener(listener) }
        }

        val user = currentUser
        if (user == null) {
            AuthScreen(
                onAuthSuccess = { currentUser = Firebase.auth.currentUser },
            )
        } else {
            val uid = user.uid
            val defaultName = user.displayName?.takeIf { it.isNotBlank() } ?: "عميل Aya's Market"
            key(uid) {
                val marketViewModel: MarketViewModel = viewModel(
                    key = uid,
                    factory = viewModelFactory {
                        initializer {
                            val app = checkNotNull(this[APPLICATION_KEY]) {
                                "APPLICATION_KEY missing from CreationExtras"
                            }
                            val databaseId = app.getString(R.string.firestore_database_id)
                            val db = FirebaseFirestore.getInstance(databaseId)
                            MarketViewModel(
                                repository = MarketRepository(db),
                                currentUserId = uid,
                                defaultDisplayName = defaultName,
                            )
                        }
                    },
                )
                MarketScaffold(
                    viewModel = marketViewModel,
                    defaultDisplayName = defaultName,
                    userEmail = user.email,
                )
            }
        }
    }
}

@Composable
private fun MarketScaffold(
    viewModel: MarketViewModel,
    defaultDisplayName: String,
    userEmail: String?,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val controls by viewModel.controls.collectAsStateWithLifecycle()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val cartState by viewModel.cartEntriesState.collectAsStateWithLifecycle()
    val ordersState by viewModel.ordersState.collectAsStateWithLifecycle()
    val cartQuantities by viewModel.cartQuantities.collectAsStateWithLifecycle()
    val cartLines by viewModel.cartLines.collectAsStateWithLifecycle()
    val pricing by viewModel.cartPricing.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val filteredProducts by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val favoriteProducts by viewModel.favoriteProducts.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val userProfile = (profileState as? DataUiState.Success)?.data

    LaunchedEffect(controls.statusMessage) {
        val msg = controls.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeStatusMessage()
        }
    }

    // Return to Market home tab on back press when on a secondary tab
    BackHandler(enabled = controls.selectedTab != MarketTab.MARKET) {
        viewModel.selectTab(MarketTab.MARKET)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        MarketTab.entries.forEach { tab ->
                            val selected = controls.selectedTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        selected = selected,
                                        cartCount = pricing.totalItems,
                                        favoriteCount = favoriteIds.size,
                                    )
                                },
                                label = { Text(tabLabel(tab)) },
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}"),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        MarketTab.entries.forEach { tab ->
                            val selected = controls.selectedTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        selected = selected,
                                        cartCount = pricing.totalItems,
                                        favoriteCount = favoriteIds.size,
                                    )
                                },
                                label = { Text(tabLabel(tab)) },
                                modifier = Modifier.testTag("nav_rail_${tab.name.lowercase()}"),
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (controls.selectedTab) {
                        MarketTab.MARKET -> MarketHomeTab(
                            userProfile = userProfile,
                            selectedCategory = controls.selectedCategory,
                            searchQuery = controls.searchQuery,
                            products = filteredProducts,
                            cartQuantities = cartQuantities,
                            favoriteIds = favoriteIds,
                            onSearchChange = viewModel::updateSearchQuery,
                            onSelectCategory = viewModel::selectCategory,
                            onOpenProfile = { viewModel.setProfileOpen(true) },
                            onOpenProduct = viewModel::openProductDetail,
                            onToggleFavorite = viewModel::toggleFavorite,
                            onQuantityChange = viewModel::updateCartQuantity,
                        )
                        MarketTab.FAVORITES -> FavoritesTab(
                            favoriteProducts = favoriteProducts,
                            cartQuantities = cartQuantities,
                            onBrowseMarket = { viewModel.selectTab(MarketTab.MARKET) },
                            onOpenProduct = viewModel::openProductDetail,
                            onToggleFavorite = viewModel::toggleFavorite,
                            onQuantityChange = viewModel::updateCartQuantity,
                        )
                        MarketTab.CART -> CartTab(
                            cartState = cartState,
                            cartLines = cartLines,
                            pricing = pricing,
                            onBrowseMarket = { viewModel.selectTab(MarketTab.MARKET) },
                            onQuantityChange = viewModel::updateCartQuantity,
                            onClearCart = viewModel::clearCart,
                            onProceedCheckout = { viewModel.setCheckoutOpen(true) },
                        )
                        MarketTab.ORDERS -> OrdersTab(
                            ordersState = ordersState,
                            onBrowseMarket = { viewModel.selectTab(MarketTab.MARKET) },
                            onCancelOrder = viewModel::cancelOrder,
                        )
                    }
                }
            }
        }
    }

    controls.selectedProduct?.let { product ->
        ProductDetailSheet(
            product = product,
            currentCartQuantity = cartQuantities[product.id] ?: 0,
            isFavorite = product.id in favoriteIds,
            onDismiss = viewModel::closeProductDetail,
            onToggleFavorite = { viewModel.toggleFavorite(product.id) },
            onApplyQuantity = { qty -> viewModel.updateCartQuantity(product.id, qty) },
        )
    }

    if (controls.isCheckoutOpen) {
        CheckoutSheet(
            userProfile = userProfile,
            defaultName = defaultDisplayName,
            pricing = pricing,
            isSubmitting = controls.isSubmittingOrder,
            onDismiss = { viewModel.setCheckoutOpen(false) },
            onSubmitOrder = viewModel::placeOrder,
        )
    }

    if (controls.isProfileOpen) {
        ProfileSheet(
            userProfile = userProfile,
            defaultName = defaultDisplayName,
            userEmail = userEmail,
            isSaving = controls.isSavingProfile,
            onDismiss = { viewModel.setProfileOpen(false) },
            onSaveProfile = viewModel::saveProfile,
            onSignOut = {
                viewModel.setProfileOpen(false)
                signOut(credentialManager = credentialManager, scope = scope)
            },
        )
    }
}

@Composable
private fun tabLabel(tab: MarketTab): String = when (tab) {
    MarketTab.MARKET -> stringResource(R.string.nav_market)
    MarketTab.FAVORITES -> stringResource(R.string.nav_favorites)
    MarketTab.CART -> stringResource(R.string.nav_cart)
    MarketTab.ORDERS -> stringResource(R.string.nav_orders)
}

@Composable
private fun TabIconWithBadge(
    tab: MarketTab,
    selected: Boolean,
    cartCount: Int,
    favoriteCount: Int,
) {
    val icon = when (tab) {
        MarketTab.MARKET -> if (selected) Icons.Filled.Storefront else Icons.Outlined.Storefront
        MarketTab.FAVORITES -> if (selected) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
        MarketTab.CART -> if (selected) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart
        MarketTab.ORDERS -> if (selected) Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong
    }

    val badgeCount = when (tab) {
        MarketTab.CART -> cartCount
        MarketTab.FAVORITES -> favoriteCount
        else -> 0
    }

    if (badgeCount > 0) {
        BadgedBox(
            badge = {
                Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                    Text(badgeCount.toString())
                }
            },
        ) {
            Icon(imageVector = icon, contentDescription = tabLabel(tab))
        }
    } else {
        Icon(imageVector = icon, contentDescription = tabLabel(tab))
    }
}
