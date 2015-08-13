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

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

public class Organization {
    private UUID guid;
    private String name;
    private Collection<Space> spaces;
    private boolean manager;

    public Organization(UUID guid, String name) {
        this.guid = guid;
        this.name = name;
        this.spaces = new ArrayList<>();
    }

    public Organization(CcOrg org) {
        this.guid = org.getGuid();
        this.name = org.getName();
        this.spaces = new ArrayList<>();
    }

    public void addSpace(Space space) {
        spaces.add(space);
    }

    public UUID getGuid() {
        return guid;
    }

    public String getName() {
        return name;
    }

    public Collection<Space> getSpaces() {
        return spaces;
    }

    public boolean getManager() {
        return manager;
    }

    public void setManager(boolean manager) {
        this.manager = manager;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        if (stateNotEqual((Organization) o))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = guid != null ? guid.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (spaces != null ? spaces.hashCode() : 0);
        return result;
    }

    private boolean stateNotEqual(Organization o) {
        Organization that = o;

        if (guid != null ? !guid.equals(that.guid) : that.guid != null) {
            return true;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return true;
        }
        if (spaces != null ? !spaces.equals(that.spaces) : that.spaces != null) {
            return true;
        }
        return false;
    }
}
