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
package org.trustedanalytics.user.invite.keyvaluestore;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class InMemoryStore<T> implements KeyValueStore<T> {
    private final Map<String, T> valuesMap = new HashMap<String, T>();

    @Override
    public boolean hasKey(String key) {
        return valuesMap.containsKey(key);
    }

    @Override
    public T get(String key) {
        return valuesMap.get(key);
    }

    @Override
    public void remove(String key) {
        valuesMap.remove(key);
    }

    @Override
    public void put(String key, T value) {
        valuesMap.put(key, value);
    }

    @Override
    public boolean putIfAbsent(String key, T value) {
        return valuesMap.putIfAbsent(key, value) == null;
    }

    @Override
    public Collection<T> values() {
        return valuesMap.values();
    }
}
