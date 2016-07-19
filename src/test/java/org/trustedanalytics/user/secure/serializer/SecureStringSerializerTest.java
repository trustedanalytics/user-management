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

import org.apache.commons.lang.NotImplementedException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.trustedanalytics.user.secure.EncryptionService;

@RunWith(MockitoJUnitRunner.class)
public class SecureStringSerializerTest {

    private static final String CIPHER = "16-DigitsCipherK";
    private static final String SALT = "Randomly_Ganareted_32-DigitsSalt";

    private static final EncryptionService ENCRYPTION_SERVICE = new EncryptionService(CIPHER, SALT);
    private static final HashedStringRedisSerializer HASHED_STRING_REDIS_SERIALIZER = new HashedStringRedisSerializer(ENCRYPTION_SERVICE);

    @Test
    public void testStringSerialize_allOK() {
        String userMail = "email@example.com";

        byte[] sut = HASHED_STRING_REDIS_SERIALIZER.serialize(userMail);

        Assert.assertArrayEquals(sut, "jVxfRzp42MAbwvZj3nyMkZKPXriLhRh2uH7lMvxsmbw=".getBytes());
    }

    @Test(expected = NotImplementedException.class)
    public void testStringDeserialize_exceptionThrown() {
        String sut = HASHED_STRING_REDIS_SERIALIZER.deserialize("anything".getBytes());
    }
}
