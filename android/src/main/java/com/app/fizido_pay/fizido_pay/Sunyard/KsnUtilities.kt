package com.app.fizido_pay.fizido_pay.Sunyard

import java.io.ByteArrayOutputStream
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec


class KSNUtilities {

    var latestKsn: String = "";

    fun getWorkingKey(IPEK: String  , KSN: String ): String {
        var initialIPEK: String = IPEK
        val ksn = KSN.padStart(20, '0')
        var sessionKey = ""
        val newKSN = xORorANDorORfunction(ksn, "0000FFFFFFFFFFE00000", "&")
        val counterKSN = ksn.substring(ksn.length - 5).padStart(16, '0')
        var newKSNtoleft16 = newKSN.substring(newKSN.length - 16)
        val counterKSNbin = Integer.toBinaryString(counterKSN.toInt())
        var binaryCount = counterKSNbin
        for (i in counterKSNbin.indices) {
            val len: Int = binaryCount.length
            var result = ""
            if (binaryCount.substring(0, 1) == "1") {
                result = "1".padEnd(len, '0')
                binaryCount = binaryCount.substring(1)
            } else {
                binaryCount = binaryCount.substring(1)
                continue
            }
            val counterKSN2 = Integer.toHexString(Integer.parseInt(result, 2)).uppercase().padStart(16, '0')
            val newKSN2 = xORorANDorORfunction(newKSNtoleft16, counterKSN2, "|")
            sessionKey = blackBoxLogic(newKSN2, initialIPEK)
            newKSNtoleft16 = newKSN2
            initialIPEK = sessionKey
        }
        return xORorANDorORfunction(sessionKey, "00000000000000FF00000000000000FF", "^")
    }


    private fun blackBoxLogic(ksn: String, iPek: String): String {
        if (iPek.length < 32) {
            latestKsn = ksn;
            val msg = xORorANDorORfunction(iPek, ksn, "^")
            val desreslt = desEncrypt(msg, iPek)
            val rsesskey = xORorANDorORfunction(desreslt, iPek, "^")
            return rsesskey
        }
        val current_sk = iPek
        val ksn_mod = ksn
        val leftIpek = xORorANDorORfunction(current_sk, "FFFFFFFFFFFFFFFF0000000000000000", "&").substring(16)
        val rightIpek = xORorANDorORfunction(current_sk, "0000000000000000FFFFFFFFFFFFFFFF", "&").substring(16)
        val message = xORorANDorORfunction(rightIpek, ksn_mod, "^")
        val desresult = desEncrypt(message, leftIpek)
        val rightSessionKey = xORorANDorORfunction(desresult, rightIpek, "^")
        val resultCurrent_sk = xORorANDorORfunction(current_sk, "C0C0C0C000000000C0C0C0C000000000", "^")
        val leftIpek2 = xORorANDorORfunction(resultCurrent_sk, "FFFFFFFFFFFFFFFF0000000000000000", "&").substring(0, 16)
        val rightIpek2 = xORorANDorORfunction(resultCurrent_sk, "0000000000000000FFFFFFFFFFFFFFFF", "&").substring(16)
        val message2 = xORorANDorORfunction(rightIpek2, ksn_mod, "^")
        val desresult2 = desEncrypt(message2, leftIpek2)
        val leftSessionKey = xORorANDorORfunction(desresult2, rightIpek2, "^")
        return leftSessionKey + rightSessionKey
    }


    private fun hexStringToByteArray(key: String): ByteArray {
        var result: ByteArray = ByteArray(0)
        for (i in 0 until key.length step 2) {
            result += Integer.parseInt(key.substring(i, (i + 2)), 16).toByte()
        }
        return result
    }

    private fun byteArrayToHexString(key: ByteArray): String {
        var st = ""
        for (b in key) {
            st += String.format("%02X", b)
        }
        return st
    }

    private fun desEncrypt(desData: String, key: String): String {
        val keyData = hexStringToByteArray(key)
        val bout = ByteArrayOutputStream()
        try {
            val keySpec: KeySpec = DESKeySpec(keyData)
            val key: SecretKey = SecretKeyFactory.getInstance("DES").generateSecret(keySpec)
            val cipher: Cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            bout.write(cipher.doFinal(hexStringToByteArray(desData)))
        } catch (e: Exception) {
            print("Exception DES Encryption.. " + e.printStackTrace())
        }
        return byteArrayToHexString(bout.toByteArray()).substring(0, 16)
    }


    private fun encryptPinBlock(pan: String, pin: String): String {
        val pan = pan.substring(pan.length - 13).take(12).padStart(16, '0')
        val pin = '0' + pin.length.toString(16) + pin.padEnd(16, 'F')
        return xORorANDorORfunction(pan, pin, "^")
    }

    private fun xORorANDorORfunction(valueA: String, valueB: String, symbol: String = "|"): String {
        val a = valueA.toCharArray();
        val b = valueB.toCharArray()
        var result = ""
        for (i in 0 until a.lastIndex + 1) {
            if (symbol === "|") {
                result += (Integer.parseInt(a[i].toString(), 16).or(Integer.parseInt(b[i].toString(), 16)).toString(16).uppercase())
            } else if (symbol === "^") {
                result += (Integer.parseInt(a[i].toString(), 16).xor
                (Integer.parseInt(b[i].toString(), 16)).toString(16).uppercase())
            } else {
                result += (Integer.parseInt(a[i].toString(), 16).and
                (Integer.parseInt(b[i].toString(), 16))).toString(16).uppercase()
            }
        }
        return result
    }

     fun desEncryptDukpt(workingKey: String, pan: String, clearPin: String): String {
        val pinBlock = xORorANDorORfunction(workingKey, encryptPinBlock(pan, clearPin), "^")
        val keyData = hexStringToByteArray(workingKey)
        val bout = ByteArrayOutputStream()
        try {
            val keySpec: KeySpec = DESKeySpec(keyData)
            val key: SecretKey = SecretKeyFactory.getInstance("DES").generateSecret(keySpec)
            val cipher: Cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            bout.write(cipher.doFinal(hexStringToByteArray(pinBlock)))
            //DES  Encryption
        } catch (e: Exception) {
            println("Exception .. " + e.message)
        }
        return xORorANDorORfunction(
                workingKey, byteArrayToHexString(bout.toByteArray()).substring(
                0,
                16
        ), "^")
    }


}


