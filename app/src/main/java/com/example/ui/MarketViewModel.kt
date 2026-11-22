package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CartEntry
import com.example.data.MarketCatalog
import com.example.data.MarketCategory
import com.example.data.MarketOrder
import com.example.data.MarketProduct
import com.example.data.MarketRepository
import com.example.data.PaymentMethod
import com.example.data.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

sealed interface DataUiState<out T> {
    data object Loading : DataUiState<Nothing>
    data class Success<T>(val data: T) : DataUiState<T>
    data class Error(val message: String) : DataUiState<Nothing>
}

data class CartPricingSummary(
    val subtotal: Double,
    val deliveryFee: Double,
    val total: Double,
    val totalItems: Int,
    val amountForFreeDelivery: Double,
)

object CartPricing {
    fun calculate(lines: List<Pair<MarketProduct, Int>>): CartPricingSummary {
        val totalItems = lines.sumOf { it.second }
        val subtotalRaw = lines.sumOf { (product, qty) -> product.price * qty }
        val subtotal = (subtotalRaw * 100.0).roundToInt() / 100.0
        val deliveryFee = when {
            lines.isEmpty() -> 0.0
            subtotal >= MarketCatalog.FREE_DELIVERY_THRESHOLD -> 0.0
            else -> MarketCatalog.STANDARD_DELIVERY_FEE
        }
        val total = ((subtotal + deliveryFee) * 100.0).roundToInt() / 100.0
        val remaining = ((MarketCatalog.FREE_DELIVERY_THRESHOLD - subtotal).coerceAtLeast(0.0) * 100.0).roundToInt() / 100.0
        return CartPricingSummary(
            subtotal = subtotal,
            deliveryFee = deliveryFee,
            total = total,
            totalItems = totalItems,
            amountForFreeDelivery = remaining,
        )
    }
}

enum class MarketTab {
    MARKET,
    FAVORITES,
    CART,
    ORDERS,
}

data class MarketUiControls(
    val selectedTab: MarketTab = MarketTab.MARKET,
    val selectedCategory: MarketCategory = MarketCategory.ALL,
    val searchQuery: String = "",
    val selectedProduct: MarketProduct? = null,
    val isCheckoutOpen: Boolean = false,
    val isProfileOpen: Boolean = false,
    val isSubmittingOrder: Boolean = false,
    val isSavingProfile: Boolean = false,
    val statusMessage: String? = null,
)

