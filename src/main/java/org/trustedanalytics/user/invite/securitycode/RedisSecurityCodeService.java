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
package org.trustedanalytics.user.invite.securitycode;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisOperations;

import java.util.UUID;

public class RedisSecurityCodeService implements SecurityCodeService {

    private static final String SECURITY_CODES_KEY = "security-codes";
    private final HashOperations<String, String, SecurityCode> hashOps;

    public RedisSecurityCodeService(RedisOperations<String, SecurityCode> template) {
        this.hashOps = template.opsForHash();
    }

    @Override
    public SecurityCode generateCode(String email) {
        int attempts=3;

        SecurityCode code;
        boolean added;
        do {
            code = new SecurityCode(email, UUID.randomUUID().toString());
            added = hashOps.putIfAbsent(SECURITY_CODES_KEY, code.getCode(), code);
        } while (!added && --attempts > 0);

        if (!added) {
            throw new SecurityCodeGenerationException("Security code generating conflict");
        }

        return code;
    }

    @Override
    public SecurityCode use(SecurityCode code) {
        hashOps.delete(SECURITY_CODES_KEY, code.getCode());
        return code;
    }

    @Override
    public SecurityCode verify(String code) {
        if (!hashOps.hasKey(SECURITY_CODES_KEY, code)) {
            throw new InvalidSecurityCodeException("Invalid security code " + code);
        }

        return hashOps.get(SECURITY_CODES_KEY, code);
    }
}
