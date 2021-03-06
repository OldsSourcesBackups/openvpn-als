/*
 * Copyright (C) 2006-2008 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */

package org.alfresco.jlan.app;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.alfresco.jlan.debug.Debug;
import org.alfresco.jlan.debug.DebugConfigSection;
import org.alfresco.jlan.netbios.win32.Win32NetBIOS;
import org.alfresco.jlan.server.auth.CifsAuthenticator;
import org.alfresco.jlan.server.auth.UserAccount;
import org.alfresco.jlan.server.auth.UserAccountList;
import org.alfresco.jlan.server.auth.acl.ACLParseException;
import org.alfresco.jlan.server.auth.acl.AccessControl;
import org.alfresco.jlan.server.auth.acl.AccessControlList;
import org.alfresco.jlan.server.auth.acl.AccessControlParser;
import org.alfresco.jlan.server.auth.acl.InvalidACLTypeException;
import org.alfresco.jlan.server.config.CoreServerConfigSection;
import org.alfresco.jlan.server.config.GlobalConfigSection;
import org.alfresco.jlan.server.config.InvalidConfigurationException;
import org.alfresco.jlan.server.config.SecurityConfigSection;
import org.alfresco.jlan.server.config.ServerConfiguration;
import org.alfresco.jlan.server.core.DeviceContextException;
import org.alfresco.jlan.server.core.ShareType;
import org.alfresco.jlan.server.core.SharedDeviceList;
import org.alfresco.jlan.server.filesys.DiskDeviceContext;
import org.alfresco.jlan.server.filesys.DiskInterface;
import org.alfresco.jlan.server.filesys.DiskSharedDevice;
import org.alfresco.jlan.server.filesys.FilesystemsConfigSection;
import org.alfresco.jlan.server.filesys.SrvDiskInfo;
import org.alfresco.jlan.server.filesys.VolumeInfo;
import org.alfresco.jlan.server.thread.ThreadRequestPool;
import org.alfresco.jlan.smb.Dialect;
import org.alfresco.jlan.smb.DialectSelector;
import org.alfresco.jlan.smb.server.CIFSConfigSection;
import org.alfresco.jlan.smb.server.SMBSrvSession;
import org.alfresco.jlan.smb.util.DriveMapping;
import org.alfresco.jlan.smb.util.DriveMappingList;
import org.alfresco.jlan.util.IPAddress;
import org.alfresco.jlan.util.MemorySize;
import org.alfresco.jlan.util.Platform;
import org.alfresco.jlan.util.StringList;
import org.alfresco.jlan.util.X64;
import org.alfresco.config.ConfigElement;
import org.alfresco.config.element.GenericConfigElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Cifs Only XML File Server Configuration Class
 * 
 * <p>
 * XML implementation of the SMB server configuration.
 * 
 * @author gkspencer
 */
public class CifsOnlyXMLServerConfiguration extends ServerConfiguration {

	// Constants
	//
	// Node type for an Element

	private static final int ELEMENT_TYPE = 1;

	// CIFS session debug type strings
	//
	// Must match the bit mask order.

	private static final String m_sessDbgStr[] = { "NETBIOS", "STATE", "RXDATA", "TXDATA", "DUMPDATA", "NEGOTIATE", "TREE",
			"SEARCH", "INFO", "FILE", "FILEIO", "TRANSACT", "ECHO", "ERROR", "IPC", "LOCK", "PKTTYPE", "DCERPC", "STATECACHE",
			"TIMING", "NOTIFY", "STREAMS", "SOCKET", "PKTPOOL", "PKTSTATS", "THREADPOOL", "BENCHMARK" };

	// Default session debug flags, if enabled

	private static final int DEFAULT_SESSDEBUG = SMBSrvSession.DBG_ERROR + SMBSrvSession.DBG_INFO + SMBSrvSession.DBG_SEARCH
			+ SMBSrvSession.DBG_TREE + SMBSrvSession.DBG_TRAN + SMBSrvSession.DBG_STATE;

	// Valid drive letter names for mapped drives

	private static final String _driveLetters = "CDEFGHIJKLMNOPQRSTUVWXYZ";

	// Default thread pool size
	
	private static final int DefaultThreadPoolInit	= 25;
	private static final int DefaultThreadPoolMax	= 50;
	
	// Default memory pool settings
	
	private static final int[] DefaultMemoryPoolBufSizes  = { 256, 4096, 16384, 65536 };
	private static final int[] DefaultMemoryPoolInitAlloc = {  20,   20,     5,     5 };
	private static final int[] DefaultMemoryPoolMaxAlloc  = { 100,   50,    50,    50 };
	
	// Memory pool packet size limits
	
	private static final int MemoryPoolMinimumPacketSize	= 256;
	private static final int MemoryPoolMaximumPacketSize	= 128 * (int) MemorySize.KILOBYTE; 
		
	// Memory pool allocation limits
	
	private static final int MemoryPoolMinimumAllocation	= 5;
	private static final int MemoryPoolMaximumAllocation    = 500;
	
	// Maximum session timeout
	
	private static final int MaxSessionTimeout				= 60 * 60;	// 1 hour
	
	// Date formatter

	private SimpleDateFormat m_dateFmt = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss");

	/**
	 * Default constructor
	 */
	public CifsOnlyXMLServerConfiguration() {
		super("");
	}

	/**
	 * Load the configuration from the specified file.
	 * 
	 * @param fname java.lang.String
	 * @exception IOException
	 * @exception InvalidConfigurationException
	 */
	public final void loadConfiguration(String fname)
		throws IOException, InvalidConfigurationException {

		// Open the configuration file

		InputStream inFile = new FileInputStream(fname);
		Reader inRead = new InputStreamReader(inFile);

		// Call the main parsing method

		loadConfiguration(inRead);
	}

	/**
	 * Load the configuration from the specified input stream
	 * 
	 * @param in Reader
	 * @exception IOException
	 * @exception InvalidConfigurationException
	 */
	public final void loadConfiguration(Reader in)
		throws IOException, InvalidConfigurationException {

		// Reset the current configuration to the default settings

		removeAllConfigSections();

		// Load and parse the XML configuration document

		try {

			// Load the configuration from the XML file

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();

			InputSource xmlSource = new InputSource(in);
			Document doc = builder.parse(xmlSource);

			// Parse the document

			loadConfiguration(doc);
		}
		catch (Exception ex) {

			// Rethrow the exception as a configuration exeception

			throw new InvalidConfigurationException("XML error", ex);
		}
		finally {

			// Close the input file

			in.close();
		}
	}

	/**
	 * Load the configuration from the specified document
	 * 
	 * @param doc Document
	 * @exception IOException
	 * @exception InvalidConfigurationException
	 */
	public void loadConfiguration(Document doc)
		throws IOException, InvalidConfigurationException {

		// Reset the current configuration to the default settings

		removeAllConfigSections();

		// Parse the XML configuration document

		try {

			// Access the root of the XML document, get a list of the child nodes

			Element root = doc.getDocumentElement();
			NodeList childNodes = root.getChildNodes();

			// Process the debug settings element

			procDebugElement(findChildNode("debug", childNodes));

			// Process the core server configuration settings
			
			procServerCoreElement(findChildNode("server-core", childNodes));
			
			// Process the global configuration settings

			procGlobalElement(findChildNode("global", childNodes));

			// Process the security element

			procSecurityElement(findChildNode("security", childNodes));

			// Process the shares element

			procSharesElement(findChildNode("shares", childNodes));

			// Process the SMB server specific settings

			procSMBServerElement(findChildNode("SMB", childNodes));

			// Process the drive mappings settings

			procDriveMappingsElement(findChildNode("DriveMappings", childNodes));
		}
		catch (Exception ex) {

			// Rethrow the exception as a configuration exeception

			throw new InvalidConfigurationException("XML error", ex);
		}
	}

