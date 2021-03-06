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
import { useQuery } from 'common/GraphQLHooks';
import gql from 'graphql-tag';
import React, { useContext, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';

const getProjectQuery = gql`
  query getProject($projectId: ID!) {
    viewer {
      id
      username
      project(projectId: $projectId) {
        id
        name
        visibility
        accessLevel
      }
    }
  }
`.loc.source.body;

export const ProjectContext = React.createContext({});

const ProjectProvider = ({ children }) => {
  const [state, setState] = useState({});
  const { projectId } = useParams();
  const { loading, data } = useQuery(getProjectQuery, { projectId }, 'getProject');
  useEffect(() => {
    if (!loading && data) {
      setState(data.data.viewer.project);
    }
  }, [loading, data, setState]);

  return <ProjectContext.Provider value={state}>{children}</ProjectContext.Provider>;
};

export const withProject = (Child) => {
  return () => {
    return (
      <ProjectProvider>
        <Child />
      </ProjectProvider>
    );
  };
};

export const useProject = () => {
  return useContext(ProjectContext);
};
