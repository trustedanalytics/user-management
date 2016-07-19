/**
 *  Copyright (c) 2016 Intel Corporation 
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.trustedanalytics.user.secure;

import com.google.common.base.Preconditions;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;


public class EncryptionService {
    private MessageDigest sha;
    private SecretKeySpec key;
    private SecureRandom secureRandom;

    public EncryptionService(String cipher, String salt) {
        this(cipher, salt, new SecureRandom());
    }

    public EncryptionService(String cipher, String salt, SecureRandom sRandom) {
        Preconditions.checkNotNull(cipher);
        Preconditions.checkArgument(cipher.length() == 16, "Encryption key has to be 128-bit long");
        Preconditions.checkNotNull(salt);
        Preconditions.checkArgument(salt.length() == 32, "Hash salt should be 256-bit long");

        secureRandom = sRandom;
        key = new SecretKeySpec(cipher.getBytes(), "AES");

        try {
            sha = MessageDigest.getInstance("SHA-256");
            sha.update(salt.getBytes());

        } catch (NoSuchAlgorithmException e)  {
            throw new RuntimeEncryptionException("Unable to create encryption service", e);
        }
    }

    public SecureJson encrypt(byte[] toEncrypt) throws EncryptionException {
        try {
            Cipher encryptionCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            encryptionCipher.init(Cipher.ENCRYPT_MODE, key, secureRandom);
            byte[] ivBytes = encryptionCipher.getIV();
            byte[] valueBytes = encryptionCipher.doFinal(toEncrypt);

            return new SecureJson(ivBytes, valueBytes);
        } catch (NoSuchAlgorithmException|
                NoSuchPaddingException e) {
            throw new RuntimeEncryptionException("Unable to create encryption cipher", e);
        } catch (InvalidKeyException |
                IllegalBlockSizeException |
                BadPaddingException e) {
            throw new EncryptionException("Unable to encrypt message", e);
        }
    }

    public byte[] decrypt(SecureJson toDecrypt) throws EncryptionException {
        try {
            Cipher decryptionCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            IvParameterSpec iv = new IvParameterSpec(toDecrypt.getIv());
            decryptionCipher.init(Cipher.DECRYPT_MODE, key, iv);

            return decryptionCipher.doFinal(toDecrypt.getValue());
        } catch (NoSuchAlgorithmException|
                NoSuchPaddingException e)  {
            throw new RuntimeEncryptionException("Unable to create decryption cipher", e);
        } catch (InvalidKeyException |
                InvalidAlgorithmParameterException |
                IllegalBlockSizeException |
                BadPaddingException e) {
            throw new EncryptionException("Unable to decrypt message", e);
        }
    }

    public String hash(String toHash) {
        return new String(Base64.getEncoder().encode(sha.digest(toHash.getBytes())));
    }
}
