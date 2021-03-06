package chemtrail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

import org.globus.exec.generated.JobDescriptionType;
import org.globus.exec.generated.MultiJobDescriptionType;
import org.globus.rft.generated.RFTOptionsType;
import org.globus.rft.generated.TransferRequestType;
import org.globus.rft.generated.TransferType;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.globus.exec.utils.ManagedJobConstants;
import org.globus.exec.utils.ManagedJobFactoryConstants;
import org.globus.exec.utils.client.ManagedJobFactoryClientHelper;
import org.apache.axis.message.addressing.AttributedURI;
import java.net.URL;

import chemtrail.SubmitJob;
import chemtrail.MDS4Client;

public class Chemtrail {

	static String contact = "lima";

	public static EndpointReferenceType getFactoryEPR (String contact, String factoryType)
	throws Exception {
   			URL factoryUrl = ManagedJobFactoryClientHelper.getServiceURL(contact).getURL();
    		return ManagedJobFactoryClientHelper.getFactoryEndpoint(factoryUrl, factoryType);
	}
	
	public static EndpointReferenceType getFactoryEPR (URL factoryURL, String factoryType)
	throws Exception {
   		return ManagedJobFactoryClientHelper.getFactoryEndpoint(factoryURL, factoryType);
	}


	//Method for creating one  
	private static JobDescriptionType createJob (String InputFileName, int Width, int Heigth, int y,int yStep, int Offset, String target) {

		JobDescriptionType tempJob = new JobDescriptionType();
		String BaseName = (new File(InputFileName)).getName();
		String InputFileOnNode = "/tmp/griduser9" + BaseName;
		
		
		tempJob.setExecutable("/usr/bin/povray");
		String arguments[] = new String[9];
		//
		arguments[0] =  "-I" + InputFileName;
		arguments[1] =  "+FT";
		arguments[2] = "-W" + Width;
		arguments[3] = "-H" + Heigth;
		arguments[4] = "-SR" + (y + 1);
		arguments[5] = "-ER" + (y + yStep + Offset);
		arguments[6] = "-SC1";
		arguments[7] = "-EC" + Width;
		// TODO: Proper output filename
		arguments[8] = "+O" + "/tmp/" + "griduser9" + y + "_" + BaseName +  ".tga";
		tempJob.setArgument(arguments);
		
		tempJob.setDirectory("/tmp");
		tempJob.setStdout("/tmp/griduser9.stdout");
		tempJob.setStderr("/tmp/griduser9.stderr");

		TransferType inFileTransfer = new TransferType();
		inFileTransfer.setSourceUrl("file://" + InputFileName);
		inFileTransfer.setDestinationUrl("gsiftp://"+ target  + InputFileOnNode);
	
		System.out.println(inFileTransfer.getSourceUrl());
		System.out.println(inFileTransfer.getDestinationUrl());
		TransferRequestType request = new TransferRequestType();
		request.setTransfer(new TransferType[1]);
		request.setTransfer(0, inFileTransfer);
		
		//As soon as we do filestaging, the job fails with actually no error output. Therefore, filestaging is disabled so far. 
		//tempJob.setFileStageIn(request);
	
		String factoryType = ManagedJobFactoryConstants.FACTORY_TYPE.FORK;
		try {
			EndpointReferenceType factoryEndpoint = Chemtrail.getFactoryEPR(target,factoryType);
			tempJob.setFactoryEndpoint(factoryEndpoint);	
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		
		System.out.print(tempJob.getExecutable());
		for(int i=0; i<tempJob.getArgument().length;i++)
		{
			System.out.print(" " + tempJob.getArgument(i));
		}
		System.out.println();
		return tempJob;
	}

	public static void main(String[] args) {
		String usage = "Usage:\n./chemtrail inputfile width height outputfile";
		if (args.length != 4) {
			System.out.println(usage);
			System.exit(-1);
		}

		String InputFileName = args[0];
		String OutputFileName = args[3];

		int Width = Integer.parseInt(args[1]);
		int Heigth = Integer.parseInt(args[2]);

		System.out.println("Processing " + InputFileName + " with size of "
				+ Width + "x" + Heigth);

		//Using the MDS4 client to get some information
		chemtrail.MDS4Client mds4 = new chemtrail.MDS4Client();
		ArrayList list = null;
		String stringAddress = "https://localhost:8443/wsrf/services/DefaultIndexService";
		try
		{
			//Query the defaultIndexService for all available ManagedJobFactoryServices
			list = mds4.queryForManagedJobFactoryService(stringAddress);
			
			//Debug output: List ALL the factories we found
			for (Iterator it= list.iterator(); it.hasNext();) {
				//System.out.println(it.next());
				System.out.println(((AttributedURI)it.next()).getHost());
			}
			System.out.println("Found " + list.size() + " nodes");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	
		

		//Objects that will store our multiple render jobs
		MultiJobDescriptionType multi = new MultiJobDescriptionType();
		List<JobDescriptionType> multiJobs = new ArrayList<JobDescriptionType>();

		//Setting number of nodes we want to use
		int Nodes = 2;
		if(list != null) {
			Nodes = list.size();
		}

		//Job calculation and creation
		int yStep = Math.round(Heigth / Nodes);
		int yRest = Heigth % Nodes;

		Iterator it = list.iterator();
		for (int y = 0; y < Heigth - yStep; y += yStep) {
			int Offset = 0;
			if (yRest != 0) {
				Offset = 1;
				yRest--;
			}
			multiJobs.add(Chemtrail.createJob(InputFileName,Width,Heigth,y,yStep,Offset,((AttributedURI)it.next()).getHost()));
			System.out.println();
		}


		//Submit multijob
		multi.setJob((JobDescriptionType[]) multiJobs
				.toArray(new JobDescriptionType[0]));
		
		SubmitJob jobsubmitter = new SubmitJob();
		try {
			jobsubmitter.submitJob(multi);
			System.out.println("Waiting for notification messages");
			synchronized (SubmitJob.getWaiter()) {
				SubmitJob.getWaiter().wait();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
