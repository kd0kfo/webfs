package com.davecoss.uploader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.davecoss.java.Logger;

public class UploadOutputStream extends OutputStream {

	static Logger L = Logger.getInstance();
	
	public static final int DEFAULT_BUFFER_SIZE = 4096;// may be set using the system property, upload.buffer_size
	public static final String PROPERTY_BUFFER_SIZE = "upload.buffer_size"; // System property name for buffer size.
	
	protected HTTPSClient client;
	protected String destinationURL;
	protected int bufferSize = DEFAULT_BUFFER_SIZE;
	
	
	private int currIndex = 0;
	private String baseFilename;
	private File tempfile;
	private FileOutputStream stream;
	private int bytesWritten = 0;
	private WebResponse uploadResponse = null;
	
	public UploadOutputStream(HTTPSClient client, String destinationURL) throws IOException {
		tempfile = getTempFile();
		stream = new FileOutputStream(tempfile);
		baseFilename = tempfile.getName();
		this.client = client;
		this.destinationURL = destinationURL;
		loadBufferSize();
	}
	
	public UploadOutputStream(String baseFilename, HTTPSClient client, String destinationURL) throws IOException {
		tempfile = getTempFile();
		this.baseFilename = baseFilename;
		stream = new FileOutputStream(tempfile);
		this.client = client;
		this.destinationURL = destinationURL;
		loadBufferSize();
		
		while(this.baseFilename.charAt(0) == '/')
			this.baseFilename = this.baseFilename.substring(1);
	}
	
	public int setBufferSize(int newsize) {
		bufferSize = newsize;
		return bufferSize;
	}
	
	public int getBufferSize() {
		return bufferSize;
	}
	
	
	@Override 
	public void flush() throws IOException {
		if(stream == null)
		{
			L.info("No stream to flush");
			return;
		}
		L.info("Flushing Contents of UploadOutputStream");
		// Null check
		if(client == null)
			throw new IOException("Client not connected.");

		// If stream is null, we are between two segments, but haven't written anything. Nothing to flush.
		
		// Close stream
		stream.close();
		
		// Move file to what it should be when uploaded
		File newfile = new File(tempfile.getParentFile(), String.format("%s.%d", baseFilename, currIndex++));
		L.info(String.format("Renaming %s to %s", tempfile.getAbsolutePath(), newfile.getAbsolutePath()));
		if(!tempfile.renameTo(newfile)) {
			L.debug("Rename failed for " + newfile.getName());
			stream = null;
			throw new IOException("Could not rename " + tempfile.getName() + " to " + newfile.getName());
		}
		
		// Upload it
		uploadResponse = uploadFile(newfile);
		
		// Cleanup
		newfile.delete();
		L.info("Cleaned up " + newfile.getName());
		bytesWritten = 0;
		stream = null;

		if(uploadResponse.status != WebResponse.SUCCESS) {
			throw new IOException("Error flushing uploader stream: " + uploadResponse.message);
		}
	}
	
	@Override
	public void close() throws IOException {
		// See if flush is in order.
		flush();
		
		// Tie up any loose ends
		stream = null;
	}
	
	@Override
	public void write(int b) throws IOException {
		if(stream == null)
			stream = new FileOutputStream(tempfile);
		if(bytesWritten == bufferSize)
			flush();
		stream.write(b);
	}
	
	@Override
	public void write(byte[] buf, int offset, int len) throws IOException {
		int amountToWrite = len;
		int currWriteAmount = 0;
		int totalAmountWritten = 0;
		
		while(amountToWrite > 0) {
			// If buffer's full, flush.
			if(bytesWritten == bufferSize)
				flush();
			
			// Figure out how much to write on this pass.
			if(amountToWrite  > bufferSize - bytesWritten)
				currWriteAmount = bufferSize - bytesWritten;
			else
				currWriteAmount = amountToWrite;
			
			// If stream is closed, open a new one.
			if(stream == null)
				stream = new FileOutputStream(tempfile);
			// Do write.
			stream.write(buf, offset + totalAmountWritten, currWriteAmount);
			
			// Update counters.
			totalAmountWritten += currWriteAmount;
			bytesWritten += currWriteAmount;
			amountToWrite -= currWriteAmount;
			L.debug(String.format("Wrote %d bytes %d remaining. (Offset: %d, Length: %d)", currWriteAmount, amountToWrite, offset + totalAmountWritten, len));
		}
		
		if(bytesWritten == bufferSize) {
			flush();
		}
	}
	
	
	private static File getTempFile() throws IOException {
		File temp = File.createTempFile("tmp", null);
		temp.deleteOnExit();
		return temp;
	}
	
	protected WebResponse uploadFile(File file) throws IOException {
		return client.postFile(this.destinationURL, file);
	}
	
	public WebResponse getUploadResponse() {
		return uploadResponse;
	}
	
	protected void loadBufferSize() {
		String strBufferSize = System.getProperty(PROPERTY_BUFFER_SIZE, String.valueOf(DEFAULT_BUFFER_SIZE));
		try {
			bufferSize = Integer.parseInt(strBufferSize);
		} catch(NumberFormatException nfe) {
			L.error("Invalid buffer size in system property:");
			L.error(String.valueOf(bufferSize));
			throw nfe;
		}
	}
}
