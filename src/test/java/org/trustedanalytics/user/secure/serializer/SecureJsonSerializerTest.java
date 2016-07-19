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
package org.trustedanalytics.user.secure.serializer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.trustedanalytics.user.invite.securitycode.SecurityCode;
import org.trustedanalytics.user.secure.EncryptionException;
import org.trustedanalytics.user.secure.EncryptionService;
import org.trustedanalytics.user.secure.SecureJson;

import java.security.GeneralSecurityException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SecureJsonSerializerTest {

    private static final byte[] IV = new byte[]{64, -1, 109, 62, -93, -110, 2, 112, -86, 12, -25, -100, -93, 34, 39, 90};
    private static final byte[] VALUE = new byte[]{-77, 122, 65, 36, 109, -43, -21, -35, 74, 101, 43, 112, 23, -113, 119, -43,
            122, -97, -7, 94, 117, 20, 2, -39, -112, -28, -104, 115, 37, -117, -64, 113, 32, -57, 32, -100, -77, -124, -88, -75,
            -15, 105, -26, -89, -56, 66, 117, -32, 101, 73, -43, -120, -78, 124, 54, 81, -85, 102, 74, 100, -49, 82, -30, -28};
    private static final SecureJson SECURE_JSON = new SecureJson(IV, VALUE);
    private static final String SERIALIZED_JSON = "{\"iv\":\"QP9tPqOSAnCqDOecoyInWg==\"," +
            "\"value\":\"s3pBJG3V691KZStwF4931Xqf+V51FALZkOSYcyWLwHEgxyCcs4SotfFp5qfIQnXgZUnViLJ8NlGrZkpkz1Li5A==\"}";
    private static final SecurityCode SECURITY_CODE = new SecurityCode("email@example.com", "code");
    private static final byte[] PLAIN_SECURITY_CODE = new JacksonJsonRedisSerializer<SecurityCode>(SecurityCode.class).serialize(SECURITY_CODE);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private EncryptionService encryptionService;

    SecureJacksonJsonRedisSerializer<SecurityCode> secureJsonRedisSerializer;


    @Before
    public void setUp() {
        secureJsonRedisSerializer = new SecureJacksonJsonRedisSerializer<SecurityCode>(SecurityCode.class, encryptionService);
    }

    @Test
    public void testJsonSeralization_allOK() throws EncryptionException {
        when(encryptionService.encrypt(PLAIN_SECURITY_CODE)).thenReturn(SECURE_JSON);

        byte[] sut = secureJsonRedisSerializer.serialize(SECURITY_CODE);

        Assert.assertEquals(new String(sut),SERIALIZED_JSON);
    }

    @Test
    public void testJsonDeseralization_allOK() throws EncryptionException {
        when(encryptionService.decrypt(eq(SECURE_JSON))).thenReturn(PLAIN_SECURITY_CODE);

        SecurityCode sut = secureJsonRedisSerializer.deserialize(SERIALIZED_JSON.getBytes());

        Assert.assertEquals(sut.getCode(),"code");
        Assert.assertEquals(sut.getEmail(),"email@example.com");
    }

    @Test
    public void testJsonDeseralization_badJson_exceptionThrown() throws EncryptionException {
        thrown.expect(SerializationException.class);
        thrown.expectMessage("Could not read Secure-JSON");

        SecurityCode sut = secureJsonRedisSerializer.deserialize("{".getBytes());
    }

    @Test
    public void testJsonDeseralization_SecurityException_exceptionThrown() throws EncryptionException {
        thrown.expect(SerializationException.class);
        thrown.expectMessage("Could not decrypt Secure-JSON");
        when(encryptionService.decrypt(any())).thenThrow(new EncryptionException("a", new GeneralSecurityException()));

        SecurityCode sut = secureJsonRedisSerializer.deserialize(SERIALIZED_JSON.getBytes());
    }

    @Test
    public void testJsonSeralization_SecurityException_exceptionThrown() throws EncryptionException {
        thrown.expect(SerializationException.class);
        thrown.expectMessage("Could not encrypt Secure-JSON");
        when(encryptionService.encrypt(any())).thenThrow(new EncryptionException("a", new GeneralSecurityException()));

        byte[] sut = secureJsonRedisSerializer.serialize(SECURITY_CODE);
    }
}
