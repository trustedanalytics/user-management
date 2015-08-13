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

import java.util.Objects;

public class SecurityCode {

    private long id;

    private String email;

    private String code;

    public SecurityCode() {
    }

    public SecurityCode(String email, String code) {
        this(0, email, code);
    }

    public SecurityCode(long id, String email, String code) {
        this.id = id;
        this.email = email;
        this.code = code;
    }

    public String getEmail() {
        return email;
    }

    public String getCode() {
        return code;
    }

    public long getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, code);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SecurityCode other = (SecurityCode) obj;
        return Objects.equals(email, other.email) && Objects.equals(code, other.code);
    }

    @Override
    public String toString() {
        return "SecurityCode [id=" + id + ", email=" + email + ", code=" + code
            + "]";
    }
}
