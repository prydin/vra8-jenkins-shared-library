import com.cloudbees.groovy.cps.NonCPS
import net.virtualviking.vra8jenkins.VRAClient

import java.util.concurrent.TimeoutException

class  VRA8 implements Serializable {
    private static ThreadLocal logger = new ThreadLocal()

    private VRAClient client
    VRA8(steps, String url, String token) {
        client = new VRAClient(url, token)
        logger.set(steps)
    }

    @NonCPS
    static def log(String s) {
        logger.get().println(s)
    }

    /**
     * Initializes deployment from a catalog item and waits for it to complete.
     *
     * @param catalogItem The name of the catalog item as it appears in vRA
     * @param version The version og the catalog item
     * @param projectName The project name under which to deploy the item
     * @param deploymentName The name of the deployment. Auto generated if blank
     * @param reason A description of the deployment. May be left blank
     * @param timeout Timeout waiting for the deployment to finish, in seconds
     * @return A deployment record as described here: https://code.vmware.com/apis/979#/Deployments
     */
    @NonCPS
    def deployFromCatalog(String catalogItem, String version, String projectName, String deploymentName = null, String reason = null, long timeout = 300) {
        log("Entering deployFromCatalog")
        def dep = deployFromCatalogNoWait(catalogItem, version, projectName, deploymentName, reason)
        return client.waitForDeployment(dep.deploymentId, timeout * 1000)
    }

    /**
     * Initializes deployment from a catalog item and continues without waiting for it to complete.
     *
     * @param catalogItem The name of the catalog item as it appears in vRA
     * @param version The version og the catalog item
     * @param projectName The project name under which to deploy the item
     * @param deploymentName The name of the deployment. Auto generated if blank
     * @param reason A description of the deployment. May be left blank
     * @return A deployment record as described here: https://code.vmware.com/apis/979#/Deployments
     */
    @NonCPS
    def deployFromCatalogNoWait(String catalogItem, String version, String projectName, String deploymentName = null, String reason = null) {
        try {
            log("Entering deployFromCatalogNoWait")
            System.err.println("This is stderr!")
            if (deploymentName == null) {
                deploymentName = "Invoked from Jenkins " + UUID.randomUUID().toString()
            }
            def dep = client.provisionFromCatalog(catalogItem, version, projectName, deploymentName, reason)
            assert dep != null
            log("Exiting deployFromCatalogNoWait")
            return dep
        } catch(Exception e) {
            log(e.toString())
            throw e
        }
    }

    /**
     * Waits for a deployment to complete, either successfully or with a failure.
     * @param deploymentId The deployment id to wait for
     * @param timeout Timeout waiting for the deployment to finish, in seconds
     * @return A deployment record as described here: https://code.vmware.com/apis/979#/Deployments
     */
    @NonCPS
    def waitForDeployment(String deploymentId, long timeout = 300) {
        log("Entering waitForDeployment")
        def dep = client.waitForDeployment(deploymentId, timeout * 1000)
        assert dep != null
        log("Exiting waitForDeployment")
        return dep
    }

    /**
     * Waits for a resource within a deployment to be allocated an IP address. Sometimes, e.g. when
     * DHCP is used, the IP address can't be determined at the moment the deployment finishes. This method
     * should be used in such situations to make sure execution is paused until an IP address is available.
     * @param deploymentId The deployment id to wait for
     * @param resourceName The resource within that deployment
     * @param timeout A timeout in seconds
     * @return The IP address as a String
     */
    @NonCPS
    def waitForIPAddress(String deploymentId, String resourceName, long timeout = 300) {
        log("Entering waitForIPAddress")
        timeout *= 1000
        def start = System.currentTimeMillis()
        def dep = client.waitForDeployment(deploymentId, timeout)
        for(;;) {
            def resource = dep.resources.find { it.name == resourceName }
            if(resource == null) {
                continue
            }
            def ip = resource?.properties?.address
            if(ip != null) {
                log("Exiting waitForIPAddress")
                return ip
            }

            def remaining = timeout - (System.currentTimeMillis() - start)
            if(remaining <= 0) {
                throw new TimeoutException("Timeout while waiting for IP address")
            }
            Thread.sleep(Math.min(remaining, 30000))
            dep = client.getDeployment(deploymentId, true)
        }
    }

    /**
     * Deletes a deployment and destroys any resources associated with it without waiting for completion
     * @param deploymentId The deployment id to delete
     * @return A deployment record as described here: https://code.vmware.com/apis/979#/Deployments
     */
    @NonCPS
    def deleteDeploymentNoWait(String deploymentId) {
        log("Entering deleteDeploymentNoWait")
        def dep = client.deleteDeploymentNoWait(deploymentId)
        assert dep != null
        log("Exiting deleteDeploymentNoWait")
        return dep
    }

    /**
     * Deletes a deployment and destroys any resources associated with it and waits for completion
     * @param deploymentId The deployment id to delete
     * @param timeout A timeout in seconds
     * @return A deployment record as described here: https://code.vmware.com/apis/979#/Deployments
     */
    @NonCPS
    def deleteDeployment(String deploymentId, long timeout = 300) {
        log("Entering deleteDeployment")
        def dep = client.deleteDeployment(deploymentId, timeout * 1000)
        assert dep != null
        log("Exiting deleteDeployment")
        return dep
    }

    @NonCPS
    def deployCatalogItemFromYaml(String filename, long timeout = 300) {

    }
}
