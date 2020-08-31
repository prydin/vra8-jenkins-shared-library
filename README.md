# vra8-jenkins-shared-library
A Jenkins shared library for interacting with vRealize Automation 8.x

This library contains functions for creating, deleting and examining deployments of vRealize Automation catalog items.

## Examples

### Deploy using hardcoded parameters

```groovy
@Library('vra8@master')_

def vmIp 
def vra
withCredentials([string(credentialsId: 'vRACloudToken', variable: 'vraToken')]) {
  vra = new VRA8(this, "https://api.mgmt.cloud.vmware.com", "$vraToken")
}

pipeline {
  agent any
  stages {
    stage('Build') {
      steps {
        sh './gradlew build'
      }
    }

    stage('Test') {
      steps {
        sh './gradlew test'
        junit 'build/test-results/test/*.xml'
      }
    }

    stage('Publish') {
      steps {
        archiveArtifacts(artifacts: 'build/libs/zipcode-*.jar ', fingerprint: true, onlyIfSuccessful: true)
      }
    }
    
    stage('Deploy') {
      steps {
        script {
          def dep = vra.deployFromCatalog('plain-ubuntu-18', '6', 'Pontus Project', 'Invoked from Jenkins ' + System.currentTimeMillis())
          assert dep != null
          vmIp = vra.waitForIPAddress(dep.id)
        }
        echo "Address of machine is: $vmIp"
     }
   }
 }
}
```

### Deploy from an infrastructure description file

```groovy
@Library('vra8@master')_

def vmIp 
def vra
withCredentials([string(credentialsId: 'vRACloudToken', variable: 'vraToken')]) {
  vra = new VRA8(this, "https://api.mgmt.cloud.vmware.com", "$vraToken")
}

pipeline {
  agent any
  stages {
    stage('Build') {
      steps {
        sh './gradlew build'
      }
    }

    stage('Test') {
      steps {
        sh './gradlew test'
        junit 'build/test-results/test/*.xml'
      }
    }

    stage('Publish') {
      steps {
        archiveArtifacts(artifacts: 'build/libs/zipcode-*.jar ', fingerprint: true, onlyIfSuccessful: true)
      }
    }
    
    stage('Deploy') {
      steps {
        script {
          def dep = vra.deployCatalogItemFromConfig(readYaml(file: './infrastructure.yaml'))
          assert dep != null
          vmIp = vra.waitForIPAddress(dep.id, 'UbuntuMachine')
        }
        echo "Address of machine is: $vmIp"
     }
   }
 }
}
```

infrastructure.yaml:
```yaml
  
count: 1
catalogItem: "plain-ubuntu-18"
version: "6"
deploymentName: "vexpress-zipcode-#"
project: "Pontus Project"
reason: "Deployment from Jenkins pipeline"
inputs:
  user: "testuser"
```