	/**
	 * Process the server core settings XML element
	 * 
	 * @param srvCore Element
	 * @exception InvalidConfigurationException
	 */
	protected final void procServerCoreElement(Element srvCore)
		throws InvalidConfigurationException {

		// Create the core server configuration section

		CoreServerConfigSection coreConfig = new CoreServerConfigSection(this);

		// Check if the server core element has been specified

		if ( srvCore == null) {
			
			// Configure a default memory pool
			
			coreConfig.setMemoryPool( DefaultMemoryPoolBufSizes, DefaultMemoryPoolInitAlloc, DefaultMemoryPoolMaxAlloc);
			
			// Configure a default thread pool size
			
			coreConfig.setThreadPool( DefaultThreadPoolInit, DefaultThreadPoolMax);
			return;
		}

		// Check if the thread pool size has been specified
		
		Element elem = findChildNode("threadPool", srvCore.getChildNodes());
		if ( elem != null) {
			
			// Get the initial thread pool size
			
			String initSizeStr = elem.getAttribute("init");
			if ( initSizeStr == null || initSizeStr.length() == 0)
				throw new InvalidConfigurationException("Thread pool initial size not specified");
			
			// Validate the initial thread pool size
			
			int initSize = 0;
			
			try {
				initSize = Integer.parseInt( initSizeStr);
			}
			catch (NumberFormatException ex) {
				throw new InvalidConfigurationException("Invalid thread pool size value, " + initSizeStr);
			}
			
			// Range check the thread pool size
			
			if ( initSize < ThreadRequestPool.MinimumWorkerThreads)
				throw new InvalidConfigurationException("Thread pool size below minimum allowed size");
			
			if ( initSize > ThreadRequestPool.MaximumWorkerThreads)
				throw new InvalidConfigurationException("Thread pool size above maximum allowed size");
			
			// Get the maximum thread pool size
			
			String maxSizeStr = elem.getAttribute("max");
			int maxSize = initSize;
			
			if ( maxSizeStr.length() > 0) {
				
				// Validate the maximum thread pool size
				
				try {
					maxSize = Integer.parseInt( maxSizeStr);
				}
				catch (NumberFormatException ex) {
					throw new InvalidConfigurationException(" Invalid thread pool maximum size value, " + maxSizeStr);
				}
				
				// Range check the maximum thread pool size
				
				if ( maxSize < ThreadRequestPool.MinimumWorkerThreads)
					throw new InvalidConfigurationException("Thread pool maximum size below minimum allowed size");
				
				if ( maxSize > ThreadRequestPool.MaximumWorkerThreads)
					throw new InvalidConfigurationException("Thread pool maximum size above maximum allowed size");
				
				if ( maxSize < initSize)
					throw new InvalidConfigurationException("Initial size is larger than maxmimum size");
			}
			else if ( maxSizeStr != null)
				throw new InvalidConfigurationException("Thread pool maximum size not specified");
			
			// Configure the thread pool
			
			coreConfig.setThreadPool( initSize, maxSize);
		}
		else {
			
			// Configure a default thread pool size
			
			coreConfig.setThreadPool( DefaultThreadPoolInit, DefaultThreadPoolMax);
		}
		
		// Check if thread pool debug output is enabled
		
		if ( findChildNode("threadPoolDebug", srvCore.getChildNodes()) != null)
			coreConfig.getThreadPool().setDebug( true);
		
		// Check if the memory pool configuration has been specified
		
		elem = findChildNode("memoryPool", srvCore.getChildNodes());
		if ( elem != null) {
			
			// Check if the packet sizes/allocations have been specified
			
			Element pktElem = findChildNode("packetSizes", elem.getChildNodes());
			if ( pktElem != null) {

				// Calculate the array size for the packet size/allocation arrays
				
				NodeList nodeList = pktElem.getChildNodes();
				int elemCnt = 0;
				
				for ( int i = 0; i < nodeList.getLength(); i++) {
					if ( nodeList.item( i).getNodeType() == ELEMENT_TYPE)
						elemCnt++;
				}
				
				// Create the packet size, initial allocation and maximum allocation arrays
				
				int[] pktSizes  = new int[elemCnt];
				int[] initSizes = new int[elemCnt];
				int[] maxSizes  = new int[elemCnt];
				
				int elemIdx = 0;
				
				// Process the packet size elements
				
				for ( int i = 0; i < nodeList.getLength(); i++) {
					
					// Get the current element node
					
					Node curNode = nodeList.item( i);
					if ( curNode.getNodeType() == ELEMENT_TYPE) {
						
						// Get the element and check if it is a packet size element
						
						Element curElem = (Element) curNode;
						if ( curElem.getNodeName().equals("packet")) {
							
							// Get the packet size
							
							int pktSize   = -1;
							int initAlloc = -1;
							int maxAlloc  = -1;
							
							String pktSizeStr = curElem.getAttribute("size");
							if ( pktSizeStr == null || pktSizeStr.length() == 0)
								throw new InvalidConfigurationException("Memory pool packet size not specified");
							
							// Parse the packet size
							
							try {
								pktSize = MemorySize.getByteValueInt( pktSizeStr);
							}
							catch ( NumberFormatException ex) {
								throw new InvalidConfigurationException("Memory pool packet size, invalid size value, " + pktSizeStr);
							}

							// Make sure the packet sizes have been specified in ascending order
							
							if ( elemIdx > 0 && pktSizes[elemIdx - 1] >= pktSize)
								throw new InvalidConfigurationException("Invalid packet size specified, less than/equal to previous packet size");
							
							// Get the initial allocation for the current packet size
							
							String initSizeStr = curElem.getAttribute("init");
							if ( initSizeStr == null || initSizeStr.length() == 0)
								throw new InvalidConfigurationException("Memory pool initial allocation not specified");
							
							// Parse the initial allocation
							
							try {
								initAlloc = Integer.parseInt( initSizeStr);
							}
							catch (NumberFormatException ex) {
								throw new InvalidConfigurationException("Invalid initial allocation, " + initSizeStr);
							}
							
							// Range check the initial allocation
							
							if ( initAlloc < MemoryPoolMinimumAllocation)
								throw new InvalidConfigurationException("Initial memory pool allocation below minimum of " + MemoryPoolMinimumAllocation);
							
							if ( initAlloc > MemoryPoolMaximumAllocation)
								throw new InvalidConfigurationException("Initial memory pool allocation above maximum of " + MemoryPoolMaximumAllocation);
							
							// Get the maximum allocation for the current packet size

							String maxSizeStr = curElem.getAttribute("max");
							if ( maxSizeStr == null || maxSizeStr.length() == 0)
								throw new InvalidConfigurationException("Memory pool maximum allocation not specified");
							
							// Parse the maximum allocation
							
							try {
								maxAlloc = Integer.parseInt( maxSizeStr);
							}
							catch (NumberFormatException ex) {
								throw new InvalidConfigurationException("Invalid maximum allocation, " + maxSizeStr);
							}

							// Range check the maximum allocation
							
							if ( maxAlloc < MemoryPoolMinimumAllocation)
								throw new InvalidConfigurationException("Maximum memory pool allocation below minimum of " + MemoryPoolMinimumAllocation);
							
							if ( initAlloc > MemoryPoolMaximumAllocation)
								throw new InvalidConfigurationException("Maximum memory pool allocation above maximum of " + MemoryPoolMaximumAllocation);

							// Set the current packet size elements
							
							pktSizes[elemIdx]  = pktSize;
							initSizes[elemIdx] = initAlloc;
							maxSizes[elemIdx]  = maxAlloc;
							
							elemIdx++;
						}
					}
						
				}
				
				// Check if all elements were used in the packet size/allocation arrays
				
				if ( elemIdx < pktSizes.length) {
					
					// Re-allocate the packet size/allocation arrays
					
					int[] newPktSizes  = new int[elemIdx];
					int[] newInitSizes = new int[elemIdx];
					int[] newMaxSizes  = new int[elemIdx];
					
					// Copy the values to the shorter arrays
					
					System.arraycopy(pktSizes, 0, newPktSizes, 0, elemIdx);
					System.arraycopy(initSizes, 0, newInitSizes, 0, elemIdx);
					System.arraycopy(maxSizes, 0, newMaxSizes, 0, elemIdx);
					
					// Move the new arrays into place
					
					pktSizes  = newPktSizes;
					initSizes = newInitSizes;
					maxSizes  = newMaxSizes;
				}
				
				// Configure the memory pool
				
				coreConfig.setMemoryPool( pktSizes, initSizes, maxSizes);
			}
		}
		else {
			
			// Configure a default memory pool
			
			coreConfig.setMemoryPool( DefaultMemoryPoolBufSizes, DefaultMemoryPoolInitAlloc, DefaultMemoryPoolMaxAlloc);
		}
	}

