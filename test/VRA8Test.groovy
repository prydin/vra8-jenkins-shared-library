import org.yaml.snakeyaml.Yaml
import spock.lang.Specification

class VRA8Test extends Specification {
    private VRA8 cachedInstance

    private VRA8 getInstance() {
        if (cachedInstance != null) {
            return cachedInstance
        }
        cachedInstance = new VRA8(System.err, System.getenv("VRA_URL"), System.getenv("VRA_TOKEN"))
    }

    def "test instantiation"() {
        if (System.getenv("VRA_URL") == null) {
            System.err.println("VRA_URL not defined. Skipping test")
            return
        }
        def instance = getInstance()

        expect:
        instance != null
    }

    def "test deployment"() {
        if (System.getenv("VRA_URL") == null) {
            System.err.println("VRA_URL not defined. Skipping test")
            return
        }
        def vra = getInstance()
        def dep = vra.deployFromCatalog("plain-ubuntu-18", "6", "Pontus Project", "Test " + UUID.randomUUID().toString(), "API Test")
        assert dep != null
        assert dep.id != null
        def ip = vra.waitForIPAddress(dep.id, "UbuntuMachine")
        dep = vra.deleteDeployment(dep.id)

        expect:
        ip != null
        ip =~ "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\$"
        dep != null
    }

    def "test yaml deployment"() {
        if (System.getenv("VRA_URL") == null) {
            System.err.println("VRA_URL not defined. Skipping test")
            return
        }
        def yaml = """
count: 1
catalogItem: "plain-ubuntu-18"
deploymentName: "vexpress-zipcode-#"
project: "Pontus Project"
reason: "Deployment from Jenkins pipeline"
version: "6"
inputs:
  user: "demouser"
"""
        def payload = new Yaml().load(yaml)
        def vra = getInstance()
        def dep = vra.deployCatalogItemFromConfig(payload)
        assert dep != null
        assert dep.id != null
        def ip = vra.waitForIPAddress(dep.id, "UbuntuMachine")
        dep = vra.deleteDeployment(dep.id)

        expect:
        ip != null
        ip =~ "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\$"
        dep != null
    }
}
