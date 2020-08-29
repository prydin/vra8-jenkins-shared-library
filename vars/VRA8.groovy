import net.virtualviking.vra8jenkins.VRAClient

class  VRA8 implements Serializable {
    private VRAClient client
    VRA8(String url, String token) {
        client = new VRAClient(url, token)
    }

    def deployFromCatalog(String catalogItem, String version, String projectName, String deploymentName = null, String reason = null, long timeout = 300) {
        if(deploymentName == null) {
            deploymentName = "Jenkins " + UUID.randomUUID().toString()
        }
        def dep = client.provisionFromCatalog(catalogItem, version, projectName, deploymentName, reason)
        assert dep != null
        client.waitForDeployment(dep.deploymentId, timeout * 1000)
    }
}
