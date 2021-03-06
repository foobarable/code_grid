package de.unigoe.sub.fe.goegrid.practicalcourse;

import java.net.URISyntaxException;

import org.gridlab.gat.URI;

import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.ResourceFactory;
import chemtrail.GAThandler;

/**
 *
 */
public class MyResourceFactory implements ResourceFactory {

	private static final boolean debug = false;
	private static final boolean verbose = true;
	
	private GAThandler gatfs;
	String protocol = "gsiftp://";
	String gridhost = "";

	public MyResourceFactory() {
		System.out.println("Setting up GAT handler");
		gatfs = new GAThandler("/tmp/x509up_u1013", "gridftp");
		if (gatfs != null) {
			System.out.println("GAT handler running");
		}
	}

	/**
	 * Get a new Gridresource.
	 * 
	 * @param host host of the requested resource
	 * @param path path to the requested resource
	 * 
	 * @return the resource described by the parameters.
	 * 
	 * @see com.bradmcevoy.http.ResourceFactory#getResource(java.lang.String,
	 * java.lang.String)
	 */
	public Resource getResource(String host, String path) {
		System.out.println("Host " + host + " Path " + path);
		
		String[] tokens = path.split("/");
		String newpath = "";
		
		for (int i = 1; i< tokens.length; i++)
		{
			if(debug) System.out.println("Token #" + i + ": " + tokens[i]);
			
			if(i != 1) {
				newpath = newpath + "/" + tokens[i];
			}
			else {
				gridhost = tokens[i];
			}
		}
		
		if(path.equals("/")) {
			//TODO Query for a list of available nodes, create folder resource with all available nodes being folder resources with / as parent folder resource
		}
		
		
		URI target = null;
		try {
			target = new URI(protocol + gridhost + "/" + newpath);
			System.out.println("Generated URI:" + target);
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
		}
		Resource gfs = null;
		if (gatfs.exists(target)) {
			if (gatfs.isDirectory(target)) {
				gfs = new GridFolderResource(gatfs, target);

			} else if (gatfs.isFile(target)) {
				gfs = new GridFileResource(gatfs, target);
			}
		}
		else {
			if(verbose) System.out.println("RESOURCEFACTORY: Requested resource \""+target+"\" does not exist");
		}

		return gfs;
	}

}
