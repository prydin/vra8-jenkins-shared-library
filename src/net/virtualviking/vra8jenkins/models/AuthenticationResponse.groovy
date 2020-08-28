package net.virtualviking.vra8jenkins.models

class AuthenticationResponse {
    String grant_type

    String refresh_token

    String code

    String state

    String redirect_uri

    String client_id

    String client_secret

    String scope

    String orgId

    AuthenticationResponse(String grant_type, String refresh_token, String code, String state, String redirect_uri, String client_id, String client_secret, String scope, String orgId) {
        this.grant_type = grant_type
        this.refresh_token = refresh_token
        this.code = code
        this.state = state
        this.redirect_uri = redirect_uri
        this.client_id = client_id
        this.client_secret = client_secret
        this.scope = scope
        this.orgId = orgId
    }

    String getGrant_type() {
        return grant_type
    }

    void setGrant_type(String grant_type) {
        this.grant_type = grant_type
    }

    String getRefresh_token() {
        return refresh_token
    }

    void setRefresh_token(String refresh_token) {
        this.refresh_token = refresh_token
    }

    String getCode() {
        return code
    }

    void setCode(String code) {
        this.code = code
    }

    String getState() {
        return state
    }

    void setState(String state) {
        this.state = state
    }

    String getRedirect_uri() {
        return redirect_uri
    }

    void setRedirect_uri(String redirect_uri) {
        this.redirect_uri = redirect_uri
    }

    String getClient_id() {
        return client_id
    }

    void setClient_id(String client_id) {
        this.client_id = client_id
    }

    String getClient_secret() {
        return client_secret
    }

    void setClient_secret(String client_secret) {
        this.client_secret = client_secret
    }

    String getScope() {
        return scope
    }

    void setScope(String scope) {
        this.scope = scope
    }

    String getOrgId() {
        return orgId
    }

    void setOrgId(String orgId) {
        this.orgId = orgId
    }
}
