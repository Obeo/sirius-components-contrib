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

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EPackage.Registry;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.sirius.emfjson.resource.JsonResource;
import org.eclipse.sirius.web.diagrams.Diagram;
import org.eclipse.sirius.web.persistence.entities.IdMappingEntity;
import org.eclipse.sirius.web.persistence.repositories.IIdMappingRepository;
import org.eclipse.sirius.web.representations.IRepresentation;
import org.eclipse.sirius.web.services.api.document.Document;
import org.eclipse.sirius.web.services.api.document.IDocumentService;
import org.eclipse.sirius.web.services.api.projects.IProjectExportService;
import org.eclipse.sirius.web.services.api.projects.IProjectService;
import org.eclipse.sirius.web.services.api.projects.Project;
import org.eclipse.sirius.web.services.api.projects.ProjectManifest;
import org.eclipse.sirius.web.services.api.projects.RepresentationManifest;
import org.eclipse.sirius.web.services.api.representations.IRepresentationService;
import org.eclipse.sirius.web.services.api.representations.RepresentationDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

/**
 * Service used to export a project in zip.
 *
 * <p>
 * The zip will contains models, representations and a manifest used to store meta data. The structure of the zip is
 * described as follow:
 * <ul>
 * <li>The root file of the zip is a directory whose name is the project name.</li>
 * <li>The <i>manifest.json</i> file is inside the root directory.</li>
 * <li>Representations are put in a <i>representations</i> directory which is inside the root directory.</li>
 * <li>Models are put in a <i>models</i> directory which is inside the root directory.</li>
 * <li>The name of representations is the representation id.</li>
 * <li>The name of models is the document id.</li>
 * </ul>
 * </p>
 *
 * @author gcoutable
 */
@Service
public class ProjectExportService implements IProjectExportService {

    private static final String CURRENT_MANIFEST_VERSION = "1.0"; //$NON-NLS-1$

    private final Logger logger = LoggerFactory.getLogger(ProjectExportService.class);

    private final ObjectMapper objectMapper;

    private final BuildProperties buildProperties;

    private final IProjectService projectService;

    private final IDocumentService documentService;

    private final IRepresentationService representationService;

    private final Registry ePackageRegistry;

    private final IIdMappingRepository idMappingRepository;

    public ProjectExportService(IProjectService projectService, IDocumentService documentService, IRepresentationService representationService, EPackage.Registry ePackageRegistry,
            IIdMappingRepository idMappingRepository, ObjectMapper objectMapper, BuildProperties buildProperties) {
        this.projectService = Objects.requireNonNull(projectService);
        this.documentService = Objects.requireNonNull(documentService);
        this.representationService = Objects.requireNonNull(representationService);
        this.ePackageRegistry = Objects.requireNonNull(ePackageRegistry);
        this.idMappingRepository = Objects.requireNonNull(idMappingRepository);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.buildProperties = Objects.requireNonNull(buildProperties);
    }

    @Override
    public byte[] exportProjectAsZip(UUID projectId) {
        return this.projectService.getProject(projectId).map(this::toZip).orElse(new byte[0]);
    }

    private byte[] toZip(Project project) {
        byte[] zip = new byte[0];
        UUID projectId = project.getId();
        String projectName = project.getName();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (var zippedOut = new ZipOutputStream(outputStream)) {

            Map<String, String> id2DocumentName = this.addDocuments(projectId, projectName, zippedOut);

            Map<String, RepresentationManifest> representationsManifests = this.addRepresentation(projectId, projectName, zippedOut);

            this.addManifest(projectName, id2DocumentName, representationsManifests, zippedOut);
        } catch (IOException e) {
            this.logger.error(e.getMessage(), e);
            outputStream.reset();
        }

        if (outputStream.size() > 0) {
            zip = outputStream.toByteArray();
        }

        return zip;
    }

