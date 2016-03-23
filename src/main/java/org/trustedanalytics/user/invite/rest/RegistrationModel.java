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
package org.trustedanalytics.user.invite.rest;

import lombok.Getter;
import lombok.Setter;

/**
 * Created by GER\sbultrow on 11/21/14
 */
public class RegistrationModel {
    @Getter @Setter
    private String password;

    @Getter @Setter
    private String org;

    @Getter @Setter
    private String orgGuid;

    @Getter @Setter
    private String userGuid;
}
