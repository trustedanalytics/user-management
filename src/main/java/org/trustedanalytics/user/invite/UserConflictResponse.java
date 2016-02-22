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
package org.trustedanalytics.user.invite;

import lombok.Getter;
import lombok.Setter;

public final class UserConflictResponse {
    public enum ConflictedField {
        USER("user"),
        ORG("organization");

        private String value;

        ConflictedField(String value) {
            this.value = value;
        }
    }

    @Getter  @Setter
    private String field;

    @Getter @Setter
    private String message;

    private UserConflictResponse() {
    }

    public static UserConflictResponse of(ConflictedField field, String message) {
        UserConflictResponse response = new UserConflictResponse();
        response.setField(field.value);
        response.setMessage(message);
        return response;
    }
}
