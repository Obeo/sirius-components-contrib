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
package org.eclipse.sirius.web.spring.collaborative.diagrams;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.sirius.web.collaborative.api.services.IRepresentationConfiguration;
import org.eclipse.sirius.web.collaborative.api.services.IRepresentationEventProcessor;
import org.eclipse.sirius.web.collaborative.api.services.IRepresentationEventProcessorFactory;
import org.eclipse.sirius.web.collaborative.api.services.ISubscriptionManagerFactory;
import org.eclipse.sirius.web.collaborative.diagrams.api.DiagramConfiguration;
import org.eclipse.sirius.web.collaborative.diagrams.api.DiagramCreationParameters;
import org.eclipse.sirius.web.collaborative.diagrams.api.IDiagramEventHandler;
import org.eclipse.sirius.web.collaborative.diagrams.api.IDiagramEventProcessor;
import org.eclipse.sirius.web.collaborative.diagrams.api.IDiagramRefreshManager;
import org.eclipse.sirius.web.collaborative.diagrams.api.IDiagramService;
import org.eclipse.sirius.web.diagrams.Diagram;
import org.eclipse.sirius.web.diagrams.description.DiagramDescription;
import org.eclipse.sirius.web.diagrams.layout.api.ILayoutService;
import org.eclipse.sirius.web.services.api.Context;
import org.eclipse.sirius.web.services.api.objects.IEditingContext;
import org.eclipse.sirius.web.services.api.objects.IObjectService;
import org.eclipse.sirius.web.services.api.representations.IRepresentationDescriptionService;
import org.eclipse.sirius.web.services.api.representations.IRepresentationService;
import org.springframework.stereotype.Service;

/**
 * Used to create the diagram event processors.
 *
 * @author sbegaudeau
 */
@Service
public class DiagramEventProcessorFactory implements IRepresentationEventProcessorFactory {

    private final IRepresentationDescriptionService representationDescriptionService;

    private final IDiagramService diagramService;

    private final IRepresentationService representationService;

    private final ILayoutService layoutService;

    private final IObjectService objectService;

    private final List<IDiagramEventHandler> diagramEventHandlers;

    private final ISubscriptionManagerFactory subscriptionManagerFactory;

    public DiagramEventProcessorFactory(IRepresentationDescriptionService representationDescriptionService, IDiagramService diagramService, IObjectService objectService,
            IRepresentationService representationService, ILayoutService layoutService, List<IDiagramEventHandler> diagramEventHandlers, ISubscriptionManagerFactory subscriptionManagerFactory) {
        this.representationDescriptionService = Objects.requireNonNull(representationDescriptionService);
        this.diagramService = Objects.requireNonNull(diagramService);
        this.representationService = Objects.requireNonNull(representationService);
        this.layoutService = Objects.requireNonNull(layoutService);
        this.objectService = Objects.requireNonNull(objectService);
        this.diagramEventHandlers = Objects.requireNonNull(diagramEventHandlers);
        this.subscriptionManagerFactory = Objects.requireNonNull(subscriptionManagerFactory);
    }

    @Override
    public <T extends IRepresentationEventProcessor> boolean canHandle(Class<T> representationEventProcessorClass) {
        return IDiagramEventProcessor.class.isAssignableFrom(representationEventProcessorClass);
    }

    @Override
    public <T extends IRepresentationEventProcessor> Optional<T> createRepresentationEventProcessor(Class<T> representationEventProcessorClass, IRepresentationConfiguration configuration,
            IEditingContext editingContext, Context context) {
        if (IDiagramEventProcessor.class.isAssignableFrom(representationEventProcessorClass) && configuration instanceof DiagramConfiguration) {
            DiagramConfiguration diagramConfiguration = (DiagramConfiguration) configuration;
            var optionalDiagram = this.diagramService.findById(diagramConfiguration.getId());
            if (optionalDiagram.isPresent()) {
                Diagram diagram = optionalDiagram.get();

                var optionalObject = this.objectService.getObject(editingContext, diagram.getTargetObjectId());
                // @formatter:off
                var optionalDiagramDescription = this.representationDescriptionService.findRepresentationDescriptionById(diagram.getDescriptionId())
                        .filter(DiagramDescription.class::isInstance)
                        .map(DiagramDescription.class::cast);
                // @formatter:on
                if (optionalObject.isPresent() && optionalDiagramDescription.isPresent()) {
                    Object object = optionalObject.get();
                    DiagramDescription diagramDescription = optionalDiagramDescription.get();

                    // @formatter:off
                    DiagramCreationParameters diagramCreationParameters = DiagramCreationParameters.newDiagramCreationParameters(diagram.getId())
                            .label(diagram.getLabel())
                            .object(object)
                            .diagramDescription(diagramDescription)
                            .editingContext(editingContext)
                            .build();

                    IDiagramRefreshManager diagramRefreshManager = new DiagramRefreshManager(this.representationService, this.diagramService, this.layoutService);
                    IRepresentationEventProcessor diagramEventProcessor = new DiagramEventProcessor(diagramCreationParameters,
                            editingContext, this.diagramEventHandlers, this.subscriptionManagerFactory.create(),  diagramRefreshManager);

                    return Optional.of(diagramEventProcessor)
                            .filter(representationEventProcessorClass::isInstance)
                            .map(representationEventProcessorClass::cast);
                    // @formatter:on
                }
            }
        }
        return Optional.empty();
    }
}
