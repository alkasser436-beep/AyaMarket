package com.example

import com.example.data.MarketCatalog
import com.example.data.MarketCategory
import com.example.data.OrderStatus
import com.example.data.PaymentMethod
import com.example.ui.CartPricing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun `catalog contains core market categories and gas products`() {
        val gasProducts = MarketCatalog.products.filter { it.category == MarketCategory.GAS }
        assertTrue(gasProducts.isNotEmpty())
        assertNotNull(MarketCatalog.findById("gas_12kg"))
        assertNotNull(MarketCatalog.findById("tomato_1kg"))
    }

    @Test
    fun `cart pricing applies delivery fee below threshold and waives it above threshold`() {
        val gas = MarketCatalog.findById("gas_12kg")!!
        val below = CartPricing.calculate(listOf(gas to 2)) // 14.00 JOD
        assertEquals(14.00, below.subtotal, 0.001)
        assertEquals(1.50, below.deliveryFee, 0.001)
        assertEquals(15.50, below.total, 0.001)
        assertEquals(6.00, below.amountForFreeDelivery, 0.001)

        val above = CartPricing.calculate(listOf(gas to 3)) // 21.00 JOD
        assertEquals(21.00, above.subtotal, 0.001)
        assertEquals(0.0, above.deliveryFee, 0.001)
        assertEquals(21.00, above.total, 0.001)
        assertEquals(0.0, above.amountForFreeDelivery, 0.001)
    }

    @Test
    fun `enum deserializers fall back safely on unknown raw values`() {
        assertEquals(OrderStatus.PENDING, OrderStatus.fromRaw("unknown_status"))
        assertEquals(OrderStatus.DELIVERED, OrderStatus.fromRaw("delivered"))
        assertEquals(PaymentMethod.CASH_ON_DELIVERY, PaymentMethod.fromRaw("unexpected"))
        assertEquals(PaymentMethod.CARD_ON_DELIVERY, PaymentMethod.fromRaw("card_on_delivery"))
    }
}
