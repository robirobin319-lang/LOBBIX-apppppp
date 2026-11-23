package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DepositRequest
import com.example.model.PaymentConfig
import com.example.model.PlayerProfile
import com.example.model.UiState
import com.example.model.WalletTransaction
import com.example.model.WithdrawalRequest
import com.example.ui.components.CenteredResponsiveColumn
import com.example.ui.components.EmptyArenaCard
import com.example.ui.components.EmptyStateVariant
import com.example.ui.components.StatusPill
import com.example.ui.components.TacticalCard
import com.example.ui.theme.AmberPending
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.CrimsonDark
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.GoldTrophy
import com.example.ui.theme.GunmetalCard
import com.example.ui.theme.NeonScarlet
import com.example.ui.theme.SteelBorder
import com.example.util.QrCodeGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

private val WALLET_UPI_REGEX = Regex("^[a-zA-Z0-9._-]{2,}@[a-zA-Z0-9]{2,}$")

@Composable
fun WalletScreen(
    profileState: UiState<PlayerProfile?>,
    paymentConfigState: UiState<PaymentConfig>,
    depositRequestsState: UiState<List<DepositRequest>>,
    withdrawalRequestsState: UiState<List<WithdrawalRequest>>,
    transactionsState: UiState<List<WalletTransaction>>,
    isBusy: Boolean,
    onBack: () -> Unit,
    onSubmitDeposit: (
        amount: Long,
        utrNumber: String,
        adminUpiId: String,
        screenshotName: String,
        screenshotBase64: String,
        onDone: () -> Unit
    ) -> Unit,
    onSubmitWithdrawal: (
        upiId: String,
        amount: Long,
        onDone: () -> Unit
    ) -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val profile = (profileState as? UiState.Success)?.data
    val balance = profile?.walletBalance ?: 0L
    val lockedBalance = profile?.lockedBalance ?: 0L
    val paymentConfig = (paymentConfigState as? UiState.Success)?.data ?: PaymentConfig()
    val deposits = (depositRequestsState as? UiState.Success)?.data.orEmpty()
    val withdrawals = (withdrawalRequestsState as? UiState.Success)?.data.orEmpty()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0: Add Money, 1: Withdraw, 2: History

    CenteredResponsiveColumn {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with proper status bar safe area + 20dp downward offset
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 20.dp, start = 8.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("wallet_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CrispWhite)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("PLAYER WALLET", style = MaterialTheme.typography.headlineLarge, color = CrispWhite)
            }

            // Wallet Balance Card (Clean numeric display without currency suffixes)
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = GunmetalCard,
                    border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(GunmetalCard, CrimsonDark.copy(alpha = 0.45f))
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = GoldTrophy)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AVAILABLE BALANCE", style = MaterialTheme.typography.labelMedium, color = CoolSlate)
                            }
                            if (lockedBalance > 0L) {
                                StatusPill(
                                    text = "LOCKED: $lockedBalance",
                                    containerColor = AmberPending.copy(alpha = 0.18f),
                                    contentColor = AmberPending,
                                    icon = Icons.Filled.Lock
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = balance.toString(),
                            style = MaterialTheme.typography.displayLarge,
                            color = CrispWhite,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("wallet_balance_value")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CarbonSurface,
                contentColor = CrispWhite,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = CrimsonRed
                        )
                    }
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Add Money", style = MaterialTheme.typography.labelLarge) },
                    modifier = Modifier.testTag("wallet_tab_deposit")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Withdraw", style = MaterialTheme.typography.labelLarge) },
                    modifier = Modifier.testTag("wallet_tab_withdraw")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("History", style = MaterialTheme.typography.labelLarge) },
                    modifier = Modifier.testTag("wallet_tab_history")
                )
            }

            when (selectedTab) {
                0 -> AddMoneyTab(
                    paymentConfig = paymentConfig,
                    deposits = deposits,
                    isBusy = isBusy,
                    onCopyUpi = { upi ->
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("Admin UPI ID", upi))
                        Toast.makeText(context, "UPI ID copied", Toast.LENGTH_SHORT).show()
                    },
                    onSubmitDeposit = onSubmitDeposit
                )

                1 -> WithdrawTab(
                    availableBalance = balance,
                    withdrawals = withdrawals,
                    isBusy = isBusy,
                    onSubmitWithdrawal = onSubmitWithdrawal
                )

                else -> TransactionHistoryTab(transactionsState = transactionsState)
            }
        }
    }
}

