package net.openvpn.als.sample;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.openvpn.als.core.CoreEvent;
import net.openvpn.als.core.CoreListener;
import net.openvpn.als.core.CoreMenuTree;
import net.openvpn.als.core.CoreServlet;
import net.openvpn.als.core.MenuItem;
import net.openvpn.als.core.PageTaskMenuTree;
import net.openvpn.als.navigation.MenuTree;
import net.openvpn.als.navigation.NavigationManager;
import net.openvpn.als.plugin.Plugin;
import net.openvpn.als.plugin.PluginDefinition;
import net.openvpn.als.plugin.PluginException;
import net.openvpn.als.policyframework.Permission;
import net.openvpn.als.policyframework.PolicyConstants;
import net.openvpn.als.security.SessionInfo;
import net.openvpn.als.security.UserAttributeDefinition;

/**
 * Sample implementation of a {@link Plugin} that amongst other things provides
 * a new {@link net.openvpn.als.sample.Sample} resource that can be managed in
 * OpenVPN-ALS.
 * <p>
 * The resources are held in memory for the life of the server.
 * 
 * @author James D Robinson <a href="mailto:james@localhost">&lt;james@localhost&gt;</a>
 */
public class SamplePlugin implements Plugin {

    // Event code constants
    static SampleDatabase database;

    /**
     * New sample record created
     */
    public final static int EVT_SAMPLE_CREATED = 50000;
    public final static int EVT_SAMPLE_UPDATED = 50001;
    public final static int EVT_SAMPLE_DELETED = 50002;

    final static Log log = LogFactory.getLog(SamplePlugin.class);

