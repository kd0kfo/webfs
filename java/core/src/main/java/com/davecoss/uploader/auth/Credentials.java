package com.davecoss.uploader.auth;

import java.util.Arrays;

import com.davecoss.java.Logger;
import com.davecoss.java.utils.CredentialPair;

public class Credentials extends CredentialPair {

	private static Logger L = Logger.getInstance();
	
	public Credentials(String username, char[] passphrase) throws Exception {
		super(username, passphrase);
	}
	
	public static AuthHash generatePassHash(String username, char[] passphrase, String serverSalt) throws Exception {
		byte[] secretBytes = null;
		byte[] passbytes = null;
		AuthHash retval = null;
		if(serverSalt == null)
			throw new Exception("No server salt provided.");
		try {
			byte[] saltbytes = serverSalt.getBytes();
			passbytes = AuthHash.charArray2byteArray(passphrase);
			secretBytes = new byte[saltbytes.length + passbytes.length];
			int secretIdx = 0;
			for(int i = 0;i<passbytes.length;i++)
				secretBytes[secretIdx++] = passbytes[i];
			for(int i = 0;i<saltbytes.length;i++)
				secretBytes[secretIdx++] = saltbytes[i];
			retval = AuthHash.getInstance(username, secretBytes);
		} finally {
			if(secretBytes != null)
				for(int i = 0;i<secretBytes.length;i++)
					secretBytes[i] = (byte)0;
			if(passbytes != null)
				for(int i = 0;i<passbytes.length;i++)
					passbytes[i] = (byte)0;
		}
		
		return retval;
	}
	
}
