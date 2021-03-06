
				/*
 *  OpenVPNALS
 *
 *  Copyright (C) 2003-2006 3SP LTD. All Rights Reserved
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2 of
 *  the License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
			
package net.openvpn.als.properties.actions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import net.openvpn.als.boot.AbstractPropertyKey;
import net.openvpn.als.boot.ContextConfig;
import net.openvpn.als.boot.ContextKey;
import net.openvpn.als.boot.PropertyDefinition;
import net.openvpn.als.policyframework.Permission;
import net.openvpn.als.policyframework.PolicyConstants;
import net.openvpn.als.policyframework.PolicyUtil;
import net.openvpn.als.properties.forms.AbstractPropertiesForm;
import net.openvpn.als.properties.impl.realms.RealmKey;
import net.openvpn.als.properties.impl.realms.RealmProperties;
import net.openvpn.als.properties.impl.systemconfig.SystemConfigKey;
import net.openvpn.als.security.SessionInfo;

public class SystemConfigurationPropertiesAction extends AbstractPropertiesAction {
    static Log log = LogFactory.getLog(SystemConfigurationPropertiesAction.class);

    public ActionForward commit(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response)
                    throws Exception {
        PolicyUtil.checkPermission(PolicyConstants.SYSTEM_CONFIGURATION_RESOURCE_TYPE, PolicyConstants.PERM_CHANGE, request);
        return super.commit(mapping, form, request, response);
    }

    public ActionForward unspecified(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                     HttpServletResponse response) throws Exception {
        // Initialise form
        PolicyUtil.checkPermissions(PolicyConstants.SYSTEM_CONFIGURATION_RESOURCE_TYPE, new Permission[] { PolicyConstants.PERM_CHANGE }, request);
        AbstractPropertiesForm pf = (AbstractPropertiesForm) form;
        pf.setParentCategory(0);
        pf.setSelectedCategory(0);
        return super.unspecified(mapping, form, request, response);
    }

    public int getNavigationContext(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        return SessionInfo.MANAGEMENT_CONSOLE_CONTEXT;
    }

    public AbstractPropertyKey createKey(PropertyDefinition definition, AbstractPropertiesForm form, SessionInfo sessionInfo) {
        if (definition.getPropertyClass().getName().equals(ContextConfig.NAME)) {
            return new ContextKey(definition.getName());
        } else if (definition.getPropertyClass().getName().equals(RealmProperties.NAME)) {
            return new RealmKey(definition.getName(), sessionInfo.getRealm());
        }
        return new SystemConfigKey(definition.getName());
    }
}