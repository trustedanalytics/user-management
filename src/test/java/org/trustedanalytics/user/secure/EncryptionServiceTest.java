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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.security.SecureRandom;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;

@RunWith(MockitoJUnitRunner.class)
public class EncryptionServiceTest {

    private static final String CIPHER = "16-DigitsCipherK";
    private static final String SALT = "Randomly_Ganareted_32-DigitsSalt";

    private static final String ORGINAL = "{\"iv\":\"ab==\",\"value\":\"def=\"}";
    private static final byte[] IV = new byte[]{64, -1, 109, 62, -93, -110, 2, 112, -86, 12, -25, -100, -93, 34, 39, 90};
    private static final byte[] ENCRYPTED = new byte[]{-91, -6, 17, -61, -36, -3, 7, -106, -66, -12, 87, 62, 102, -109, -9, -112, 125, -120, 107, -53, 11, 42, 100, 36, -54, 43, 38, 60, -69, -94, 37, -74};
    private static final SecureJson SECURE_JSON = new SecureJson(IV, ENCRYPTED);

    private EncryptionService encryptionService;

    @Mock
    private SecureRandom secureRandom;

    @Before
    public void setUp() {
        encryptionService = new EncryptionService(CIPHER, SALT, secureRandom);
    }

    @Test
    public void testHash_allOK() {
        String userMail = "email@example.com";

        String sut = encryptionService.hash(userMail);

        Assert.assertEquals("jVxfRzp42MAbwvZj3nyMkZKPXriLhRh2uH7lMvxsmbw=",sut);
    }

    @Test
    public void testEncryption_allOK() throws EncryptionException {
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                int length = ((byte[])args[0]).length;
                System.arraycopy(IV, 0, args[0], 0, length);
                return null;
            }
        }).when(secureRandom).nextBytes(any(byte[].class));

        SecureJson sut = encryptionService.encrypt(ORGINAL.getBytes());

        Assert.assertEquals(SECURE_JSON, sut);
    }

    @Test
    public void testDecryption_allOK() throws EncryptionException {
        byte[] sut = encryptionService.decrypt(SECURE_JSON);

        Assert.assertEquals(ORGINAL, new String(sut));
    }

    @Test(expected = EncryptionException.class)
    public void testDecrypt_wrongIV_exceptionThrown() throws EncryptionException {
        SecureJson secureJson = new SecureJson("aaa".getBytes(),"bbb".getBytes());

        byte[] sut = encryptionService.decrypt(secureJson);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test256bitCipher_exceptionThrown() {
        EncryptionService sut = new EncryptionService(CIPHER + CIPHER, SALT);
    }
}