    /**
     * Adds a {@link ZipEntry} for every documents in the project, in the given {@link ZipOutputStream}.
     *
     * <p>
     * The name of the {@link ZipEntry} is [projectName]/documents/[documentId], where '/' are used as path separator in
     * the zip.
     * </p>
     *
     * <p>
     * Returns a map of documentId to document name. This map will be used to store the mapping between documentId and
     * document name in the manifest file.
     * </p>
     *
     * @param projectId
     *            The id of the project we want to export
     * @param projectName
     *            The name of the project we want to export
     * @param zippedOut
     *            The {@link ZipOutputStream} used to build the zip
     * @return the mapping between document id and document name
     * @throws IOException
     *             if an I/O error occurred
     */
    private Map<String, String> addDocuments(UUID projectId, String projectName, ZipOutputStream zippedOut) throws IOException {
        List<Document> documents = this.documentService.getDocuments(projectId);
        Map<String, String> id2DocumentName = new HashMap<>();
        for (Document document : documents) {
            Optional<byte[]> optionalBytes = this.documentService.getBytes(document, IDocumentService.RESOURCE_KIND_JSON);
            if (optionalBytes.isPresent()) {
                byte[] bytes = optionalBytes.get();
                id2DocumentName.put(document.getId().toString(), document.getName());
                String name = projectName + "/documents/" + document.getId(); //$NON-NLS-1$
                ZipEntry zipEntry = this.createZipEntry(name, bytes.length);
                zippedOut.putNextEntry(zipEntry);
                zippedOut.write(bytes);
                zippedOut.closeEntry();
            } else {
                this.logger.error("The serialization of the document {} has failed.", document.getName()); //$NON-NLS-1$
            }
        }
        return id2DocumentName;
    }

    /**
     * Adds a {@link ZipEntry} for every representations in the project, in the given {@link ZipOutputStream}.
     *
     * <p>
     * The name of the {@link ZipEntry} is [projectName]/representations/[representationId], where '/' are used as path
     * separator in the zip.
     * </p>
     *
     * <p>
     * Returns a map of representation IDs to {@link RepresentationManifest}. This map will be stored in the manifest
     * file.
     * </p>
     *
     * @param projectId
     *            The id of the project we want to export
     * @param projectName
     *            The name of the project we want to export
     * @param zippedOut
     *            The {@link ZipOutputStream} used to build the zip
     * @return the mapping between representation IDs and their {@link RepresentationManifest}
     * @throws IOException
     *             if an I/O error occurred
     */
    private Map<String, RepresentationManifest> addRepresentation(UUID projectId, String projectName, ZipOutputStream zippedout) throws IOException {
        List<RepresentationDescriptor> representationsDescriptor = this.representationService.getRepresentationDescriptorsForProjectId(projectId);
        Map<String, RepresentationManifest> representationManifests = new HashMap<>();
        ResourceSet resourceSet = this.loadAllDocuments(projectId);

        for (RepresentationDescriptor representationDescriptor : representationsDescriptor) {
            IRepresentation representation = representationDescriptor.getRepresentation();
            if (representation instanceof Diagram) {
                RepresentationManifest representationManifest = this.createRepresentationManifest((Diagram) representation, representationDescriptor, resourceSet);
                if (representationManifest != null) {
                    UUID representationId = representationDescriptor.getId();
                    representationManifests.put(representationId.toString(), representationManifest);

                    byte[] bytes = new ObjectMapper().writeValueAsBytes(representationDescriptor);
                    String name = projectName + "/representations/" + representationId; //$NON-NLS-1$
                    ZipEntry zipEntry = this.createZipEntry(name, bytes.length);
                    zippedout.putNextEntry(zipEntry);
                    zippedout.write(bytes);
                    zippedout.closeEntry();
                }
            }
        }

        return representationManifests;
    }

