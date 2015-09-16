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
import org.trustedanalytics.user.invite.WrongEmailAddressException;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.List;

public class SpaceUserRolesValidator implements UserRolesValidator {
    private static final Log LOGGER = LogFactory.getLog(SpaceUserRolesValidator.class);

    public SpaceUserRolesValidator() {}

    private void validateSpaceUserRoles(List<Role> roles) {
        if(roles.isEmpty()) {
            throw new WrongUserRolesException("You must have at least one role.");
        }
    }

    @Override
    public void validate(List<Role> roles) {
        validateSpaceUserRoles(roles);
    }

}