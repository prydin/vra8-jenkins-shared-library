package net.virtualviking.vr8jenkins

import net.virtualviking.vra8jenkins.VRAClient
import spock.lang.Specification
import wslite.rest.RESTClientException

class VRAClientTest extends Specification {
    private VRAClient cachedClient

    def getClient() {
        if(cachedClient != null) {
            return cachedClient
        }
        cachedClient = new VRAClient(System.getenv("VRA_URL"), System.getenv("VRA_TOKEN"))
        return cachedClient
    }

    def "test login"() {
        def url = System.getenv("VRA_URL")
        if(url == null) {
            System.err.println("VRA_URL not defined. Skipping test")
            return
        }
        def token = System.getenv("VRA_TOKEN")
        def vra = new VRAClient(url, token)

        expect:
        vra != null
        vra.token != null
    }

    def "test get blueprint"() {
        def url = System.getenv("VRA_URL")
        if(url == null) {
            System.err.println("VRA_URL not defined. Skipping test")
            return
        }
        def client = getClient()
        def bp = client.getBlueprintByName("plain-ubuntu-18")

        expect:
        bp != null
        bp.name == "plain-ubuntu-18"
    }

    def "test get catalog item"() {
        def url = System.getenv("VRA_URL")
        if(url == null) {
            System.err.println("VRA_URL not defined. Skipping test")
            return
        }
        def client = getClient()
        def ci = client.getCatalogItemByName("plain-ubuntu-18")

        expect:
        ci != null
        ci.name == "plain-ubuntu-18"
    }

    def "test get project"() {
        def url = System.getenv("VRA_URL")
        if(url == null) {
            System.err.println("VRA_URL not defined. Skipping test")
            return
        }
        def client = getClient()
        def proj = client.getProjectByName("Pontus Project")

        expect:
        proj != null
        proj.name == "Pontus Project"
    }

    def "test provision from catalog"() {
        def url = System.getenv("VRA_URL")
        if (url == null) {
            System.err.println("VRA_URL not defined. Skipping test")
            return
        }
        def client = getClient()
        def response = client.provisionFromCatalog("plain-ubuntu-18", "4", "Pontus Project", "Test " + UUID.randomUUID().toString(), "API Test")
        assert response != null
        assert response.deploymentId != null
        assert response.deploymentName.startsWith("Test ")

        String dId = response.deploymentId
        def dep = client.getDeployment(dId)
        assert dep != null
        assert dep.status != null

        def status = client.waitForDeployment(dId, 300000)
        assert status != null
        assert status.status == "CREATE_SUCCESSFUL"

        status = client.deleteDeployment(dId, 300000)

        expect:
        status != null
        status.status == "CREATE_SUCCESSFUL"
    }
}
