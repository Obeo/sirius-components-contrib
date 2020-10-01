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
import { useCapabilities } from 'capabilities/useCapabilities';
import React from 'react';
import { Link } from 'react-router-dom';

export const Go = ({ to, children, ...props }) => {
  const capabilities = useCapabilities();
  if (capabilities?.overrides(to)) {
    return (
      <a {...props} href={to}>
        {children}
      </a>
    );
  } else {
    return (
      <Link {...props} to={to}>
        {children}
      </Link>
    );
  }
};
