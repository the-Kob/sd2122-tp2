package tp1.impl.servers.common;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.gson.Gson;
import org.pac4j.scribe.builder.api.DropboxApi20;
import tp1.api.service.java.Files;
import tp1.api.service.java.Result;
import util.Token;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import static java.lang.Long.parseLong;
import static tp1.api.service.java.Result.ErrorCode.FORBIDDEN;
import static tp1.api.service.java.Result.ErrorCode.NOT_FOUND;
import static tp1.api.service.java.Result.error;
import static tp1.api.service.java.Result.ok;
import static util.Hash.of;

public class JavaDropboxFiles implements Files {

    private record DeleteArg(String path) {
    }

    static final String DELIMITER = "$$$";

    private static final String UPLOAD_URL = "https://content.dropboxapi.com/2/files/upload";
    private static final String DOWNLOAD_URL = "https://content.dropboxapi.com/2/files/download";
    private static final String DELETE_URL = "https://api.dropboxapi.com/2/files/delete_v2";

    private static final int HTTP_SUCCESS = 200;
    private static final int HTTP_CONFLICT = 409;
    private static final String CONTENT_TYPE_HDR = "Content-Type";
    private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
    private static final String OCTET_STREAM_CONTENT_TYPE = "application/octet-stream";

    private static final String DROPBOX_API_ARG_HDR = "Dropbox-API-Arg";

    private static final String MODE_PARAM = "mode";
    private static final String MODE_PARAM_VALUE = "overwrite";

    private static final String ROOT = "files";

    private static final int TKN_TIMEOUT = 10000;

    private final Gson json;
    private final OAuth20Service service;
    private final OAuth2AccessToken accessToken;

    public JavaDropboxFiles(String apiKey, String apiSecret, String accessTokenStr, boolean flag) {
        this.json = new Gson();
        this.accessToken = new OAuth2AccessToken(accessTokenStr);
        this.service = new ServiceBuilder(apiKey).apiSecret(apiSecret).build(DropboxApi20.INSTANCE);

        if(flag) {
            deleteFile(ROOT, token(ROOT, System.currentTimeMillis()));
        }
    }

    @Override
    public Result<byte[]> getFile(String fileId, String token) {
        if(!validateToken(fileId, token)) return error( FORBIDDEN );
        try {
            String filePath = String.format("%s/%s", ROOT, fileId.replace(DELIMITER, "/"));

            var downloadFile = new OAuthRequest(Verb.POST, DOWNLOAD_URL);
            downloadFile.addHeader(CONTENT_TYPE_HDR, OCTET_STREAM_CONTENT_TYPE);
            downloadFile.addHeader(DROPBOX_API_ARG_HDR, String.format("{\"path\": \"/%s\"}", filePath));

            service.signRequest(accessToken, downloadFile);

            Response r = service.execute(downloadFile);
            byte[] data = r.getStream().readAllBytes();

            if(r.getCode() != HTTP_SUCCESS) {
                return error(NOT_FOUND);
            }

            return data!= null ? ok(data) : error(NOT_FOUND);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Result<Void> deleteFile(String fileId, String token) {
        if(!validateToken(fileId, token)) return error( FORBIDDEN );
        try {
            String filePath = String.format("%s/%s", ROOT, fileId.replace(DELIMITER, "/"));

            var deleteFile = new OAuthRequest(Verb.POST, DELETE_URL);
            deleteFile.addHeader(CONTENT_TYPE_HDR, JSON_CONTENT_TYPE);

            if(fileId.equals(ROOT)) {
                deleteFile.setPayload(json.toJson(new DeleteArg(String.format("/%s", ROOT))));

                service.signRequest(accessToken, deleteFile);

                Response r = service.execute(deleteFile);

                // The HTTP_CONFLICT is due to the fact that the files root folder isn't created initially
                if (r.getCode() != HTTP_SUCCESS && r.getCode() != HTTP_CONFLICT)
                    throw new RuntimeException(String.format("Failed to delete file: %s at %s, Status: %d, \nReason: %s\n", fileId, filePath, r.getCode(), r.getBody()));

            } else {
                deleteFile.setPayload(json.toJson(new DeleteArg(String.format("/%s", filePath))));

                service.signRequest(accessToken, deleteFile);

                Response r = service.execute(deleteFile);

                if (r.getCode() != HTTP_SUCCESS)
                    throw new RuntimeException(String.format("Failed to delete file: %s at %s, Status: %d, \nReason: %s\n", fileId, filePath, r.getCode(), r.getBody()));
            }

            return ok();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Result<Void> writeFile(String fileId, byte[] data, String token) {
        if(!validateToken(fileId, token)) return error( FORBIDDEN );
        try {
            String filePath = String.format("%s/%s", ROOT, fileId.replace(DELIMITER, "/"));

            var uploadFile = new OAuthRequest(Verb.POST, UPLOAD_URL);
            uploadFile.addHeader(CONTENT_TYPE_HDR, OCTET_STREAM_CONTENT_TYPE);
            uploadFile.addHeader(DROPBOX_API_ARG_HDR, String.format("{\"path\": \"/%s\", \"%s\": \"%s\"}", filePath, MODE_PARAM, MODE_PARAM_VALUE));

            uploadFile.setPayload(data);

            service.signRequest(accessToken, uploadFile);

            Response r = service.execute(uploadFile);

            if (r.getCode() != HTTP_SUCCESS)
                throw new RuntimeException(String.format("Failed to create file: %s at %s, Status: %d, \nReason: %s\n", fileId, filePath, r.getCode(), r.getBody()));

            return ok();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Result<Void> deleteUserFiles(String userId, String token) {
        if(!validateToken(userId, token)) return error( FORBIDDEN );
        try {
            String filePath = String.format("%s/%s", ROOT, userId);

            var deleteFile = new OAuthRequest(Verb.POST, DELETE_URL);
            deleteFile.addHeader(CONTENT_TYPE_HDR, JSON_CONTENT_TYPE);

            deleteFile.setPayload(json.toJson(new DeleteArg(filePath)));

            service.signRequest(accessToken, deleteFile);

            Response r = service.execute(deleteFile);

            if (r.getCode() != HTTP_SUCCESS)
                throw new RuntimeException(String.format("Failed to delete user files: %s, Status: %d, \nReason: %s\n", userId, r.getCode(), r.getBody()));

            return ok();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
