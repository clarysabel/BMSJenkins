
properties([[$class: 'BuildDiscarderProperty',
             strategy: [$class: 'LogRotator', numToKeepStr: '5']],
             [$class: 'ParametersDefinitionProperty', parameterDefinitions:
[[$class: 'StringParameterDefinition', defaultValue: 'master', description: 'Branch Name', name : 'branchName'],
[$class: 'BooleanParameterDefinition', defaultValue: true, description: 'Run Tests', name: 'runTests']]]])

node {

  cleanup ()

  def MIDDLEWARE_FORK_BRANCH = '*/v3.x-dual-db'
  def masterBranch = '*/master'

  def repositories = ["Middleware", "Commons", "GermplasmStudyBrowser", "BreedingManager", "Fieldbook", "Workbench",
              "WebService", "DBScripts", "BMSAPI", "BMSConfig", "Migrator3to4", "GDMS", "WorkbenchLauncher"] as String[]

  stage ('Checkout MiddlewareFork') {
    checkout([$class: 'GitSCM', branches: [[name: MIDDLEWARE_FORK_BRANCH]], doGenerateSubmoduleConfigurations: false,
      extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'MiddlewareFork']], submoduleCfg: [],
      userRemoteConfigs: [[url: 'private-github:digitalabs/MiddlewareFork.git']]])
  }

  for (i=0; i<repositories.size(); i++) {
    def repository=repositories[i]

    stage ('Checkout '+repository) {
      checkout([$class: 'GitSCM', branches: [[name: masterBranch]], doGenerateSubmoduleConfigurations: false,
        extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: repository]], submoduleCfg: [],
        userRemoteConfigs: [[url: getGitURL(repository)]]])

      dir(repository) {
        def branchExists = sh (
            script: 'git rev-parse --verify origin/' + branchName,
            returnStatus: true
        )==0

  	    if (branchExists) {
          echo ("Checking out branch " + branchName + " in " + repository)
          sh 'git checkout -b ' + branchName + ' origin/' + branchName
          sh 'git merge origin/master'
  	    } else {
  	      echo ("Sticking to master since " + branchName?.trim() + " was not found.")
  	    }
      }
    }
  }

  stage ("Running "+runTests?"tests":"installation") {
    def mvnHome = tool 'Local Maven3'

    dir('BMSConfig') {
        if (runTests) {
          sh "${mvnHome}/bin/mvn clean test -Duser.name=ci"
        } else {
          sh "${mvnHome}/bin/mvn clean install -Dmaven.test.skip -Duser.name=ci"
        }
    }
  }
}

String getGitURL (repository) {
  return 'private-github:IntegratedBreedingPlatform/' + repository + '.git'
}

void cleanup () {
 sh 'rm -rf *'
}
