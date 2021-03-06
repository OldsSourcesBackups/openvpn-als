
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
			
package net.openvpn.als.tunnels.actions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

import net.openvpn.als.agent.DefaultAgentManager;
import net.openvpn.als.boot.Util;
import net.openvpn.als.core.BundleActionMessage;
import net.openvpn.als.core.CoreException;
import net.openvpn.als.core.RedirectWithMessages;
import net.openvpn.als.core.actions.AuthenticatedAction;
import net.openvpn.als.policyframework.LaunchSession;
import net.openvpn.als.policyframework.LaunchSessionFactory;
import net.openvpn.als.policyframework.NoPermissionException;
import net.openvpn.als.security.SessionInfo;
import net.openvpn.als.tunnels.TunnelPlugin;
import net.openvpn.als.tunnels.TunnelingService;

/**
 * Implementation of {@link net.openvpn.als.core.actions.AuthenticatedAction}
 * that starts a lauched <i>SSL Tunnel</i>
 * <p>
 * A <i>Launch Session</i> must already have been configured
 */
public class StartTunnelAction extends AuthenticatedAction {

    /*
     * (non-Javadoc)
     * 
     * @see net.openvpn.als.core.actions.AuthenticatedAction#getNavigationContext(org.apache.struts.action.ActionMapping,
     *      org.apache.struts.action.ActionForm,
     *      javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    public int getNavigationContext(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        return SessionInfo.USER_CONSOLE_CONTEXT | SessionInfo.MANAGEMENT_CONSOLE_CONTEXT;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.openvpn.als.core.actions.AuthenticatedAction#onExecute(org.apache.struts.action.ActionMapping,
     *      org.apache.struts.action.ActionForm,
     *      javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    public ActionForward onExecute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response)
                    throws Exception {
        String launchId = request.getParameter(LaunchSession.LAUNCH_ID);
        if (Util.isNullOrTrimmedBlank(launchId)) {
            throw new Exception("No launchId parameter supplied.");
        }
        String returnTo = request.getParameter("returnTo");
        if (Util.isNullOrTrimmedBlank(returnTo)) {
            throw new Exception("No returnTo parameter supplied.");
        }
        LaunchSession launchSession = LaunchSessionFactory.getInstance().getLaunchSession(launchId);
        if (launchSession == null) {
            throw new Exception("Invalid launchId parameter supplied.");
        }
        try {
            launchSession.checkAccessRights(null, getSessionInfo(request));
            ((TunnelingService) DefaultAgentManager.getInstance().getService(TunnelingService.class))
                            .startTunnel(launchSession);
            ActionMessages msgs = new ActionMessages();
            msgs.add(Globals.MESSAGE_KEY, new BundleActionMessage(TunnelPlugin.MESSAGE_RESOURCES_KEY, "tunnels.started", launchSession.getResource().getResourceName()));
            saveMessages(request, msgs);
            if (!Util.isNullOrTrimmedBlank(request.getParameter("returnTo"))) {
                return new RedirectWithMessages(request.getParameter("returnTo"), request);
            }
        } catch (CoreException ce) {
            ActionMessages msgs = new ActionMessages();
            msgs.add(Globals.ERROR_KEY, ce.getBundleActionMessage());
            saveErrors(request, msgs);
        } catch (NoPermissionException e) {
            ActionMessages msgs = new ActionMessages();
            msgs.add(Globals.ERROR_KEY, new ActionMessage("tunnels.noPermission", launchSession.getResource().getResourceName()));
            saveErrors(request, msgs);
        }
        return new RedirectWithMessages(returnTo, request);

    }
}