	/**
	 * Process the global settings XML element
	 * 
	 * @param global Element
	 * @exception InvalidConfigurationException
	 */
	protected final void procGlobalElement(Element global)
		throws InvalidConfigurationException {

		// Create the global configuration section

		GlobalConfigSection globalConfig = new GlobalConfigSection(this);

		// Check if the global element has been specified

		if ( global == null)
			return;

		// Check if the timezone has been specified

		Element elem = findChildNode("timezone", global.getChildNodes());
		if ( elem != null) {

			// Check for the timezone name

			String tzName = elem.getAttribute("name");
			if ( tzName != null && tzName.length() > 0)
				globalConfig.setTimeZone(tzName);

			// Check for the timezone offset value

			String tzOffset = elem.getAttribute("offset");
			if ( tzOffset != null && tzOffset.length() > 0 && tzName != null && tzName.length() > 0)
				throw new InvalidConfigurationException("Specify name or offset for timezone");

			// Validate the timezone offset

			if ( tzOffset != null && tzOffset.length() > 0) {
				int offset = 0;

				try {
					offset = Integer.parseInt(tzOffset);
				}
				catch (NumberFormatException ex) {
					throw new InvalidConfigurationException("Invalid timezone offset value, " + tzOffset);
				}

				// Range check the timezone offset value

				if ( offset < -1440 || offset > 1440)
					throw new InvalidConfigurationException("Invalid timezone offset, value out of valid range, " + tzOffset);

				// Set the timezone offset in minutes from UTC

				globalConfig.setTimeZoneOffset(offset);
			}
		}
	}

	/**
	 * Process the SMB server XML element
	 * 
	 * @param smb Element
	 * @exception InvalidConfigurationException
	 */
	protected final void procSMBServerElement(Element smb)
		throws InvalidConfigurationException {

		// Check if the SMB element is valid

		if ( smb == null)
			throw new InvalidConfigurationException("SMB section must be specified");

		// Create the CIFS server configuration section

		CIFSConfigSection cifsConfig = new CIFSConfigSection(this);

		// Process the main SMB server settings

		procHostElement(findChildNode("host", smb.getChildNodes()), cifsConfig);

		// Debug settings are now specified within the SMB server configuration block

		// Check if NetBIOS debug is enabled

		Element elem = findChildNode("netbiosDebug", smb.getChildNodes());
		if ( elem != null)
			cifsConfig.setNetBIOSDebug(true);

		// Check if host announcement debug is enabled

		elem = findChildNode("announceDebug", smb.getChildNodes());
		if ( elem != null)
			cifsConfig.setHostAnnounceDebug(true);

		// Check if session debug is enabled

		elem = findChildNode("sessionDebug", smb.getChildNodes());
		if ( elem != null) {

			// Check for session debug flags

			String flags = elem.getAttribute("flags");
			int sessDbg = DEFAULT_SESSDEBUG;

			if ( flags != null) {

				// Clear the default debug flags

				sessDbg = 0;

				// Parse the flags

				flags = flags.toUpperCase();
				StringTokenizer token = new StringTokenizer(flags, ",");

				while (token.hasMoreTokens()) {

					// Get the current debug flag token

					String dbg = token.nextToken().trim();

					// Find the debug flag name

					int idx = 0;

					while (idx < m_sessDbgStr.length && m_sessDbgStr[idx].equalsIgnoreCase(dbg) == false)
						idx++;

					if ( idx >= m_sessDbgStr.length)
						throw new InvalidConfigurationException("Invalid session debug flag, " + dbg);

					// Set the debug flag

					sessDbg += 1 << idx;
				}
			}

			// Set the session debug flags

			cifsConfig.setSessionDebugFlags(sessDbg);
		}
		
		// Check if NIO based code should be disabled
		
		if ( findChildNode( "disableNIO", smb.getChildNodes()) != null)
			cifsConfig.setDisableNIOCode( true);
		
		// Check if an authenticator has been specified

		Element authElem = findChildNode("authenticator", smb.getChildNodes());
		if ( authElem != null) {

			// Get the authenticator class and security mode

			Element classElem = findChildNode("class", authElem.getChildNodes());
			String authClass = null;

			if ( classElem == null) {

				// Check if the authenticator type has been specified

				String authType = authElem.getAttribute("type");

				if ( authType == null)
					throw new InvalidConfigurationException("Authenticator class not specified");

				// Check the authenticator type and set the appropriate authenticator class

				if ( authType.equalsIgnoreCase("local"))
					authClass = "org.alfresco.jlan.server.auth.LocalAuthenticator";
				else if ( authType.equalsIgnoreCase("passthru"))
					authClass = "org.alfresco.jlan.server.auth.passthru.PassthruAuthenticator";
				else if ( authType.equalsIgnoreCase("enterprise"))
					authClass = "org.alfresco.jlan.server.auth.EnterpriseCifsAuthenticator";
			}
			else {

				// Set the authenticator class

				authClass = getText(classElem);
			}

			Element modeElem = findChildNode("mode", authElem.getChildNodes());
			int accessMode = CifsAuthenticator.USER_MODE;

			if ( modeElem != null) {

				// Validate the authenticator mode

				String mode = getText(modeElem);
				if ( mode.equalsIgnoreCase("user"))
					accessMode = CifsAuthenticator.USER_MODE;
				else if ( mode.equalsIgnoreCase("share"))
					accessMode = CifsAuthenticator.SHARE_MODE;
				else
					throw new InvalidConfigurationException("Invalid authentication mode, must be USER or SHARE");
			}

			// Get the allow guest setting

			Element allowGuest = findChildNode("allowGuest", authElem.getChildNodes());

			// Get the parameters for the authenticator class

			ConfigElement params = buildConfigElement(authElem);
			cifsConfig.setAuthenticator(authClass, params, accessMode, allowGuest != null ? true : false);
		}
	}

