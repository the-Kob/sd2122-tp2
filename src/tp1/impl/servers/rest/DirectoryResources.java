package tp1.impl.servers.rest;

import static java.lang.Long.parseLong;
import static tp1.impl.clients.Clients.FilesClients;
import static util.Hash.of;

import java.net.URI;
import java.util.*;
import java.util.logging.Logger;

import jakarta.inject.Singleton;
import tp1.api.FileInfo;
import tp1.api.service.java.Directory;
import tp1.api.service.java.Result.ErrorCode;
import tp1.api.service.rest.RestDirectory;
import tp1.impl.servers.common.JavaDirectory;
import util.Token;
import tp1.impl.servers.kafka.KafkaSubscriber;

@Singleton
public class DirectoryResources extends RestResource implements RestDirectory {
	private static Logger Log = Logger.getLogger(DirectoryResources.class.getName());

	private static final String SOAP = "/soap/";

	static final String KAFKA_BROKERS = "kafka:9092";
	static final String TOPIC = "delete_user";
	private static final String FROM_BEGINNING = "earliest";


	final Directory impl;

	public DirectoryResources() {
		impl = new JavaDirectory();

		//var subscriber = KafkaSubscriber.createSubscriber(KAFKA_BROKERS , List.of(TOPIC),
		//FROM_BEGINNING);

		//subscriber.start(false, (r) -> {

		//	String[] message = r.value().split(" ");
			
		//	deleteUserFiles(message[0], message[1], Token.get());
		//});
	}

	public FileInfo writeFile(String filename, byte[] data, String userId, String password) {
		Log.info(String.format("REST writeFile: filename = %s, data.length = %d, userId = %s, password = %s \n",
				filename, data.length, userId, password));

		return super.resultOrThrow(impl.writeFile(filename, data, userId, password));
	}

	@Override
	public void deleteFile(String filename, String userId, String password) {
		Log.info(String.format("REST deleteFile: filename = %s, userId = %s, password =%s\n", filename, userId,
				password));

		super.resultOrThrow(impl.deleteFile(filename, userId, password));
	}

	@Override
	public void shareFile(String filename, String userId, String userIdShare, String password) {
		Log.info(String.format("REST shareFile: filename = %s, userId = %s, userIdShare = %s, password =%s\n", filename,
				userId, userIdShare, password));

		super.resultOrThrow(impl.shareFile(filename, userId, userIdShare, password));
	}

	@Override
	public void unshareFile(String filename, String userId, String userIdShare, String password) {
		Log.info(String.format("REST unshareFile: filename = %s, userId = %s, userIdShare = %s, password =%s\n",
				filename, userId, userIdShare, password));

		super.resultOrThrow(impl.unshareFile(filename, userId, userIdShare, password));
	}

	@Override
	public byte[] getFile(String filename, String userId, String accUserId, String password) {
		Log.info(String.format("REST getFile: filename = %s, userId = %s, accUserId = %s, password =%s\n", filename,
				userId, accUserId, password));

		var res = impl.getFile(filename, userId, accUserId, password);
		if (res.error() == ErrorCode.REDIRECT) {
			List<String> restl = new ArrayList<>();
			List<String> soapl = new ArrayList<>();
			Queue<String> uris = res.errorValue();
			while(!uris.isEmpty()) {
				String loc = uris.remove();
				if (loc.contains(SOAP)) {
					soapl.add(loc);
					String fileId = JavaDirectory.fileId(filename, userId);
					res = FilesClients.get(loc).getFile(fileId, Token.createToken(fileId, System.currentTimeMillis()));
					if(res.isOK()) break;
				} else restl.add(loc);
			}
			uris.addAll(restl);
			uris.addAll(soapl);
		}

		return super.resultOrThrow(res);
	}

	@Override
	public List<FileInfo> lsFile(String userId, String password) {
		long T0 = System.currentTimeMillis();
		try {

			Log.info(String.format("REST lsFile: userId = %s, password = %s\n", userId, password));

			return super.resultOrThrow(impl.lsFile(userId, password));
		} finally {
			System.err.println("TOOK:" + (System.currentTimeMillis() - T0));
		}
	}

	@Override
	public void deleteUserFiles(String userId, String password, String token) {
		Log.info(String.format("REST deleteUserFiles: user = %s, password = %s, token = %s\n", userId, password, token));

		super.resultOrThrow(impl.deleteUserFiles(userId, password, token));
	}
}
