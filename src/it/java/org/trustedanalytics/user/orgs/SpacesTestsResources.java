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

import org.trustedanalytics.cloud.cc.api.CcSpace;
import org.trustedanalytics.cloud.cc.api.CcSpacesList;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SpacesTestsResources {

    public static CcSpacesList getSpacesReturnedByCf() {
        CcSpacesList ccSpacesList = new CcSpacesList();

        List<CcSpace> spaces = new ArrayList<>(3);
        spaces.add(new CcSpace(UUID.fromString("0a5ea71f-d5f3-4037-9fbe-0029a08f88f1"), "dev", UUID.fromString("c02595b9-4603-4be3-8d71-08243d479918")));
        spaces.add(new CcSpace(UUID.fromString("1824631a-2a96-4bf5-94c0-8687ad6e12db"), "dev", UUID.fromString("ac4f8585-5cba-4c1e-a6bb-4b236fee7fff")));
        spaces.add(new CcSpace(UUID.fromString("63ddabd0-bf28-46d5-a21e-e8e4aa45448f"), "dev", UUID.fromString("af6ccea5-65ee-4601-b95c-3bc09554ca75")));
        
        ccSpacesList.setSpaces(spaces);
        return ccSpacesList;
    }

    public static String getSpacesExpectedToBeReturnedBySc() {
        return
            // @formatter:off
            "[" +
              "{" +
                 "\"metadata\":{" +
                    "\"guid\":\"0a5ea71f-d5f3-4037-9fbe-0029a08f88f1\"" +
                 "}," +
                 "\"entity\":{" +
                    "\"name\":\"dev\"," +
                    "\"organization_guid\":\"c02595b9-4603-4be3-8d71-08243d479918\"" +
                 "}" +
              "}," +
              "{" +
                 "\"metadata\":{" +
                    "\"guid\":\"1824631a-2a96-4bf5-94c0-8687ad6e12db\"" +
                 "}," +
                 "\"entity\":{" +
                    "\"name\":\"dev\"," +
                    "\"organization_guid\":\"ac4f8585-5cba-4c1e-a6bb-4b236fee7fff\"" +
                 "}" +
              "}," +
              "{" +
                 "\"metadata\":{" +
                    "\"guid\":\"63ddabd0-bf28-46d5-a21e-e8e4aa45448f\"" +
                 "}," +
                 "\"entity\":{" +
                    "\"name\":\"dev\"," +
                    "\"organization_guid\":\"af6ccea5-65ee-4601-b95c-3bc09554ca75\"" +
                 "}" +
              "}" +
           "]";
            // @formatter:on
    }
}
