import java.io.File;

import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.server.TServer.Args;

public class Server {

	public static FileStoreHandler handler;

	public static FileStore.Processor processor;

	public static int port;

	public static void main(String[] args) {
		if(args.length != 1){
			System.err.println("Only one argument is expected: port number");
			System.exit(0);
		}
		try {
		      handler = new FileStoreHandler();
		      processor = new FileStore.Processor(handler);
		      port= Integer.valueOf(args[0]);
		      Runnable simple = new Runnable() {
		        @Override
				public void run() {
		          simple(processor);
		        }
		      };      
		    
		      new Thread(simple).start();
		     

		    } catch (NumberFormatException x) {
		    	System.err.println("Port Number should be integer: "+x.getMessage());
		    	System.exit(0);
		    } catch (Exception e) {
				System.err.println(e.getMessage());
				System.exit(0);
			}

	}
	
	public static void simple(FileStore.Processor processor) {
	    try {
	      TServerTransport serverTransport = new TServerSocket(port);
	      TServer server = new TSimpleServer(new Args(serverTransport).processor(processor));

	      System.out.println("Starting the FileStore server...");
	      File fileStoreDirectory = new File("FileStore");
	      deleteFileStore(fileStoreDirectory);
	      fileStoreDirectory.mkdir();
	      server.serve();
	    } catch (Exception e) {
	      System.err.println(e.getMessage());
	    }
	}
	
	/**
	 * deletes directory and its contents
	 * @param directory
	 */
	private static void deleteFileStore(File directory) {
	    if (directory.isDirectory()) {
	        for (File subdir : directory.listFiles()) {
	        	deleteFileStore(subdir);
	        }
	    }
	    directory.delete();
	}

}