    /**
     * Creates a {@link RepresentationManifest} for the given {@link Diagram}.
     *
     * @param diagram
     *            The given {@link Diagram}
     * @param resourceSet
     *            The {@link ResourceSet} containing all loaded documents
     * @param representationDescriptor
     *            The {@link RepresentationDescriptor} of the given {@link Diagram}
     * @return the {@link RepresentationManifest} for the given {@link Diagram}
     * @throws IOException
     *             if an I/O error occurred
     */
    private RepresentationManifest createRepresentationManifest(Diagram diagram, RepresentationDescriptor representationDescriptor, ResourceSet resourceSet) throws IOException {
        UUID descriptionId = diagram.getDescriptionId();
        Optional<IdMappingEntity> optionalIdMappingEntity = this.idMappingRepository.findById(descriptionId);
        if (optionalIdMappingEntity.isPresent()) {
            IdMappingEntity idMappingEntity = optionalIdMappingEntity.get();

            String uriFragment = ""; //$NON-NLS-1$
            String targetObjectId = diagram.getTargetObjectId();
            for (Resource resource : resourceSet.getResources()) {
                EObject eObject = resource.getEObject(targetObjectId);
                if (eObject != null) {
                    uriFragment = EcoreUtil.getURI(eObject).toString();
                    break;
                }
            }
            if (uriFragment.isEmpty()) {
                this.logger.error("The serialization of the representationManifest won't be complete."); //$NON-NLS-1$
            }
            // @formatter:off
            return RepresentationManifest.newRepresentationManifest()
                    .type(diagram.getClass().getSimpleName())
                    .descriptionURI(idMappingEntity.getExternalId())
                    .targetObjectURI(uriFragment)
                    .build();
            // @formatter:on

        }
        return null;
    }

    /**
     * Load all documents of the given project in a {@link ResourceSet}.
     *
     * @param projectId
     *            the ID of the project to export.
     * @return a {@link ResourceSet} containing all project documents.
     */
    private ResourceSet loadAllDocuments(UUID projectId) {
        List<Document> documents = this.documentService.getDocuments(projectId);
        ResourceSet resourceSet = new ResourceSetImpl();
        for (Document document : documents) {
            URI uri = URI.createURI(document.getId().toString());
            JsonResource resource = new SiriusWebJSONResourceFactoryImpl().createResource(uri);
            Optional<byte[]> optionalBytes = this.documentService.getBytes(document, IDocumentService.RESOURCE_KIND_JSON);
            if (optionalBytes.isPresent()) {
                try (var inputStream = new ByteArrayInputStream(optionalBytes.get())) {
                    resource.load(inputStream, null);
                    resourceSet.getResources().add(resource);
                } catch (IOException exception) {
                    this.logger.error(exception.getMessage(), exception);
                }
            }
        }
        return resourceSet;
    }

    /**
     * Adds a {@link ZipEntry} for the manifest file in the given {@link ZipOutputStream}.
     *
     * <p>
     * The name of the {@link ZipEntry} is [projectName]/manifest.json, where '/' are used as path separator in the zip.
     * </p>
     *
     * @param projectName
     *            The name of the project we want to export
     * @param id2DocumentName
     *            The map of document id to document name
     * @param representationsManifests
     *            The map of representation id to {@link RepresentationManifest}
     * @param zippedOut
     *            The {@link ZipOutputStream} used to build the zip
     * @return the mapping between document id and document name
     * @throws IOException
     *             if an I/O error occurred
     */
    private void addManifest(String projectName, Map<String, String> id2DocumentName, Map<String, RepresentationManifest> representationsManifests, ZipOutputStream zippedout) throws IOException {
        // @formatter:off
        ProjectManifest projectManifest = ProjectManifest.newProjectManifest(CURRENT_MANIFEST_VERSION, this.buildProperties.getVersion())
                .metamodels(new ArrayList<>(this.ePackageRegistry.keySet()))
                .documentIdsToName(id2DocumentName)
                .representations(representationsManifests)
                .build();
        // @formatter:on

        byte[] manifestContent = this.objectMapper.writeValueAsBytes(projectManifest);
        ZipEntry zipEntry = new ZipEntry(projectName + "/manifest.json"); //$NON-NLS-1$
        zippedout.putNextEntry(zipEntry);
        zippedout.write(manifestContent);
        zippedout.closeEntry();
    }

    private ZipEntry createZipEntry(String name, int length) {
        ZipEntry zipEntry = new ZipEntry(name);
        zipEntry.setSize(length);
        zipEntry.setTime(System.currentTimeMillis());
        // Add here other zip entry options
        return zipEntry;
    }

}
