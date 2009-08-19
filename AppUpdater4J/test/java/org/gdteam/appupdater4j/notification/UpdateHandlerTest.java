package org.gdteam.appupdater4j.notification;

import java.util.ArrayList;

import junit.framework.Assert;

import org.gdteam.appupdater4j.model.ApplicationVersion;
import org.junit.Test;

public class UpdateHandlerTest {

    @Test
    public void testBasic() {
        
        UpdateHandler notifier = new UpdateHandler(this.getClass().getClassLoader().getResource("basictestrss.xml"));
        notifier.parse();
        
        Assert.assertTrue("Version list is not empty", notifier.getInstallVersionList("myappid", "1.0.1").isEmpty());
        Assert.assertTrue("Version list is not empty", notifier.getInstallVersionList("myappid", "1.0.0").isEmpty());
        Assert.assertEquals(1, notifier.getInstallVersionList("myappid", "0.0.9").size());
        
        ArrayList<ApplicationVersion> versions = (ArrayList<ApplicationVersion>) notifier.getInstallVersionList("myappid", "0.0.4");
        
        Assert.assertEquals(3, versions.size());
        Assert.assertEquals("Version 0.0.5", versions.get(0).getName());
        Assert.assertEquals("Version 0.0.5.1", versions.get(1).getName());
        Assert.assertEquals("Version 1.0.0", versions.get(2).getName());
    }
    
}
