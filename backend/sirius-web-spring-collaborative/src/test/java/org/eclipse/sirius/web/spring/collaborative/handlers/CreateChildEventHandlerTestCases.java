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
package org.eclipse.sirius.web.spring.collaborative.handlers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;

import org.eclipse.sirius.web.collaborative.api.services.EventHandlerResponse;
import org.eclipse.sirius.web.services.api.Context;
import org.eclipse.sirius.web.services.api.dto.ErrorPayload;
import org.eclipse.sirius.web.services.api.objects.CreateChildInput;
import org.eclipse.sirius.web.services.api.objects.CreateChildSuccessPayload;
import org.eclipse.sirius.web.services.api.objects.IEditService;
import org.eclipse.sirius.web.services.api.objects.IEditingContext;
import org.eclipse.sirius.web.services.api.objects.IObjectService;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/**
 * Tests of the create child event handler.
 *
 * @author sbegaudeau
 */
public class CreateChildEventHandlerTestCases {
    @Test
    public void testCreateChild() {
        Object createdObject = new Object();
        IEditService editService = new NoOpEditService() {
            @Override
            public Optional<Object> createChild(IEditingContext editingContext, Object object, String childCreationDescriptionId) {
                return Optional.of(createdObject);
            }
        };

        EventHandlerResponse response = this.handle(editService);
        assertThat(response.getPayload()).isInstanceOf(CreateChildSuccessPayload.class);
        assertThat(((CreateChildSuccessPayload) response.getPayload()).getObject()).isEqualTo(createdObject);
    }

    @Test
    public void testCreateChildFailed() {
        IEditService editService = new NoOpEditService() {
            @Override
            public Optional<Object> createChild(IEditingContext editingContext, Object object, String childCreationDescriptionId) {
                return Optional.empty();
            }
        };

        EventHandlerResponse response = this.handle(editService);
        assertThat(response.getPayload()).isInstanceOf(ErrorPayload.class);
    }

    EventHandlerResponse handle(IEditService editService) {
        IObjectService objectService = new NoOpObjectService() {
            @Override
            public Optional<Object> getObject(IEditingContext editingContext, String objectId) {
                return Optional.of(new Object());
            }
        };

        CreateChildEventHandler handler = new CreateChildEventHandler(objectService, editService, new NoOpCollaborativeMessageService());
        var input = new CreateChildInput(UUID.randomUUID(), "parentObjectId", "childCreationDescriptionId"); //$NON-NLS-1$//$NON-NLS-2$
        var context = new Context(new UsernamePasswordAuthenticationToken(null, null));

        assertThat(handler.canHandle(input)).isTrue();

        EventHandlerResponse response = handler.handle(null, input, context);
        return response;
    }
}
