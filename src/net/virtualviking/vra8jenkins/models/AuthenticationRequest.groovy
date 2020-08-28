package net.virtualviking.vra8jenkins.models

class AuthenticationRequest {
    private String username;

    private String password;

    AuthenticationRequest(String username, String password) {
        this.username = username
        this.password = password
    }

    String getUsername() {
        return username
    }

    void setUsername(String username) {
        this.username = username
    }

    String getPassword() {
        return password
    }

    void setPassword(String password) {
        this.password = password
    }
}
