package com.example.quizz

import android.net.Uri
import androidx.activity.ComponentActivity
import com.google.gson.annotations.SerializedName
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.ConnectionIdentity
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import com.solana.mobilewalletadapter.clientlib.successPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.math.BigInteger

object SolanaAchievementManager {

    private const val APP_NAME = "AI Quest Companion"

    private const val APP_URI =
        "https://aiquest.app"

    private const val DEVNET_RPC =
        "https://api.devnet.solana.com/"

    private const val MEMO_PROGRAM_ID =
        "MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr"

    private const val BASE58_ALPHABET =
        "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

    private var walletAdapter: MobileWalletAdapter? = null

    private var sender: ActivityResultSender? = null

    private val rpcService: SolanaRpcService by lazy {

        Retrofit.Builder()
            .baseUrl(DEVNET_RPC)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(SolanaRpcService::class.java)
    }

    // ---------------------------------------------------------
    // INITIALIZE
    // ---------------------------------------------------------

    fun initialize(activity: ComponentActivity) {

        val identity =
            ConnectionIdentity(
                identityUri = Uri.parse(APP_URI),
                iconUri = Uri.parse("icon.png"),
                identityName = APP_NAME
            )

        walletAdapter =
            MobileWalletAdapter(
                connectionIdentity = identity
            )

        sender =
            ActivityResultSender(activity)
    }

    // ---------------------------------------------------------
    // VERIFY ACHIEVEMENT
    // ---------------------------------------------------------

