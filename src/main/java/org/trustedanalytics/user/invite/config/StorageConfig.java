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
package org.trustedanalytics.user.invite.config;

import org.springframework.data.redis.connection.RedisServer;
import org.trustedanalytics.user.invite.access.AccessInvitations;
import org.trustedanalytics.user.invite.access.AccessInvitationsService;
import org.trustedanalytics.user.invite.access.AccessInvitationsStore;
import org.trustedanalytics.user.invite.access.InMemoryAccessInvitationsStore;
import org.trustedanalytics.user.invite.access.RedisAccessInvitationsStore;
import org.trustedanalytics.user.invite.securitycode.InMemorySecurityCodeService;
import org.trustedanalytics.user.invite.securitycode.RedisSecurityCodeService;
import org.trustedanalytics.user.invite.securitycode.SecurityCode;
import org.trustedanalytics.user.invite.securitycode.SecurityCodeService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

public class StorageConfig {

    private StorageConfig() {
    }

    @Profile("in-memory")
    @Configuration
    public static class InMemoryStorageConfig {

        @Bean
        SecurityCodeService inMemorySecurityCodeService() {
            return new InMemorySecurityCodeService();
        }

        @Bean
        AccessInvitationsStore inMemoryAccessInvitationsStore() {
            return new InMemoryAccessInvitationsStore();
        }

        @Bean
        AccessInvitationsService inMemoryAccessInvitationsService(AccessInvitationsStore inMemoryAccessInvitationsStore) {
            return new AccessInvitationsService(inMemoryAccessInvitationsStore);
        }
    }

    @Profile("redis")
    @Configuration
    public static class RedisStorageConfig {

        @Bean
        protected SecurityCodeService redisSecurityCodeService(RedisOperations<String, SecurityCode> redisTemplate) {
            return new RedisSecurityCodeService(redisTemplate);
        }

        @Bean
        public RedisOperations<String, SecurityCode> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
            return CommonConfiguration.redisTemplate(redisConnectionFactory,
                    new JacksonJsonRedisSerializer<SecurityCode>(SecurityCode.class));
        }
    }

    @Profile("redis")
    @Configuration
    public static class RedisInvitationStorageConfig {
        @Bean
        public AccessInvitationsStore redisAccessInvitationsStore(
                RedisOperations<String, AccessInvitations> redisAccessInvitationsTemplate) {
            return new RedisAccessInvitationsStore(redisAccessInvitationsTemplate);
        }

        @Bean
        AccessInvitationsService redisAccessInvitationsService(AccessInvitationsStore redisAccessInvitationsStore) {
            return new AccessInvitationsService(redisAccessInvitationsStore);
        }

        @Bean
        public RedisOperations<String, AccessInvitations> redisAccessInvitationsTemplate(
                RedisConnectionFactory redisConnectionFactory) {
            return CommonConfiguration.redisTemplate(redisConnectionFactory,
                    new JacksonJsonRedisSerializer<AccessInvitations>(AccessInvitations.class));
        }
    }

    private static class CommonConfiguration {
        private static <T> RedisOperations<String, T>
            redisTemplate(RedisConnectionFactory redisConnectionFactory, RedisSerializer<T> valueSerializer) {
            RedisTemplate<String, T> template = new RedisTemplate<String, T>();
            template.setConnectionFactory(redisConnectionFactory);

            RedisSerializer<String> stringSerializer = new StringRedisSerializer();
            template.setKeySerializer(stringSerializer);
            template.setValueSerializer(valueSerializer);
            template.setHashKeySerializer(stringSerializer);
            template.setHashValueSerializer(valueSerializer);

            return template;
        }
    }
}
