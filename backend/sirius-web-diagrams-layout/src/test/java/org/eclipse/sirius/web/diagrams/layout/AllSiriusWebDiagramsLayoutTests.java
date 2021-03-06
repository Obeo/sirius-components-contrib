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
package org.eclipse.sirius.web.diagrams.layout;

import org.eclipse.sirius.web.diagrams.layout.architecture.CodingRulesTestCases;
import org.eclipse.sirius.web.diagrams.layout.architecture.ImmutableTestCases;
import org.eclipse.sirius.web.diagrams.layout.services.DiagramConverterTestCases;
import org.eclipse.sirius.web.diagrams.layout.services.ImageNodeStyleSizeServiceTestCases;
import org.eclipse.sirius.web.diagrams.layout.services.ImageSizeServiceTestCases;
import org.eclipse.sirius.web.diagrams.layout.services.LayoutedDiagramProviderTestCases;
import org.eclipse.sirius.web.diagrams.layout.services.TextBoundsServiceTestCases;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Test suite used to run all the unit tests of the forms.
 *
 * @author sbegaudeau
 */
@RunWith(Suite.class)
@SuiteClasses({ CodingRulesTestCases.class, ImmutableTestCases.class, DiagramConverterTestCases.class, ImageNodeStyleSizeServiceTestCases.class, ImageSizeServiceTestCases.class,
        LayoutedDiagramProviderTestCases.class, TextBoundsServiceTestCases.class })
public final class AllSiriusWebDiagramsLayoutTests {
    private AllSiriusWebDiagramsLayoutTests() {
        // Prevent
    }
}
