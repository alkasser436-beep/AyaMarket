package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.MarketProduct
import com.example.data.PaymentMethod
import com.example.data.UserProfile
import com.example.ui.CartPricingSummary
import com.example.ui.components.ProductVisualBadge
import com.example.ui.components.QuantityStepper
import com.example.ui.components.formatJod

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailSheet(
    product: MarketProduct,
    currentCartQuantity: Int,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onApplyQuantity: (Int) -> Unit,
) {
    var desiredQty by remember(product.id, currentCartQuantity) {
        mutableIntStateOf(currentCartQuantity.coerceAtLeast(1))
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                ProductVisualBadge(product = product, size = 76.dp)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.category.labelAr,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        text = product.nameAr,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = product.unitAr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("sheet_fav_button"),
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = stringResource(R.string.cd_favorite),
                        tint = if (isFavorite) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = product.descriptionAr,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    Text(
                        text = "السعر للوحدة",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${formatJod(product.price)} ${stringResource(R.string.currency_jod)}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }

                QuantityStepper(
                    quantity = desiredQty,
                    onDecrease = { if (desiredQty > 1) desiredQty-- },
                    onIncrease = { if (desiredQty < 99) desiredQty++ },
                    testTagPrefix = "sheet_${product.id}",
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    onApplyQuantity(desiredQty)
                    onDismiss()
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("sheet_add_button"),
            ) {
                val totalForProduct = formatJod(product.price * desiredQty)
                Text(
                    text = if (currentCartQuantity > 0) {
                        "تحديث السلة • $totalForProduct د.أ"
                    } else {
                        "أضف للسلة • $totalForProduct د.أ"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutSheet(
    userProfile: UserProfile?,
    defaultName: String,
    pricing: CartPricingSummary,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmitOrder: (String, String, String, String, PaymentMethod) -> Unit,
) {
    var name by remember(userProfile) {
        mutableStateOf(userProfile?.displayName?.ifBlank { defaultName } ?: defaultName)
    }
    var phone by remember(userProfile) { mutableStateOf(userProfile?.phone.orEmpty()) }
    var address by remember(userProfile) { mutableStateOf(userProfile?.address.orEmpty()) }
    var notes by remember(userProfile) { mutableStateOf(userProfile?.notes.orEmpty()) }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH_ON_DELIVERY) }
    var validationError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(
                text = stringResource(R.string.checkout_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("الاسم الكامل") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("checkout_name_input"),
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("رقم الهاتف للتواصل") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("checkout_phone_input"),
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("عنوان التوصيل بالتفصيل (المنطقة، الشارع، البناية)") },
                minLines = 2,
                maxLines = 3,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("checkout_address_input"),
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("ملاحظات للمندوب (اختياري)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("checkout_notes_input"),
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "طريقة الدفع",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))

            PaymentMethod.entries.forEach { method ->
                Surface(
                    onClick = { paymentMethod = method },
                    shape = RoundedCornerShape(14.dp),
                    color = if (paymentMethod == method) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    border = BorderStroke(
                        1.dp,
                        if (paymentMethod == method) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        RadioButton(
                            selected = paymentMethod == method,
                            onClick = { paymentMethod = method },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = method.labelAr,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

            if (!validationError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = validationError.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "المبلغ المستحق (${pricing.totalItems} صنف)",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "${formatJod(pricing.total)} ${stringResource(R.string.currency_jod)}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val cleanName = name.trim()
                    val cleanPhone = phone.trim()
                    val cleanAddress = address.trim()
                    when {
                        cleanName.isEmpty() -> validationError = "يرجى إدخال اسم المستلم"
                        cleanPhone.length < 5 -> validationError = "يرجى إدخال رقم هاتف صحيح (5 أرقام على الأقل)"
                        cleanAddress.length < 5 -> validationError = "يرجى كتابة عنوان التوصيل بالتفصيل"
                        else -> {
                            validationError = null
                            onSubmitOrder(cleanName, cleanPhone, cleanAddress, notes.trim(), paymentMethod)
                        }
                    }
                },
                enabled = !isSubmitting,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("confirm_order_button"),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(22.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.checkout_confirm),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSheet(
    userProfile: UserProfile?,
    defaultName: String,
    userEmail: String?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSaveProfile: (String, String, String, String) -> Unit,
    onSignOut: () -> Unit,
) {
    var name by remember(userProfile) {
        mutableStateOf(userProfile?.displayName?.ifBlank { defaultName } ?: defaultName)
    }
    var phone by remember(userProfile) { mutableStateOf(userProfile?.phone.orEmpty()) }
    var address by remember(userProfile) { mutableStateOf(userProfile?.address.orEmpty()) }
    var notes by remember(userProfile) { mutableStateOf(userProfile?.notes.orEmpty()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(
                text = stringResource(R.string.profile_address_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            if (!userEmail.isNullOrBlank()) {
                Text(
                    text = userEmail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("الاسم") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("رقم الهاتف") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("عنوان التوصيل الافتراضي") },
                minLines = 2,
                maxLines = 3,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("ملاحظات التوصيل") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            if (!errorMsg.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMsg.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isEmpty()) {
                        errorMsg = "يرجى إدخال الاسم"
                    } else {
                        errorMsg = null
                        onSaveProfile(trimmed, phone.trim(), address.trim(), notes.trim())
                    }
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_profile_button"),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Text("حفظ بيانات التوصيل")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onSignOut,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sign_out_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.sign_out))
            }
        }
    }
}
