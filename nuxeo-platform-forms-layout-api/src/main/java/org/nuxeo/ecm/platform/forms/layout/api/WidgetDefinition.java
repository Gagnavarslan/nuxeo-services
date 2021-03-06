/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: WidgetDefinition.java 28498 2008-01-05 11:46:25Z sfermigier $
 */

package org.nuxeo.ecm.platform.forms.layout.api;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Widget definition interface.
 * <p>
 * A widget knows how to render itself in a given mode.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public interface WidgetDefinition extends Serializable {

    String RENDERED_PROPERTY_NAME = "rendered";

    String REQUIRED_PROPERTY_NAME = "required";

    /**
     * Returns the widget name used to identify it within a layout.
     */
    String getName();

    /**
     * @since 5.5
     */
    void setName(String name);

    /**
     * Returns the widget type used to render it.
     */
    String getType();

    /**
     * @since 5.5
     */
    void setType(String type);

    /**
     * Returns the list of fields managed by this widget.
     */
    FieldDefinition[] getFieldDefinitions();

    /**
     * @since 5.5
     */
    void setFieldDefinitions(FieldDefinition[] fieldDefinitions);

    /**
     * Returns the optional mode used to override the layout mode.
     * <p>
     * Can be a string or an EL ValueExpression.
     *
     * @param layoutMode the layout (or parent widget) mode
     * @return the overriding widget mode or null if none is defined.
     */
    String getMode(String layoutMode);

    Map<String, String> getModes();

    /**
     * @since 5.5
     */
    void setModes(Map<String, String> modes);

    /**
     * Returns an EL expression evaluating to true if the widget is required in
     * given mode.
     * <p>
     * This value is computed from the property "required" in given mode. and
     * can be a string or an EL ValueExpression. Defaults to "false".
     */
    String getRequired(String layoutMode, String mode);

    /**
     * Returns the label to use in a given mode.
     */
    String getLabel(String mode);

    /**
     * Returns labels by mode.
     */
    Map<String, String> getLabels();

    /**
     * @since 5.5
     */
    void setLabels(Map<String, String> labels);

    /**
     * Returns the help label to use in a given mode.
     */
    String getHelpLabel(String mode);

    /**
     * Returns help labels by mode.
     */
    Map<String, String> getHelpLabels();

    /**
     * @since 5.5
     */
    void setHelpLabels(Map<String, String> helpLabels);

    /**
     * Returns true if all labels are messages that need to be translated.
     * <p>
     * Default is true.
     */
    boolean isTranslated();

    /**
     * @since 5.5
     */
    void setTranslated(boolean translated);

    /**
     * Returns a map of properties to use in a given mode.
     * <p>
     * A property value can be a string or an EL ValueExpression.
     * <p>
     * The way that properties will be mapped to rendered components is managed
     * by the widget type.
     */
    Map<String, Serializable> getProperties(String layoutMode, String mode);

    /**
     * Returns properties by mode.
     */
    Map<String, Map<String, Serializable>> getProperties();

    /**
     * @since 5.5
     */
    void setProperties(Map<String, Map<String, Serializable>> properties);

    /**
     * Returns properties by widget mode.
     */
    Map<String, Map<String, Serializable>> getWidgetModeProperties();

    /**
     * @since 5.5
     */
    void setWidgetModeProperties(
            Map<String, Map<String, Serializable>> properties);

    /**
     * Returns sub widget definitions.
     */
    WidgetDefinition[] getSubWidgetDefinitions();

    /**
     * @since 5.5
     */
    void setSubWidgetDefinitions(WidgetDefinition[] subWidgets);

    /**
     * Returns the select options for this widget.
     *
     * @since 5.4.2
     */
    WidgetSelectOption[] getSelectOptions();

    /**
     * @since 5.5
     */
    void setSelectOptions(WidgetSelectOption[] selectOptions);

    /**
     * Returns the map of rendering information per mode.
     * <p>
     * Useful for preview management where some configuration needs to be
     * changed: what's changed can be set as rendering information here to be
     * displayed.
     *
     * @since 5.5
     */
    Map<String, List<RenderingInfo>> getRenderingInfos();

    /**
     * Returns the list of rendering information for given mode.
     *
     * @since 5.5
     */
    List<RenderingInfo> getRenderingInfos(String mode);

    /**
     * @since 5.5
     */
    void setRenderingInfos(Map<String, List<RenderingInfo>> renderingInfos);

    /**
     * Returns a clone instance of this widget definition.
     * <p>
     * Useful for conversion of widget definition during export.
     *
     * @since 5.5
     */
    WidgetDefinition clone();

}
