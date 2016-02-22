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

import org.trustedanalytics.cloud.cc.api.manageusers.Role;

import java.util.List;

public class FormatUserRolesValidator implements UserRolesValidator {
    private void validateSpaceUserRoles(List<Role> roles) {
        if(roles == null || roles.isEmpty()) {
            throw new WrongUserRolesException("You must have at least one role.");
        }
    }

    private void validateOrgUserRoles(List<Role> roles) {
        if(roles == null) {
            throw new WrongUserRolesException("You cannot perform request without specified roles.");
        }
    }

    @Override
    public void validateSpaceRoles(List<Role> roles) {
        validateSpaceUserRoles(roles);
    }

    @Override
    public void validateOrgRoles(List<Role> roles) {
        validateOrgUserRoles(roles);
    }
}