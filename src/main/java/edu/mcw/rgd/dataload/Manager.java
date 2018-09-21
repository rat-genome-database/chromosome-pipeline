package edu.mcw.rgd.dataload;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

/**
 * @author mtutaj
 * Date: Aug 13, 2010
 * Time: 10:07:53 AM
 */
public class Manager {

    private String version;

    /**
     * starts the pipeline; properties are read from properties/AppConfigure.xml file
     * @param args cmd line arguments, like species
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        
	    // usage
        if( args.length<4 ) {
            usage();
            return;
        }

        // load manager class
        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new FileSystemResource("properties/AppConfigure.xml"));
        Manager manager=(Manager) (bf.getBean("manager"));
        System.out.println(manager.getVersion());

        // parse args
        int mapKey = 0;
        boolean loadScaffolds = false;
        String load = null;
        for( int i=0; i<args.length; i++ ) {
            if( args[i].equals("--map_key") ) {
                mapKey = Integer.parseInt(args[++i]);
            }
            else if( args[i].equals("--load") ) {
                load = args[++i];
            }
            else if( args[i].equals("--load_scaffolds") ) {
                loadScaffolds = true;
            }
        }

        // run the selected loader
        if( load==null || mapKey==0 ) {
            usage();
        }
        else if( load.equals("chrSizes") ) {
            ChromosomeSizesLoader loader = (ChromosomeSizesLoader) bf.getBean("chromosomeSizesLoader");
            loader.run(mapKey, loadScaffolds);
        }
        else if( load.equals("cytomap") || load.equals("cytoband") ) {
            CytomapLoader loader = (CytomapLoader) bf.getBean("cytomapLoader");
            loader.run(mapKey);
        } else {
            usage();
        }
    }

    static void usage() {
        System.out.println("Usage: ChromosomePipeline parameters: ");
        System.out.println("   --load <chrSizes|cytomap|cytoband> --map_key <map_key> [--load_scaffolds]");
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}
