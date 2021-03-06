
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
			
package net.openvpn.als.webforwards.actions;

import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import com.maverick.multiplex.MultiplexedConnection;
import net.openvpn.als.agent.AgentTunnel;
import net.openvpn.als.agent.DefaultAgentManager;
import net.openvpn.als.boot.Util;
import net.openvpn.als.core.CoreEvent;
import net.openvpn.als.core.CoreServlet;
import net.openvpn.als.core.actions.AuthenticatedAction;
import net.openvpn.als.core.stringreplacement.VariableReplacement;
import net.openvpn.als.policyframework.LaunchSession;
import net.openvpn.als.policyframework.LaunchSessionFactory;
import net.openvpn.als.policyframework.ResourceAccessEvent;
import net.openvpn.als.properties.Property;
import net.openvpn.als.properties.impl.profile.ProfilePropertyKey;
import net.openvpn.als.security.SessionInfo;
import net.openvpn.als.webforwards.WebForward;
import net.openvpn.als.webforwards.WebForwardEventConstants;
import net.openvpn.als.webforwards.WebForwardTypeItem;
import net.openvpn.als.webforwards.WebForwardTypes;

/**
 * Implementation of {@link net.openvpn.als.core.actions.AuthenticatedAction}
 * that launches a <i>Tunneled Site Web Forward</i>.
 */
public class LaunchTunneledSiteAction extends AuthenticatedAction {

    /**
     * Constructor.
     * 
     */
    public LaunchTunneledSiteAction() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.openvpn.als.core.actions.AuthenticatedAction#isIgnoreSessionLock()
     */
    protected boolean isIgnoreSessionLock() {
        return true;
    }

    public ActionForward onExecute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response)
                    throws Exception {
        // Get the web forward

        String launchId = request.getParameter(LaunchSession.LAUNCH_ID);
        if (Util.isNullOrTrimmedBlank(launchId)) {
            throw new Exception("No launch ID supplied.");
        }
        
        LaunchSession launchSession = LaunchSessionFactory.getInstance().getLaunchSession(launchId);
        launchSession.checkAccessRights(null, getSessionInfo(request));
        WebForward wf = (WebForward)launchSession.getResource();

        CoreEvent evt = new ResourceAccessEvent(this, WebForwardEventConstants.WEB_FORWARD_STARTED, wf, launchSession.getPolicy(), launchSession.getSession(),
                        CoreEvent.STATE_SUCCESSFUL).addAttribute(WebForwardEventConstants.EVENT_ATTR_WEB_FORWARD_URL,
            wf.getDestinationURL()).addAttribute(WebForwardEventConstants.EVENT_ATTR_WEB_FORWARD_TYPE,
            ((WebForwardTypeItem) WebForwardTypes.WEB_FORWARD_TYPES.get(wf.getType())).getName());
        CoreServlet.getServlet().fireCoreEvent(evt);
        
        VariableReplacement replacer = new VariableReplacement();
        replacer.setLaunchSession(launchSession);
        URL url = new URL(replacer.replace(wf.getDestinationURL()));
        MultiplexedConnection agent = DefaultAgentManager.getInstance().getAgentBySession(getSessionInfo(request));
        int port = DefaultAgentManager.getInstance().openURL((AgentTunnel)agent, url, launchSession);
        if(port == -1) {
        	throw new Exception("Agent couldn't open tunnel.");
        }
        
        // BPS to LDP - We have to wait for the serversocket on the agent end to startup
        // as it is in a thread. Is there a better way of doing this?
        Thread.sleep(1000); 
        
        return new ActionForward(url.getProtocol() + "://" + Property.getProperty(new ProfilePropertyKey("client.localhostAddress", launchSession.getSession())) + ":" + port + url.getFile(), true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.openvpn.als.core.actions.CoreAction#getNavigationContext(org.apache.struts.action.ActionMapping,
     *      org.apache.struts.action.ActionForm,
     *      javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    public int getNavigationContext(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        return SessionInfo.MANAGEMENT_CONSOLE_CONTEXT | SessionInfo.USER_CONSOLE_CONTEXT;
    }
}
