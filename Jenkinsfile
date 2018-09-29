#!groovy

def aServer = Artifactory.server('jfrog')
def rtMaven = Artifactory.newMavenBuild()
def buildInfo

pipeline {

    agent { node { label 'docker-host' } }

    // See https://jenkins.io/doc/book/pipeline/syntax/#options
    options {
        // See https://support.cloudbees.com/hc/en-us/articles/115000237071-How-do-I-set-discard-old-builds-for-a-Multi-Branch-Pipeline-Job
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '30'))
        disableConcurrentBuilds()
        timeout(time: 10, unit: 'MINUTES')
        timestamps()
    }

    stages {

        stage('Init') {
            steps {
                script { sh '''
                  echo "PATH = ${PATH}"
                  echo "M2_HOME = ${M2_HOME}"
                ''' }
            }
        }

        stage('Build') {
            steps {
                script {
                    rtMaven.resolver server: aServer, releaseRepo: 'svhk-libs-all', snapshotRepo: 'svhk-libs-all'
                    rtMaven.deployer server: aServer, releaseRepo: 'svhk-libs', snapshotRepo: 'svhk-libs'
                    rtMaven.tool = 'Default'
                    rtMaven.opts = '-Xms64m -Xmx64m -Djson-unit.libraries=gson'
                    buildInfo = rtMaven.run pom: 'pom.xml', goals: 'install'
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '*/target/surefire-reports/**.xml'
                }
            }
        }

        stage('Artifactory Publish') {
            steps {
                script {
                    aServer.publishBuildInfo buildInfo
                }
            }
        }

    }
}