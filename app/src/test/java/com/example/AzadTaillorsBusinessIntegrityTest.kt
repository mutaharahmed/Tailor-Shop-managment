package com.example

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupInspectResult
import com.example.data.backup.BackupKind
import com.example.data.backup.BackupManager
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CustomOptionPair
import com.example.data.local.entity.ExpenseCategory
import com.example.data.local.entity.GarmentType
import com.example.data.local.entity.MeasurementFieldValue
import com.example.data.local.entity.MeasurementUnit
import com.example.data.local.entity.OrderStatus
import com.example.data.local.entity.PaymentMethod
import com.example.data.local.entity.PaymentTransactionType
import com.example.data.local.entity.StockMovementType
import com.example.data.preferences.ShopPreferencesRepository
import com.example.data.repository.CustomerRepository
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.InventoryRepository
import com.example.data.repository.MeasurementRepository
import com.example.data.repository.OrderAndPaymentRepository
import com.example.data.repository.OrderDraftInput
import com.example.data.repository.PaymentPostResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AzadTaillorsBusinessIntegrityTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var customerRepo: CustomerRepository
    private lateinit var measurementRepo: MeasurementRepository
    private lateinit var orderRepo: OrderAndPaymentRepository
    private lateinit var inventoryRepo: InventoryRepository
    private lateinit var expenseRepo: ExpenseRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val provider = { db }
        customerRepo = CustomerRepository(provider)
        measurementRepo = MeasurementRepository(provider)
        orderRepo = OrderAndPaymentRepository(provider)
        inventoryRepo = InventoryRepository(provider)
        expenseRepo = ExpenseRepository(provider)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `family members can share phone number and duplicate warning detects match`() = runTest {
        val c1 = customerRepo.createCustomer("Ali Bhutto", "03001234567", "Qasimabad", "").getOrThrow()
        val dupCheck = customerRepo.checkPossibleDuplicates("Bilal Bhutto", "03001234567")
        assertTrue(dupCheck.hasPossibleDuplicate)

        // Legitimate family profile still allowed
        val c2 = customerRepo.createCustomer("Bilal Bhutto", "03001234567", "Qasimabad", "Brother").getOrThrow()
        assertTrue(c1.id != c2.id)
        assertTrue(c1.customerCode != c2.customerCode)
    }

    @Test
    fun `historical measurement versions remain immutable when new versions are saved`() = runTest {
        val customer = customerRepo.createCustomer("Tariq Soomro", "03331112223", "", "").getOrThrow()

        val v1 = measurementRepo.saveNewVersion(
            customerId = customer.id,
            garmentType = GarmentType.SHALWAR_KAMEEZ,
            customGarmentName = "",
            unit = MeasurementUnit.INCHES,
            fields = listOf(
                MeasurementFieldValue("length", "Length", "40"),
                MeasurementFieldValue("chest", "Chest", "42"),
                MeasurementFieldValue("daman", "Daman", "23", isCustom = true)
            ),
            notes = "Initial fit"
        ).getOrThrow()
        assertEquals(1, v1.versionNumber)

        val order1 = orderRepo.createOrder(
            OrderDraftInput(
                customerId = customer.id,
                measurementProfileId = v1.id,
                garmentType = GarmentType.SHALWAR_KAMEEZ,
                customGarmentName = "",
                quantity = 1,
                collarStyle = "Ban",
                frontDesign = "Simple Patti",
                cuffStyle = "Single Cuff",
                pocketStyle = "Front + 2 Side",
                sleeveStyle = "Full Fitted",
                bottomStyle = "Round Daman",
                fitStyle = "Regular",
                embroideryStyle = "None",
                customOptions = listOf(CustomOptionPair("Button", "Wooden")),
                additionalInstructions = "",
                stitchingPricePkr = 2500L,
                initialAdvancePkr = 1000L,
                advancePaymentMethod = PaymentMethod.CASH,
                orderDate = System.currentTimeMillis(),
                deliveryDate = System.currentTimeMillis() + 86_400_000L * 5,
                status = OrderStatus.CONFIRMED,
                advanceIdempotencyKey = UUID.randomUUID().toString()
            )
        ).getOrThrow()

        // Customer updates measurements -> creates v2 without mutating v1
        val v2 = measurementRepo.saveNewVersion(
            customerId = customer.id,
            garmentType = GarmentType.SHALWAR_KAMEEZ,
            customGarmentName = "",
            unit = MeasurementUnit.INCHES,
            fields = listOf(
                MeasurementFieldValue("length", "Length", "41.5"),
                MeasurementFieldValue("chest", "Chest", "44")
            ),
            notes = "Looser chest"
        ).getOrThrow()
        assertEquals(2, v2.versionNumber)

        // Verify v1 still has original length 40 and order1 still references v1
        val reloadedV1 = measurementRepo.getById(v1.id)!!
        val fieldsV1 = measurementRepo.decodeFields(reloadedV1.fieldsJson)
        assertEquals("40", fieldsV1.first { it.key == "length" }.value)
        assertEquals(v1.id, db.orderDao().getById(order1.id)?.measurementProfileId)
    }

    @Test
    fun `payments enforce idempotency, allow legitimate installments, and protect refund bounds`() = runTest {
        val customer = customerRepo.createCustomer("Sajid Memon", "03219998877", "", "").getOrThrow()
        val order = orderRepo.createOrder(
            OrderDraftInput(
                customerId = customer.id,
                measurementProfileId = null,
                garmentType = GarmentType.SHIRT,
                customGarmentName = "",
                quantity = 2,
                collarStyle = "Collar",
                frontDesign = "",
                cuffStyle = "",
                pocketStyle = "",
                sleeveStyle = "",
                bottomStyle = "",
                fitStyle = "Slim",
                embroideryStyle = "",
                customOptions = emptyList(),
                additionalInstructions = "",
                stitchingPricePkr = 3000L,
                initialAdvancePkr = 500L,
                advancePaymentMethod = PaymentMethod.CASH,
                orderDate = System.currentTimeMillis(),
                deliveryDate = System.currentTimeMillis() + 86_400_000L * 3,
                status = OrderStatus.CONFIRMED,
                advanceIdempotencyKey = "adv-key-1"
            )
        ).getOrThrow()

        // Same idempotency key twice -> replays instead of duplicating
        val key1 = "pay-inst-1"
        val r1 = orderRepo.postPaymentTransaction(
            orderId = order.id,
            transactionType = PaymentTransactionType.INSTALLMENT,
            method = PaymentMethod.CASH,
            otherMethodLabel = "",
            amountPkr = 1000L,
            note = "1st installment",
            idempotencyKey = key1
        )
        assertTrue(r1 is PaymentPostResult.Success && !r1.wasIdempotentReplay)

        val r1Replay = orderRepo.postPaymentTransaction(
            orderId = order.id,
            transactionType = PaymentTransactionType.INSTALLMENT,
            method = PaymentMethod.CASH,
            otherMethodLabel = "",
            amountPkr = 1000L,
            note = "1st installment",
            idempotencyKey = key1
        )
        assertTrue(r1Replay is PaymentPostResult.Success && r1Replay.wasIdempotentReplay)

        // Rapid same-amount payment with a NEW key prompts confirmation, then succeeds when confirmed
        val key2 = "pay-inst-2"
        val r2Prompt = orderRepo.postPaymentTransaction(
            orderId = order.id,
            transactionType = PaymentTransactionType.INSTALLMENT,
            method = PaymentMethod.CASH,
            otherMethodLabel = "",
            amountPkr = 1000L,
            note = "2nd installment",
            idempotencyKey = key2,
            bypassRecentDuplicateCheck = false
        )
        assertTrue(r2Prompt is PaymentPostResult.RequiresDuplicateConfirmation)

        val r2Confirmed = orderRepo.postPaymentTransaction(
            orderId = order.id,
            transactionType = PaymentTransactionType.INSTALLMENT,
            method = PaymentMethod.CASH,
            otherMethodLabel = "",
            amountPkr = 1000L,
            note = "2nd installment",
            idempotencyKey = key2,
            bypassRecentDuplicateCheck = true
        )
        assertTrue(r2Confirmed is PaymentPostResult.Success)

        // Net paid = 500 (adv) + 1000 + 1000 = 2500 -> Remaining = 500
        val summary = orderRepo.observeOrder(order.id).first()!!
        assertEquals(2500L, summary.netPaidPkr)
        assertEquals(500L, summary.remainingBalancePkr)

        // Refund exceeding net paid (3000 > 2500) must fail
        val badRefund = orderRepo.postPaymentTransaction(
            orderId = order.id,
            transactionType = PaymentTransactionType.REFUND,
            method = PaymentMethod.CASH,
            otherMethodLabel = "",
            amountPkr = 3000L,
            note = "Over-refund",
            idempotencyKey = "refund-bad"
        )
        assertTrue(badRefund is PaymentPostResult.Error)

        // Valid refund of 500 adjusts net paid to 2000 while keeping full audit history
        val goodRefund = orderRepo.postPaymentTransaction(
            orderId = order.id,
            transactionType = PaymentTransactionType.REFUND,
            method = PaymentMethod.CASH,
            otherMethodLabel = "",
            amountPkr = 500L,
            note = "Discount correction",
            idempotencyKey = "refund-good"
        )
        assertTrue(goodRefund is PaymentPostResult.Success)
        val afterRefundSummary = orderRepo.observeOrder(order.id).first()!!
        assertEquals(2000L, afterRefundSummary.netPaidPkr)
        assertEquals(1000L, afterRefundSummary.remainingBalancePkr)
        assertEquals(4, orderRepo.observePaymentsForOrder(order.id).first().size)
    }

    @Test
    fun `inventory tracks traceable movements and prevents overselling`() = runTest {
        val product = inventoryRepo.createProduct(
            name = "Cotton Dress Shirt",
            category = "Shirts",
            size = "L",
            color = "White",
            costPricePkr = 1200L,
            sellingPricePkr = 1800L,
            initialQuantity = 5,
            lowStockThreshold = 2,
            idempotencyKey = "init-1"
        ).getOrThrow()

        // Sell 4 units -> remaining 1 (triggers low stock)
        inventoryRepo.recordStockMovement(
            productId = product.id,
            movementType = StockMovementType.SALE,
            units = 4,
            unitPricePkr = 1800L,
            paymentMethod = PaymentMethod.CASH,
            note = "Walk-in customer",
            idempotencyKey = "sale-1"
        ).getOrThrow()

        // Attempt to sell 2 more when only 1 left -> must fail
        val oversell = inventoryRepo.recordStockMovement(
            productId = product.id,
            movementType = StockMovementType.SALE,
            units = 2,
            unitPricePkr = 1800L,
            paymentMethod = PaymentMethod.CASH,
            note = "",
            idempotencyKey = "sale-2"
        )
        assertTrue(oversell.isFailure)

        val lowStockList = inventoryRepo.observeLowStockProducts().first()
        assertEquals(1, lowStockList.size)
        assertEquals(1, lowStockList.first().quantity)

        val movements = inventoryRepo.observeMovementsForProduct(product.id).first()
        assertEquals(2, movements.size) // INITIAL_STOCK + SALE
    }

    @Test
    fun `backup creates valid archive, detects corruption, and restores with pre-restore safety backup`() = runTest {
        // Use the real singleton AppDatabase for BackupManager file-level test
        AppDatabase.closeForRestore()
        context.deleteDatabase(AppDatabase.DATABASE_NAME)
        File(context.filesDir, "backups").deleteRecursively()
        try {
            val prefs = ShopPreferencesRepository(context)
            val backupMgr = BackupManager(context, prefs)
            val liveCustomerRepo = CustomerRepository { AppDatabase.getInstance(context) }

            val c1 = liveCustomerRepo.createCustomer("Nadir Shah", "03005554433", "Hyderabad", "").getOrThrow()
            assertNotNull(c1)

            val backupEntry = backupMgr.createLocalBackup(BackupKind.MANUAL).getOrThrow()
            assertTrue(backupEntry.file.exists())
            assertEquals(1, backupEntry.manifest?.customerCount)

            // Guard: cannot delete the only remaining valid local backup
            val deleteAttempt = backupMgr.deleteLocalBackup(backupEntry)
            assertTrue(deleteAttempt.isFailure)

            // Corruption detection: write garbage .atbackup and try to inspect it
            val corruptFile = File(context.cacheDir, "corrupt.atbackup").apply {
                writeText("NOT A VALID ZIP OR SQLITE FILE")
            }
            val corruptInspect = backupMgr.inspectBackupFromUri(Uri.fromFile(corruptFile), "corrupt.atbackup")
            assertTrue(corruptInspect is BackupInspectResult.Corrupted)
            corruptFile.delete()

            // Add another customer AFTER backup
            liveCustomerRepo.createCustomer("Zafar Ali", "03112223344", "", "").getOrThrow()
            assertEquals(2, AppDatabase.getInstance(context).customerDao().countCustomers())

            // Inspect original backup -> Restore Preview shows 1 customer
            val validInspect = backupMgr.inspectLocalBackup(backupEntry)
            assertTrue(validInspect is BackupInspectResult.Valid)
            val preview = (validInspect as BackupInspectResult.Valid).preview
            assertEquals(1, preview.customerCount)

            // Perform restore -> creates SAFETY backup of 2-customer DB, then restores 1-customer DB
            backupMgr.performRestore(preview).getOrThrow()
            assertEquals(1, AppDatabase.getInstance(context).customerDao().countCustomers())

            val allBackupsAfterRestore = backupMgr.listLocalBackups()
            assertTrue(allBackupsAfterRestore.any { it.kind == BackupKind.SAFETY })
        } finally {
            AppDatabase.closeForRestore()
            File(context.filesDir, "backups").deleteRecursively()
        }
    }
}
