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
package org.eclipse.sirius.web.spring.collaborative.diagrams.handlers;

import java.util.Objects;
import java.util.Optional;

import org.eclipse.sirius.web.collaborative.api.services.EventHandlerResponse;
import org.eclipse.sirius.web.collaborative.diagrams.api.IDiagramEventHandler;
import org.eclipse.sirius.web.collaborative.diagrams.api.IDiagramInput;
import org.eclipse.sirius.web.collaborative.diagrams.api.IDiagramService;
import org.eclipse.sirius.web.collaborative.diagrams.api.IToolService;
import org.eclipse.sirius.web.collaborative.diagrams.api.dto.InvokeNodeToolOnDiagramInput;
import org.eclipse.sirius.web.collaborative.diagrams.api.dto.InvokeNodeToolOnDiagramSuccessPayload;
import org.eclipse.sirius.web.diagrams.Diagram;
import org.eclipse.sirius.web.diagrams.Node;
import org.eclipse.sirius.web.diagrams.tools.CreateNodeTool;
import org.eclipse.sirius.web.representations.Status;
import org.eclipse.sirius.web.representations.VariableManager;
import org.eclipse.sirius.web.services.api.dto.ErrorPayload;
import org.eclipse.sirius.web.services.api.objects.IEditingContext;
import org.eclipse.sirius.web.services.api.objects.IObjectService;
import org.eclipse.sirius.web.spring.collaborative.diagrams.messages.ICollaborativeDiagramMessageService;
import org.springframework.stereotype.Service;

/**
 * Handle "Invoke node tool on diagram" events.
 *
 * @author pcdavid
 */
@Service
public class InvokeNodeToolOnDiagramEventHandler implements IDiagramEventHandler {

    private final IObjectService objectService;

    private final IDiagramService diagramService;

    private final IToolService toolService;

    private final ICollaborativeDiagramMessageService messageService;

    public InvokeNodeToolOnDiagramEventHandler(IObjectService objectService, IDiagramService diagramService, IToolService toolService, ICollaborativeDiagramMessageService messageService) {
        this.objectService = Objects.requireNonNull(objectService);
        this.diagramService = Objects.requireNonNull(diagramService);
        this.toolService = Objects.requireNonNull(toolService);
        this.messageService = Objects.requireNonNull(messageService);
    }

    @Override
    public boolean canHandle(IDiagramInput diagramInput) {
        return diagramInput instanceof InvokeNodeToolOnDiagramInput;
    }

    @Override
    public EventHandlerResponse handle(IEditingContext editingContext, Diagram diagram, IDiagramInput diagramInput) {
        if (diagramInput instanceof InvokeNodeToolOnDiagramInput) {
            InvokeNodeToolOnDiagramInput input = (InvokeNodeToolOnDiagramInput) diagramInput;
         // @formatter:off
            var optionalTool = this.toolService.findToolById(diagram, input.getToolId())
                    .filter(CreateNodeTool.class::isInstance)
                    .map(CreateNodeTool.class::cast);
            // @formatter:on
            if (optionalTool.isPresent()) {
                Status status = this.executeTool(editingContext, diagram, input.getDiagramElementId(), optionalTool.get());
                if (Objects.equals(status, Status.OK)) {
                    return new EventHandlerResponse(true, representation -> true, new InvokeNodeToolOnDiagramSuccessPayload(diagram));
                }
            }
        }
        String message = this.messageService.invalidInput(diagramInput.getClass().getSimpleName(), InvokeNodeToolOnDiagramInput.class.getSimpleName());
        return new EventHandlerResponse(false, representation -> false, new ErrorPayload(message));
    }

    private Status executeTool(IEditingContext editingContext, Diagram diagram, String diagramElementId, CreateNodeTool tool) {
        Status result = Status.ERROR;
        Optional<Node> node = this.diagramService.findNodeById(diagram, diagramElementId);
        Optional<Object> self = Optional.empty();
        if (node.isPresent()) {
            self = this.objectService.getObject(editingContext, node.get().getTargetObjectId());
        } else if (Objects.equals(diagram.getId().toString(), diagramElementId)) {
            self = this.objectService.getObject(editingContext, diagram.getTargetObjectId());
        }

        if (self.isPresent()) {
            VariableManager variableManager = new VariableManager();
            variableManager.put(IEditingContext.EDITING_CONTEXT, editingContext);
            variableManager.put(VariableManager.SELF, self.get());

            result = tool.getHandler().apply(variableManager);
        }
        return result;
    }

}
