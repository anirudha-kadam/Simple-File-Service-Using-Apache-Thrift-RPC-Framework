import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;

import java.nio.charset.Charset;
import java.nio.file.attribute.FileStoreAttributeView;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TIOStreamTransport;

public class FileStoreHandler implements FileStore.Iface {


	Map<String, Hashtable<String, RFile>> ownerFileStoreTable = null;
	
	public FileStoreHandler() {
		ownerFileStoreTable = new Hashtable<String, Hashtable<String, RFile>>();
	}
		
	private RFile setMetadata(RFile rFile){
		
		rFile.getMeta().setContentLength(rFile.getContent().length());
		rFile.getMeta().setContentHash(calculateMD5(rFile.getContent()));
		rFile.getMeta().setCreated(System.currentTimeMillis());
		rFile.getMeta().setUpdated(System.currentTimeMillis());
		rFile.getMeta().setVersion(0);
		return rFile;
	}
	
	private RFile updateMetadataAndContent(RFile currentRFile, RFile newRFile){
		currentRFile.setContent(newRFile.getContent());
		currentRFile.getMeta().setContentLength(newRFile.getContent().length());
		currentRFile.getMeta().setContentHash(calculateMD5(newRFile.getContent()));
		currentRFile.getMeta().setUpdated(System.currentTimeMillis());
		currentRFile.getMeta().setVersion(currentRFile.getMeta().getVersion() + 1);
		return currentRFile;
	}
	
	private String calculateMD5(String originalText){
		StringBuffer hashText = null; 
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("MD5");
			messageDigest.reset();
			messageDigest.update(originalText.getBytes(Charset.forName("UTF8")));
			byte[] hashBytes = messageDigest.digest();
			hashText = new StringBuffer();
	        for (int i = 0; i < hashBytes.length; i++) {
	        	hashText.append(Integer.toString((hashBytes[i] & 0xff) + 0x100, 16).substring(1));
	        }
		} catch (NoSuchAlgorithmException e) {
			System.err.println("No Such Algorithm: "+e.getMessage());
		}
		
		return hashText.toString();
	}
	
	@Override
	public List<RFileMetadata> listOwnedFiles(String user) throws SystemException, TException {
		List<RFileMetadata> fileList = null;
		Hashtable<String, RFile> fileTable = null;
		if((fileTable = ownerFileStoreTable.get(user)) != null){
			fileList = new ArrayList<RFileMetadata>();
			for (Map.Entry<String, RFile> entry : fileTable.entrySet()) {
				fileList.add(entry.getValue().getMeta());
			}
			return fileList;
		}else{
			SystemException systemException = new SystemException();
			systemException.setMessage("User "+user+" does not exist.");
			throw systemException;
		}
	}

	@Override
	public StatusReport writeFile(RFile rFile) throws SystemException, TException {
		Hashtable<String, RFile> fileTable = null;
		if((fileTable = ownerFileStoreTable.get(rFile.getMeta().getOwner())) != null){
			RFile currentRFile = null;
			if((currentRFile = fileTable.get(rFile.getMeta().getFilename())) != null){
				RFile updatedRFile = updateMetadataAndContent(currentRFile, rFile);
				fileTable.replace(updatedRFile.getMeta().getFilename(), updatedRFile);
				ownerFileStoreTable.put(updatedRFile.getMeta().getOwner(), fileTable);
				writeToFileSystem(updatedRFile);
			}else{
				fileTable.put(rFile.getMeta().getFilename(), setMetadata(rFile));
				ownerFileStoreTable.put(rFile.getMeta().getOwner(), fileTable);
				writeToFileSystem(rFile);
			}
		}else{
			fileTable = new Hashtable<String, RFile>();
			fileTable.put(rFile.getMeta().getFilename(), setMetadata(rFile));
			ownerFileStoreTable.put(rFile.getMeta().getOwner(), fileTable);
			File file = new File("FileStore/"+rFile.getMeta().getOwner());
			file.mkdir();
			writeToFileSystem(rFile);
		}
		return new StatusReport(Status.SUCCESSFUL);
	}

	
	private void writeToFileSystem(RFile rfile){
		try {
			BufferedWriter bw =  new BufferedWriter(new FileWriter("FileStore/" + rfile.getMeta().getOwner()+ "/" + rfile.getMeta().getFilename()));
			bw.write(rfile.getContent());
			bw.flush();
			bw.close();
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}
	
	
	@Override
	public RFile readFile(String filename, String owner) throws SystemException, TException {
		Hashtable<String, RFile> fileTable = null;
		if((fileTable = ownerFileStoreTable.get(owner)) != null){
			RFile rFile = null;
			if((rFile = fileTable.get(filename)) != null){
				return rFile;
			}else{
				SystemException systemException = new SystemException();
				systemException.setMessage("File "+filename+" does not exist.");
				throw systemException;
			}
		}else{
			SystemException systemException = new SystemException();
			systemException.setMessage("User "+owner+" does not exist.");
			throw systemException;
		}
	}

}
