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
package org.eclipse.sirius.web.diagrams;

import java.text.MessageFormat;

import org.eclipse.sirius.web.annotations.Immutable;
import org.eclipse.sirius.web.annotations.graphql.GraphQLField;
import org.eclipse.sirius.web.annotations.graphql.GraphQLNonNull;
import org.eclipse.sirius.web.annotations.graphql.GraphQLObjectType;

/**
 * The size of an element.
 *
 * @author sbegaudeau
 */
@Immutable
@GraphQLObjectType
public final class Size {
    // @formatter:off
    public static final Size UNDEFINED = Size.newSize()
            .width(-1)
            .height(-1)
            .build();
    // @formatter:on

    private double width;

    private double height;

    private Size() {
        // Prevent instantiation
    }

    @GraphQLField
    @GraphQLNonNull
    public double getWidth() {
        return this.width;
    }

    @GraphQLField
    @GraphQLNonNull
    public double getHeight() {
        return this.height;
    }

    public static Builder newSize() {
        return new Builder();
    }

    @Override
    public String toString() {
        String pattern = "{0} '{'width: {1}, height: {2}'}'"; //$NON-NLS-1$
        return MessageFormat.format(pattern, this.getClass().getSimpleName(), this.width, this.height);
    }

    /**
     * The builder used to create a new size.
     *
     * @author sbegaudeau
     */
    @SuppressWarnings("checkstyle:HiddenField")
    public static final class Builder {
        private double width;

        private double height;

        private Builder() {
            // Prevent instantiation
        }

        public Builder width(double width) {
            this.width = width;
            return this;
        }

        public Builder height(double height) {
            this.height = height;
            return this;
        }

        public Size build() {
            Size size = new Size();
            size.width = this.width;
            size.height = this.height;
            return size;
        }
    }
}
