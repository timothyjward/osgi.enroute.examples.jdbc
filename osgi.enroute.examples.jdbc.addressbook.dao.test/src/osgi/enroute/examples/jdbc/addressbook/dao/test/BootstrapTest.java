package osgi.enroute.examples.jdbc.addressbook.dao.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.service.transaction.control.jpa.JPAEntityManagerProvider;
import org.osgi.util.tracker.ServiceTracker;

import osgi.enroute.examples.jdbc.addressbook.dao.JDBCExampleBase;

public class BootstrapTest extends JDBCExampleBase {


    ServiceTracker<DataSourceFactory,DataSourceFactory> dsfTracker;

    ServiceTracker<JPAEntityManagerProvider,JPAEntityManagerProvider> cpTracker;

    public BootstrapTest() throws Exception {
        super();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Before
    public void setUp() throws Exception{

        configAdmin  = (ConfigurationAdmin) cmTracker.waitForService(5000);

        assertNotNull(configAdmin);
        assertNotNull(txServiceProps.getProperty("aries.dsf.target.filter"));

        Filter dsfFilter = FrameworkUtil.createFilter("(&("+
                Constants.OBJECTCLASS + "="+DataSourceFactory.class.getName()+")"+txServiceProps.getProperty("aries.dsf.target.filter")+")");

        dsfTracker = new ServiceTracker<>(context,dsfFilter, null);

        dsfTracker.open();

        Filter cpFilter =  FrameworkUtil.createFilter("(&("+
                Constants.OBJECTCLASS+"="+JPAEntityManagerProvider.class.getName()+")"
                + "(osgi.unit.name=addressBookPU)"
                +")");
        cpTracker = new ServiceTracker<>(context, cpFilter, null);
        cpTracker.open();

        localJPAProviderConfig = configAdmin.createFactoryConfiguration(FACTORY_PID_ARIES_TX_CONTROL_JPA_LOCAL,null);
        localJPAProviderConfig.update((Hashtable)txServiceProps);

    }

    @Test
    public void testDataSourceFactoryAvailable() throws Exception{

        DataSourceFactory dataSourceFactory =  dsfTracker.waitForService(3000);    

        assertNotNull(dataSourceFactory); 
    }

    @Test
    public void testJPAEntityManagerProviderRegistered() throws Exception{

        JPAEntityManagerProvider jdbcConnectionProvider =  cpTracker.waitForService(5000);
        assertNotNull(jdbcConnectionProvider);

        ServiceReference<JPAEntityManagerProvider>[] conProviderRefs =  cpTracker.getServiceReferences();
        assertNotNull(conProviderRefs);
        assertEquals(1, conProviderRefs.length);
        ServiceReference<JPAEntityManagerProvider> conProviderRef = conProviderRefs[0];
        assertEquals("addressBookPU", conProviderRef.getProperty("osgi.unit.name"));
    }    

    @After
    public void tearDown() throws Exception{
       
        if(localJPAProviderConfig!= null){
        	localJPAProviderConfig.delete();
        	localJPAProviderConfig = null;
        }

        if(cmTracker != null){
            cmTracker.close();
        }

        if(dsfTracker != null){
            dsfTracker.close();
        }

        if(cpTracker != null){
            cpTracker.close();
        }
    }

}
