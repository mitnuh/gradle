/*
 * Copyright 2015 the original author or authors.
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

package org.gradle.api.internal.artifacts.ivyservice.resolveengine.artifact;

import org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.DependencyGraphNode;

public interface DependencyArtifactsVisitor {
    // TODO:DAZ Performing the topo-sort for nodes shouldn't be done in a DependencyArtifactsVisitor
    void startArtifacts(DependencyGraphNode root);
    void visitArtifacts(DependencyGraphNode from, DependencyGraphNode to, ArtifactSet artifacts);
    void finishArtifacts();
}
