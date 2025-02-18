package com.shyandsy.cipher2

import android.content.Context
import android.util.Base64
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class Cipher2Plugin : FlutterPlugin, MethodCallHandler {
    @JvmField
    val NONCE_LENGTH_IN_BYTES = 12
    @JvmField
    val CHARSET = Charsets.UTF_8

    var channel: MethodChannel? = null

    companion object {
        @JvmStatic
        fun registerWith(registrar: PluginRegistry.Registrar) {
            val instance = Cipher2Plugin()
            instance.onAttachedToEngine(registrar.context(), registrar.messenger())
        }
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        if (call.method == "getPlatformVersion") {
            result.success("Android ${android.os.Build.VERSION.RELEASE}")
        } else if (call.method == "Encrypt_AesCbc128Padding7") {

            return Encrypt_AesCbc128Padding7(call, result)

        } else if (call.method == "Decrypt_AesCbc128Padding7") {

            return Decrypt_AesCbc128Padding7(call, result)

        } else if (call.method == "Generate_Nonce") {

            return Generate_Nonce(call, result)

        } else if (call.method == "Encrypt_AesGcm128") {

            return Encrypt_AesGcm128(call, result)

        } else if (call.method == "Decrypt_AesGcm128") {

            return Decrypt_AesGcm128(call, result)

        } else {
            result.notImplemented()
            return
        }
    }

    // AES 128 cbc padding 7
    fun Encrypt_AesCbc128Padding7(call: MethodCall, result: Result) {
        val data = call.argument<String>("data")
        val key = call.argument<String>("key")
        val iv = call.argument<String>("iv")

        if (data == null || key == null || iv == null) {
            result.error(
                "ERROR_INVALID_PARAMETER_TYPE",
                "the parameters data, key and iv must be all strings",
                null
            )
            return
        }

        val dataArray = data.toByteArray(CHARSET)
        val keyArray = key.toByteArray(CHARSET)
        val ivArray = iv.toByteArray(CHARSET)

        if (keyArray.size != 16 || ivArray.size != 16) {
            result.error(
                "ERROR_INVALID_KEY_OR_IV_LENGTH",
                "the length of key and iv must be all 128 bits",
                null
            )
            return
        }

        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        val keySpec = SecretKeySpec(keyArray, "AES")
        val ivSpec = IvParameterSpec(ivArray)

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)

        val ciphertext = cipher.doFinal(dataArray)

        val text = Base64.encodeToString(ciphertext, Base64.NO_WRAP)

        result.success(text)

        return
    }

    fun Decrypt_AesCbc128Padding7(call: MethodCall, result: Result) {
        val data = call.argument<String>("data")
        val key = call.argument<String>("key")
        val iv = call.argument<String>("iv")

        if (data == null || key == null || iv == null) {
            result.error(
                "ERROR_INVALID_PARAMETER_TYPE",
                "the parameters data, key and iv must be all strings",
                null
            )
            return
        }

        val keyArray = key.toByteArray(CHARSET)
        val ivArray = iv.toByteArray(CHARSET)

        if (keyArray.size != 16 || ivArray.size != 16) {
            result.error(
                "ERROR_INVALID_KEY_OR_IV_LENGTH",
                "the length of key and iv must be all 128 bits",
                null
            )
            return
        }

        var dataArray: ByteArray; // = ByteArray(0)

        try {
            dataArray = Base64.decode(data.toByteArray(CHARSET), Base64.DEFAULT)
            if (dataArray.size % 16 != 0) {
                throw IllegalArgumentException("")
            }
        } catch (e: IllegalArgumentException) {
            result.error(
                "ERROR_INVALID_ENCRYPTED_DATA",
                "the data should be a valid base64 string with length at multiple of 128 bits",
                null
            )
            return
        }

        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        val keySpec = SecretKeySpec(keyArray, "AES")
        val ivSpec = IvParameterSpec(ivArray)

        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)

        val ciphertext = cipher.doFinal(dataArray)

        val text = ciphertext.toString(CHARSET)

        result.success(text)

        return
    }

    /*
    Generate_Nonce

    return a base64 encoded string of 12 bytes nonce
    */
    fun Generate_Nonce(call: MethodCall, result: Result) {
        val secureRandom = SecureRandom()
        val nance = ByteArray(NONCE_LENGTH_IN_BYTES)
        secureRandom.nextBytes(nance)
        val text = Base64.encodeToString(nance, Base64.NO_WRAP)
        result.success(text)

        return
    }

    fun Encrypt_AesGcm128(call: MethodCall, result: Result) {

        val data = call.argument<String>("data")
        val key = call.argument<String>("key")
        val nonce = call.argument<String>("nonce")

        if (data == null || key == null || nonce == null) {
            result.error(
                "ERROR_INVALID_PARAMETER_TYPE",
                "the parameters data, key and nonce must be all strings",
                null
            )
            return
        }

        val dataArray = data.toByteArray(CHARSET)
        val keyArray = key.toByteArray(CHARSET)

        // decode nonce from base64 string
        var nonceArray: ByteArray;
        try {
            nonceArray = Base64.decode(nonce.toByteArray(CHARSET), Base64.DEFAULT)
        } catch (e: IllegalArgumentException) {
            result.error(
                "ERROR_INVALID_KEY_OR_IV_LENGTH",
                "the nonce should be a valid base64 string",
                null
            )
            return
        }

        if (keyArray.size != 16 || nonceArray.size != 12) {
            result.error(
                "ERROR_INVALID_KEY_OR_IV_LENGTH",
                "the length of key and nonce should be 128 bits and 92 bits",
                null
            )
            return
        }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(keyArray, "AES")
        val gcmSpec = GCMParameterSpec(128, nonceArray)

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        val ciphertext = cipher.doFinal(dataArray)

        val text = Base64.encodeToString(ciphertext, Base64.NO_WRAP)

        result.success(text)

        return
    }

    fun Decrypt_AesGcm128(call: MethodCall, result: Result) {
        val data = call.argument<String>("data")
        val key = call.argument<String>("key")
        val nonce = call.argument<String>("nonce")
        var keyArray: ByteArray;
        var nonceArray: ByteArray;
        var dataArray: ByteArray;

        if (data == null || key == null || nonce == null) {
            result.error(
                "ERROR_INVALID_PARAMETER_TYPE",
                "the parameters data, key and nonce must be all strings",
                null
            )
            return
        }

        // key byte array
        keyArray = key.toByteArray(CHARSET)

        // deocde the base64 string to get nonce byte array
        try {
            nonceArray = Base64.decode(nonce.toByteArray(CHARSET), Base64.DEFAULT)
        } catch (e: IllegalArgumentException) {
            result.error(
                "ERROR_INVALID_KEY_OR_IV_LENGTH",
                "the nonce should be a valid base64 string",
                null
            )
            return
        }

        // decode the base64 string to get the data byte array
        try {
            dataArray = Base64.decode(data.toByteArray(CHARSET), Base64.DEFAULT)
        } catch (e: IllegalArgumentException) {
            result.error(
                "ERROR_INVALID_ENCRYPTED_DATA",
                "the data should be a valid base64 string with length at multiple of 128 bits",
                null
            )
            return
        }

        if (keyArray.size != 16 || nonceArray.size != 12) {
            result.error(
                "ERROR_INVALID_KEY_OR_IV_LENGTH",
                "the length of key and nonce should be 128 bits and 92 bits",
                null
            )
            return
        }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(keyArray, "AES")
        val gcmSpec = GCMParameterSpec(128, nonceArray)

        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

        val plaintext = cipher.doFinal(dataArray)

        val text = plaintext.toString(CHARSET)

        result.success(text)

        return
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        onAttachedToEngine(binding.applicationContext, binding.binaryMessenger)
    }

    private fun onAttachedToEngine(applicationContext: Context, messenger: BinaryMessenger) {
        channel = MethodChannel(messenger, "cipher2")
        channel!!.setMethodCallHandler(Cipher2Plugin())
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel?.setMethodCallHandler(null)
        channel = null
    }
}
