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
package org.trustedanalytics.users.orgs;

import org.trustedanalytics.cloud.cc.api.CcOrg;
import org.trustedanalytics.cloud.cc.api.CcOrgsList;
import org.trustedanalytics.cloud.cc.api.CcSpace;
import org.trustedanalytics.cloud.cc.api.CcSpacesList;
import org.trustedanalytics.user.orgs.Organization;
import org.trustedanalytics.user.orgs.Space;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class OrgsTestsResources {

    private static String space1Name = "space1";
    private static String space2Name = "space2";
    private static String space3Name = "space3";
    private static String space1Guid = "b071445c-d076-4574-8c4d-4c724d471ccb";
    private static String space2Guid = "304f4c61-6350-432c-bb99-cee246f6b794";
    private static String space3Guid = "a0eec463-f56a-4c40-9aea-952651c876f0";

    private static String org1Name = "demo";
    private static String org2Name = "test";
    private static String org1Guid = "6f9ba4de-d17e-4677-af1e-185b52af9bb9";
    private static String org2Guid = "484709c1-7b88-4762-b1e3-453406d7bcfd";

    public static CcOrgsList getOrgsReturnedByCf() {

        CcOrgsList ccOrgsList = new CcOrgsList();
        
        List<CcOrg> list = new ArrayList<>(2);
        CcOrg org = new CcOrg(UUID.fromString(org1Guid), org1Name);
        org.getEntity().setStatus("active");
        list.add(org);
        org = new CcOrg(UUID.fromString(org2Guid), org2Name);
        org.getEntity().setStatus("active");
        list.add(org);

        ccOrgsList.setOrgs(list);
        return ccOrgsList;
    }

    public static UUID getFakeUUID() {
        String fakeGuid = "ae5fabc5-466b-47e3-a427-35649d3aa767";
        return UUID.fromString(fakeGuid);
    }

    public static String getOrgsExpectedToBeReturnedBySc() {
        return
            // @formatter:off
            "[" +
                "{" +
                    "\"metadata\":{" +
                        "\"guid\":\"" + org1Guid + "\"" +
                    "}," +
                    "\"entity\":{" +
                        "\"name\":\"" + org1Name + "\"," +
                        "\"status\":\"active\"" +
                    "}" +
                "}," +
                "{" +
                    "\"metadata\":{" +
                        "\"guid\":\"" + org2Guid + "\"" +
                    "}," +
                    "\"entity\":{" +
                        "\"name\":\"" + org2Name + "\"," +
                        "\"status\":\"active\"" +
                    "}" +
                "}" +
            "]";
            // @formatter:on
    }

    public static CcSpacesList getSpacesReturnedByCf() {
        CcSpacesList ccSpacesList = new CcSpacesList();

        List<CcSpace> spaces = new ArrayList<>(3);
        spaces.add(new CcSpace(UUID.fromString(space1Guid), space1Name, UUID.fromString(org1Guid)));
        spaces.add(new CcSpace(UUID.fromString(space2Guid), space2Name, UUID.fromString(org2Guid)));        
        spaces.add(new CcSpace(UUID.fromString(space3Guid), space3Name, UUID.fromString(org2Guid)));
        
        ccSpacesList.setSpaces(spaces);
        return ccSpacesList;
    }

    public static String getSpacesReturnedByCf2() {
        return
            // @formatter:off
            "{" +
                "\"total_results\":3," +
                "\"total_pages\":1," +
                "\"prev_url\":null," +
                "\"next_url\":null," +
                "\"resources\":[" +
                    "{" +
                        "\"metadata\":{" +
                            "\"guid\":\"" + space1Guid + "\"" +
                        "}," +
                        "\"entity\":{" +
                            "\"name\":\"" + space1Name + "\"," +
                            "\"organization_guid\":\"" + org1Guid + "\"" +
                        "}" +
                    "}," +
                    "{" +
                        "\"metadata\":{" +
                        "   \"guid\":\"" + space2Guid + "\"" +
                        "}," +
                        "\"entity\":{" +
                            "\"name\":\"" + space2Name + "\"," +
                            "\"organization_guid\":\"" + org2Guid + "\"" +
                        "}" +
                    "}," +
                    "{" +
                        "\"metadata\":{" +
                            "\"guid\":\"" + space3Guid + "\"" +
                        "}," +
                        "\"entity\":{" +
                            "\"name\":\"" + space3Name + "\"," +
                            "\"organization_guid\":\"" + org2Guid + "\"" +
                        "}" +
                    "}" +
                "]" +
            "}";
            // @formatter:on
    }

    public static String getOrgsWithSpacesExpectedToBeReturnedBySc() {
        return
            // @formatter:off
            "[{" +
                "\"guid\":\"" + org2Guid + "\"," +
                "\"name\":\"" + org2Name + "\"," +
                "\"spaces\":[{" +
                    "\"guid\":\"" + space2Guid + "\"," +
                    "\"name\":\"" + space2Name + "\"" +
                "}," +
                "{" +
                    "\"guid\":\"" + space3Guid + "\"," +
                    "\"name\":\"" + space3Name + "\"" +
                "}]," +
                "\"manager\":false" +
            "}," +
            "{" +
                "\"guid\":\"" + org1Guid + "\"," +
                "\"name\":\"" + org1Name + "\"," +
                "\"spaces\":[{" +
                    "\"guid\":\"" + space1Guid + "\"," +
                    "\"name\":\"" + space1Name + "\"" +
                "}]," +
                "\"manager\":false" +
            "}]";
            // @formatter:on
    }

    public static Collection<CcOrg> getOrgsReturnedByCfAdapter() {
        CcOrg org1 = new CcOrg(UUID.fromString(org1Guid), org1Name);
        CcOrg org2 = new CcOrg(UUID.fromString(org2Guid), org2Name);
        return Arrays.asList(org1, org2);
    }

    public static Collection<CcSpace> getSpacesReturnedByCfAdapter() {
        CcSpace space1 =
            new CcSpace(UUID.fromString(space1Guid), space1Name, UUID.fromString(org1Guid));
        CcSpace space2 =
            new CcSpace(UUID.fromString(space2Guid), space2Name, UUID.fromString(org2Guid));
        CcSpace space3 =
            new CcSpace(UUID.fromString(space3Guid), space3Name, UUID.fromString(org2Guid));
        return Arrays.asList(space1, space2, space3);
    }

    public static Collection<Organization> getOrgsWithSpacesExpectedToBeReturnedByScBeforeJsonization() {

        Organization org1 = new Organization(UUID.fromString(org1Guid), org1Name);
        org1.addSpace(new Space(UUID.fromString(space1Guid), space1Name));
        Organization org2 = new Organization(UUID.fromString(org2Guid), org2Name);
        org2.addSpace(new Space(UUID.fromString(space2Guid), space2Name));
        org2.addSpace(new Space(UUID.fromString(space3Guid), space3Name));

        Collection<Organization> orgs = new LinkedList<>();
        orgs.add(org2);
        orgs.add(org1);

        return orgs;
    }
}
