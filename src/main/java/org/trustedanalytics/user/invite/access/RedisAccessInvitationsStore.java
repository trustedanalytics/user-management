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
package org.trustedanalytics.user.invite.access;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisOperations;

public class RedisAccessInvitationsStore implements AccessInvitationsStore {
    private static final String ACCESS_INVITATIONS_KEY = "access-invitations";
    private final HashOperations<String, String, AccessInvitations> hashOps;

    public RedisAccessInvitationsStore(RedisOperations<String, AccessInvitations> redisTemplate) {
        hashOps = redisTemplate.opsForHash();
    }


    @Override
    public boolean hasKey(String key) {
        return hashOps.hasKey(ACCESS_INVITATIONS_KEY, key);
    }

    @Override
    public AccessInvitations get(String key) {
        return hashOps.get(ACCESS_INVITATIONS_KEY, key);
    }

    @Override
    public void remove(String key) {
        hashOps.delete(ACCESS_INVITATIONS_KEY, key);
    }

    @Override
    public void put(String key, AccessInvitations invitations) {
        hashOps.put(ACCESS_INVITATIONS_KEY, key, invitations);
    }
}
