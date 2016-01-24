import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TSimpleJSONProtocol;
import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

public class Client {

	public static void main(String[] args) {
		String operation = null, fileName = null, ownerName = null;
		if(args.length != 8 && args.length != 6){
			System.err.println("Expected number of arguments is 8 or 6\n");
			System.out.println("USAGE 8: ./client.sh <hostName> <portNumber> --operation <operation> --filename <fileName> --user <ownerName>");
			System.out.println("USAGE 6: ./client.sh <hostName> <portNumber> --operation <operation> --user <ownerName>");
			System.exit(0);
		}
		for(int i = 2; i < args.length; i = i + 2){
			if(args[i].equals("--operation")){
				operation = args[i+1];
			}
			if(args[i].equals("--filename")){
				fileName = args[i+1];
			}
			if(args[i].equals("--user")){
				ownerName = args[i+1];
			}
		}
		try {
			TTransport transport;

			transport = new TSocket(args[0], Integer.valueOf(args[1]));
			transport.open();

			TProtocol protocol = new TBinaryProtocol(transport);
			FileStore.Client client = new FileStore.Client(protocol);

			perform(client, operation, fileName, ownerName);

			transport.close();
		} catch (TException x) {
			System.err.println(x.getMessage());
			System.out.println(System.getProperty("line.separator"));
			System.exit(0);
		} catch (NumberFormatException e) {
			System.err.println("Port Number should be integer: "+e.getMessage());
	    		System.exit(0);
		} catch (Exception e) {
			System.err.println(e.getMessage());
 			System.out.println(System.getProperty("line.separator"));
			System.exit(0);
		}

	}

	/**
	 * reads entire file content
	 * @param fileName
	 * @return String file Contents
	 */
	private static String readFileContents(String fileName) {
		File file = new File(fileName);
		StringBuilder sb = null;
		BufferedReader br = null;

		try {
			br = new BufferedReader(new FileReader(file));
			sb = new StringBuilder();
			String currentLine = null;
			while ((currentLine = br.readLine()) != null) {
				sb.append(currentLine);
				sb.append(System.getProperty("line.separator"));
			}
			if(!sb.toString().equals("")){
				sb.deleteCharAt(sb.length()-1);
			}
		} catch (FileNotFoundException e) {
			System.err.println("File could not be found: " + e.getMessage());
			System.out.println(System.getProperty("line.separator"));
			System.exit(0);
		} catch (IOException e) {
			System.err.println("Found IOException: " + e.getMessage());
			System.out.println(System.getProperty("line.separator"));
			System.exit(0);
		}

		if (sb != null)
			return sb.toString();
		else
			return "";
	}

	private static void perform(FileStore.Client client, String operation, String fileName, String ownerName) throws TException {
		TIOStreamTransport transport=new TIOStreamTransport(System.out);
		TProtocol tjsonProtocol = new TJSONProtocol.Factory().getProtocol(transport);
		if(operation == null || operation.equals("")){
			System.err.println("operation not provided");
			System.exit(0);
		}
		switch (operation) {

		case "write":
			if(fileName == null || fileName.equals("")){
				System.err.println("fileName not provided");
				System.out.println(System.getProperty("line.separator"));
				System.exit(0);
			}
			if(ownerName == null || ownerName.equals("")){
				System.err.println("ownerName not provided");
				System.out.println(System.getProperty("line.separator"));
				System.exit(0);
			}
			RFile rFile = new RFile();
			rFile.setContent(readFileContents(fileName));
			RFileMetadata rFileMetadata = new RFileMetadata();
			rFileMetadata.setFilename(fileName);
			rFileMetadata.setOwner(ownerName);
			rFile.setMeta(rFileMetadata);
			StatusReport statusReport = client.writeFile(rFile);
			statusReport.write(tjsonProtocol);
			System.out.println(System.getProperty("line.separator"));
			break;

		case "read":
			if(fileName == null || fileName.equals("")){
				System.err.println("fileName not provided");
				System.out.println(System.getProperty("line.separator"));
				System.exit(0);
			}
			if(ownerName == null || ownerName.equals("")){
				System.err.println("ownerName not provided");
				System.out.println(System.getProperty("line.separator"));
				System.exit(0);
			}
			try {
				RFile rfile = client.readFile(fileName, ownerName);
				rfile.write(tjsonProtocol);
				System.out.println(System.getProperty("line.separator"));
			} catch (SystemException e) {
				e.write(tjsonProtocol);
				System.out.println(System.getProperty("line.separator"));
			}
			break;

		case "list":
			if(ownerName == null || ownerName.equals("")){
				System.err.println("ownerName not provided");
				System.out.println(System.getProperty("line.separator"));
				System.exit(0);
			}
			if(fileName != null){
				System.err.println("filename is not an expected argument for list operation");
				System.out.println(System.getProperty("line.separator"));
				System.exit(0);
			}
			try {
				List<RFileMetadata> rFileMetadataList = client.listOwnedFiles(ownerName);
				tjsonProtocol.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, rFileMetadataList.size()));
	            		for (RFileMetadata element : rFileMetadataList)
	            		{
	            			element.write(tjsonProtocol);
	            		}
	            		tjsonProtocol.writeListEnd();
				System.out.println(System.getProperty("line.separator"));

			} catch (SystemException e) {
				e.write(tjsonProtocol);
				System.out.println(System.getProperty("line.separator"));
			}
			break;
			
		default:
			System.out.println("USAGE [case sensitive]: operation can be (1) write (2) read (3) list");
			System.out.println(System.getProperty("line.separator"));
			break;
		}

	}

}
