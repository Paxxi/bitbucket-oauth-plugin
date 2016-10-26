package org.jenkinsci.plugins.api;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

import org.acegisecurity.userdetails.UserDetails;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.api.BitbucketUser.BitbucketUserResponce;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import com.google.gson.Gson;

public class BitbucketApiService {

    private static final String API_ENDPOINT = "https://api.bitbucket.org/2.0/";

    private OAuthService service;

    private String teamName;

    public BitbucketApiService(String apiKey, String apiSecret, String teamName) {
        this(apiKey, apiSecret, teamName, null);
    }

    public BitbucketApiService(String apiKey, String apiSecret, String teamName, String callback) {
        super();
        ServiceBuilder builder = new ServiceBuilder().provider(BitbucketApi.class).apiKey(apiKey).apiSecret(apiSecret);
        if (StringUtils.isNotBlank(callback)) {
            builder.callback(callback);
        }
        service = builder.build();

        this.teamName = teamName;
    }

    public Token createRquestToken() {
        return service.getRequestToken();
    }

    public String createAuthorizationCodeURL(Token requestToken) {
        return service.getAuthorizationUrl(requestToken);
    }

    public Token getTokenByAuthorizationCode(String code, Token requestToken) {
        Verifier v = new Verifier(code);
        return service.getAccessToken(requestToken, v);
    }

    public boolean verifyTeamMembership(Token accessToken) {
        OAuthRequest request = new OAuthRequest(Verb.GET, API_ENDPOINT + "teams");
        service.signRequest(accessToken, request);
        Response response = request.send();
        String json = response.getBody();
        Gson gson = new Gson();
        BitbucketUserResponce[] teams = gson.fromJson(json, BitbucketUserResponce[].class);
        if (teams == null || teams.length == 0) {
            return false;
        }
        for (BitbucketUserResponce team : teams) {
            if (teamName.equals(team.user.getUsername())) {
                return true;
            }
        }

        return false;
    }

    public BitbucketUser getUserByToken(Token accessToken) {
        OAuthRequest request = new OAuthRequest(Verb.GET, API_ENDPOINT + "user");
        service.signRequest(accessToken, request);
        Response response = request.send();
        String json = response.getBody();
        Gson gson = new Gson();
        BitbucketUserResponce userResponce = gson.fromJson(json, BitbucketUserResponce.class);
        if (userResponce == null) {
            return null;
        }

        if (!verifyTeamMembership(accessToken)) {
            return null;
        }

        return userResponce.user;
    }

    public UserDetails getUserByUsername(String username) {
        InputStreamReader reader = null;
        BitbucketUserResponce userResponce = null;
        try {
            URL url = new URL(API_ENDPOINT + "users/" + username);
            reader = new InputStreamReader(url.openStream(), "UTF-8");
            Gson gson = new Gson();
            userResponce = gson.fromJson(reader, BitbucketUserResponce.class);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(reader);
        }

        if (userResponce != null) {
            return userResponce.user;
        } else {
            return null;
        }
    }

}
