/**
 *  Copyright (c) 2015 Intel Corporation 
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
package org.trustedanalytics.user.invite.keyvaluestore;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisOperations;

import java.util.Collection;

public class RedisStore<T> implements KeyValueStore<T> {
    private final String storeKey;

    private final HashOperations<String, String, T> hashOps;

    public RedisStore(RedisOperations<String, T> redisTemplate, String key) {
        hashOps = redisTemplate.opsForHash();
        storeKey = key;
    }

    @Override
    public boolean hasKey(String key) {
        return hashOps.hasKey(storeKey, key);
    }

    @Override
    public T get(String key) {
        return hashOps.get(storeKey, key);
    }

    @Override
    public void remove(String key) {
        hashOps.delete(storeKey, key);
    }

    @Override
    public void put(String key, T invitations) {
        hashOps.put(storeKey, key, invitations);
    }

    @Override
    public boolean putIfAbsent(String key, T value) {
        return hashOps.putIfAbsent(storeKey, key, value);
    }

    @Override
    public Collection<T> values() {
        return hashOps.values(storeKey);
    }
}
