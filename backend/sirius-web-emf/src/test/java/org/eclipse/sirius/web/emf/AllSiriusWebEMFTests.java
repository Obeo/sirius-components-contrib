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
package org.eclipse.sirius.web.emf;

import org.eclipse.sirius.web.emf.architecture.CodingRulesTestCases;
import org.eclipse.sirius.web.emf.architecture.ConfigurationTestCases;
import org.eclipse.sirius.web.emf.architecture.ServiceTestCases;
import org.eclipse.sirius.web.emf.architecture.SpringCodingRulesTestCases;
import org.eclipse.sirius.web.emf.services.CreateDocumentEventHandlerTestCases;
import org.eclipse.sirius.web.emf.services.DeleteDocumentEventHandlerTestCases;
import org.eclipse.sirius.web.emf.services.EditingContextFactoryTestCases;
import org.eclipse.sirius.web.emf.services.EditingContextPersistenceServiceTestCases;
import org.eclipse.sirius.web.emf.services.LabelFeatureProviderRegistryTestCases;
import org.eclipse.sirius.web.emf.services.LabelFeatureProviderTestCases;
import org.eclipse.sirius.web.emf.services.ProjectImportServiceTestCases;
import org.eclipse.sirius.web.emf.services.RenameDocumentEventHandlerTestCases;
import org.eclipse.sirius.web.emf.services.UploadDocumentEventHandlerTestCases;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Test suite used to run all the unit tests of the forms.
 *
 * @author sbegaudeau
 */
@RunWith(Suite.class)
@SuiteClasses({ CodingRulesTestCases.class, ConfigurationTestCases.class, ServiceTestCases.class, SpringCodingRulesTestCases.class, CreateDocumentEventHandlerTestCases.class,
        UploadDocumentEventHandlerTestCases.class, DeleteDocumentEventHandlerTestCases.class, RenameDocumentEventHandlerTestCases.class, EditingContextFactoryTestCases.class,
        EditingContextPersistenceServiceTestCases.class, ProjectImportServiceTestCases.class, LabelFeatureProviderRegistryTestCases.class, LabelFeatureProviderTestCases.class })
public final class AllSiriusWebEMFTests {
    private AllSiriusWebEMFTests() {
        // Prevent instantiation
    }
}