    /**
     * Constructor
     */
    public SamplePlugin() {
        super();
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.openvpn.als.plugin.Plugin#startPlugin()
     */
    public void startPlugin() throws PluginException {
        log.info("Starting Sample Plugin");

        // Register the resource type and add the permissions
        try {
            CoreServlet.getServlet().getPolicyDatabase().registerResourceType(Sample.SAMPLE_RESOURCE_TYPE);
            Sample.SAMPLE_RESOURCE_TYPE.addPermission(PolicyConstants.PERM_CREATE_EDIT_AND_ASSIGN);
            Sample.SAMPLE_RESOURCE_TYPE.addPermission(PolicyConstants.PERM_EDIT_AND_ASSIGN);
            Sample.SAMPLE_RESOURCE_TYPE.addPermission(PolicyConstants.PERM_DELETE);
        } catch (Exception e) {
            throw new PluginException("Failed to register resource type.", e);
        }

        // Add the new items to the main menu
        MenuTree tree = NavigationManager.getMenuTree(CoreMenuTree.MENU_ITEM_MENU_TREE);
        tree.addMenuItem("resources", new MenuItem("userSamples", "sample", "/showUserSamples.do", 50, true, null,
                        SessionInfo.USER_CONSOLE_CONTEXT, null, null, Sample.SAMPLE_RESOURCE_TYPE));
        tree.addMenuItem("globalResources", new MenuItem("managementSamples", "sample", "/showSamples.do", 50, true, null,
                        SessionInfo.MANAGEMENT_CONSOLE_CONTEXT, Sample.SAMPLE_RESOURCE_TYPE, new Permission[] {
                                        PolicyConstants.PERM_CREATE_EDIT_AND_ASSIGN, PolicyConstants.PERM_EDIT_AND_ASSIGN,
                                        PolicyConstants.PERM_DELETE }, Sample.SAMPLE_RESOURCE_TYPE));

        // Add the new page tasks
        MenuTree pageTasks = NavigationManager.getMenuTree(PageTaskMenuTree.PAGE_TASK_MENU_TREE);
        pageTasks.addMenuItem(null, new MenuItem("showSamples", null, null, 100, false, SessionInfo.MANAGEMENT_CONSOLE_CONTEXT));
        pageTasks.addMenuItem("showSamples", new MenuItem("createSample", "sample", "/sampleDefaultDetails.do", 100, true, "_self",
                        SessionInfo.MANAGEMENT_CONSOLE_CONTEXT, Sample.SAMPLE_RESOURCE_TYPE,
                        new Permission[] { PolicyConstants.PERM_CREATE_EDIT_AND_ASSIGN }));

        // This demonstrates listening for events. In this case we will look for
        // 'sample created' events and display a message on the console
        CoreServlet.getServlet().addCoreListener(new CoreListener() {
            public void coreEvent(CoreEvent evt) {
                if (evt.getId() == SamplePlugin.EVT_SAMPLE_CREATED) {
                    System.err.println("****************************************");
                    System.err.println("* Someone just created a sample!!!     *");
                    System.err.println("****************************************");
                }
            }

        });

        /*
         * The following code demonstrates the registering user attribute
         * definitions. See the message resources for how to provide the
         * titles and descriptions for attributes and categories.
         */
        try {
            CoreServlet.getServlet().getUserDatabase().registerUserAttributeDefinition(
                            new UserAttributeDefinition(
                                  UserAttributeDefinition.TYPE_STRING, 
                                  "sample1", "", 20000, "", "",
                                  UserAttributeDefinition.USER_OVERRIDABLE_ATTRIBUTE, 
                                  10, "sample", false, "", "", true, true));
            CoreServlet.getServlet().getUserDatabase().registerUserAttributeDefinition(
                            new UserAttributeDefinition(
                                  UserAttributeDefinition.TYPE_STRING, 
                                  "sample2", "", 20000, "", "",
                                  UserAttributeDefinition.USER_VIEWABLE_ATTRIBUTE, 
                                  20, "sample", false, "Default Sample 2 Value", "", true, true));
            CoreServlet.getServlet().getUserDatabase().registerUserAttributeDefinition(
                            new UserAttributeDefinition(
                                  UserAttributeDefinition.TYPE_STRING, 
                                  "sample3", "", 20001, "", "",
                                  UserAttributeDefinition.USER_OVERRIDABLE_ATTRIBUTE, 
                                  20, "sample", false, "Default Sample 2 Value", "", true, true));
        }
        catch(Exception e) {
            throw new PluginException("Failed to register user attribute definitions.", e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.openvpn.als.plugin.Plugin#canStopPlugin()
     */
    public boolean canStopPlugin() {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.openvpn.als.plugin.Plugin#stopPlugin()
     */
    public void stopPlugin() throws PluginException {
        try {
            CoreServlet.getServlet().getPolicyDatabase().deregisterResourceType(Sample.SAMPLE_RESOURCE_TYPE);
        } catch (Exception e) {
            log.warn("Failed to deregister resource type.", e);
        }

        // Remove the new items from the main menu
        MenuTree tree = NavigationManager.getMenuTree(CoreMenuTree.MENU_ITEM_MENU_TREE);
        tree.removeMenuItem("resources", "userSamples");
        tree.removeMenuItem("globalResources", "managementSamples");

        // Remove the new page tasks
        MenuTree pageTasks = NavigationManager.getMenuTree(PageTaskMenuTree.PAGE_TASK_MENU_TREE);
        pageTasks.removeMenuItem(null, "showSamples");
    }

    /**
     * Get a static instance of the database used to store the samples
     * 
     * @return samples database
     */
    public static SampleDatabase getDatabase() {
        return database;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.openvpn.als.plugin.Plugin#getTilesConfigFile()
     */
    public String getTilesConfigFile() {
        return "/WEB-INF/sample-tiles-defs.xml";
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.openvpn.als.plugin.Plugin#initPlugin(net.openvpn.als.plugin.PluginDefinition)
     */
    public void initPlugin(PluginDefinition definition) throws PluginException {
        database = new SampleDatabase();
        try {
            database.open(CoreServlet.getServlet());
        } catch (Exception e) {
            throw new PluginException("Failed to open samples database.");
        }
    }



    /* (non-Javadoc)
     * @see net.openvpn.als.plugin.Plugin#installPlugin()
     */
    public void installPlugin() throws PluginException {        
    }

    /* (non-Javadoc)
     * @see net.openvpn.als.plugin.Plugin#uninstallPlugin()
     */
    public void uninstallPlugin() throws PluginException {        
    }

}