	/**
	 * Process the host XML element
	 * 
	 * @param host Element 2param cifsConfig CIFSConfigSection
	 * @exception InvalidConfigurationException
	 */
	protected final void procHostElement(Element host, CIFSConfigSection cifsConfig)
		throws InvalidConfigurationException {

		// Check if the host element is valid

		if ( host == null)
			throw new InvalidConfigurationException("Host section must be specified");

		// Get the host name attribute

		String attr = host.getAttribute("name");
		if ( attr == null || attr.length() == 0)
			throw new InvalidConfigurationException("Host name not specified or invalid");
		cifsConfig.setServerName(attr.toUpperCase());

		// If the global server name has not been set then use the CIFS server name

		if ( getServerName() == null)
			setServerName(cifsConfig.getServerName());

		// Get the domain name

		attr = host.getAttribute("domain");
		if ( attr != null && attr.length() > 0)
			cifsConfig.setDomainName(attr.toUpperCase());

		// Get the enabled SMB dialects

		Element elem = findChildNode("smbdialects", host.getChildNodes());
		if ( elem != null) {

			// Clear all configured SMB dialects

			DialectSelector diaSel = cifsConfig.getEnabledDialects();
			diaSel.ClearAll();

			// Parse the SMB dilaects list

			StringTokenizer token = new StringTokenizer(getText(elem), ",");

			while (token.hasMoreTokens()) {

				// Get the current SMB dialect token

				String dia = token.nextToken().trim();

				// Determine the dialect to be enabled

				if ( dia.equalsIgnoreCase("CORE")) {

					// Enable core dialects

					diaSel.AddDialect(Dialect.Core);
					diaSel.AddDialect(Dialect.CorePlus);
				}
				else if ( dia.equalsIgnoreCase("LANMAN")) {

					// Enable the LanMAn dialects

					diaSel.AddDialect(Dialect.DOSLanMan1);
					diaSel.AddDialect(Dialect.DOSLanMan2);
					diaSel.AddDialect(Dialect.LanMan1);
					diaSel.AddDialect(Dialect.LanMan2);
					diaSel.AddDialect(Dialect.LanMan2_1);
				}
				else if ( dia.equalsIgnoreCase("NT")) {

					// Enable the NT dialect

					diaSel.AddDialect(Dialect.NT);
				}
				else
					throw new InvalidConfigurationException("Invalid SMB dialect, " + dia);
			}

			// Set the enabled server SMB dialects

			cifsConfig.setEnabledDialects(diaSel);
		}

		// Check for a server comment

		elem = findChildNode("comment", host.getChildNodes());
		if ( elem != null)
			cifsConfig.setComment(getText(elem));

		// Check for a bind address

		elem = findChildNode("bindto", host.getChildNodes());
		if ( elem != null) {

			// Check if the network adapter name has been specified

			if ( elem.hasAttribute("adapter")) {

				// Get the IP address for the adapter

				InetAddress bindAddr = parseAdapterName(elem.getAttribute("adapter"));

				// Set the bind address for the server

				cifsConfig.setSMBBindAddress(bindAddr);
			}
			else {

				// Validate the bind address

				String bindText = getText(elem);

				try {

					// Check the bind address

					InetAddress bindAddr = InetAddress.getByName(bindText);

					// Set the bind address for the server

					cifsConfig.setSMBBindAddress(bindAddr);
				}
				catch (UnknownHostException ex) {
					throw new InvalidConfigurationException(ex.toString());
				}
			}
		}

		// Check if the host announcer should be enabled

		elem = findChildNode("hostAnnounce", host.getChildNodes());
		if ( elem != null) {

			// Check for an announcement interval

			attr = elem.getAttribute("interval");
			if ( attr != null && attr.length() > 0) {
				try {
					cifsConfig.setHostAnnounceInterval(Integer.parseInt(attr));
				}
				catch (NumberFormatException ex) {
					throw new InvalidConfigurationException("Invalid host announcement interval");
				}
			}

			// Check if the domain name has been set, this is required if the host announcer is
			// enabled

			if ( cifsConfig.getDomainName() == null)
				throw new InvalidConfigurationException("Domain name must be specified if host announcement is enabled");

			// Enable host announcement

			cifsConfig.setHostAnnouncer(true);
		}

		// Check for a host announcer port

		elem = findChildNode("HostAnnouncerPort", host.getChildNodes());
		if ( elem != null) {
			try {
				cifsConfig.setHostAnnouncerPort(Integer.parseInt(getText(elem)));
				if ( cifsConfig.getHostAnnouncerPort() <= 0 || cifsConfig.getHostAnnouncerPort() >= 65535)
					throw new InvalidConfigurationException("Host announcer port out of valid range");
			}
			catch (NumberFormatException ex) {
				throw new InvalidConfigurationException("Invalid host announcer port");
			}
		}

		// Check if NetBIOS SMB is enabled

		elem = findChildNode("netBIOSSMB", host.getChildNodes());
		if ( elem != null) {

			// Check if NetBIOS over TCP/IP is enabled for the current platform

			boolean platformOK = false;

			if ( elem.hasAttribute("platforms")) {
				
				// Get the list of platforms

				String platformsStr = elem.getAttribute("platforms");

				// Parse the list of platforms that NetBIOS over TCP/IP is to be enabled for and
				// check if the current platform is included

				List<Platform.Type> enabledPlatforms = parsePlatformString(platformsStr);
				if ( enabledPlatforms.contains(getPlatformType()))
					platformOK = true;
			}
			else {
				// No restriction on platforms

				platformOK = true;
			}

			// Enable the NetBIOS SMB support

			cifsConfig.setNetBIOSSMB(platformOK);

			// Only parse the other settings if NetBIOS based SMB is enabled for the current
			// platform

			if ( platformOK) {

				// Check for the session port

				attr = elem.getAttribute("sessionPort");
				if ( attr != null && attr.length() > 0) {
					try {
						cifsConfig.setSessionPort(Integer.parseInt(attr));
						if ( cifsConfig.getSessionPort() <= 0 || cifsConfig.getSessionPort() >= 65535)
							throw new InvalidConfigurationException("NetBIOS SMB session port out of valid range");
					}
					catch (NumberFormatException ex) {
						throw new InvalidConfigurationException("Invalid NetBIOS SMB session port");
					}
				}

				// Check for the datagram port

				attr = elem.getAttribute("datagramPort");
				if ( attr != null && attr.length() > 0) {
					try {
						cifsConfig.setNameServerPort(Integer.parseInt(attr));
						if ( cifsConfig.getNameServerPort() <= 0 || cifsConfig.getNameServerPort() >= 65535)
							throw new InvalidConfigurationException("NetBIOS SMB datagram port out of valid range");
					}
					catch (NumberFormatException ex) {
						throw new InvalidConfigurationException("Invalid NetBIOS SMB datagram port");
					}
				}

				// Check for the name server port

				attr = elem.getAttribute("namingPort");
				if ( attr != null && attr.length() > 0) {
					try {
						cifsConfig.setNameServerPort(Integer.parseInt(attr));
						if ( cifsConfig.getNameServerPort() <= 0 || cifsConfig.getNameServerPort() >= 65535)
							throw new InvalidConfigurationException("NetBIOS SMB naming port out of valid range");
					}
					catch (NumberFormatException ex) {
						throw new InvalidConfigurationException("Invalid NetBIOS SMB naming port");
					}
				}

				// Check for a bind address

				attr = elem.getAttribute("bindto");
				if ( attr != null && attr.length() > 0) {

					// Validate the bind address

					try {

						// Check the bind address

						InetAddress bindAddr = InetAddress.getByName(attr);

						// Set the bind address for the NetBIOS name server

						cifsConfig.setNetBIOSBindAddress(bindAddr);
					}
					catch (UnknownHostException ex) {
						throw new InvalidConfigurationException(ex.toString());
					}
				}

				// Check for a bind address using the adapter name

				else if ( elem.hasAttribute("adapter")) {

					// Get the bind address via the network adapter name

					InetAddress bindAddr = parseAdapterName(elem.getAttribute("adapter"));
					cifsConfig.setNetBIOSBindAddress(bindAddr);
				}
				else if ( cifsConfig.hasSMBBindAddress()) {

					// Use the SMB bind address for the NetBIOS name server

					cifsConfig.setNetBIOSBindAddress(cifsConfig.getSMBBindAddress());
				}
			}
		}
		else {

			// Disable NetBIOS SMB support

			cifsConfig.setNetBIOSSMB(false);
		}

		// Check if TCP/IP SMB is enabled

		elem = findChildNode("tcpipSMB", host.getChildNodes());
		if ( elem != null) {

			// Check if native SMB is enabled for the current platform

			boolean platformOK = false;

			if ( elem.hasAttribute("platforms")) {

				// Get the list of platforms

				String platformsStr = elem.getAttribute("platforms");

				// Parse the list of platforms that NetBIOS over TCP/IP is to be enabled for and
				// check if the current platform is included

				List<Platform.Type> enabledPlatforms = parsePlatformString(platformsStr);
				if ( enabledPlatforms.contains(getPlatformType()))
					platformOK = true;
			}
			else {

				// No restriction on platforms

				platformOK = true;
			}

			// Enable the TCP/IP SMB support

			cifsConfig.setTcpipSMB(platformOK);

			// Check if the port has been specified

			attr = elem.getAttribute("port");
			if ( attr != null && attr.length() > 0) {
				try {
					cifsConfig.setTcpipSMBPort(Integer.parseInt(attr));
					if ( cifsConfig.getTcpipSMBPort() <= 0 || cifsConfig.getTcpipSMBPort() >= 65535)
						throw new InvalidConfigurationException("TCP/IP SMB port out of valid range");
				}
				catch (NumberFormatException ex) {
					throw new InvalidConfigurationException("Invalid TCP/IP SMB port");
				}
			}
		}
		else {

			// Disable TCP/IP SMB support

			cifsConfig.setTcpipSMB(false);
		}

		// Check that the broadcast mask has been set if TCP/IP NetBIOS and/or the host announcer is
		// enabled

		if ( cifsConfig.hasNetBIOSSMB() || cifsConfig.hasEnableAnnouncer()) {

			// Parse the broadcast mask

			elem = findChildNode("broadcast", host.getChildNodes());
			if ( elem != null) {

				// Check if the broadcast mask is a valid numeric IP address

				if ( IPAddress.isNumericAddress(getText(elem)) == false)
					throw new InvalidConfigurationException("Invalid broadcast mask, must be n.n.n.n format");

				// Set the network broadcast mask

				cifsConfig.setBroadcastMask(getText(elem));
			}
			else {

				// Broadcast mask not configured

				throw new InvalidConfigurationException("Network broadcast mask not specified");
			}
		}

		// Check if Win32 NetBIOS is enabled

		elem = findChildNode("Win32NetBIOS", host.getChildNodes());
		if ( elem != null) {

			// Check if the Win32 NetBIOS server name has been specified

			attr = elem.getAttribute("name");
			if ( attr != null && attr.length() > 0) {

				// Validate the name

				if ( attr.length() > 16)
					throw new InvalidConfigurationException("Invalid Win32 NetBIOS name, " + attr);

				// Set the Win32 NetBIOS file server name

				cifsConfig.setWin32NetBIOSName(attr);
			}

			// Check if the Win32 NetBIOS client accept name has been specified

			attr = elem.getAttribute("accept");
			if ( attr != null && attr.length() > 0) {

				// Validate the client accept name

				if ( attr.length() > 15)
					throw new InvalidConfigurationException("Invalid Win32 NetBIOS accept name, " + attr);

				// Set the client accept string

				cifsConfig.setWin32NetBIOSClientAccept(attr);
			}

			// Check if the Win32 NetBIOS LANA has been specified

			attr = elem.getAttribute("lana");
			if ( attr != null && attr.length() > 0) {

				// Check if the LANA has been specified as an IP address or adapter name

				int lana = -1;

				if ( IPAddress.isNumericAddress(attr)) {

					// Convert the IP address to a LANA id

					lana = Win32NetBIOS.getLANAForIPAddress(attr);
					if ( lana == -1)
						throw new InvalidConfigurationException("Failed to convert IP address " + attr + " to a LANA");
				}
				else if ( attr.length() > 1 && Character.isLetter(attr.charAt(0))) {

					// Convert the network adapter to a LANA id

					lana = Win32NetBIOS.getLANAForAdapterName(attr);
					if ( lana == -1)
						throw new InvalidConfigurationException("Failed to convert network adapter " + attr + " to a LANA");
				}
				else {

					// Validate the LANA number

					try {
						lana = Integer.parseInt(attr);
					}
					catch (NumberFormatException ex) {
						throw new InvalidConfigurationException("Invalid Win32 NetBIOS LANA specified");
					}

					// LANA should be in the range 0-255

					if ( lana < 0 || lana > 255)
						throw new InvalidConfigurationException("Invalid Win32 NetBIOS LANA number, " + lana);
				}

				// Set the LANA number

				cifsConfig.setWin32LANA(lana);
			}

			// Check if the native NetBIOS interface has been specified, either 'winsock' or
			// 'netbios'

			attr = elem.getAttribute("api");

			if ( attr != null && attr.length() > 0) {
				// Validate the API type

				boolean useWinsock = true;

				if ( attr.equalsIgnoreCase("netbios"))
					useWinsock = false;
				else if ( attr.equalsIgnoreCase("winsock") == false)
					throw new InvalidConfigurationException("Invalid NetBIOS API type, spefify 'winsock' or 'netbios'");

				// Set the NetBIOS API to use

				cifsConfig.setWin32WinsockNetBIOS(useWinsock);
			}

			// Force the older NetBIOS API code to be used on 64Bit Windows as Winsock NetBIOS is
			// not available

			if ( cifsConfig.useWinsockNetBIOS() == true && X64.isWindows64()) {

				// Log a warning

				Debug.println("Using older Netbios() API code, Winsock NetBIOS not available on x64");

				// Use the older NetBIOS API code

				cifsConfig.setWin32WinsockNetBIOS(false);
			}

			// Check if the current operating system is supported by the Win32 NetBIOS handler

			String osName = System.getProperty("os.name");
			if ( osName.startsWith("Windows")
					&& (osName.endsWith("95") == false && osName.endsWith("98") == false && osName.endsWith("ME") == false)) {

				// Enable Win32 NetBIOS

				cifsConfig.setWin32NetBIOS(true);
			}
			else {

				// Win32 NetBIOS not supported on the current operating system

				cifsConfig.setWin32NetBIOS(false);
			}
		}
		else {

			// Disable Win32 NetBIOS

			cifsConfig.setWin32NetBIOS(false);
		}

		// Check if the host announcer should be enabled

		elem = findChildNode("Win32Announce", host.getChildNodes());
		if ( elem != null) {

			// Check for an announcement interval

			attr = elem.getAttribute("interval");
			if ( attr != null && attr.length() > 0) {
				try {
					cifsConfig.setWin32HostAnnounceInterval(Integer.parseInt(attr));
				}
				catch (NumberFormatException ex) {
					throw new InvalidConfigurationException("Invalid host announcement interval");
				}
			}

			// Check if the domain name has been set, this is required if the host announcer is
			// enabled

			if ( cifsConfig.getDomainName() == null)
				throw new InvalidConfigurationException("Domain name must be specified if host announcement is enabled");

			// Enable Win32 NetBIOS host announcement

			cifsConfig.setWin32HostAnnouncer(true);
		}

		// Check if NetBIOS and/or TCP/IP SMB have been enabled

		if ( cifsConfig.hasNetBIOSSMB() == false && cifsConfig.hasTcpipSMB() == false && cifsConfig.hasWin32NetBIOS() == false)
			throw new InvalidConfigurationException("NetBIOS SMB, TCP/IP SMB or Win32 NetBIOS must be enabled");

		// Check if server alias name(s) have been specified

		elem = findChildNode("alias", host.getChildNodes());
		if ( elem != null) {

			// Get the alias name list

			attr = elem.getAttribute("names");
			if ( attr == null || attr.length() == 0)
				throw new InvalidConfigurationException("Alias name(s) not specified");

			// Split the alias name list

			StringList names = new StringList();
			StringTokenizer nameTokens = new StringTokenizer(attr, ",");

			while (nameTokens.hasMoreTokens()) {

				// Get the current alias name

				String alias = nameTokens.nextToken().trim().toUpperCase();

				// Check if the name already exists in the alias list, or matches the main server
				// name

				if ( alias.equalsIgnoreCase(getServerName()))
					throw new InvalidConfigurationException("Alias is the same as the main server name");
				else if ( names.containsString(alias))
					throw new InvalidConfigurationException("Same alias specified twice, " + alias);
				else
					names.addString(alias);
			}

			// Set the server alias names

			cifsConfig.addAliasNames(names);
		}

		// Check if Macintosh extension SMBs should be enabled

		elem = findChildNode("macExtensions", host.getChildNodes());
		if ( elem != null) {

			// Enable Macintosh extension SMBs

			cifsConfig.setMacintoshExtensions(true);
		}

		// Check if WINS servers are configured

		elem = findChildNode("WINS", host.getChildNodes());

		if ( elem != null) {

			// Get the primary WINS server

			Element winsSrv = findChildNode("primary", elem.getChildNodes());
			if ( winsSrv == null)
				throw new InvalidConfigurationException("No primary WINS server configured");

			// Validate the WINS server address

			InetAddress primaryWINS = null;

			try {
				primaryWINS = InetAddress.getByName(getText(winsSrv));
			}
			catch (UnknownHostException ex) {
				throw new InvalidConfigurationException("Invalid primary WINS server address, " + winsSrv.getNodeValue());
			}

			// Check if a secondary WINS server has been specified

			winsSrv = findChildNode("secondary", elem.getChildNodes());
			InetAddress secondaryWINS = null;

			if ( winsSrv != null) {

				// Validate the secondary WINS server address

				try {
					secondaryWINS = InetAddress.getByName(getText(winsSrv));
				}
				catch (UnknownHostException ex) {
					throw new InvalidConfigurationException("Invalid secondary WINS server address, " + winsSrv.getNodeValue());
				}
			}

			// Set the WINS server address(es)

			cifsConfig.setPrimaryWINSServer(primaryWINS);
			if ( secondaryWINS != null)
				cifsConfig.setSecondaryWINSServer(secondaryWINS);
		}
		
		// Check if a session timeout is configured
		
		elem = findChildNode("sessionTimeout", host.getChildNodes());
		if ( elem != null) {
			
			// Validate the session timeout value

			String sessTmo = getText( elem);
			if ( sessTmo != null && sessTmo.length() > 0) {
				try {
					
					// Convert the timeout value to milliseconds
					
					int tmo = Integer.parseInt(sessTmo);
					if ( tmo < 0 || tmo > MaxSessionTimeout)
						throw new InvalidConfigurationException("Session timeout out of range (0 - " + MaxSessionTimeout + ")");
					
					// Convert the session timeout to milliseconds
					
					cifsConfig.setSocketTimeout( tmo * 1000);
				}
				catch (NumberFormatException ex) {
					throw new InvalidConfigurationException("Invalid session timeout value, " + sessTmo);
				}
			}
			else
				throw new InvalidConfigurationException("Session timeout value not specified");
		}
	}