@Composable
private fun AddMoneyTab(
    paymentConfig: PaymentConfig,
    deposits: List<DepositRequest>,
    isBusy: Boolean,
    onCopyUpi: (String) -> Unit,
    onSubmitDeposit: (
        amount: Long,
        utrNumber: String,
        adminUpiId: String,
        screenshotName: String,
        screenshotBase64: String,
        onDone: () -> Unit
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var amountText by rememberSaveable { mutableStateOf("") }
    var utrText by rememberSaveable { mutableStateOf("") }
    var screenshotName by rememberSaveable { mutableStateOf("") }
    var screenshotBase64 by rememberSaveable { mutableStateOf("") }
    var previewBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isProcessingImage by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    val parsedAmount = amountText.toLongOrNull()?.takeIf { it > 0L }
    val qrPayload = remember(paymentConfig.upiId, paymentConfig.merchantName, parsedAmount) {
        buildString {
            append("upi://pay?pa=")
            append(Uri.encode(paymentConfig.upiId))
            append("&pn=")
            append(Uri.encode(paymentConfig.merchantName))
            append("&cu=INR")
            if (parsedAmount != null) {
                append("&am=")
                append(parsedAmount)
            }
        }
    }
    val qrBitmap = remember(qrPayload) {
        QrCodeGenerator.generateQrBitmap(qrPayload, pixelSize = 512)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            formError = null
            isProcessingImage = true
            scope.launch {
                val processed = withContext(Dispatchers.IO) {
                    encodePaymentScreenshot(context, uri)
                }
                isProcessingImage = false
                if (processed == null) {
                    formError = "Could not read selected screenshot image. Try another image."
                } else {
                    screenshotName = processed.fileName
                    screenshotBase64 = processed.base64
                    previewBitmap = processed.preview
                }
            }
        }
    }

    val canSubmit = !isBusy &&
        !isProcessingImage &&
        parsedAmount != null &&
        utrText.trim().length >= 4 &&
        screenshotBase64.isNotBlank()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Admin UPI ID + Real Scannable QR Code Card
        item {
            TacticalCard(accentBorder = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.QrCode2, contentDescription = null, tint = CrimsonRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SCAN QR OR COPY ADMIN UPI ID", style = MaterialTheme.typography.titleMedium, color = CrispWhite)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = paymentConfig.instruction,
                    style = MaterialTheme.typography.bodySmall,
                    color = CoolSlate
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Real scannable QR code
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = CrispWhite,
                        border = BorderStroke(2.dp, CrimsonRed.copy(alpha = 0.65f)),
                        modifier = Modifier.size(196.dp)
                    ) {
                        Image(
                            bitmap = qrBitmap,
                            contentDescription = "Scannable Admin UPI QR Code",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp)
                                .testTag("deposit_qr_image"),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CarbonSurface,
                    border = BorderStroke(1.dp, SteelBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ADMIN UPI ID", style = MaterialTheme.typography.labelSmall, color = CoolSlate)
                            Text(
                                text = paymentConfig.upiId,
                                style = MaterialTheme.typography.titleMedium,
                                color = CrispWhite,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("deposit_admin_upi")
                            )
                        }
                        OutlinedButton(
                            onClick = { onCopyUpi(paymentConfig.upiId) },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.7f)),
                            modifier = Modifier.testTag("copy_admin_upi_button")
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy UPI ID", tint = CrimsonRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy", color = CrispWhite, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // Deposit Verification Submission Form
        item {
            TacticalCard {
                Text("SUBMIT PAYMENT PROOF", style = MaterialTheme.typography.titleMedium, color = CrispWhite)
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CarbonSurface,
                    border = BorderStroke(1.dp, SteelBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = AmberPending, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.deposit_pending_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = CoolSlate
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() }; formError = null },
                    label = { Text("Deposit Amount") },
                    placeholder = { Text("Enter amount paid") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deposit_amount_input"),
                    colors = lobbixFieldColors()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = utrText,
                    onValueChange = { utrText = it; formError = null },
                    label = { Text("Transaction ID / UTR Number") },
                    placeholder = { Text("e.g., 419283746501") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deposit_utr_input"),
                    colors = lobbixFieldColors()
                )

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !isProcessingImage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("select_screenshot_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (previewBitmap != null) EmeraldLive else CrimsonRed.copy(alpha = 0.7f))
                ) {
                    if (isProcessingImage) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CrimsonRed, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Loading Screenshot...", color = CrispWhite)
                    } else {
                        Icon(
                            imageVector = if (previewBitmap != null) Icons.Filled.CheckCircle else Icons.Filled.UploadFile,
                            contentDescription = null,
                            tint = if (previewBitmap != null) EmeraldLive else CrimsonRed
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (previewBitmap != null) "Change Payment Screenshot" else "Select Payment Screenshot",
                            color = CrispWhite,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                if (previewBitmap != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CarbonSurface,
                        border = BorderStroke(1.dp, EmeraldLive.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                bitmap = previewBitmap!!,
                                contentDescription = "Selected Payment Screenshot Preview",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, SteelBorder, RoundedCornerShape(10.dp))
                                    .testTag("deposit_screenshot_preview"),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Screenshot Attached",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldLive
                                )
                                Text(
                                    text = screenshotName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CoolSlate,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                if (formError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(formError!!, color = NeonScarlet, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        val amt = parsedAmount
                        when {
                            amt == null || amt !in 1L..100000L -> formError = "Enter a valid deposit amount."
                            utrText.trim().length < 4 -> formError = "Enter a valid Transaction ID / UTR."
                            screenshotBase64.isBlank() -> formError = "Select a payment screenshot before submitting."
                            else -> {
                                onSubmitDeposit(
                                    amt,
                                    utrText.trim(),
                                    paymentConfig.upiId,
                                    screenshotName,
                                    screenshotBase64
                                ) {
                                    amountText = ""
                                    utrText = ""
                                    screenshotName = ""
                                    screenshotBase64 = ""
                                    previewBitmap = null
                                }
                            }
                        }
                    },
                    enabled = canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_deposit_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
                ) {
                    Text("SUBMIT DEPOSIT REQUEST", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Recent Deposit Requests
        item {
            Text("DEPOSIT REQUESTS", style = MaterialTheme.typography.titleMedium, color = CrispWhite)
        }
        if (deposits.isEmpty()) {
            item {
                EmptyArenaCard(
                    title = "No Deposit Requests",
                    subtitle = "Submitted UTR & screenshot verification requests appear here as Pending until Admin review.",
                    variant = EmptyStateVariant.WALLET
                )
            }
        } else {
            items(deposits, key = { it.id }) { dep ->
                val statusColor = when (dep.status) {
                    "Approved" -> EmeraldLive
                    "Rejected" -> NeonScarlet
                    else -> AmberPending
                }
                TacticalCard(modifier = Modifier.testTag("deposit_request_${dep.id}")) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Amount: ${dep.amount}", style = MaterialTheme.typography.titleMedium, color = CrispWhite)
                            Text("UTR: ${dep.utrNumber}", style = MaterialTheme.typography.bodySmall, color = CoolSlate)
                            if (dep.screenshotName.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Image, contentDescription = null, tint = CoolSlate, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(dep.screenshotName, style = MaterialTheme.typography.labelSmall, color = CoolSlate, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        StatusPill(
                            text = dep.status,
                            containerColor = statusColor.copy(alpha = 0.16f),
                            contentColor = statusColor
                        )
                    }
                    if (dep.adminNote.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Admin: ${dep.adminNote}", style = MaterialTheme.typography.bodySmall, color = CoolSlate)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun WithdrawTab(
    availableBalance: Long,
    withdrawals: List<WithdrawalRequest>,
    isBusy: Boolean,
    onSubmitWithdrawal: (upiId: String, amount: Long, onDone: () -> Unit) -> Unit
) {
    var upiInput by rememberSaveable { mutableStateOf("") }
    var amountInput by rememberSaveable { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }

    val parsedAmount = amountInput.toLongOrNull() ?: 0L
    val hasValidUpi = WALLET_UPI_REGEX.matches(upiInput.trim())
    val hasSufficientBalance = parsedAmount in 1L..availableBalance

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            TacticalCard(accentBorder = true) {
                Text("WITHDRAW TO UPI", style = MaterialTheme.typography.titleMedium, color = CrispWhite)
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CarbonSurface,
                    border = BorderStroke(1.dp, SteelBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = AmberPending, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.withdraw_pending_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = CoolSlate
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = upiInput,
                    onValueChange = { upiInput = it; formError = null },
                    label = { Text("Your UPI ID") },
                    placeholder = { Text("e.g., player@upi") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("withdraw_upi_input"),
                    colors = lobbixFieldColors()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it.filter { ch -> ch.isDigit() }; formError = null },
                    label = { Text("Withdrawal Amount (Max $availableBalance)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("withdraw_amount_input"),
                    colors = lobbixFieldColors()
                )

                if (parsedAmount > availableBalance) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Insufficient wallet balance ($availableBalance available).",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonScarlet
                    )
                }

                if (formError != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(formError!!, color = NeonScarlet, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        when {
                            !hasValidUpi -> formError = "Enter a valid UPI ID (such as name@upi)."
                            parsedAmount <= 0L -> formError = "Enter a valid withdrawal amount."
                            parsedAmount > availableBalance -> formError = "Insufficient balance ($availableBalance available)."
                            else -> {
                                onSubmitWithdrawal(upiInput.trim(), parsedAmount) {
                                    upiInput = ""
                                    amountInput = ""
                                }
                            }
                        }
                    },
                    enabled = !isBusy && hasValidUpi && hasSufficientBalance,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_withdraw_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
                ) {
                    Text("REQUEST WITHDRAWAL", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        item {
            Text("WITHDRAWAL REQUESTS", style = MaterialTheme.typography.titleMedium, color = CrispWhite)
        }
        if (withdrawals.isEmpty()) {
            item {
                EmptyArenaCard(
                    title = "No Withdrawal Requests",
                    subtitle = "Pending requests lock funds safely and auto-refund if rejected by Admin.",
                    variant = EmptyStateVariant.WALLET
                )
            }
        } else {
            items(withdrawals, key = { it.id }) { wd ->
                val statusLabel = when {
                    wd.status == "Rejected" && wd.refundProcessed -> "Rejected • Refunded"
                    else -> wd.status
                }
                val statusColor = when (wd.status) {
                    "Approved" -> EmeraldLive
                    "Rejected" -> NeonScarlet
                    else -> AmberPending
                }
                TacticalCard(modifier = Modifier.testTag("withdraw_request_${wd.id}")) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Amount: ${wd.amount}", style = MaterialTheme.typography.titleMedium, color = CrispWhite)
                            Text("UPI: ${wd.upiId}", style = MaterialTheme.typography.bodySmall, color = CoolSlate)
                        }
                        StatusPill(
                            text = statusLabel,
                            containerColor = statusColor.copy(alpha = 0.16f),
                            contentColor = statusColor
                        )
                    }
                    if (wd.adminNote.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Admin: ${wd.adminNote}", style = MaterialTheme.typography.bodySmall, color = CoolSlate)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun TransactionHistoryTab(
    transactionsState: UiState<List<WalletTransaction>>
) {
    when (transactionsState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CrimsonRed)
            }
        }

        is UiState.Error -> {
            Box(modifier = Modifier.padding(16.dp)) {
                TacticalCard { Text(transactionsState.message, color = NeonScarlet) }
            }
        }

        is UiState.Success -> {
            val items = transactionsState.data
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (items.isEmpty()) {
                    item {
                        EmptyArenaCard(
                            title = "No Wallet Activity Yet",
                            subtitle = "Deposits, withdrawals, refunds, and match prizes appear here.",
                            variant = EmptyStateVariant.WALLET
                        )
                    }
                } else {
                    items(items, key = { it.id }) { tx ->
                        TransactionRow(tx = tx)
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun TransactionRow(tx: WalletTransaction) {
    val isCredit = tx.type == "CREDIT" || tx.type == "WINNING" || tx.type == "REFUND" ||
        (tx.type == "DEPOSIT" && tx.status == "Approved")
    val isPending = tx.status == "Pending"
    val badgeColor = when {
        isPending -> AmberPending
        isCredit -> EmeraldLive
        else -> NeonScarlet
    }

    TacticalCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(badgeColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCredit) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                    contentDescription = null,
                    tint = badgeColor
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tx.title, style = MaterialTheme.typography.titleSmall, color = CrispWhite)
                Text(
                    text = "Ref: ${tx.referenceCode} • ${tx.status}",
                    style = MaterialTheme.typography.bodySmall,
                    color = CoolSlate
                )
            }
            Text(
                text = if (isPending) "${tx.amount}" else "${if (isCredit) "+" else "-"}${tx.amount}",
                style = MaterialTheme.typography.titleMedium,
                color = badgeColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private data class ProcessedScreenshot(
    val fileName: String,
    val base64: String,
    val preview: ImageBitmap
)

private fun encodePaymentScreenshot(context: Context, uri: Uri): ProcessedScreenshot? {
    return try {
        val resolver = context.contentResolver
        val fileName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: "payment_proof.jpg"

        val decoded = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return null

        val maxSide = 280
        val largest = max(decoded.width, decoded.height)
        val scaled = if (largest > maxSide) {
            val scale = maxSide.toFloat() / largest.toFloat()
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            decoded
        }

        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 55, baos)
        val encoded = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        ProcessedScreenshot(
            fileName = fileName,
            base64 = encoded,
            preview = scaled.asImageBitmap()
        )
    } catch (_: Exception) {
        null
    }
}
