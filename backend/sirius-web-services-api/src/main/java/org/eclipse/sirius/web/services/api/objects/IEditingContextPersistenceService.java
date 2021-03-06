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
package org.eclipse.sirius.web.services.api.objects;

import java.util.UUID;

import org.eclipse.sirius.web.services.api.monitoring.IStopWatch;

/**
 * Interface used to save the editing context when a change has been performed.
 *
 * @author sbegaudeau
 */
public interface IEditingContextPersistenceService {
    void persist(UUID projectId, IEditingContext editingContext, IStopWatch stopWatch);
}
