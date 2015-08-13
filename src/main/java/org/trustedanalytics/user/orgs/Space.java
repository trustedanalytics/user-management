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

import java.util.UUID;

public class Space {
    private UUID guid;
    private String name;

    public Space(UUID guid, String name) {
        this.guid = guid;
        this.name = name;
    }

    public Space(CcSpace cfSpace) {
        guid = cfSpace.getGuid();
        name = cfSpace.getName();
    }

    public UUID getGuid() {
        return guid;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        if (stateNotEqual((Space) o))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = guid != null ? guid.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    private boolean stateNotEqual(Space o) {
        Space space = o;

        if (guid != null ? !guid.equals(space.guid) : space.guid != null) {
            return true;
        }
        if (name != null ? !name.equals(space.name) : space.name != null) {
            return true;
        }
        return false;
    }
}
