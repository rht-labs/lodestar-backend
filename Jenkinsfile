pipeline{

    agent{
        // label "" also could have been 'agent any' - that has the same meaning.
        label "master"
    }

    environment {
        // Global Vars

        https://github.com/rht-labs/open-management-portal-backend
        NAMESPACE_PREFIX="labs"
        GIT_DOMAIN = "github.com"
        GIT_ORG = "rht-labs"

        PIPELINES_NAMESPACE = "${NAMESPACE_PREFIX}-ci-cd"
        APP_NAME = "open-management-portal-backend"

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
            when {
              expression { GIT_BRANCH ==~ /(.*master)/ }
            }
            steps {
                script {
                    // Arbitrary Groovy Script executions can do in script tags
                    env.PROJECT_NAMESPACE = "${NAMESPACE_PREFIX}-test"
                    env.NODE_ENV = "test"
                    env.SPRING_PROFILES_ACTIVE = "openshift-test"
                    env.RELEASE = true
                }
            }
        }
        stage("Prepare environment for develop deploy") {
            agent {
                node {
                    label "master"
                }
            }
            when {
              expression { GIT_BRANCH ==~ /(.*develop|.*feature.*)/ }
            }
            steps {
                script {
                    // Arbitrary Groovy Script executions can do in script tags
                    env.PROJECT_NAMESPACE = "${NAMESPACE_PREFIX}-dev"
                    env.NODE_ENV = "dev"
                    env.SPRING_PROFILES_ACTIVE = "openshift-dev"
                }
            }
        }
        stage("Ansible") {
            agent {
                node {
                    label "jenkins-slave-ansible"
                }
            }
            when {
              expression { GIT_BRANCH ==~ /(.*master|.*develop|.*feature.*)/ }
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
                    label "jenkins-slave-graal"
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
                        sh 'mvn clean test'
                    }
                }
                stage("Build Project"){
                    steps{
                        echo '### Running install ###'
                        sh 'mvn clean install'
                    }
                }
                stage("Static Analysis"){
                    steps{
                        echo '### Maven Static Code Analysis ###'
                        sh 'mvn checkstyle:checkstyle'
                        sh 'mvn org.jacoco:jacoco-maven-plugin:prepare-agent install -Dmaven.test.failure.ignore=true'
                        
            
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
//                        sh 'mvn deploy'
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
                stage("Start Openshift Build"){
                    when {
                        expression { currentBuild.result != 'UNSTABLE' }
                    }
                    steps{
                        echo '### Create Linux Container Image from package ###'
                        sh  '''
                                oc project ${PIPELINES_NAMESPACE} # probs not needed
                                oc patch bc ${APP_NAME} -p "{\\"spec\\":{\\"output\\":{\\"to\\":{\\"kind\\":\\"ImageStreamTag\\",\\"name\\":\\"${APP_NAME}:${JENKINS_TAG}\\"}}}}"
                                oc start-build ${APP_NAME} --from-file=target/${ARTIFACTID}-${VERSION}.jar --follow
                            '''
                    }
                }
            }
        }
        stage("Openshift Deployment") {
            agent {
                node {
                    label "jenkins-slave-ansible"
                }
            }
            when {
                allOf{
                    expression { GIT_BRANCH ==~ /(.*master|.*develop|.*feature.*)/ }
                    expression { currentBuild.result != 'UNSTABLE' }
                }
            }
            steps {
                echo '### Apply Inventory using Ansible-Playbook ###'
                sh "ansible-playbook .applier/apply.yml -i .applier/inventory/ -e include_tags=dev"

                echo '### tag image for namespace ###'
                sh  '''
                    oc project ${PROJECT_NAMESPACE}
                    oc tag ${PIPELINES_NAMESPACE}/${APP_NAME}:${JENKINS_TAG} ${PROJECT_NAMESPACE}/${APP_NAME}:${JENKINS_TAG}
                    '''
                echo '### set env vars and image for deployment ###'
                sh '''
                    oc set env dc ${APP_NAME} NODE_ENV=${NODE_ENV}
                    oc set image dc/${APP_NAME} ${APP_NAME}=docker-registry.default.svc:5000/${PROJECT_NAMESPACE}/${APP_NAME}:${JENKINS_TAG}
                    oc label --overwrite dc ${APP_NAME} stage=${NODE_ENV}
                    oc patch dc ${APP_NAME} -p "{\\"spec\\":{\\"template\\":{\\"metadata\\":{\\"labels\\":{\\"version\\":\\"${VERSION}\\",\\"release\\":\\"${RELEASE}\\",\\"stage\\":\\"${NODE_ENV}\\",\\"git-commit\\":\\"${GIT_COMMIT}\\",\\"jenkins-build\\":\\"${JENKINS_TAG}\\"}}}}}"
                    oc rollout latest dc/${APP_NAME}
                '''
                echo '### Verify OCP Deployment ###'
                openshiftVerifyDeployment depCfg: env.APP_NAME,
                    namespace: env.PROJECT_NAMESPACE,
                    replicaCount: '1',
                    verbose: 'false',
                    verifyReplicaCount: 'true',
                    waitTime: '',
                    waitUnit: 'sec'
            }
        }
    }
}
