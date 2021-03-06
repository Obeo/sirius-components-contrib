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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.emf.common.command.BasicCommandStack;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.eclipse.sirius.emfjson.resource.JsonResource;
import org.eclipse.sirius.web.persistence.entities.DocumentEntity;
import org.eclipse.sirius.web.persistence.repositories.IDocumentRepository;
import org.eclipse.sirius.web.services.api.objects.IEditingContext;
import org.eclipse.sirius.web.services.api.objects.IEditingContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service used to create a new editing context.
 *
 * @author sbegaudeau
 */
@Service
public class EditingContextFactory implements IEditingContextFactory {

    private final Logger logger = LoggerFactory.getLogger(EditingContextFactory.class);

    private final IDocumentRepository documentRepository;

    private final ComposedAdapterFactory composedAdapterFactory;

    private final EPackage.Registry ePackageRegistry;

    public EditingContextFactory(IDocumentRepository documentRepository, ComposedAdapterFactory composedAdapterFactory, EPackage.Registry ePackageRegistry) {
        this.documentRepository = Objects.requireNonNull(documentRepository);
        this.composedAdapterFactory = Objects.requireNonNull(composedAdapterFactory);
        this.ePackageRegistry = Objects.requireNonNull(ePackageRegistry);
    }

    @Override
    public IEditingContext createEditingContext(UUID projectId) {
        this.logger.debug(MessageFormat.format("Loading the editing context of the project \"{0}\"", projectId)); //$NON-NLS-1$
        ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.setPackageRegistry(this.ePackageRegistry);

        List<DocumentEntity> documentEntities = this.documentRepository.findAllByProjectId(projectId);
        for (DocumentEntity documentEntity : documentEntities) {
            URI uri = URI.createURI(documentEntity.getId().toString());
            JsonResource resource = new SiriusWebJSONResourceFactoryImpl().createResource(uri);
            try (var inputStream = new ByteArrayInputStream(documentEntity.getContent().getBytes())) {
                resource.load(inputStream, null);

                resource.eAdapters().add(new DocumentMetadataAdapter(documentEntity.getName()));
                resourceSet.getResources().add(resource);
            } catch (IOException exception) {
                this.logger.error(exception.getMessage(), exception);
            }
        }

        EditingDomain editingDomain = new AdapterFactoryEditingDomain(this.composedAdapterFactory, new BasicCommandStack(), resourceSet);
        this.logger.debug(MessageFormat.format("{0} documents loaded for the project \"{1}\"", documentEntities.size(), projectId)); //$NON-NLS-1$

        return new EditingContext(projectId, editingDomain);
    }

}
