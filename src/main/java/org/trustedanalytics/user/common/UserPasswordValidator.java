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

package org.trustedanalytics.user.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.trustedanalytics.cloud.cc.api.manageusers.Role;

import java.util.List;

public class UserPasswordValidator implements PasswordValidator {

    private static int DEFAULT_MIN_PASSWORD_LENGTH = 6;

    public UserPasswordValidator() {}

    public UserPasswordValidator(int minPasswordLength) {
        this.DEFAULT_MIN_PASSWORD_LENGTH = minPasswordLength;
    }

    @Override
    public void validate(String password) {

        if(password.isEmpty()) {
            throw new EmptyPasswordException("Password cannot be empty.");
        }

        if(password.length() < this.DEFAULT_MIN_PASSWORD_LENGTH) {
            throw new TooShortPasswordException("Password should consist of minimum " +
                    this.DEFAULT_MIN_PASSWORD_LENGTH + " characters.");
        }
    }

}