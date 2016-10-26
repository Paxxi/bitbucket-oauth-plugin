package org.jenkinsci.plugins;

import hudson.security.SecurityRealm;

import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.providers.AbstractAuthenticationToken;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.api.BitbucketApiService;
import org.jenkinsci.plugins.api.BitbucketUser;
import org.scribe.model.Token;

public class BitbucketAuthenticationToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = -7826610577724673531L;

    private Token accessToken;
    private BitbucketUser bitbucketUser;
    private String teamName;

    public BitbucketAuthenticationToken(Token accessToken, String apiKey, String apiSecret, String teamName) {
        this.accessToken = accessToken;
        this.bitbucketUser = new BitbucketApiService(apiKey, apiSecret, teamName).getUserByToken(accessToken);
        this.teamName = teamName;
        
        boolean authenticated = false;

        if (bitbucketUser != null) {
            authenticated = true;
        }

        setAuthenticated(authenticated);
    }

    @Override
    public GrantedAuthority[] getAuthorities() {
        return this.bitbucketUser != null ? this.bitbucketUser.getAuthorities() : new GrantedAuthority[0];
    }

    /**
     * @return the accessToken
     */
    public Token getAccessToken() {
        return accessToken;
    }

    @Override
    public Object getCredentials() {
        return StringUtils.EMPTY;
    }

    @Override
    public Object getPrincipal() {
        return getName();
    }

    @Override
    public String getName() {
        return (bitbucketUser != null ? bitbucketUser.getUsername() : null);
    }

}