class MarketViewModel(
    private val repository: MarketRepository,
    private val currentUserId: String,
    private val defaultDisplayName: String,
) : ViewModel() {

    private val _controls = MutableStateFlow(MarketUiControls())
    val controls: StateFlow<MarketUiControls> = _controls.asStateFlow()

    val profileState: StateFlow<DataUiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, DataUiState<UserProfile?>> { DataUiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreErrorOrThrow(error)
            Log.w(TAG, "Error observing profile", root)
            emit(DataUiState.Error(root.localizedMessage ?: "تعذر تحميل الملف الشخصي"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = DataUiState.Loading,
        )

    val cartEntriesState: StateFlow<DataUiState<List<CartEntry>>> = repository.observeCart(currentUserId)
        .map<List<CartEntry>, DataUiState<List<CartEntry>>> { DataUiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreErrorOrThrow(error)
            Log.w(TAG, "Error observing cart", root)
            emit(DataUiState.Error(root.localizedMessage ?: "تعذر تحميل السلة"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = DataUiState.Loading,
        )

    val ordersState: StateFlow<DataUiState<List<MarketOrder>>> = repository.observeOrders(currentUserId)
        .map<List<MarketOrder>, DataUiState<List<MarketOrder>>> { DataUiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreErrorOrThrow(error)
            Log.w(TAG, "Error observing orders", root)
            emit(DataUiState.Error(root.localizedMessage ?: "تعذر تحميل الطلبات"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = DataUiState.Loading,
        )

    val cartQuantities: StateFlow<Map<String, Int>> = cartEntriesState
        .map { state ->
            when (state) {
                is DataUiState.Success -> state.data.associate { it.productId to it.quantity.toInt() }
                else -> emptyMap()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyMap())

    val cartLines: StateFlow<List<Pair<MarketProduct, Int>>> = cartQuantities
        .map { quantities ->
            quantities.mapNotNull { (productId, qty) ->
                val product = MarketCatalog.findById(productId)
                if (product != null && qty > 0) product to qty else null
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val cartPricing: StateFlow<CartPricingSummary> = cartLines
        .map { CartPricing.calculate(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            CartPricing.calculate(emptyList()),
        )

    val favoriteIds: StateFlow<Set<String>> = profileState
        .map { state ->
            when (state) {
                is DataUiState.Success -> state.data?.favoriteIds?.toSet() ?: emptySet()
                else -> emptySet()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptySet())

    val filteredProducts: StateFlow<List<MarketProduct>> = _controls
        .map { ctrl ->
            val q = ctrl.searchQuery.trim()
            MarketCatalog.products.filter { product ->
                val matchesCat = ctrl.selectedCategory == MarketCategory.ALL ||
                    product.category == ctrl.selectedCategory
                val matchesQuery = q.isEmpty() ||
                    product.nameAr.contains(q, ignoreCase = true) ||
                    product.descriptionAr.contains(q, ignoreCase = true) ||
                    product.category.labelAr.contains(q, ignoreCase = true)
                matchesCat && matchesQuery
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            MarketCatalog.products,
        )

    val favoriteProducts: StateFlow<List<MarketProduct>> = combine(
        favoriteIds,
        _controls,
    ) { favs, _ ->
        MarketCatalog.products.filter { it.id in favs }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(currentUserId, defaultDisplayName)
        }
    }

    fun selectTab(tab: MarketTab) {
        _controls.update { it.copy(selectedTab = tab) }
    }

    fun selectCategory(category: MarketCategory) {
        _controls.update { it.copy(selectedCategory = category) }
    }

    fun updateSearchQuery(query: String) {
        _controls.update { it.copy(searchQuery = query) }
    }

    fun openProductDetail(product: MarketProduct) {
        _controls.update { it.copy(selectedProduct = product) }
    }

    fun closeProductDetail() {
        _controls.update { it.copy(selectedProduct = null) }
    }

    fun setCheckoutOpen(open: Boolean) {
        _controls.update { it.copy(isCheckoutOpen = open) }
    }

    fun setProfileOpen(open: Boolean) {
        _controls.update { it.copy(isProfileOpen = open) }
    }

    fun consumeStatusMessage() {
        _controls.update { it.copy(statusMessage = null) }
    }

    fun updateCartQuantity(productId: String, newQuantity: Int) {
        val currentMap = cartQuantities.value
        val alreadyInCart = currentMap.containsKey(productId)
        viewModelScope.launch {
            val result = repository.setCartQuantity(
                userId = currentUserId,
                productId = productId,
                quantity = newQuantity,
                isCurrentlyInCart = alreadyInCart,
            )
            result.onFailure { err ->
                _controls.update {
                    it.copy(statusMessage = err.localizedMessage ?: "تعذر تحديث السلة")
                }
            }
        }
    }

    fun clearCart() {
        val ids = cartQuantities.value.keys.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.clearCart(currentUserId, ids)
                .onSuccess {
                    _controls.update { it.copy(statusMessage = "تم تفريغ السلة") }
                }
                .onFailure { err ->
                    _controls.update {
                        it.copy(statusMessage = err.localizedMessage ?: "تعذر تفريغ السلة")
                    }
                }
        }
    }

    fun toggleFavorite(productId: String) {
        val currentList = favoriteIds.value.toList()
        viewModelScope.launch {
            repository.toggleFavorite(
                userId = currentUserId,
                productId = productId,
                currentFavorites = currentList,
                fallbackName = defaultDisplayName,
            ).onFailure { err ->
                _controls.update {
                    it.copy(statusMessage = err.localizedMessage ?: "تعذر تحديث المفضلة")
                }
            }
        }
    }

    fun saveProfile(
        displayName: String,
        phone: String,
        address: String,
        notes: String,
    ) {
        viewModelScope.launch {
            _controls.update { it.copy(isSavingProfile = true) }
            val res = repository.updateProfile(currentUserId, displayName, phone, address, notes)
            _controls.update { state ->
                state.copy(
                    isSavingProfile = false,
                    isProfileOpen = res.isFailure,
                    statusMessage = if (res.isSuccess) {
                        "تم حفظ بيانات التوصيل بنجاح"
                    } else {
                        res.exceptionOrNull()?.localizedMessage ?: "تعذر حفظ البيانات"
                    },
                )
            }
        }
    }

    fun placeOrder(
        customerName: String,
        phone: String,
        deliveryAddress: String,
        deliveryNotes: String,
        paymentMethod: PaymentMethod,
    ) {
        val lines = cartLines.value
        if (lines.isEmpty()) {
            _controls.update { it.copy(statusMessage = "السلة فارغة") }
            return
        }
        viewModelScope.launch {
            _controls.update { it.copy(isSubmittingOrder = true) }
            val result = repository.placeOrder(
                userId = currentUserId,
                customerName = customerName,
                phone = phone,
                deliveryAddress = deliveryAddress,
                deliveryNotes = deliveryNotes,
                paymentMethod = paymentMethod,
                cartLines = lines,
            )
            _controls.update { state ->
                if (result.isSuccess) {
                    val orderCode = result.getOrNull().orEmpty()
                    state.copy(
                        isSubmittingOrder = false,
                        isCheckoutOpen = false,
                        selectedTab = MarketTab.ORDERS,
                        statusMessage = "تم إرسال طلبك بنجاح ($orderCode)",
                    )
                } else {
                    state.copy(
                        isSubmittingOrder = false,
                        statusMessage = result.exceptionOrNull()?.localizedMessage
                            ?: "تعذر إرسال الطلب",
                    )
                }
            }
        }
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            repository.cancelOrder(currentUserId, orderId)
                .onSuccess {
                    _controls.update { it.copy(statusMessage = "تم إلغاء الطلب") }
                }
                .onFailure { err ->
                    _controls.update {
                        it.copy(statusMessage = err.localizedMessage ?: "تعذر إلغاء الطلب")
                    }
                }
        }
    }

    private fun unwrapFirestoreErrorOrThrow(error: Throwable): Throwable {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        return firestoreError ?: error
    }

    private companion object {
        const val TAG = "MarketVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
