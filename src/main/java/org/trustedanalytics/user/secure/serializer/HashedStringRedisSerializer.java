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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.trustedanalytics.user.secure.EncryptionService;

public class HashedStringRedisSerializer extends StringRedisSerializer {

    private EncryptionService encryptionService;

    @Autowired
    public HashedStringRedisSerializer(EncryptionService encryptionService) {
        super();
        this.encryptionService = encryptionService;
    }

    @Override
    public String deserialize(byte[] bytes) {
        throw new NotImplementedException("Hashed value cannot be deserialized");
    }

    @Override
    public byte[] serialize(String string) {
        return super.serialize(hash(string));
    }

    private String hash(String key) {
        return encryptionService.hash(key);
    }
}