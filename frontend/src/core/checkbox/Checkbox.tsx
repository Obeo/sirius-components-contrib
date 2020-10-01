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
import { Text } from 'core/text/Text';
import PropTypes from 'prop-types';
import React from 'react';
import styles from './Checkbox.module.css';

const propTypes = {
  name: PropTypes.string.isRequired,
  checked: PropTypes.bool.isRequired,
  onChange: PropTypes.func,
  readOnly: PropTypes.bool,
  label: PropTypes.oneOfType([PropTypes.string, PropTypes.node]).isRequired,
  'data-testid': PropTypes.string.isRequired,
};

export const Checkbox = ({ name, checked, onChange, readOnly, label, 'data-testid': dataTestId }) => {
  let checkboxClass = `${styles.checkbox}`;
  if (checked) {
    checkboxClass = `${checkboxClass} ${styles.checked}`;
  }
  return (
    <label className={checkboxClass}>
      <input
        type="checkbox"
        name={name}
        checked={checked}
        readOnly={readOnly}
        onChange={onChange}
        data-testid={dataTestId}
      />
      <span className={styles.checkmark}></span>
      <Text className={styles.label}>{label}</Text>
    </label>
  );
};
Checkbox.propTypes = propTypes;
