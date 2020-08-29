package net.virtualviking.vra8jenkins

import com.cloudbees.groovy.cps.NonCPS
import groovy.json.JsonSlurper

import java.nio.charset.Charset
import java.util.concurrent.TimeoutException

import groovy.json.JsonOutput

class VRAClient implements Serializable {
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

    @NonCPS
    def getBlueprintByName(String name) {
        def bps = get(url + "/blueprint/api/blueprints", [ name: name, apiVersion: apiVersion ])
        checkRespoonseSingleton(bps)
        return bps.content[0]
    }

    @NonCPS
    def getCatalogItemByName(String name) {
        def cis = get(url + "/catalog/api/items", [ apiVersion: apiVersion, 'search': name, size: "100000" ])
        checkRespoonseSingleton(cis)
        cis.content.removeIf { it.name != name }
        cis.numberOfElements = cis.content.size()
        return cis.content[0]
    }

    @NonCPS
    def getProjectByName(String name) {
        def projs = get(url + "/iaas/api/projects", [ apiVersion: apiVersion, '$filter': "name eq '$name'"])
        checkRespoonseSingleton(projs)
        return projs.content[0]
    }

    @NonCPS
    def provisionFromCatalog(String ciName, String version, String project, String deploymentName, String reason, Map inputs = [:], int count = 1) {
        def ci = getCatalogItemByName(ciName)
        def ciId = ci.id
        System.err.println("Mapped catalog item $ciName to $ci.id")

        def proj = getProjectByName(project)
        def projectId = proj.id
        System.err.println("Mapped project name $project to $proj.id")

        def payload = [
                bulkRequestCount: count,
                deploymentName: deploymentName,
                reason: reason,
                projectId: projectId,
                inputs: inputs,
                version: version
        ]
        def dep = post(url + "/catalog/api/items/$ciId/request", payload, [ apiVersion: apiVersion ])
        System.err.println("Deployment request returned: " + JsonOutput.toJson(dep))
        return dep
    }

    @NonCPS
    def getDeployment(String deploymentId, boolean expandResources = false) {
        return get(url + "/deployment/api/deployments/$deploymentId", [ expandResources: expandResources.toString(), apiVersion: apiVersion])
    }

    @NonCPS
    def waitForDeployment(String deploymentId, long timeout = 60000) {
        def start = System.currentTimeMillis()
        for(;;) {
            def dep = getDeployment(deploymentId)
            if(dep != null || dep.status != null) {
                if (!dep.status.endsWith("_INPROGRESS")) {
                    return getDeployment(deploymentId, true)
                }
            }
            def remaining = timeout - (System.currentTimeMillis() - start)
            System.err.println("Waiting for deployment $remaining ms to timeout")
            if(remaining <= 0) {
                throw new TimeoutException("Timeout while waiting for deployment to finish");
            }
            Thread.sleep(Math.min(remaining, deploymentPollInterval))
        }
    }

    @NonCPS
    def deleteDeploymentNoWait(String deploymentId) {
        return delete(url + "/deployment/api/deployments/$deploymentId", [ apiVersion: apiVersion ])
    }

    @NonCPS
    def deleteDeployment(String deploymentId, long timeout = 60000) {
        def dep = deleteDeploymentNoWait(deploymentId)
        assert dep != null
        return waitForDeployment(deploymentId)
    }

    @NonCPS
    private post(String url, Map payload, Map query = null) {
        if(query != null) {
           url += buildQueryString(query)
        }
        def u = new URL(url)
        def conn = u.openConnection()
        conn.setRequestMethod("POST")
        conn.setRequestProperty("Authorization", "Bearer "+ token)
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setDoOutput(true)
        def writer = new OutputStreamWriter(conn.outputStream);
        writer.write(JsonOutput.toJson(payload));
        writer.flush();
        writer.close();
        conn.connect()

        assert conn.responseCode == 200
        return new JsonSlurper().parse(conn.content)
    }

    @NonCPS
    private get(String url, Map query = null) {
        if(query != null) {
            url += buildQueryString(query)
        }
        def u = new URL(url)
        def conn = u.openConnection()
        conn.setRequestMethod("GET")
        conn.setRequestProperty("Authorization", "Bearer "+ token)
        conn.setRequestProperty("Accept", "application/json")
        conn.connect()
        assert conn.responseCode == 200
        return new JsonSlurper().parse(conn.content)
    }

    @NonCPS
    private delete(String url, Map query = null) {
        if(query != null) {
            url += buildQueryString(query)
        }
        def u = new URL(url)
        def conn = u.openConnection()
        conn.setRequestMethod("DELETE")
        conn.setRequestProperty("Authorization", "Bearer "+ token)
        conn.setRequestProperty("Accept", "application/json")
        conn.connect()
        assert conn.responseCode == 200
        return new JsonSlurper().parse(conn.content)
    }

    @NonCPS
    private checkRespoonseSingleton(response) {
        assert response != null
        assert response.content != null
        assert response.content.size() == 1
    }

    @NonCPS
    private buildQueryString(Map q) {
        def s = "?"
        q.eachWithIndex { k, v, i ->
            if(i > 0) {
                s += "&"
            }
            s += k + "=" + URLEncoder.encode(v, Charset.defaultCharset())
        }
        return s
    }
}