	/**
	 * Process the debug XML element
	 * 
	 * @param debug Element
	 * @exception InvalidConfigurationException
	 */
	protected final void procDebugElement(Element debug)
		throws InvalidConfigurationException {

		// Check if the debug section has been specified

		if ( debug == null)
			return;

		// Create the debug configuration section

		DebugConfigSection debugConfig = new DebugConfigSection(this);

		// Get the debug output class and parameters

		Element elem = findChildNode("output", debug.getChildNodes());
		if ( elem == null)
			throw new InvalidConfigurationException("Output class must be specified to enable debug output");

		// Get the debug output class

		Element debugClass = findChildNode("class", elem.getChildNodes());
		if ( debugClass == null)
			throw new InvalidConfigurationException("Class must be specified for debug output");

		// Get the parameters for the debug class

		ConfigElement params = buildConfigElement(elem);
		debugConfig.setDebug(getText(debugClass), params);
	}

	/**
	 * Process the shares XML element
	 * 
	 * @param shares Element
	 * @exception InvalidConfigurationException
	 */
	protected final void procSharesElement(Element shares)
		throws InvalidConfigurationException {

		// Check if the shares element is valid

		if ( shares == null)
			return;

		// Create the filesystems configuration section

		FilesystemsConfigSection filesysConfig = new FilesystemsConfigSection(this);

		// Iterate the child elements

		NodeList children = shares.getChildNodes();

		if ( children != null) {

			// Iterate the child elements and process the disk/print share elements

			for (int i = 0; i < children.getLength(); i++) {

				// Get the current child node

				Node node = children.item(i);

				if ( node.getNodeType() == ELEMENT_TYPE) {

					// Get the next element from the list

					Element child = (Element) node;

					// Check if this is a disk or print share element

					if ( child.getNodeName().equalsIgnoreCase("diskshare"))
						addDiskShare(child, filesysConfig);
				}
			}
		}
	}

