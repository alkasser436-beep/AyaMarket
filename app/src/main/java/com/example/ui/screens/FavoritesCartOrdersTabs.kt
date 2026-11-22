package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.MarketCatalog
import com.example.data.MarketOrder
import com.example.data.MarketProduct
import com.example.data.OrderStatus
import com.example.data.PaymentMethod
import com.example.ui.CartPricingSummary
import com.example.ui.DataUiState
import com.example.ui.components.ProductCard
import com.example.ui.components.ProductVisualBadge
import com.example.ui.components.QuantityStepper
import com.example.ui.components.formatJod
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun FavoritesTab(
    favoriteProducts: List<MarketProduct>,
    cartQuantities: Map<String, Int>,
    onBrowseMarket: () -> Unit,
    onOpenProduct: (MarketProduct) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onQuantityChange: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (favoriteProducts.isEmpty()) {
        MarketEmptyState(
            icon = Icons.Outlined.FavoriteBorder,
            title = stringResource(R.string.empty_favorites_title),
            subtitle = stringResource(R.string.empty_favorites_subtitle),
            actionLabel = stringResource(R.string.browse_market),
            onAction = onBrowseMarket,
            modifier = modifier,
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        item(key = "fav_title") {
            Text(
                text = stringResource(R.string.nav_favorites),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        items(favoriteProducts, key = { it.id }) { product ->
            ProductCard(
                product = product,
                quantityInCart = cartQuantities[product.id] ?: 0,
                isFavorite = true,
                onCardClick = { onOpenProduct(product) },
                onToggleFavorite = { onToggleFavorite(product.id) },
                onQuantityChange = { qty -> onQuantityChange(product.id, qty) },
            )
        }
    }
}

@Composable
fun CartTab(
    cartState: DataUiState<*>,
    cartLines: List<Pair<MarketProduct, Int>>,
    pricing: CartPricingSummary,
    onBrowseMarket: () -> Unit,
    onQuantityChange: (String, Int) -> Unit,
    onClearCart: () -> Unit,
    onProceedCheckout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (cartState) {
        is DataUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is DataUiState.Error -> {
            MarketEmptyState(
                icon = Icons.Outlined.ShoppingCart,
                title = "تعذر تحميل السلة",
                subtitle = cartState.message,
                actionLabel = stringResource(R.string.browse_market),
                onAction = onBrowseMarket,
                modifier = modifier,
            )
        }
        is DataUiState.Success -> {
            if (cartLines.isEmpty()) {
                MarketEmptyState(
                    icon = Icons.Outlined.ShoppingCart,
                    title = stringResource(R.string.empty_cart_title),
                    subtitle = stringResource(R.string.empty_cart_subtitle),
                    actionLabel = stringResource(R.string.browse_market),
                    onAction = onBrowseMarket,
                    modifier = modifier,
                )
                return
            }

            Column(modifier = modifier.fillMaxSize()) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    item(key = "cart_header") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "${stringResource(R.string.nav_cart)} (${pricing.totalItems})",
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            TextButton(
                                onClick = onClearCart,
                                modifier = Modifier.testTag("clear_cart_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.clear_cart))
                            }
                        }
                    }

                    item(key = "free_delivery_meter") {
                        val progress = (pricing.subtotal / MarketCatalog.FREE_DELIVERY_THRESHOLD)
                            .toFloat()
                            .coerceIn(0f, 1f)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.LocalShipping,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (pricing.amountForFreeDelivery <= 0.0) {
                                            "مبروك! حصلت على توصيل مجاني"
                                        } else {
                                            "أضف ${formatJod(pricing.amountForFreeDelivery)} د.أ للحصول على توصيل مجاني"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surface,
                                )
                            }
                        }
                    }

                    items(cartLines, key = { it.first.id }) { (product, qty) ->
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                            ) {
                                ProductVisualBadge(product = product, size = 56.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.nameAr,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${formatJod(product.price * qty)} ${stringResource(R.string.currency_jod)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                QuantityStepper(
                                    quantity = qty,
                                    onDecrease = { onQuantityChange(product.id, qty - 1) },
                                    onIncrease = { onQuantityChange(product.id, qty + 1) },
                                    testTagPrefix = "cart_${product.id}",
                                )
                            }
                        }
                    }
                }

                Surface(
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SummaryRow(
                            label = stringResource(R.string.subtotal_label),
                            value = "${formatJod(pricing.subtotal)} ${stringResource(R.string.currency_jod)}",
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        SummaryRow(
                            label = stringResource(R.string.delivery_fee_label),
                            value = if (pricing.deliveryFee == 0.0) {
                                stringResource(R.string.free_delivery)
                            } else {
                                "${formatJod(pricing.deliveryFee)} ${stringResource(R.string.currency_jod)}"
                            },
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(R.string.total_label),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text = "${formatJod(pricing.total)} ${stringResource(R.string.currency_jod)}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onProceedCheckout,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("checkout_button"),
                        ) {
                            Text(
                                text = stringResource(R.string.checkout_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrdersTab(
    ordersState: DataUiState<List<MarketOrder>>,
    onBrowseMarket: () -> Unit,
    onCancelOrder: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (ordersState) {
        is DataUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is DataUiState.Error -> {
            MarketEmptyState(
                icon = Icons.Outlined.ReceiptLong,
                title = "تعذر تحميل الطلبات",
                subtitle = ordersState.message,
                actionLabel = stringResource(R.string.browse_market),
                onAction = onBrowseMarket,
                modifier = modifier,
            )
        }
        is DataUiState.Success -> {
            val orders = ordersState.data
            if (orders.isEmpty()) {
                MarketEmptyState(
                    icon = Icons.Outlined.ReceiptLong,
                    title = stringResource(R.string.empty_orders_title),
                    subtitle = stringResource(R.string.empty_orders_subtitle),
                    actionLabel = stringResource(R.string.browse_market),
                    onAction = onBrowseMarket,
                    modifier = modifier,
                )
                return
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = modifier.fillMaxSize(),
            ) {
                item(key = "orders_header") {
                    Text(
                        text = stringResource(R.string.nav_orders),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
                items(orders, key = { it.id }) { order ->
                    OrderHistoryCard(
                        order = order,
                        onCancel = { onCancelOrder(order.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderHistoryCard(
    order: MarketOrder,
    onCancel: () -> Unit,
) {
    val status = OrderStatus.fromRaw(order.status)
    val payment = PaymentMethod.fromRaw(order.paymentMethod)
    val dateLabel = order.createdAt?.toDate()?.let { date ->
        SimpleDateFormat("dd/MM/yyyy • hh:mm a", Locale("ar")).format(date)
    } ?: "الآن"

    val statusColor = when (status) {
        OrderStatus.PENDING -> MaterialTheme.colorScheme.secondary
        OrderStatus.PREPARING, OrderStatus.ON_THE_WAY -> MaterialTheme.colorScheme.primary
        OrderStatus.DELIVERED -> MaterialTheme.colorScheme.primary
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.error
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("order_card_${order.id}"),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    Text(
                        text = "طلب ${order.orderNumber}",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusColor.copy(alpha = 0.14f),
                ) {
                    Text(
                        text = status.labelAr,
                        style = MaterialTheme.typography.labelLarge,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = order.itemsSummary,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "العنوان: ${order.deliveryAddress}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "الدفع: ${payment.labelAr}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "${formatJod(order.totalAmount)} ${stringResource(R.string.currency_jod)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )

                if (status == OrderStatus.PENDING) {
                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("cancel_order_${order.id}"),
                    ) {
                        Text(stringResource(R.string.cancel_order))
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun MarketEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 420.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                modifier = Modifier.size(84.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(22.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("empty_state_browse_button"),
            ) {
                Text(actionLabel)
            }
        }
    }
}
