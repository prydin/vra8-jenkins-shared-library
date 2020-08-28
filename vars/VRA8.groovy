import net.virtualviking.vra8jenkins.VRAClient

class VRA8 implements Serializable {
    private VRAClient client
    VRA8(String url, String token) {
        client = new VRAClient(url, token)
    }

    deployFromCatalog(String catalogItem, String version, String projectName, String deploymentName = null, long timeout = 300) {
        if(deploymentName == null) {
            deploymentName = "Jenkins " + UUID.randomUUID().toString()
        }
        def dep = client.provisionFromCatalog(catalogItem, verson, project, deploymentName)
        assert dep != null
        client.waitForDeployment(dep, timeout * 1000)
    }
}
