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
package org.eclipse.sirius.web.compat.operations;

import static org.junit.Assert.assertEquals;

import java.text.MessageFormat;
import java.util.UUID;

import org.eclipse.emf.common.command.BasicCommandStack;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.impl.EPackageRegistryImpl;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreAdapterFactory;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.eclipse.emf.edit.provider.ReflectiveItemProviderAdapterFactory;
import org.eclipse.sirius.viewpoint.description.tool.ChangeContext;
import org.eclipse.sirius.viewpoint.description.tool.CreateInstance;
import org.eclipse.sirius.viewpoint.description.tool.ToolFactory;
import org.eclipse.sirius.web.compat.services.representations.EPackageService;
import org.eclipse.sirius.web.representations.Status;
import org.eclipse.sirius.web.services.api.objects.IEditingContext;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests of the CreateInstance operation handler.
 *
 * @author lfasani
 */
public class CreateInstanceOperationHandlerTestCases {
    private static final String VARIABLE_NAME = "myVariableName"; //$NON-NLS-1$

    private static final String REFERENCE_NAME = "eClassifiers"; //$NON-NLS-1$

    private static final String TYPE_NAME = "ecore::EClass"; //$NON-NLS-1$

    private CreateInstanceOperationHandler createInstanceOperationHandler;

    private CreateInstance createInstanceOperation;

    private OperationTestContext operationTestContext;

    @Before
    public void initialize() {
        this.operationTestContext = new OperationTestContext();

        ComposedAdapterFactory composedAdapterFactory = new ComposedAdapterFactory();
        composedAdapterFactory.addAdapterFactory(new EcoreAdapterFactory());
        composedAdapterFactory.addAdapterFactory(new ReflectiveItemProviderAdapterFactory());

        EPackage.Registry ePackageRegistry = new EPackageRegistryImpl();
        ePackageRegistry.put(EcorePackage.eINSTANCE.getNsURI(), EcorePackage.eINSTANCE);

        ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.setPackageRegistry(ePackageRegistry);

        EditingDomain editingDomain = new AdapterFactoryEditingDomain(composedAdapterFactory, new BasicCommandStack(), resourceSet);
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
        this.operationTestContext.getVariables().put(IEditingContext.EDITING_CONTEXT, editingContext);

        this.createInstanceOperation = ToolFactory.eINSTANCE.createCreateInstance();
        this.createInstanceOperationHandler = new CreateInstanceOperationHandler(this.operationTestContext.getInterpreter(), new EPackageService(), new ChildModelOperationHandler(),
                this.createInstanceOperation);
    }

    @Test
    public void createInstanceOperationHandlerNominalCaseTest() {
        // used to check that the variable name is added in variable scope
        String className = "newClass"; //$NON-NLS-1$
        ChangeContext subChangeContext = ToolFactory.eINSTANCE.createChangeContext();
        subChangeContext.setBrowseExpression("aql:" + VARIABLE_NAME + ".renameENamedElementService('" + className + "'))"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
        this.createInstanceOperation.getSubModelOperations().add(subChangeContext);

        // check the nominal case
        this.createInstanceOperation.setReferenceName(REFERENCE_NAME);
        this.createInstanceOperation.setTypeName(TYPE_NAME);
        this.createInstanceOperation.setVariableName(VARIABLE_NAME);

        Status handleResult = this.createInstanceOperationHandler.handle(this.operationTestContext.getVariables());

        assertEquals(Status.OK, handleResult);
        assertEquals(2, this.operationTestContext.getRootPackage().getEClassifiers().size());
        assertEquals(className, this.operationTestContext.getRootPackage().getEClassifiers().get(1).getName());

        // check that an empty variable name is a valid case
        this.createInstanceOperation.setVariableName(""); //$NON-NLS-1$

        handleResult = this.createInstanceOperationHandler.handle(this.operationTestContext.getVariables());

        assertEquals(Status.OK, handleResult);
        assertEquals(3, this.operationTestContext.getRootPackage().getEClassifiers().size());
        assertEquals(null, this.operationTestContext.getRootPackage().getEClassifiers().get(2).getName());
    }

    @Test
    public void createInstanceOperationHandlerChangeSelfCaseTest() {
        // used to check that the variable name is added in variable scope
        String className = "newClass"; //$NON-NLS-1$
        ChangeContext subChangeContext = ToolFactory.eINSTANCE.createChangeContext();
        subChangeContext.setBrowseExpression("aql:self.renameENamedElementService('" + className + "'))"); //$NON-NLS-1$//$NON-NLS-2$
        this.createInstanceOperation.getSubModelOperations().add(subChangeContext);

        // check the nominal case
        this.createInstanceOperation.setReferenceName(REFERENCE_NAME);
        this.createInstanceOperation.setTypeName(TYPE_NAME);
        this.createInstanceOperation.setVariableName(VARIABLE_NAME);

        Status handleResult = this.createInstanceOperationHandler.handle(this.operationTestContext.getVariables());

        assertEquals(Status.OK, handleResult);
        assertEquals(2, this.operationTestContext.getRootPackage().getEClassifiers().size());
        assertEquals(className, this.operationTestContext.getRootPackage().getEClassifiers().get(1).getName());

        // check that an empty variable name is a valid case
        this.createInstanceOperation.setVariableName(""); //$NON-NLS-1$

        handleResult = this.createInstanceOperationHandler.handle(this.operationTestContext.getVariables());

        assertEquals(Status.OK, handleResult);
        assertEquals(3, this.operationTestContext.getRootPackage().getEClassifiers().size());
        assertEquals(className, this.operationTestContext.getRootPackage().getEClassifiers().get(2).getName());
    }

    /**
     * Check that a null or empty data do not stop the handle of subOperations.</br>
     */
    @Test
    public void createInstanceOperationHandlerErrorCasesTest() {
        // Add a SubModelOperations to check that it is handled
        ChangeContext subChangeContext = ToolFactory.eINSTANCE.createChangeContext();
        this.createInstanceOperation.getSubModelOperations().add(subChangeContext);

        // Check null expression case
        this.handleAndCheckExecution(null, null, null, this.operationTestContext.getRootPackage());

        // Check empty expression case
        this.handleAndCheckExecution("", "", "", this.operationTestContext.getRootPackage()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        this.handleAndCheckExecution(REFERENCE_NAME, "UnknownClass", VARIABLE_NAME, this.operationTestContext.getRootPackage()); //$NON-NLS-1$
    }

    /**
     * Execute the root operation and check that the sub ChangeContext operation is properly executed.
     */
    private void handleAndCheckExecution(String referenceName, String typeName, String variableName, ENamedElement renamedElement) {
        String newName = UUID.randomUUID().toString();
        String renameExpression = MessageFormat.format(ModelOperationServices.AQL_RENAME_EXPRESSION, newName);
        ((ChangeContext) this.createInstanceOperation.getSubModelOperations().get(0)).setBrowseExpression(renameExpression);

        // execute
        this.createInstanceOperation.setReferenceName(referenceName);
        this.createInstanceOperation.setTypeName(typeName);
        this.createInstanceOperation.setVariableName(variableName);

        Status handleResult = this.createInstanceOperationHandler.handle(this.operationTestContext.getVariables());

        // check
        assertEquals(Status.OK, handleResult);
        assertEquals(newName, renamedElement.getName());
    }

}
