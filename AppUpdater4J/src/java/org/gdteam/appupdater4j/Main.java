package org.gdteam.appupdater4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.gdteam.appupdater4j.install.InstallationHelper;
import org.gdteam.appupdater4j.model.UpdateFile;
import org.gdteam.appupdater4j.model.Version;
import org.gdteam.appupdater4j.wrapper.ApplicationLauncher;

public class Main {
    
    private File propertyFile;
    private Properties properties = null;
    private UpdateManager updateManager = null;
    private InstallationHelper installationHelper = new InstallationHelper();
    private ApplicationLauncher applicationLauncher;
    
    public void loadProperties(String[] args) throws Exception {
        if (args.length != 1) {
            StringBuilder message = new StringBuilder("Usage : java -jar appupdater4j.jar <propertyfile>\n");
            message.append("      - propertyfile : path to file which contains appupdater4j configuration properties");
            
            throw new Exception(message.toString());
        }
        
        this.properties = new Properties();
        FileInputStream fis = null;
        try {
            propertyFile = new File(args[0]);
            fis = new FileInputStream(propertyFile);
            this.properties.load(fis);
        } catch (Exception e) {
            throw new Exception("Cannot open/read property file : " + args[0]);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception e) {
                }
            }
        }
    }
    
    public void configureUpdateManager() {
        this.updateManager = new UpdateManager();
        this.updateManager.configure(properties);
    }
    
    public void configureApplicationLauncher(){
        this.applicationLauncher = new ApplicationLauncher(new File(this.properties.getProperty("application.jar")), new String[0]);
    }
    
    /**
     * Install updates which are stored in specific folder
     * @return installed version
     */
    public Version installAutoUpdate() {
        Version installedVersion = null;
        
        List<UpdateFile> files = this.updateManager.getFilesToAutomaticallyInstall();
        
        if (!files.isEmpty()) {
            try {
                for (UpdateFile updateFile : files) {                
                    this.installationHelper.installUpdate(updateFile);
                    installedVersion = updateFile.getPreviousVersion();
                    //Delete updateFile
                    updateFile.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            if (installedVersion != null) {
                this.properties.put("application.version", installedVersion.toString());
                FileOutputStream out = null;
                try {
                    out = new FileOutputStream(this.propertyFile);
                    this.properties.store(out, "Generated by AppUpdater4J at " + new Date().toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        return installedVersion;
    }
    
    /**
     * Check for update and install update if necessary. Wait for the end of this method to start application
     */
    public void performModalCheck() {
        this.updateManager.performCheckForUpdate();
        
        if (this.updateManager.needUpdate()) {
            UpdateController controller = UpdateControllerFactory.getUpdateController();
            this.installationHelper.addInstallationListener(controller);
            controller.displayController();
            
        }
        
    }
    
    public void runApplication() throws Exception {
        this.applicationLauncher.extractManifestInfo();
        this.applicationLauncher.run();
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        
        Main application = new Main();
        try {
            application.loadProperties(args);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
        
        application.configureUpdateManager();
        Version installedVersion = application.installAutoUpdate();
        
        if (installedVersion == null) {
            //No autoupdate
            application.performModalCheck();
        }
        
        //Start application
        try {
            application.runApplication();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

}