	/**
	 * Process the security XML element
	 * 
	 * @param security Element
	 * @exception InvalidConfigurationException
	 */
	protected final void procSecurityElement(Element security)
		throws InvalidConfigurationException {

		// Check if the security element is valid

		if ( security == null)
			return;

		// Create the security configuration section

		SecurityConfigSection secConfig = new SecurityConfigSection(this);

		// Check if an access control manager has been specified

		Element aclElem = findChildNode("accessControlManager", security.getChildNodes());
		if ( aclElem != null) {

			// Get the access control manager class and security mode

			Element classElem = findChildNode("class", aclElem.getChildNodes());
			if ( classElem == null)
				throw new InvalidConfigurationException("Access control manager class not specified");

			// Get the parameters for the access control manager class

			ConfigElement params = buildConfigElement(aclElem);
			secConfig.setAccessControlManager(getText(classElem), params);
		}
		else {

			// Use the default access control manager

			secConfig.setAccessControlManager("org.alfresco.jlan.server.auth.acl.DefaultAccessControlManager",
					new GenericConfigElement("aclManager"));
		}

		// Check if global access controls have been specified

		Element globalACLs = findChildNode("globalAccessControl", security.getChildNodes());
		if ( globalACLs != null) {

			// Parse the access control list

			AccessControlList acls = procAccessControlElement(globalACLs, secConfig);
			if ( acls != null)
				secConfig.setGlobalAccessControls(acls);
		}

		// Check if a JCE provider class has been specified

		Element jceElem = findChildNode("JCEProvider", security.getChildNodes());
		if ( jceElem != null) {

			// Set the JCE provider

			secConfig.setJCEProvider(getText(jceElem));
		}

		// Add the users

		Element usersElem = findChildNode("users", security.getChildNodes());
		if ( usersElem != null) {

			// Get the list of user elements

			NodeList userList = usersElem.getChildNodes();

			for (int i = 0; i < userList.getLength(); i++) {

				// Get the current user node

				Node node = userList.item(i);

				if ( node.getNodeType() == ELEMENT_TYPE) {
					Element userElem = (Element) node;
					addUser(userElem, secConfig);
				}
			}
		}

		// Check if a share mapper has been specified

		Element mapper = findChildNode("shareMapper", security.getChildNodes());

		if ( mapper != null) {

			// Get the share mapper class

			Element classElem = findChildNode("class", mapper.getChildNodes());
			if ( classElem == null)
				throw new InvalidConfigurationException("Share mapper class not specified");

			// Get the parameters for the share mapper class

			ConfigElement params = buildConfigElement(mapper);
			secConfig.setShareMapper(getText(classElem), params);
		}

		// Check if the users interface has been specified

		Element usersIface = findChildNode("usersInterface", security.getChildNodes());

		if ( usersIface != null) {

			// Get the users interface class

			Element classElem = findChildNode("class", usersIface.getChildNodes());
			if ( classElem == null)
				throw new InvalidConfigurationException("Users interface class not specified");

			// Get the parameters for the users interface class

			ConfigElement params = buildConfigElement(usersIface);
			secConfig.setUsersInterface(getText(classElem), params);
		}
	}

	/**
	 * Process the drive mappings XML element
	 * 
	 * @param mappings Element
	 * @exception InvalidConfigurationException
	 */
	protected final void procDriveMappingsElement(Element mappings)
		throws InvalidConfigurationException {

		// Check if the drive mappings element is valid

		if ( mappings == null)
			return;

		// Create the drive mappings configuration section

		DriveMappingsConfigSection mapConfig = new DriveMappingsConfigSection(this);

		// Parse each drive mapping element

		NodeList mapElems = mappings.getChildNodes();
		DriveMappingList mapList = null;

		if ( mapElems != null && mapElems.getLength() > 0) {

			// Create the mapped drive list

			mapList = new DriveMappingList();

			// Access the CIFS server configuration

			CIFSConfigSection cifsConfig = (CIFSConfigSection) getConfigSection(CIFSConfigSection.SectionName);

			// Get a list of the available shares

			SecurityConfigSection secConfig = (SecurityConfigSection) getConfigSection(SecurityConfigSection.SectionName);
			SharedDeviceList shareList = secConfig.getShareMapper().getShareList(getServerName(), null, false);

			// Process each drive mapping element

			for (int i = 0; i < mapElems.getLength(); i++) {

				// Get the current mapped drive details

				Node node = mapElems.item(i);

				if ( node.getNodeType() == ELEMENT_TYPE) {

					// Access the mapped drive element

					Element elem = (Element) node;

					if ( elem.getNodeName().equals("mapDrive")) {

						// Get the mapped drive local drive and remote path details

						String localPath = elem.getAttribute("drive").toUpperCase();
						String shareName = elem.getAttribute("share");

						// Check the local path string

						if ( localPath.length() != 2)
							throw new InvalidConfigurationException("Invalid local drive specified, " + localPath);

						if ( localPath.charAt(1) != ':' || _driveLetters.indexOf(localPath.charAt(0)) == -1)
							throw new InvalidConfigurationException("Invalid local drive specified, " + localPath);

						// Check if the share name is a valid local disk share

						if ( shareName.length() == 0)
							throw new InvalidConfigurationException("Empty share name for mapped drive, " + localPath);

						if ( shareList.findShare(shareName, ShareType.DISK, true) == null)
							throw new InvalidConfigurationException("Mapped drive share " + shareName + " does not exist");

						// Get the username/password to be used to connect the mapped drive

						String userName = null;
						String password = null;

						if ( elem.hasAttribute("username"))
							userName = elem.getAttribute("username");

						if ( elem.hasAttribute("password"))
							password = elem.getAttribute("password");

						// Get the options flags

						boolean interact = false;
						boolean prompt = false;

						if ( elem.hasAttribute("interactive")) {
							if ( elem.getAttribute("interactive").equalsIgnoreCase("YES"))
								interact = true;
						}

						if ( elem.hasAttribute("prompt")) {
							if ( elem.getAttribute("prompt").equalsIgnoreCase("YES"))
								prompt = true;
						}

						// Build the remote path

						StringBuffer remPath = new StringBuffer();
						remPath.append("\\\\");

						if ( cifsConfig.hasWin32NetBIOS() && cifsConfig.getWin32ServerName() != null)
							remPath.append(cifsConfig.getWin32ServerName());
						else
							remPath.append(getServerName());
						remPath.append("\\");
						remPath.append(shareName.toUpperCase());

						// Add a drive mapping

						mapList.addMapping(new DriveMapping(localPath, remPath.toString(), userName, password, interact, prompt));
					}
				}
			}

			// Set the mapped drive list

			mapConfig.setMappedDrives(mapList);
		}
	}

	/**
	 * Process an access control sub-section and return the access control list
	 * 
	 * @param acl Element
	 * @param secConfig SecutiryConfigSection
	 * @throws InvalidConfigurationException
	 */
	protected final AccessControlList procAccessControlElement(Element acl, SecurityConfigSection secConfig)
		throws InvalidConfigurationException {

		// Check if there is an access control manager configured

		if ( secConfig.getAccessControlManager() == null)
			throw new InvalidConfigurationException("No access control manager configured");

		// Create the access control list

		AccessControlList acls = new AccessControlList();

		// Check if there is a default access level for the ACL group

		String attrib = acl.getAttribute("default");

		if ( attrib != null && attrib.length() > 0) {

			// Get the access level and validate

			try {

				// Parse the access level name

				int access = AccessControlParser.parseAccessTypeString(attrib);

				// Set the default access level for the access control list

				acls.setDefaultAccessLevel(access);
			}
			catch (InvalidACLTypeException ex) {
				throw new InvalidConfigurationException("Default access level error, " + ex.toString());
			}
			catch (ACLParseException ex) {
				throw new InvalidConfigurationException("Default access level error, " + ex.toString());
			}
		}

		// Parse each access control element and create the required access control

		NodeList aclElems = acl.getChildNodes();

		if ( aclElems != null && aclElems.getLength() > 0) {

			// Create the access controls

			GenericConfigElement params = null;
			String type = null;

			for (int i = 0; i < aclElems.getLength(); i++) {

				// Get the current ACL details

				Node node = aclElems.item(i);

				if ( node.getNodeType() == ELEMENT_TYPE) {

					// Access the ACL element

					Element elem = (Element) node;
					type = elem.getNodeName();

					// Create a new config element

					params = new GenericConfigElement("acl");

					// Convert the element attributes into a list of name value pairs

					NamedNodeMap attrs = elem.getAttributes();

					if ( attrs == null || attrs.getLength() == 0)
						throw new InvalidConfigurationException("Missing attribute(s) for access control " + type);

					for (int j = 0; j < attrs.getLength(); j++) {

						// Create a name/value pair from the current attribute and add to the
						// parameter list

						Node attr = attrs.item(j);
						GenericConfigElement childElem = new GenericConfigElement(attr.getNodeName());
						childElem.setValue(attr.getNodeValue());

						params.addChild(childElem);
					}

					try {

						// Create the access control and add to the list

						acls.addControl(secConfig.getAccessControlManager().createAccessControl(type, params));
					}
					catch (InvalidACLTypeException ex) {
						throw new InvalidConfigurationException("Invalid access control type - " + type);
					}
					catch (ACLParseException ex) {
						throw new InvalidConfigurationException("Access control parse error (" + type + "), " + ex.toString());
					}
				}
			}
		}

		// Check if there are no access control rules but the default access level is set to 'None',
		// this is not allowed
		// as the share would not be accessible or visible.

		if ( acls.getDefaultAccessLevel() == AccessControl.NoAccess && acls.numberOfControls() == 0)
			throw new InvalidConfigurationException("Empty access control list and default access 'None' not allowed");

		// Return the access control list

		return acls;
	}

