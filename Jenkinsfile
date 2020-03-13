pipeline{

    agent{
        // label "" also could have been 'agent any' - that has the same meaning.
        label "master"
    }

    environment {
        // Global Vars

        JWT_PUBLIC_KEY_URL="http://sso-cluster-internal:8080/auth/realms/omp/protocol/openid-connect/certs"

        NAMESPACE_PREFIX="omp"

        PIPELINES_NAMESPACE = "${NAMESPACE_PREFIX}-ci-cd"
        APP_NAME = "omp-backend"

        JENKINS_TAG = "${JOB_NAME}.${BUILD_NUMBER}".replace("/", "-")
        JOB_NAME = "${JOB_NAME}".replace("/", "-")

        DISPLAY = 0

        pom = readMavenPom file: 'pom.xml'

        ARTIFACTID = pom.getArtifactId();
        VERSION = pom.getVersion();

        RELEASE = false
    }

    // The options directive is for configuration that applies to the whole job.
    options {
        buildDiscarder(logRotator(numToKeepStr:'10'))
        timeout(time: 15, unit: 'MINUTES')
        ansiColor('xterm')
        timestamps()
    }

    stages{
        stage("Prepare environment for master deploy") {
            agent {
                node {
                    label "master"
                }
            }
            steps {
                script {
                    // Arbitrary Groovy Script executions can do in script tags
                    env.PROJECT_NAMESPACE = "${NAMESPACE_PREFIX}-test"
                    env.NODE_ENV = "test"
                    env.QUARKUS_PROFILE = "openshift-test"
                    env.RELEASE = true
                }
            }
        }
        stage("Ansible") {
            agent {
                node {
                    label "jenkins-slave-ansible"
                }
            }
            stages{
                stage("Ansible Galaxy") {
                    steps {
                        echo '### Ansible Galaxy Installing Requirements ###'
                        sh "ansible-galaxy install -r .applier/requirements.yml --roles-path=.applier/roles"
                    }
                }
                stage("Apply Inventory using Ansible-Playbook") {
                    steps {
                        echo '### Apply Inventory using Ansible-Playbook ###'
                        sh "ansible-playbook .applier/apply.yml -i .applier/inventory/ -e include_tags=build"
                    }
                }
            }
        }
        stage("Test/Build/Nexus/OpenShift Build"){
            agent {
                node {
                    label "jenkins-slave-mvn"
                }
            }
            stages{
                stage("Print ArtifactID and Version"){
                    steps{
                        script{
                            // Arbitrary Groovy Script executions can do in script tags
                            echo "Version ::: ${VERSION}"
                            echo "artifactId ::: ${ARTIFACTID}"
                        }
                    }
                }
                stage("Test Project"){
                    steps {
                        sh 'printenv'

                        echo '### Running tests ###'
                        sh './mvnw clean test'
                    }
                }
                stage("Build Project"){
                    steps{
                        echo '### Running install ###'
                        sh './mvnw clean install'
                    }
                }
                stage("Static Analysis"){
                    steps{
                        echo '### Maven Static Code Analysis ###'
                        sh './mvnw checkstyle:checkstyle'
                        sh './mvnw org.jacoco:jacoco-maven-plugin:prepare-agent install -Dmaven.test.failure.ignore=true'
                        
            
                    }
                }
//                stage("Sonar Quality Gate"){
//                    steps {
//                        timeout(time: 1, unit: 'HOURS') {
//                            waitForQualityGate abortPipeline: false
//                        }
//                    }
//                }
                stage("Deploy to Nexus"){
                    when {
                        expression { currentBuild.result != 'UNSTABLE' }
                    }
                    steps{
                        echo '### Running deploy ###'
//                        sh './mvnw deploy'
//
                    }
                    // Post can be used both on individual stages and for the entire build.
                    post {
                        always{
                            archive "**"
                            junit testResults: '**/target/surefire-reports/TEST-*.xml'

                            // Notify slack or some such
                        }
                        success {
                            echo "SUCCESS"
                        }
                        failure {
                            echo "FAILURE"
                        }
                    }
                }
            }

            stage("Build a Container Image and Push it to Quay") {
                agent {
                    node {
                        label "master"  
                    }
                }
                when {
                    expression { GIT_BRANCH ==~ /(.*tags\/release.*)/  }
                }
                steps {
                    
                    echo '### Create Container Image ###'
                    sh  '''
                            oc project ${PIPELINES_NAMESPACE} # probs not needed
                            oc patch bc ${APP_NAME} -p "{\\"spec\\":{\\"output\\":{\\"to\\":{\\"kind\\":\\"DockerImage\\",\\"name\\":\\"quay.io/open-innovation-labs/${APP_NAME}:${JENKINS_TAG}\\"}}}}"
                            oc start-build ${APP_NAME} --from-file=target/${ARTIFACTID}-${VERSION}-runner.jar --follow
                        '''
                }
                post {
                    always {
                        archive "**"
                    }
                }
            }

            stage("Build a Container Image") {
                agent {
                    node {
                        label "master"  
                    }
                }
                when {
                    expression { GIT_BRANCH ==~ /(.*master)/  }
                }
                steps {
                    
                    echo '### Create Container Image ###'
                    sh  '''
                            oc project ${PIPELINES_NAMESPACE} # probs not needed
                            oc patch bc ${APP_NAME} -p "{\\"spec\\":{\\"output\\":{\\"to\\":{\\"kind\\":\\"ImageStreamTag\\",\\"name\\":\\"${APP_NAME}:${JENKINS_TAG}\\"}}}}"
                            oc start-build ${APP_NAME} --from-file=target/${ARTIFACTID}-${VERSION}-runner.jar --follow
                        '''
                }
                post {
                    always {
                        archive "**"
                    }
                }
            }

        }

    }
}
