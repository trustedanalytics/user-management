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
package org.trustedanalytics.user.orgs;

import org.trustedanalytics.cloud.cc.api.CcOrg;
import org.trustedanalytics.cloud.cc.api.CcSpace;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class FormatTranslator {
    private FormatTranslator() {
    }

    public static Collection<Organization> getOrganizationsWithSpaces(Collection<CcOrg> orgs, Collection<CcOrg> mngOrgs,
                                                                      Collection<CcSpace> spacesList) {

        Map<UUID, Organization> outputOrgs = new TreeMap<>();
        for (CcOrg cfOrg : orgs) {
            outputOrgs.put(cfOrg.getGuid(), new Organization(cfOrg) );
        }

        mngOrgs.forEach(mngorg -> {
            if (outputOrgs.containsKey(mngorg.getGuid())){
                outputOrgs.get(mngorg.getGuid()).setManager(true);
            }
        });

        for (CcSpace cfSpace : spacesList) {
            Organization org = outputOrgs.get(cfSpace.getOrgGuid());
            if (org != null) {
                org.addSpace(new Space(cfSpace));
            }
        }

        return new LinkedList<>(outputOrgs.values());
    }


}
