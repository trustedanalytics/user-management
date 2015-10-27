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
package org.trustedanalytics.user.current;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import org.trustedanalytics.cloud.uaa.ChangePasswordRequest;
import org.trustedanalytics.cloud.uaa.UaaOperations;
import org.trustedanalytics.user.common.UserPasswordValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/users/current")
public class CurrentUserController {

    private final UaaOperations uaaClient;

    private final UserDetailsFinder detailsFinder;

    private final UserPasswordValidator passwordValidator;

    @Autowired
    public CurrentUserController(UaaOperations uaaClient, UserDetailsFinder detailsFinder, UserPasswordValidator passwordValidator) {
        this.uaaClient = uaaClient;
        this.detailsFinder = detailsFinder;
        this.passwordValidator = passwordValidator;
    }

    @RequestMapping(method = RequestMethod.GET)
    public UserModel getUser(Authentication auth) {
        UserModel user = new UserModel();
        user.setEmail(auth.getName());
        user.setRole(detailsFinder.getRole(auth));

        return user;
    }

    @RequestMapping(value = "/password", method = PUT,
            produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public void changePassword(@RequestBody ChangePasswordRequest request, Authentication auth) {
        passwordValidator.validate(request.getNewPassword());
        uaaClient.changePassword(detailsFinder.findUserId(auth), request);
    }

}
