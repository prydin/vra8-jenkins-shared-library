package net.virtualviking.vra8jenkins

import wslite.rest.ContentType
import wslite.rest.RESTClient

import java.util.concurrent.TimeoutException

class VRAClient {
    private static final int timeout = 30000

    private static final int deploymentPollInterval = 30000

    private static final String apiVersion = "2019-09-12"
    private String token

    private String url

    VRAClient(String url, String token) {
        if(url.endsWith("/")) {
            url = url.substring(0, url.length() - 1)
        }
        this.url = url
        this.token = post(url + "/iaas/api/login", [ refreshToken: token ], [apiVersion: apiVersion]).token
    }

    def getBlueprintByName(String name) {
        def bps = get(url + "/blueprint/api/blueprints", [ name: name, apiVersion: apiVersion ])
        checkRespoonseSingleton(bps)
        return bps.content[0]
    }

    def getCatalogItemByName(String name) {
        def cis = get(url + "/catalog/api/items", [ apiVersion: apiVersion, 'search': name, size: "100000" ])
        checkRespoonseSingleton(cis)
        cis.content.removeIf { it.name != name }
        cis.numberOfElements = cis.content.size()
        return cis.content[0]
    }

    def getProjectByName(String name) {
        def projs = get(url + "/iaas/api/projects", [ apiVersion: apiVersion, '$filter': "name eq '$name'"])
        checkRespoonseSingleton(projs)
        return projs.content[0]
    }

    def provisionFromCatalog(String ciName, String version, String project, String deploymentName, String reason, Map inputs = [:], int count = 1) {
        def ci = getCatalogItemByName(ciName)
        def ciId = ci.id

        def proj = getProjectByName(project)
        def projectId = proj.id

        def payload = [
                bulkRequestCount: count,
                deploymentName: deploymentName,
                reason: reason,
                projectId: projectId,
                inputs: inputs,
                version: version
        ]
        return post(url + "/catalog/api/items/$ciId/request", payload, [ apiVersion: apiVersion ])
    }

    def getDeployment(String deploymentId, boolean expandResources = false) {
        return get(url + "/deployment/api/deployments/$deploymentId", [ expandResources: expandResources.toString(), apiVersion: apiVersion])
    }

    def waitForDeployment(String deploymentId, long timeout = 60000) {
        def start = System.currentTimeMillis()
        for(;;) {
            def dep = getDeployment(deploymentId)
            if(!dep.status.endsWith("_INPROGRESS")) {
                return getDeployment(deploymentId, true)
            }
            def remaining = timeout - (System.currentTimeMillis() - start)
            if(remaining <= 0) {
                throw new TimeoutException();
            }
            Thread.sleep(Math.min(remaining, deploymentPollInterval))
        }
    }

    def deleteDeploymentNoWait(String deploymentId) {
        return delete(url + "/deployment/api/deployments/$deploymentId", [ apiVersion: apiVersion ])
    }

    def deleteDeployment(String deploymentId, long timeout = 60000) {
        def dep = deleteDeploymentNoWait(deploymentId)
        assert dep != null
        return waitForDeployment(dep.id)
    }

    private post(String url, Map payload, Map query = null) {
        RESTClient client = new RESTClient(url)
        def response = client.post(query: query, headers : token != null ? [ "Authorization": "Bearer " + token] : null) {
            type ContentType.JSON
            json payload
        }
        assert response.statusCode == 200
        return response.json
    }

    private get(String url, Map query = null) {
        RESTClient client = new RESTClient(url)
        def response = client.get(
                accept: ContentType.JSON,
                headers: [ "Authorization": "Bearer " + token],
                query: query
        )
        assert response.statusCode == 200
        return response.json
    }

    private delete(String url, Map query = null) {
        RESTClient client = new RESTClient(url)
        def response = client.delete(
                accept: ContentType.JSON,
                headers: [ "Authorization": "Bearer " + token],
                query: query
        )
        assert response.statusCode == 200
        return response.json
    }

    private checkRespoonseSingleton(response) {
        assert response != null
        assert response.content != null
        assert response.content.size() == 1
    }
}
