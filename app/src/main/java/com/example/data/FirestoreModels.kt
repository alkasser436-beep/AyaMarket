package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class OrderStatus(val raw: String, val labelAr: String) {
    PENDING("pending", "قيد التأكيد"),
    PREPARING("preparing", "جاري التجهيز"),
    ON_THE_WAY("on_the_way", "في الطريق إليك"),
    DELIVERED("delivered", "تم التوصيل"),
    CANCELLED("cancelled", "ملغي");

    companion object {
        fun fromRaw(raw: String?): OrderStatus =
            entries.firstOrNull { it.raw == raw } ?: PENDING
    }
}

enum class PaymentMethod(val raw: String, val labelAr: String) {
    CASH_ON_DELIVERY("cash_on_delivery", "الدفع عند الاستلام (نقداً)"),
    CARD_ON_DELIVERY("card_on_delivery", "بطاقة بنكية عند الاستلام");

    companion object {
        fun fromRaw(raw: String?): PaymentMethod =
            entries.firstOrNull { it.raw == raw } ?: CASH_ON_DELIVERY
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val phone: String = "",
    val address: String = "",
    val notes: String = "",
    val favoriteIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class CartEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val quantity: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class OrderLineItem(
    val productId: String = "",
    val nameAr: String = "",
    val unitPrice: Double = 0.0,
    val quantity: Long = 1L,
)

@Keep
@IgnoreExtraProperties
data class MarketOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderNumber: String = "",
    val customerName: String = "",
    val phone: String = "",
    val deliveryAddress: String = "",
    val deliveryNotes: String? = null,
    val paymentMethod: String = PaymentMethod.CASH_ON_DELIVERY.raw,
    val status: String = OrderStatus.PENDING.raw,
    val itemsSummary: String = "",
    val items: List<OrderLineItem> = emptyList(),
    val totalItems: Long = 0L,
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val totalAmount: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)
