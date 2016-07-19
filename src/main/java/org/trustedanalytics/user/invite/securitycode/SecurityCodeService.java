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

import org.trustedanalytics.user.invite.keyvaluestore.KeyValueStore;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class SecurityCodeService {
    private final KeyValueStore<SecurityCode> store;

    public SecurityCodeService( KeyValueStore<SecurityCode> store) {
        this.store = store;
    }

    public SecurityCode generateCode(String email) {
        int attempts = 3;
        SecurityCode code;

        for(int i = 0; i < attempts; i++) {
            code = new SecurityCode(email, UUID.randomUUID().toString());
            if(store.putIfAbsent(code.getCode(), code)) {
                return code;
            }
        }
        throw new SecurityCodeGenerationException("Security code generating conflict");
    }

    public SecurityCode redeem(SecurityCode code) {
        store.remove(code.getCode());
        return code;
    }

    public SecurityCode verify(String code) {
        if (!store.hasKey(code)) {
            throw new InvalidSecurityCodeException("Invalid security code " + code);
        }

        return store.get(code);
    }

    public Optional<SecurityCode> findByMail(String email) {
        return store.values()
                .stream()
                .filter(entry -> entry.getEmail().equals(email))
                .findFirst();
    }

    public Set<String> getKeys() {
        return store.values()
                .stream()
                .map(entry -> entry.getEmail())
                .collect(Collectors.toSet());
    }
}
