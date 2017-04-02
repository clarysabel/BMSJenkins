node {

    // Mark the code checkout 'stage'....
    stage 'Checkout'

    // Middleware fork is special just checkit out in the begning
    sh 'rm -rf ./MiddlewareFork'
    checkout([$class: 'GitSCM', branches: [[name: '*/v3.x-dual-db']], doGenerateSubmoduleConfigurations: false,
        extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'MiddlewareFork']], submoduleCfg: [],
        userRemoteConfigs: [[url: 'private-github:digitalabs/MiddlewareFork.git']]])


    def masterBranch = '*/master'


    def repositories = ["Middleware", "Commons", "GermplasmStudyBrowser", "BreedingManager", "Fieldbook", "Workbench",
                "Workbench", "WebService", "DBScripts", "BMSAPI", "BMSConfig", "Migrator3to4", "GDMS", "WorkbenchLauncher"] as String[]


    for( int i = 0; i < repositories.size(); i++) {
        def repositoryName = repositories[i]

        echo "Processing Repository - '" + repositoryName + "'"

	    def gitUrl = 'private-github:IntegratedBreedingPlatform/' + repositoryName + '.git'
	    sh 'rm -rf ./' + repositoryName

	    echo "Git URL - '" + gitUrl + "'"

        checkout([$class: 'GitSCM', branches: [[name: masterBranch]], doGenerateSubmoduleConfigurations: false,
            extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: repositoryName]], submoduleCfg: [],
            userRemoteConfigs: [[url: gitUrl]]])

        dir(repositoryName) {
            // lets assume the branch exists
            sh 'echo 0 > branch_status'
            // Check if branch exists. If branch failes then record the branch status which is a number that
            // does not equal to 0
            sh 'git rev-parse --verify origin/' + branchName?.trim() +' || echo $? > branch_status'
            def branchStatus = readFile('branch_status').trim()

            echo "Branch Status - '" + branchStatus + "'"
    	    if (branchStatus.toInteger() == 0) {
                echo "Checking out branch '" + branchStatus + "'"
                sh 'git checkout -b ' + branchName + ' origin/' + branchName
                sh 'git merge origin/master'
    	    } else {
    	       echo "Sticking to master since '" + branchName?.trim() + "' not found."

    	    }
        }
    }

    def mvnHome = tool 'Local Maven3'

    dir('BMSConfig') {
        sh "${mvnHome}/bin/mvn clean test -Duser.name=ci"
    }

}