	/**
	 * Add a user
	 * 
	 * @param user Element
	 * @param secConfig SecurityConfigSection
	 * @exception InvalidConfigurationException
	 */
	protected final void addUser(Element user, SecurityConfigSection secConfig)
		throws InvalidConfigurationException {

		// Get the username

		String attr = user.getAttribute("name");
		if ( attr == null || attr.length() == 0)
			throw new InvalidConfigurationException("User name not specified, or zero length");

		// Check if the user already exists

		String userName = attr;

		if ( secConfig.hasUserAccounts() && secConfig.getUserAccounts().findUser(userName) != null)
			throw new InvalidConfigurationException("User " + userName + " already defined");

		// Get the MD4 hashed password

		byte[] md4 = null;
		String password = null;

		Element elem = findChildNode("md4", user.getChildNodes());
		if ( elem != null) {

			// Get the MD4 hashed password string

			String md4Str = getText(elem);
			if ( md4Str == null || md4Str.length() != 32)
				throw new InvalidConfigurationException("Invalid MD4 hashed password for user " + userName);

			// Decode the MD4 string

			md4 = new byte[16];
			for (int i = 0; i < 16; i++) {

				// Get a hex pair and convert

				String hexPair = md4Str.substring(i * 2, (i * 2) + 2);
				md4[i] = (byte) Integer.parseInt(hexPair, 16);
			}
		}
		else {

			// Get the password for the account

			elem = findChildNode("password", user.getChildNodes());
			if ( elem == null)
				throw new InvalidConfigurationException("No password specified for user " + userName);

			// Get the plaintext password

			password = getText(elem);
		}

		// Create the user account

		UserAccount userAcc = new UserAccount(userName, password);
		userAcc.setMD4Password(md4);

		// Check if the user in an administrator

		elem = findChildNode("administrator", user.getChildNodes());
		if ( elem != null)
			userAcc.setAdministrator(true);

		// Get the real user name and comment

		elem = findChildNode("realname", user.getChildNodes());
		if ( elem != null)
			userAcc.setRealName(getText(elem));

		elem = findChildNode("comment", user.getChildNodes());
		if ( elem != null)
			userAcc.setComment(getText(elem));

		// Get the home directory

		elem = findChildNode("home", user.getChildNodes());
		if ( elem != null)
			userAcc.setHomeDirectory(getText(elem));

		// Add the user account

		UserAccountList accList = secConfig.getUserAccounts();
		if ( accList == null)
			secConfig.setUserAccounts(new UserAccountList());
		secConfig.getUserAccounts().addUser(userAcc);
	}

	/**
	 * Add a disk share
	 * 
	 * @param disk Element 2param filesysConfig FilesystemConfigSection
	 * @exception InvalidConfigurationException
	 */
	protected final void addDiskShare(Element disk, FilesystemsConfigSection filesysConfig)
		throws InvalidConfigurationException {

		// Get the share name and comment attributes

		String attr = disk.getAttribute("name");
		if ( attr == null || attr.length() == 0)
			throw new InvalidConfigurationException("Disk share name must be specified");

		String name = attr;
		String comment = null;

		attr = disk.getAttribute("comment");
		if ( attr != null && attr.length() > 0)
			comment = attr;

		// Get the disk driver details

		Element driverElem = findChildNode("driver", disk.getChildNodes());
		if ( driverElem == null)
			throw new InvalidConfigurationException("No driver specified for disk share " + name);

		Element classElem = findChildNode("class", driverElem.getChildNodes());
		if ( classElem == null || getText(classElem).length() == 0)
			throw new InvalidConfigurationException("No driver class specified for disk share " + name);

		// Get the security configuration section

		SecurityConfigSection secConfig = (SecurityConfigSection) getConfigSection(SecurityConfigSection.SectionName);

		// Check if an access control list has been specified

		AccessControlList acls = null;
		Element aclElem = findChildNode("accessControl", disk.getChildNodes());

		if ( aclElem != null) {

			// Parse the access control list

			acls = procAccessControlElement(aclElem, secConfig);
		}
		else {

			// Use the global access control list for this disk share

			acls = secConfig.getGlobalAccessControls();
		}

		// Get the parameters for the driver

		ConfigElement params = buildConfigElement(driverElem);

		// Check if change notification should be disabled for this device

		boolean changeNotify = findChildNode("disableChangeNotification", disk.getChildNodes()) != null ? false : true;

		// Check if the volume information has been specified

		Element volElem = findChildNode("volume", disk.getChildNodes());
		VolumeInfo volInfo = null;

		if ( volElem != null) {

			// Create the volume information

			volInfo = new VolumeInfo("");

			// Get the volume label

			attr = volElem.getAttribute("label");
			if ( attr != null && attr.length() > 0)
				volInfo.setVolumeLabel(attr);

			// Get the serial number

			attr = volElem.getAttribute("serial");
			if ( attr != null && attr.length() > 0) {
				try {
					volInfo.setSerialNumber(Integer.parseInt(attr));
				}
				catch (NumberFormatException ex) {
					throw new InvalidConfigurationException("Volume serial number invalid, " + attr);
				}
			}

			// Get the creation date/time

			attr = volElem.getAttribute("created");
			if ( attr != null && attr.length() > 0) {
				try {
					volInfo.setCreationDateTime(m_dateFmt.parse(attr));
				}
				catch (ParseException ex) {
					throw new InvalidConfigurationException("Volume creation date/time invalid, " + attr);
				}
			}
		}
		else {

			// Create volume information using the share name

			volInfo = new VolumeInfo(name, (int) System.currentTimeMillis(), new Date(System.currentTimeMillis()));
		}

		// Check if the disk sizing information has been specified

		SrvDiskInfo diskInfo = null;
		Element sizeElem = findChildNode("size", disk.getChildNodes());

		if ( sizeElem != null) {

			// Get the total disk size in bytes

			long totSize = -1L;
			long freeSize = 0;

			attr = sizeElem.getAttribute("totalSize");
			if ( attr != null && attr.length() > 0)
				totSize = MemorySize.getByteValue(attr);

			if ( totSize == -1L)
				throw new InvalidConfigurationException("Total disk size invalid or not specified");

			// Get the free size in bytes

			attr = sizeElem.getAttribute("freeSize");
			if ( attr != null && attr.length() > 0)
				freeSize = MemorySize.getByteValue(attr);
			else
				freeSize = (totSize / 10L) * 9L;

			if ( freeSize == -1L)
				throw new InvalidConfigurationException("Free disk size invalid or not specified");

			// Get the block size and blocks per unit values, if specified

			long blockSize = 512L;
			long blocksPerUnit = 64L; // 32Kb units

			attr = sizeElem.getAttribute("blockSize");
			if ( attr != null && attr.length() > 0) {
				try {
					blockSize = Long.parseLong(attr);

					// Check for a multiple of 512 bytes

					if ( blockSize <= 0 || blockSize % 512 != 0)
						throw new InvalidConfigurationException("Block size must be a multiple of 512");
				}
				catch (NumberFormatException ex) {
					throw new InvalidConfigurationException("Invalid block size specified, " + attr);
				}
			}

			attr = sizeElem.getAttribute("blocksPerUnit");
			if ( attr != null && attr.length() > 0) {
				try {
					blocksPerUnit = Long.parseLong(attr);

					// Check for a valid blocks per unit value

					if ( blocksPerUnit <= 0)
						throw new InvalidConfigurationException("Invalid blocks per unit, must be greater than zero");
				}
				catch (NumberFormatException ex) {
					throw new InvalidConfigurationException("Invalid blocks per unit value");
				}
			}

			// Calculate the sizes and set the disk sizing information

			long unitSize = blockSize * blocksPerUnit;
			long totUnits = totSize / unitSize;
			long freeUnits = freeSize / unitSize;

			diskInfo = new SrvDiskInfo(totUnits, blocksPerUnit, blockSize, freeUnits);
		}
		else {

			// Default to a 80Gb sized disk with 90% free space

			diskInfo = new SrvDiskInfo(2560000, 64, 512, 2304000);
		}

		// Check if a share with this name already exists

		if ( filesysConfig.getShares().findShare(name) != null)
			throw new InvalidConfigurationException("Share " + name + " already exists");

		// Validate the driver class, create a device context and add the new disk share

		try {

			// Load the driver class

			Object drvObj = Class.forName(getText(classElem)).newInstance();
			if ( drvObj instanceof DiskInterface) {

				// Create the driver

				DiskInterface diskDrv = (DiskInterface) drvObj;

				// Create a context for this share instance, save the configuration parameters as
				// part of the context

				DiskDeviceContext devCtx = (DiskDeviceContext) diskDrv.createContext(name, params);
				devCtx.setConfigurationParameters(params);

				// Enable/disable change notification for this device

				devCtx.enableChangeHandler(changeNotify);

				// Set the volume information, may be null

				devCtx.setVolumeInformation(volInfo);

				// Set the disk sizing information, may be null

				devCtx.setDiskInformation(diskInfo);

				// Set the share name in the context

				devCtx.setShareName(name);

				// Create the disk shared device and add to the server's list of shares

				DiskSharedDevice diskDev = new DiskSharedDevice(name, diskDrv, devCtx);
				diskDev.setComment(comment);

				// Add any access controls to the share

				diskDev.setAccessControlList(acls);

				// Start the filesystem

				devCtx.startFilesystem(diskDev);

				// Add the new share to the list of available shares

				filesysConfig.addShare(diskDev);
			}
		}
		catch (ClassNotFoundException ex) {
			throw new InvalidConfigurationException("Disk driver class " + getText(classElem) + " not found");
		}
		catch (DeviceContextException ex) {
			throw new InvalidConfigurationException("Driver context error, " + ex.toString());
		}
		catch (Exception ex) {
			throw new InvalidConfigurationException("Disk share setup error, " + ex.toString());
		}
	}

