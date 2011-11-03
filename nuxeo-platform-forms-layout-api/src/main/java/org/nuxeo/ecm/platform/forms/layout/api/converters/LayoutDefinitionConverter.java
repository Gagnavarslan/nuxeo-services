/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.api.converters;

import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;

/**
 * Converter for a layout definition.
 *
 * @since 5.5
 */
public interface LayoutDefinitionConverter {

    /**
     * Returns the original layout definition, or a clone if it needs to be
     * changed. Can also return null if layout should be removed.
     */
    LayoutDefinition getLayoutDefinition(LayoutDefinition orig,
            LayoutConversionContext ctx);

}
