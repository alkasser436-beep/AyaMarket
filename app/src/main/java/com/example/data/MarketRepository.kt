package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToInt
import kotlin.random.Random

class MarketRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id),
        ),
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user changed; refusing cross-user profile read." }
        val docRef = db.collection("users").document(uid)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    )
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.GET, docRef.path, uid)
                    throw error
                },
        )
    }

    fun observeCart(userId: String): Flow<List<CartEntry>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user changed; refusing cross-user cart read." }
        val path = "users/$uid/cart"
        emitAll(
            db.collection("users").document(uid).collection("cart")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CartEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    )
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                },
        )
    }

    fun observeOrders(userId: String): Flow<List<MarketOrder>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user changed; refusing cross-user orders read." }
        val path = "users/$uid/orders"
        emitAll(
            db.collection("users").document(uid).collection("orders")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MarketOrder::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    ).sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                },
        )
    }

    suspend fun ensureUserProfile(userId: String, defaultName: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val docRef = db.collection("users").document(uid)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val safeName = defaultName.trim().ifEmpty { "عميل Aya's Market" }.take(80)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "favoriteIds" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        userId: String,
        displayName: String,
        phone: String,
        address: String,
        notes: String,
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val cleanName = displayName.trim().take(80)
            require(cleanName.isNotEmpty()) { "يرجى إدخال الاسم" }
            val cleanPhone = phone.trim().take(30)
            val cleanAddress = address.trim().take(250)
            val cleanNotes = notes.trim().take(300)

            val docRef = db.collection("users").document(uid)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to cleanName,
                    "phone" to cleanPhone,
                    "address" to cleanAddress,
                    "notes" to cleanNotes,
                    "favoriteIds" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "displayName" to cleanName,
                        "phone" to cleanPhone,
                        "address" to cleanAddress,
                        "notes" to cleanNotes,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleFavorite(
        userId: String,
        productId: String,
        currentFavorites: List<String>,
        fallbackName: String,
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val updated = if (productId in currentFavorites) {
                currentFavorites.filterNot { it == productId }
            } else {
                (currentFavorites + productId).distinct().take(50)
            }
            val docRef = db.collection("users").document(uid)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to fallbackName.trim().ifEmpty { "عميل Aya's Market" }.take(80),
                        "favoriteIds" to updated,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "favoriteIds" to updated,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun setCartQuantity(
        userId: String,
        productId: String,
        quantity: Int,
        isCurrentlyInCart: Boolean,
    ): Result<Unit> {
        val path = "users/$userId/cart/$productId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            require(productId.isNotBlank() && productId.length <= 64) { "Invalid productId" }

            val docRef = db.collection("users").document(uid).collection("cart").document(productId)
            if (quantity <= 0) {
                docRef.delete().await()
            } else {
                val boundedQty = quantity.coerceIn(1, 99)
                val exists = isCurrentlyInCart || docRef.get().await().exists()
                if (exists) {
                    docRef.update(
                        mapOf(
                            "quantity" to boundedQty,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                    ).await()
                } else {
                    docRef.set(
                        mapOf(
                            "userId" to uid,
                            "productId" to productId,
                            "quantity" to boundedQty,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun clearCart(userId: String, productIds: List<String>): Result<Unit> {
        val path = "users/$userId/cart"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            if (productIds.isEmpty()) return Result.success(Unit)
            val batch = db.batch()
            val cartCol = db.collection("users").document(uid).collection("cart")
            productIds.distinct().forEach { productId ->
                batch.delete(cartCol.document(productId))
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun placeOrder(
        userId: String,
        customerName: String,
        phone: String,
        deliveryAddress: String,
        deliveryNotes: String,
        paymentMethod: PaymentMethod,
        cartLines: List<Pair<MarketProduct, Int>>,
    ): Result<String> {
        val ordersPath = "users/$userId/orders"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }

            val cleanName = customerName.trim().take(80)
            val cleanPhone = phone.trim().take(30)
            val cleanAddress = deliveryAddress.trim().take(250)
            val cleanNotes = deliveryNotes.trim().take(300).takeIf { it.isNotEmpty() }

            require(cleanName.isNotEmpty()) { "يرجى إدخال اسم المستلم" }
            require(cleanPhone.length >= 5) { "يرجى إدخال رقم هاتف صحيح للتواصل" }
            require(cleanAddress.length >= 5) { "يرجى إدخال عنوان التوصيل بالتفصيل" }
            require(cartLines.isNotEmpty() && cartLines.size <= 40) { "السلة فارغة" }

            val totalItems = cartLines.sumOf { it.second }.coerceIn(1, 200)
            val subtotalRaw = cartLines.sumOf { (product, qty) -> product.price * qty }
            val subtotal = (subtotalRaw * 100.0).roundToInt() / 100.0
            val deliveryFee = if (subtotal >= MarketCatalog.FREE_DELIVERY_THRESHOLD) {
                0.0
            } else {
                MarketCatalog.STANDARD_DELIVERY_FEE
            }
            val totalAmount = ((subtotal + deliveryFee) * 100.0).roundToInt() / 100.0

            val summary = cartLines.joinToString(" • ") { (product, qty) ->
                "${qty}× ${product.nameAr}"
            }.take(1000)

            val itemMaps = cartLines.map { (product, qty) ->
                mapOf(
                    "productId" to product.id,
                    "nameAr" to product.nameAr,
                    "unitPrice" to product.price,
                    "quantity" to qty,
                )
            }

            val orderNumber = "AM-${Random.nextInt(1000, 9999)}"
            val orderDocRef = db.collection("users").document(uid).collection("orders").document()

            val orderPayload = mutableMapOf<String, Any>(
                "userId" to uid,
                "orderNumber" to orderNumber,
                "customerName" to cleanName,
                "phone" to cleanPhone,
                "deliveryAddress" to cleanAddress,
                "paymentMethod" to paymentMethod.raw,
                "status" to OrderStatus.PENDING.raw,
                "itemsSummary" to summary,
                "items" to itemMaps,
                "totalItems" to totalItems,
                "subtotal" to subtotal,
                "deliveryFee" to deliveryFee,
                "totalAmount" to totalAmount,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (cleanNotes != null) {
                orderPayload["deliveryNotes"] = cleanNotes
            }

            val batch = db.batch()
            batch.set(orderDocRef, orderPayload)
            val cartCol = db.collection("users").document(uid).collection("cart")
            cartLines.forEach { (product, _) ->
                batch.delete(cartCol.document(product.id))
            }
            batch.commit().await()

            // Best-effort save of latest delivery info to user profile
            updateProfile(
                userId = uid,
                displayName = cleanName,
                phone = cleanPhone,
                address = cleanAddress,
                notes = cleanNotes.orEmpty(),
            )

            Result.success(orderNumber)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ordersPath)
            Result.failure(e)
        }
    }

    suspend fun cancelOrder(userId: String, orderId: String): Result<Unit> {
        val path = "users/$userId/orders/$orderId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            require(orderId.isNotBlank()) { "Order ID missing" }
            db.collection("users").document(uid).collection("orders").document(orderId)
                .update(
                    mapOf(
                        "status" to OrderStatus.CANCELLED.raw,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    private fun logStreamErrorIfActive(
        error: Throwable,
        operationType: OperationType,
        path: String,
        uid: String,
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val toLog = firestoreError ?: error
        if (auth.currentUser?.uid == uid) {
            handleFirestoreError(toLog, operationType, path)
        }
    }
}
