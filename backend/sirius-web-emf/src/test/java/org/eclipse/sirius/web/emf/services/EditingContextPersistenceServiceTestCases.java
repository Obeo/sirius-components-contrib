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
package org.eclipse.sirius.web.emf.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.sirius.emfjson.resource.JsonResource;
import org.eclipse.sirius.web.persistence.entities.DocumentEntity;
import org.eclipse.sirius.web.persistence.entities.ProjectEntity;
import org.eclipse.sirius.web.persistence.repositories.IDocumentRepository;
import org.eclipse.sirius.web.services.api.monitoring.IStopWatch;
import org.eclipse.sirius.web.services.api.objects.IEditingContext;
import org.eclipse.sirius.web.services.api.objects.IEditingContextPersistenceService;
import org.junit.Test;

/**
 * Unit tests of the editing context persistence service.
 *
 * @author sbegaudeau
 */
public class EditingContextPersistenceServiceTestCases {
    @Test
    public void testDocumentPersistence() {
        UUID projectId = UUID.randomUUID();

        String name = "New Document"; //$NON-NLS-1$
        UUID id = UUID.randomUUID();
        JsonResource resource = new SiriusWebJSONResourceFactoryImpl().createResource(URI.createURI(id.toString()));
        resource.eAdapters().add(new DocumentMetadataAdapter(name));

        EClass eClass = EcoreFactory.eINSTANCE.createEClass();
        eClass.setName("Concept"); //$NON-NLS-1$
        resource.getContents().add(eClass);

        EditingDomain editingDomain = new EditingDomainFactory().create();
        editingDomain.getResourceSet().getResources().add(resource);

        ProjectEntity projectEntity = new ProjectEntity();
        projectEntity.setId(projectId);
        projectEntity.setName(""); //$NON-NLS-1$

        DocumentEntity existingEntity = new DocumentEntity();
        existingEntity.setId(id);
        existingEntity.setProject(projectEntity);
        existingEntity.setName(name);
        existingEntity.setContent(""); //$NON-NLS-1$

        List<DocumentEntity> entities = new ArrayList<>();
        IDocumentRepository documentRepository = new NoOpDocumentRepository() {
            @Override
            public <S extends DocumentEntity> S save(S entity) {
                entities.add(entity);
                return entity;
            }

            @Override
            public Optional<DocumentEntity> findById(UUID id) {
                return Optional.of(existingEntity);
            }
        };
        IEditingContextPersistenceService editingContextPersistenceService = new EditingContextPersistenceService(documentRepository, new NoOpApplicationEventPublisher());
        assertThat(entities).hasSize(0);

        IEditingContext editingContext = new IEditingContext() {
            @Override
            public UUID getProjectId() {
                return null;
            }

            @Override
            public Object getDomain() {
                return editingDomain;
            }
        };

        IStopWatch stopWatch = new IStopWatch() {

            @Override
            public void stop() {
                // Do nothing
            }

            @Override
            public void start(String taskName) {
                // Do nothing
            }

            @Override
            public String prettyPrint() {
                return ""; //$NON-NLS-1$
            }
        };

        editingContextPersistenceService.persist(projectId, editingContext, stopWatch);
        assertThat(entities).hasSize(1);

        DocumentEntity documentEntity = entities.get(0);
        assertThat(documentEntity.getId()).isEqualTo(id);
        assertThat(documentEntity.getName()).isEqualTo(name);
        assertThat(documentEntity.getProject().getId()).isEqualTo(projectId);
    }
}
