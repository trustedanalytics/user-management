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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InMemorySecurityCodeService implements SecurityCodeService {

    private Map<String, SecurityCode> codes = new HashMap<String, SecurityCode>();
    
    @Override
    public SecurityCode generateCode(String email) {
        SecurityCode code = new SecurityCode(email, UUID.randomUUID().toString());
        codes.put(code.getCode(), code);
        return code;
    }

    @Override
    public SecurityCode use(SecurityCode code) {
        if (!codes.containsKey(code.getCode())) {
            throw new InvalidSecurityCodeException("Bad security code: "+code.getCode());
        }
        return codes.remove(code);
    }

    @Override
    public SecurityCode verify(String code) {
        if (!codes.containsKey(code)) {
            throw new InvalidSecurityCodeException("Bad security code: "+code);
        }
        return codes.get(code);
    }
}