	/**
	 * Find the specified child node in the node list
	 * 
	 * @param name String
	 * @param list NodeList
	 * @return Element
	 */
	protected final Element findChildNode(String name, NodeList list) {

		// Check if the list is valid

		if ( list == null)
			return null;

		// Search for the required element

		for (int i = 0; i < list.getLength(); i++) {

			// Get the current child node

			Node child = list.item(i);
			if ( child.getNodeName().equals(name) && child.getNodeType() == ELEMENT_TYPE)
				return (Element) child;
		}

		// Element not found

		return null;
	}

	/**
	 * Get the value text for the specified element
	 * 
	 * @param elem Element
	 * @return String
	 */
	protected final String getText(Element elem) {

		// Check if the element has children

		NodeList children = elem.getChildNodes();
		String text = "";

		if ( children != null && children.getLength() > 0 && children.item(0).getNodeType() != ELEMENT_TYPE)
			text = children.item(0).getNodeValue();

		// Return the element text value

		return text;
	}

	/**
	 * Build a configuration element list from an elements child nodes
	 * 
	 * @param root Element
	 * @return GenericConfigElement
	 */
	protected final GenericConfigElement buildConfigElement(Element root) {
		return buildConfigElement(root, null);
	}

	/**
	 * Build a configuration element list from an elements child nodes
	 * 
	 * @param root Element
	 * @param cfgElem GenericConfigElement
	 * @return GenericConfigElement
	 */
	protected final GenericConfigElement buildConfigElement(Element root, GenericConfigElement cfgElem) {

		// Create the top level element, if not specified

		GenericConfigElement rootElem = cfgElem;

		if ( rootElem == null) {

			// Create the root element

			rootElem = new GenericConfigElement(root.getNodeName());

			// Add any attributes

			NamedNodeMap attribs = root.getAttributes();
			if ( attribs != null) {
				for (int i = 0; i < attribs.getLength(); i++) {
					Node attribNode = attribs.item(i);
					rootElem.addAttribute(attribNode.getNodeName(), attribNode.getNodeValue());
				}
			}
		}

		// Get the child node list

		NodeList nodes = root.getChildNodes();
		if ( nodes == null)
			return rootElem;

		// Process the child node list

		GenericConfigElement childElem = null;

		for (int i = 0; i < nodes.getLength(); i++) {

			// Get the current node

			Node node = nodes.item(i);

			if ( node.getNodeType() == ELEMENT_TYPE) {

				// Access the Element

				Element elem = (Element) node;

				// Check if the element has any child nodes

				NodeList children = elem.getChildNodes();

				if ( children != null && children.getLength() > 1) {

					// Add the child nodes as child configuration elements

					childElem = buildConfigElement(elem, null);
				}
				else {

					// Create a normal name/value

					if ( children.getLength() > 0) {
						childElem = new GenericConfigElement(elem.getNodeName());
						childElem.setValue(children.item(0).getNodeValue());
					}
					else
						childElem = new GenericConfigElement(elem.getNodeName());

					// Add any attributes

					NamedNodeMap attribs = elem.getAttributes();
					if ( attribs != null) {
						for (int j = 0; j < attribs.getLength(); j++) {
							Node attribNode = attribs.item(j);
							childElem.addAttribute(attribNode.getNodeName(), attribNode.getNodeValue());
						}
					}
				}

				// Add the child configuration element

				rootElem.addChild(childElem);
			}
		}

		// Return the configuration element

		return rootElem;
	}

	/**
	 * Add a configuration element
	 */
	/**
	 * Parse a platform type string into a list of platform ids
	 * 
	 * @param platforms String
	 * @return List<Integer>
	 * @exception InvalidConfigurationException
	 */
	protected final List<Platform.Type> parsePlatformString(String platforms)
		throws InvalidConfigurationException {
		// Create the list to hold the platform ids

		List<Platform.Type> platformIds = new ArrayList<Platform.Type>();

		if ( platforms == null)
			return platformIds;

		// Split the platform list

		StringTokenizer tokens = new StringTokenizer(platforms.toUpperCase(Locale.ENGLISH), ",");

		while (tokens.hasMoreTokens()) {

			// Get the current platform token and validate

			String platform = tokens.nextToken().trim();

			// Validate the platform id

			Platform.Type id = Platform.Type.Unknown;

			if ( platform.equalsIgnoreCase("WINDOWS"))
				id = Platform.Type.WINDOWS;
			else if ( platform.equalsIgnoreCase("LINUX"))
				id = Platform.Type.LINUX;
			else if ( platform.equalsIgnoreCase("MACOSX"))
				id = Platform.Type.MACOSX;
			else if ( platform.equalsIgnoreCase("SOLARIS"))
				id = Platform.Type.SOLARIS;

			if ( id == Platform.Type.Unknown)
				throw new InvalidConfigurationException("Invalid platform type '" + platform + "'");

			// Add the platform id to the list

			platformIds.add(id);
		}

		// Return the platform id list

		return platformIds;
	}

	/**
	 * Parse an adapter name string and return the matching address
	 * 
	 * @param adapter String
	 * @return InetAddress
	 * @exception InvalidConfigurationException
	 */
	protected final InetAddress parseAdapterName(String adapter)
		throws InvalidConfigurationException {

		NetworkInterface ni = null;

		try {
			ni = NetworkInterface.getByName(adapter);
		}
		catch (SocketException ex) {
			throw new InvalidConfigurationException("Invalid adapter name, " + adapter);
		}

		if ( ni == null)
			throw new InvalidConfigurationException("Invalid network adapter name, " + adapter);

		// Get the IP address for the adapter

		InetAddress adapAddr = null;
		Enumeration<InetAddress> addrEnum = ni.getInetAddresses();

		while (addrEnum.hasMoreElements() && adapAddr == null) {

			// Get the current address

			InetAddress addr = addrEnum.nextElement();
			if ( IPAddress.isNumericAddress(addr.getHostAddress()))
				adapAddr = addr;
		}

		// Check if we found the IP address to bind to

		if ( adapAddr == null)
			throw new InvalidConfigurationException("Adapter " + adapter + " does not have a valid IP address");

		// Return the adapter address

		return adapAddr;
	}
}
