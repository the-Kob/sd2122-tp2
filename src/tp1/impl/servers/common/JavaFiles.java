package tp1.impl.servers.common;

import static java.lang.Long.parseLong;
import static tp1.api.service.java.Result.ErrorCode.*;
import static tp1.api.service.java.Result.error;
import static tp1.api.service.java.Result.ok;
import static util.Hash.of;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.regex.Pattern;

import tp1.api.service.java.Files;
import tp1.api.service.java.Result;
import util.IO;
import util.Token;

public class JavaFiles implements Files {

	static final String DELIMITER = "$$$";
	static final String ROOT = "/tmp/";
	static final int TKN_TIMEOUT = 10000;
	
	public JavaFiles() {
		new File( ROOT ).mkdirs();
	}

	@Override
	public Result<byte[]> getFile(String fileId, String token) {
		if(!validateToken(fileId, token)) return error( FORBIDDEN );
		fileId = fileId.replace( DELIMITER, "/");
		byte[] data = IO.read( new File( ROOT + fileId ));
		return data != null ? ok( data) : error( NOT_FOUND );
	}

	@Override
	public Result<Void> deleteFile(String fileId, String token) {
		if(!validateToken(fileId, token)) return error( FORBIDDEN );
		fileId = fileId.replace( DELIMITER, "/");
		boolean res = IO.delete( new File( ROOT + fileId ));	
		return res ? ok() : error( NOT_FOUND );
	}

	@Override
	public Result<Void> writeFile(String fileId, byte[] data, String token) {
		if(!validateToken(fileId, token)) return error( FORBIDDEN );
		fileId = fileId.replace( DELIMITER, "/");
		File file = new File(ROOT + fileId);
		file.getParentFile().mkdirs();
		IO.write( file, data);
		return ok();
	}

	@Override
	public Result<Void> deleteUserFiles(String userId, String token) {
		if(!validateToken(userId, token)) return error( FORBIDDEN );
		File file = new File(ROOT + userId);
		try {
			java.nio.file.Files.walk(file.toPath())
			.sorted(Comparator.reverseOrder())
			.map(Path::toFile)
			.forEach(File::delete);
		} catch (IOException e) {
			e.printStackTrace();
			return error(INTERNAL_ERROR);
		}
		return ok();
	}

	public static String fileId(String filename, String userId) {
		return userId + JavaFiles.DELIMITER + filename;
	}

	private String token(String id, long currTime) {
		String msg = id + currTime + Token.get();
		return of(msg) + "/" + currTime;
	}

	private boolean validateToken(String id, String token) {
		if(token == null) return false;
		String[] tokens = token.split("/");
		long time = parseLong(tokens[1]);
		String newToken = token(id, time);
		if(System.currentTimeMillis() - time > TKN_TIMEOUT) return false;
		if(!token.equals(newToken)) return false;
		return true;
	}
}
