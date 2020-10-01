/*******************************************************************************
 * Copyright (c) 2019, 2020 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.sirius.web.collaborative.trees.api;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.sirius.web.collaborative.api.services.IRepresentationConfiguration;

/**
 * The configuration of the tree event processor.
 *
 * @author sbegaudeau
 */
public class TreeConfiguration implements IRepresentationConfiguration {

    private final UUID treeId;

    private final List<String> expanded;

    public TreeConfiguration(UUID projectId, List<String> expanded) {
        String uniqueId = projectId.toString() + expanded.toString();
        this.treeId = UUID.nameUUIDFromBytes(uniqueId.getBytes());
        this.expanded = Objects.requireNonNull(expanded);
    }

    @Override
    public UUID getId() {
        return this.treeId;
    }

    public List<String> getExpanded() {
        return this.expanded;
    }

}
