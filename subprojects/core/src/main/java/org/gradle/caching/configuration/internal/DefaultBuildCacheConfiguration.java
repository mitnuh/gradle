/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.caching.configuration.internal;

import org.gradle.api.Action;
import org.gradle.caching.BuildCacheService;
import org.gradle.caching.configuration.BuildCache;
import org.gradle.caching.configuration.BuildCacheServiceBuilder;
import org.gradle.caching.configuration.LocalBuildCache;
import org.gradle.caching.internal.CompositeBuildCacheService;

public class DefaultBuildCacheConfiguration implements BuildCacheConfigurationInternal {

    private static final BuildCacheServiceBuilder<BuildCache> NON_CONFIGURABLE_REMOTE_BUILDER = new BuildCacheServiceBuilder<BuildCache>() {
        @Override
        public BuildCache getConfiguration() {
            return new BuildCache() {
                @Override
                public boolean isEnabled() {
                    return false;
                }

                @Override
                public void setEnabled(boolean enabled) {
                    throw new IllegalStateException("Cannot enable remote build cache as it has not been configured");
                }

                @Override
                public void setPush(boolean enabled) {
                    throw new IllegalStateException("Cannot allow pushing to remote build cache as it has not been configured");
                }
            };
        }

        @Override
        public BuildCacheService build() {
            return BuildCacheService.NO_OP;
        }
    };

    private final BuildCacheServiceBuilder<? extends LocalBuildCache> local;
    private final BuildCacheServiceFactoryRegistry buildCacheServiceFactoryRegistry;
    private BuildCacheServiceBuilder<?> remote = NON_CONFIGURABLE_REMOTE_BUILDER;

    public DefaultBuildCacheConfiguration(BuildCacheServiceFactoryRegistry buildCacheServiceFactoryRegistry) {
        this.buildCacheServiceFactoryRegistry = buildCacheServiceFactoryRegistry;
        this.local = buildCacheServiceFactoryRegistry.createServiceBuilder(LocalBuildCache.class);
    }

    @Override
    public LocalBuildCache getLocal() {
        return local.getConfiguration();
    }

    @Override
    public void local(Action<? super LocalBuildCache> configuration) {
        LocalBuildCache cfg = local.getConfiguration();
        configuration.execute(cfg);
    }

    @Override
    public <T extends BuildCache> void remote(Class<T> type, Action<? super T> configuration) {
        BuildCacheServiceBuilder<? extends T> remote = buildCacheServiceFactoryRegistry.createServiceBuilder(type);
        configuration.execute(remote.getConfiguration());
        this.remote = remote;
    }

    @Override
    public BuildCache getRemote() {
        return remote.getConfiguration();
    }

    @Override
    public void remote(Action<? super BuildCache> configuration) {
        configuration.execute(remote.getConfiguration());
    }

    @Override
    public BuildCacheService build() {
        BuildCache localConfig = local.getConfiguration();
        BuildCache remoteConfig = remote.getConfiguration();
        if (localConfig.isEnabled()) {
            if (remoteConfig.isEnabled()) {
                return new CompositeBuildCacheService(local.build(), remote.build());
            } else {
                return local.build();
            }
        } else if (remoteConfig.isEnabled()) {
            return remote.build();
        } else {
            return null;
        }
    }
}
