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

import java.util.Objects;
import java.util.Optional;

import org.eclipse.sirius.web.collaborative.api.services.EventHandlerResponse;
import org.eclipse.sirius.web.collaborative.api.services.IProjectEventHandler;
import org.eclipse.sirius.web.services.api.Context;
import org.eclipse.sirius.web.services.api.dto.ErrorPayload;
import org.eclipse.sirius.web.services.api.dto.IProjectInput;
import org.eclipse.sirius.web.services.api.objects.CreateChildInput;
import org.eclipse.sirius.web.services.api.objects.CreateChildSuccessPayload;
import org.eclipse.sirius.web.services.api.objects.IEditService;
import org.eclipse.sirius.web.services.api.objects.IEditingContext;
import org.eclipse.sirius.web.services.api.objects.IObjectService;
import org.eclipse.sirius.web.spring.collaborative.messages.ICollaborativeMessageService;
import org.springframework.stereotype.Service;

/**
 * Handler used to create a new child.
 *
 * @author sbegaudeau
 */
@Service
public class CreateChildEventHandler implements IProjectEventHandler {

    private final IObjectService objectService;

    private final IEditService editService;

    private final ICollaborativeMessageService messageService;

    public CreateChildEventHandler(IObjectService objectService, IEditService editService, ICollaborativeMessageService messageService) {
        this.objectService = Objects.requireNonNull(objectService);
        this.editService = Objects.requireNonNull(editService);
        this.messageService = Objects.requireNonNull(messageService);
    }

    @Override
    public boolean canHandle(IProjectInput projectInput) {
        return projectInput instanceof CreateChildInput;
    }

    @Override
    public EventHandlerResponse handle(IEditingContext editingContext, IProjectInput projectInput, Context context) {
        String message = this.messageService.invalidInput(projectInput.getClass().getSimpleName(), CreateChildInput.class.getSimpleName());
        if (projectInput instanceof CreateChildInput) {
            CreateChildInput input = (CreateChildInput) projectInput;
            String parentObjectId = input.getObjectId();
            String childCreationDescriptionId = input.getChildCreationDescriptionId();

            Optional<Object> createdChildOptional = this.objectService.getObject(editingContext, parentObjectId).flatMap(parent -> {
                return this.editService.createChild(editingContext, parent, childCreationDescriptionId);
            });

            if (createdChildOptional.isPresent()) {
                return new EventHandlerResponse(true, representation -> true, new CreateChildSuccessPayload(createdChildOptional.get()));
            } else {
                message = this.messageService.objectCreationFailed();
            }
        }

        return new EventHandlerResponse(false, representation -> false, new ErrorPayload(message));
    }
}