    suspend fun verifyAchievement(
        achievementId: String,
        achievementTitle: String
    ): Result<VerificationResult> {

        return try {

            val adapter =
                walletAdapter
                    ?: return Result.failure(
                        IllegalStateException(
                            "SolanaAchievementManager has not been initialized."
                        )
                    )

            val activityResultSender =
                sender
                    ?: return Result.failure(
                        IllegalStateException(
                            "Solana ActivityResultSender has not been initialized."
                        )
                    )

            // -------------------------------------------------
            // 1. GET DEVNET BLOCKHASH
            // -------------------------------------------------

            val blockhash =
                withContext(Dispatchers.IO) {

                    val response =
                        rpcService.getLatestBlockhash(
                            RpcRequest(
                                method = "getLatestBlockhash",
                                params = listOf(
                                    mapOf(
                                        "commitment" to "confirmed"
                                    )
                                )
                            )
                        )

                    if (response.error != null) {
                        throw IllegalStateException(
                            "Solana RPC error: ${response.error.message}"
                        )
                    }

                    response.result
                        ?.value
                        ?.blockhash
                        ?: throw IllegalStateException(
                            "Could not obtain Devnet blockhash."
                        )
                }



            // -------------------------------------------------
            // 2. OPEN WALLET
            // -------------------------------------------------

            val transactionResult = adapter.transact(activityResultSender) {
                val authResult = authorize(
                    identityUri = Uri.parse(APP_URI),
                    iconUri = Uri.parse("icon.png"),
                    identityName = APP_NAME,
                    rpcCluster = com.solana.mobilewalletadapter.clientlib.RpcCluster.Devnet
                )

                val account = authResult.accounts.firstOrNull()
                    ?: throw IllegalStateException("No Solana account was returned by the wallet.")

                val walletPublicKey = account.publicKey




                    val walletAddress =
                        base58Encode(
                            walletPublicKey
                        )

                    // -----------------------------------------
                    // 3. CREATE MEMO
                    // -----------------------------------------

                    val memo =
                        "$APP_NAME | Achievement: " +
                                "$achievementId | $achievementTitle"

                    // -----------------------------------------
                    // 4. CREATE RAW DEVNET TRANSACTION
                    // -----------------------------------------

                    val transactionBytes =
                        buildMemoTransaction(
                            payerPublicKey =
                                walletPublicKey,

                            recentBlockhash =
                                base58Decode(blockhash),

                            memo =
                                memo
                        )

                    // -----------------------------------------
                    // 5. WALLET SIGNS + SENDS
                    // -----------------------------------------

                    val sendResult =
                        signAndSendTransactions(
                            arrayOf(
                                transactionBytes
                            )
                        )

                    VerificationPayload(
                        walletAddress =
                            walletAddress,

                        achievementId =
                            achievementId,

                        achievementTitle =
                            achievementTitle,

                        signatures =
                            sendResult.signatures.toList()
                    )
                }

            // -------------------------------------------------
            // 6. HANDLE MWA RESULT
            // -------------------------------------------------

            when (transactionResult) {

                is TransactionResult.Success -> {

                    val payload =
                        transactionResult.successPayload

                    if (payload == null) {

                        Result.failure(
                            Exception(
                                "Wallet returned no verification payload."
                            )
                        )

                    } else {

                        val signatureBytes =
                            payload.signatures.firstOrNull()

                        if (signatureBytes == null) {

                            Result.failure(
                                Exception(
                                    "No transaction signature was returned."
                                )
                            )

                        } else {

                            val signature =
                                base58Encode(
                                    signatureBytes
                                )

                            Result.success(
                                VerificationResult(
                                    walletAddress =
                                        payload.walletAddress,

                                    achievementId =
                                        payload.achievementId,

                                    achievementTitle =
                                        payload.achievementTitle,

                                    transactionSignature =
                                        signature
                                )
                            )
                        }
                    }
                }

                is TransactionResult.Failure -> {

                    Result.failure(
                        transactionResult.e
                    )
                }

                is TransactionResult.NoWalletFound -> {

                    Result.failure(
                        Exception(
                            transactionResult.message
                        )
                    )
                }
            }

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    // =========================================================
    // BUILD SOLANA MEMO TRANSACTION
    // =========================================================

    private fun buildMemoTransaction(
        payerPublicKey: ByteArray,
        recentBlockhash: ByteArray,
        memo: String
    ): ByteArray {

        require(
            payerPublicKey.size == 32
        ) {
            "Payer public key must contain 32 bytes."
        }

        require(
            recentBlockhash.size == 32
        ) {
            "Recent blockhash must contain 32 bytes."
        }

        val memoProgramPublicKey =
            base58Decode(
                MEMO_PROGRAM_ID
            )

        require(
            memoProgramPublicKey.size == 32
        ) {
            "Memo program public key must contain 32 bytes."
        }

        val memoBytes =
            memo.toByteArray(
                Charsets.UTF_8
            )

        /*
         * Legacy Solana Message:
         *
         * Header:
         *   required signatures = 1
         *   readonly signed = 0
         *   readonly unsigned = 1
         *
         * Accounts:
         *   0 = payer
         *   1 = Memo program
         */

        val message =
            concat(
                byteArrayOf(
                    1,
                    0,
                    1
                ),

                // account count
                encodeShortVec(2),

                // payer
                payerPublicKey,

                // memo program
                memoProgramPublicKey,

                // recent blockhash
                recentBlockhash,

                // instruction count
                encodeShortVec(1),

                // program id index
                byteArrayOf(1),

                // accounts count = 0
                byteArrayOf(0),

                // memo data length
                encodeShortVec(
                    memoBytes.size
                ),

                // memo data
                memoBytes
            )

        /*
         * Transaction:
         *
         * signature count = 1
         * 64 zero bytes = unsigned signature placeholder
         * message
         */

        return concat(
            encodeShortVec(1),

            ByteArray(64),

            message
        )
    }

    // =========================================================
    // SHORT VEC ENCODING
    // =========================================================

    private fun encodeShortVec(
        value: Int
    ): ByteArray {

        require(value >= 0)

        var remaining =
            value

        val result =
            ArrayList<Byte>()

        do {

            var elem =
                remaining and 0x7F

            remaining =
                remaining ushr 7

            if (remaining != 0) {
                elem =
                    elem or 0x80
            }

            result.add(
                elem.toByte()
            )

        } while (remaining != 0)

        return result.toByteArray()
    }

    // =========================================================
    // BASE58 ENCODE
    // =========================================================

    private fun base58Encode(
        input: ByteArray
    ): String {

        if (input.isEmpty()) {
            return ""
        }

        var value =
            BigInteger(
                1,
                input
            )

        val result =
            StringBuilder()

        val fiftyEight =
            BigInteger.valueOf(58)

        while (
            value > BigInteger.ZERO
        ) {

            val remainder =
                value.mod(
                    fiftyEight
                )

            result.append(
                BASE58_ALPHABET[
                    remainder.toInt()
                ]
            )

            value =
                value.divide(
                    fiftyEight
                )
        }

        for (byte in input) {

            if (byte.toInt() == 0) {
                result.append('1')
            } else {
                break
            }
        }

        return result
            .reverse()
            .toString()
    }

    // =========================================================
    // BASE58 DECODE
    // =========================================================

    private fun base58Decode(
        input: String
    ): ByteArray {

        if (input.isEmpty()) {
            return ByteArray(0)
        }

        var value =
            BigInteger.ZERO

        val fiftyEight =
            BigInteger.valueOf(58)

        for (character in input) {

            val index =
                BASE58_ALPHABET.indexOf(
                    character
                )

            require(index >= 0) {
                "Invalid Base58 character: $character"
            }

            value =
                value
                    .multiply(fiftyEight)
                    .add(
                        BigInteger.valueOf(
                            index.toLong()
                        )
                    )
        }

        val raw =
            if (value == BigInteger.ZERO) {

                ByteArray(0)

            } else {

                val bytes =
                    value.toByteArray()

                if (
                    bytes.isNotEmpty() &&
                    bytes[0].toInt() == 0
                ) {
                    bytes.copyOfRange(
                        1,
                        bytes.size
                    )
                } else {
                    bytes
                }
            }

        var leadingZeros =
            0

        for (character in input) {

            if (character == '1') {
                leadingZeros++
            } else {
                break
            }
        }

        return ByteArray(leadingZeros) + raw
    }

    // =========================================================
    // BYTE ARRAY CONCAT
    // =========================================================

    private fun concat(
        vararg arrays: ByteArray
    ): ByteArray {

        val totalSize =
            arrays.sumOf {
                it.size
            }

        val result =
            ByteArray(totalSize)

        var position =
            0

        for (array in arrays) {

            array.copyInto(
                destination = result,
                destinationOffset = position
            )

            position +=
                array.size
        }

        return result
    }

    // =========================================================
    // DATA CLASSES
    // =========================================================

    data class VerificationPayload(
        val walletAddress: String,
        val achievementId: String,
        val achievementTitle: String,
        val signatures: List<ByteArray>
    )

    data class VerificationResult(
        val walletAddress: String,
        val achievementId: String,
        val achievementTitle: String,
        val transactionSignature: String
    )

    // =========================================================
    // RETROFIT
    // =========================================================

    private interface SolanaRpcService {

        @POST(".")
        suspend fun getLatestBlockhash(
            @Body request: RpcRequest
        ): RpcResponse<LatestBlockhashResult>
    }

    private data class RpcRequest(
        @SerializedName("jsonrpc")
        val jsonrpc: String = "2.0",

        @SerializedName("id")
        val id: Int = 1,

        @SerializedName("method")
        val method: String,

        @SerializedName("params")
        val params: List<Any>
    )

    private data class RpcResponse<T>(
        @SerializedName("jsonrpc")
        val jsonrpc: String? = null,

        @SerializedName("result")
        val result: T? = null,

        @SerializedName("error")
        val error: RpcError? = null
    )

    private data class RpcError(
        @SerializedName("code")
        val code: Int? = null,

        @SerializedName("message")
        val message: String? = null
    )

    private data class LatestBlockhashResult(
        @SerializedName("context")
        val context: RpcContext? = null,

        @SerializedName("value")
        val value: LatestBlockhashValue? = null
    )

    private data class LatestBlockhashValue(
        @SerializedName("blockhash")
        val blockhash: String,

        @SerializedName("lastValidBlockHeight")
        val lastValidBlockHeight: Long
    )

    private data class RpcContext(
        @SerializedName("slot")
        val slot: Long? = null
    )
}