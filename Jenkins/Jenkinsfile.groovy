String[] LW_HE_CHANGES = params.GERRIT_PATCHSET_LW_HE.split('/')
String[] ONBOARDING_CHANGES = params.GERRIT_PATCHSET_ONBOARDING.split('/')


pipeline {
    options {
        disableConcurrentBuilds()
    }

    agent {
        node {
            label 'cnam'
        }
    }

    environment {
        FUNC_NAMESPACE = 'test-light-helmfile'
        GERRIT_ID = 'cdbc7c5b-9a86-4074-a15e-3e6558998860'
        KUBE_CONF_CI_JENKINS = 'ccd-hahn117.kube.conf'
        KUBE_CONF_NAME = 'kube_config'
        LW_HE_DIR = './lcm-helm-orchestrator'
        LW_HE_DOCKER = "armdocker.rnd.ericsson.se/proj-ra-cnam/eric-lcm-helm-executor"
        LW_HE_REPO = 'gerrit.ericsson.se/a/OSS/com.ericsson.orchestration.mgmt/eric-lcm-helm-orchestrator'
        ONBOARDING_DIR = './cnam-onboarding'
        ONBOARDING_DOCKER = 'armdocker.rnd.ericsson.se/proj-ra-cnam/eric-cnam-onboarding'
        ONBOARDING_REPO = 'gerrit.ericsson.se/a/OSS/com.ericsson.orchestration.mgmt/eric-cnam-onboarding'
    }

    stages {
        stage('Verify') {
            steps {
                script {
                    if (GERRIT_REFSPEC.contains('changes')){
                        sh "mvn -s mvn-settings.xml clean install"
                    } else {
                        print "Skipping this step ..."
                    }
                }
            }
        }
        stage('Preperation') {
            steps {
                withCredentials([ usernamePassword(credentialsId: "${GERRIT_ID}", usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD') ])
                {
                    script {
                        if ( LW_HE_CHANGES.contains('master') ){
                            sh "git clone --single-branch -b master https://${GERRIT_USERNAME}:${GERRIT_PASSWORD}@${LW_HE_REPO} ${LW_HE_DIR}"
                        } else {
                            String[] lw_he_refspec = sh(returnStdout: true, script: "ssh -p 29418 gerrit.ericsson.se gerrit query --current-patch-set --format=json change:" +LW_HE_CHANGES[5] ).split('\n')
                            def lw_he_ref = readJSON text: lw_he_refspec[0]
                            sh "git --git-dir ${LW_HE_DIR} --work-tree ${LW_HE_DIR} init"
                            sh "git --git-dir ${LW_HE_DIR} --work-tree ${LW_HE_DIR} fetch https://${GERRIT_USERNAME}:${GERRIT_PASSWORD}@gerrit.ericsson.se/a/OSS/com.ericsson.orchestration.mgmt/eric-lcm-helm-orchestrator ${lw_he_ref.currentPatchSet.ref}"
                            sh "git --git-dir ${LW_HE_DIR} --work-tree ${LW_HE_DIR} checkout FETCH_HEAD"
                        }
                        if ( ONBOARDING_CHANGES.contains('master') ){
                            sh "git clone --single-branch -b master https://${GERRIT_USERNAME}:${GERRIT_PASSWORD}@${ONBOARDING_REPO} ${ONBOARDING_DIR}"
                        } else{
                            String[] onboarding_refspec = sh(returnStdout: true, script: "ssh -p 29418 gerrit.ericsson.se gerrit query --current-patch-set --format=json change:" +ONBOARDING_CHANGES[5] ).split('\n')
                            def onboarding_ref = readJSON text: onboarding_refspec[0]
                            sh "git --git-dir ${ONBOARDING_DIR} --work-tree ${ONBOARDING_DIR} init"
                            sh "git --git-dir ${ONBOARDING_DIR} --work-tree ${ONBOARDING_DIR} fetch https://${GERRIT_USERNAME}:${GERRIT_PASSWORD}@gerrit.ericsson.se/a/OSS/com.ericsson.orchestration.mgmt/eric-cnam-onboarding ${onboarding_ref.currentPatchSet.ref}"
                            sh "git --git-dir ${ONBOARDING_DIR} --work-tree ${ONBOARDING_DIR} checkout FETCH_HEAD"
                        }
                    }
                }
            }
        }
        stage('Run Onboarding apps') {
            steps {
                script {
                    if (ONBOARDING_CHANGES.contains('master')) {
                        ONBOARDING_IMAGE = readFile "${ONBOARDING_DIR}/DROP_VERSION"
                    } else {
                        def onboard_verion = sh(returnStdout: true, script: "git --git-dir ${ONBOARDING_DIR} --work-tree ${ONBOARDING_DIR} rev-parse --short HEAD")
                        def onboarding_pref = readFile "${ONBOARDING_DIR}/VERSION_PREFIX"
                        ONBOARDING_IMAGE = ONBOARDING_DOCKER + ':' + onboarding_pref + '-' + onboard_verion
                    }
                    sh '''
                        docker network create git-repo-tests;
                        docker build -t ci-git-repo ${ONBOARDING_DIR}/local_env/dependencies/git-repo/;
                        docker run --rm -d --env-file ${ONBOARDING_DIR}/Docker/CI/ci_env_variables --network git-repo-tests --name ci-git-repo ci-git-repo
                    '''
                    sh 'docker run -d --env-file ${ONBOARDING_DIR}/Docker/CI/ci_env_variables --name ci-onboarding -p 8681:8080 --network git-repo-tests -v /etc/ssl/certs/ca-bundle.crt:/var/lib/ca-certificates/ca-bundle.pem ' + ONBOARDING_IMAGE
                }
            }
            post {
                failure {
                    sh "docker logs ci-onboarding > ci-onboarding.log; docker stop ci-git-repo ci-onboarding"
                    archiveArtifacts 'ci-onboarding.log'
                }
            }
        }
        stage('Run LW_HE apps') {
            steps {
                script {
                     if ( LW_HE_CHANGES.contains('master') ){
                        LW_HE_VERSION = readYaml file: "./lcm-helm-orchestrator/charts/eric-lcm-helm-executor/Chart.yaml"
                     } else {
                        def lw_he_suf = sh(returnStdout: true, script: "git --git-dir ${LW_HE_DIR} --work-tree ${LW_HE_DIR} rev-parse --short HEAD")
                        def lw_he_pref = readFile "${LW_HE_DIR}/VERSION_PREFIX"
                        def lw_he_ver = lw_he_pref + '-' + lw_he_suf
                        LW_HE_VERSION = [version: lw_he_ver]
                     }
                     sh 'docker run -d --env-file ${ONBOARDING_DIR}/Docker/CI/ci_env_variables --name lw_he --network git-repo-tests -p 8008:8888 -v /etc/ssl/certs/ca-bundle.crt:/var/lib/ca-certificates/ca-bundle.pem ' + LW_HE_DOCKER + ':' + LW_HE_VERSION.version
                }
            }
            post {
                failure {
                    sh "docker logs lw_he > lw_he.log; docker stop ci-git-repo ci-onboarding lw_he"
                    archiveArtifacts 'lw_he.log'

                }
            }
        }
        stage('Integration test') {
            steps {
                script {
                    withCredentials([
                            file(credentialsId: "${KUBE_CONF_CI_JENKINS}", variable: 'KUBE_CONF'),
                    ])
                    {   writeFile file: ".${KUBE_CONF_NAME}", text: readFile(KUBE_CONF)
                        sh '''
                        sleep 10
                        mvn -s mvn-settings.xml clean test -Pacceptance -Dsurefire.suiteXmlFiles=src/main/resources/suites/tests.xml \
                        -Donboarding.host=localhost -Donboarding.port=8681 -Dexecutor.host=localhost -Dexecutor.port=8008 \
                        -Dfunc_namespace=${FUNC_NAMESPACE} -Dkubeconfig_path=${PWD}/.${KUBE_CONF_NAME}
                        '''
                    }

                }
            }
            post {
                failure {
                    sh "docker logs lw_he > lw_he.log; docker logs ci-onboarding > ci-onboarding.log;"
                    archiveArtifacts 'ci-onboarding.log'
                    archiveArtifacts 'lw_he.log'
                }
            }
        }
    }
    post {
        always {
            cleanWs()
            script {
                sh "docker stop ci-git-repo ci-onboarding lw_he"
                sh "docker rm ci-onboarding lw_he"
                sh "docker network rm git-repo-tests"
            }
        }
    }
}
