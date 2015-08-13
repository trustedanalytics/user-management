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

import org.trustedanalytics.user.invite.access.AccessInvitations;
import org.trustedanalytics.user.invite.access.AccessInvitationsService;
import org.trustedanalytics.user.invite.access.InMemoryAccessInvitationsService;
import org.trustedanalytics.user.invite.access.RedisAccessInvitationsService;
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
        AccessInvitationsService inMemoryAccessInvitationsService() {
            return new InMemoryAccessInvitationsService();
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
            RedisTemplate<String, SecurityCode> template = new RedisTemplate<String, SecurityCode>();

            template.setConnectionFactory(redisConnectionFactory);

            RedisSerializer<String> stringSerializer = new StringRedisSerializer();
            RedisSerializer<SecurityCode> albumSerializer = new JacksonJsonRedisSerializer<SecurityCode>(SecurityCode.class);

            template.setKeySerializer(stringSerializer);
            template.setValueSerializer(albumSerializer);
            template.setHashKeySerializer(stringSerializer);
            template.setHashValueSerializer(albumSerializer);

            return template;
        }
    }

    @Profile("redis")
    @Configuration
    public static class RedisInvitationStorageConfig {
        @Bean
        public AccessInvitationsService redisAccessInvitationsService(
                RedisOperations<String, AccessInvitations> redisTemplate) {
            return new RedisAccessInvitationsService(redisTemplate);
        }

        @Bean
        public RedisOperations<String, AccessInvitations> redisAccessInvitationsTemplate(
                RedisConnectionFactory redisConnectionFactory) {
            RedisTemplate<String, AccessInvitations> template = new RedisTemplate<String, AccessInvitations>();
            template.setConnectionFactory(redisConnectionFactory);

            RedisSerializer<String> emailSerializer = new StringRedisSerializer();
            RedisSerializer<AccessInvitations> accessSerializer =
                    new JacksonJsonRedisSerializer<AccessInvitations>(AccessInvitations.class);

            template.setKeySerializer(emailSerializer);
            template.setValueSerializer(accessSerializer);
            template.setHashKeySerializer(emailSerializer);
            template.setHashValueSerializer(accessSerializer);

            return template;
        }
    }
}
