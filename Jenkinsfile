#!groovy                                                                           
                                                                                   
properties(                                                                        
    [                                                                              
		buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10')),
		[$class: 'CopyArtifactPermissionProperty', projectNames: '*'],
		pipelineTriggers([[$class: 'PeriodicFolderTrigger', interval: '1d']])
    ]                                                                              
)                                                                                  

node {
	stage "Prep"

	deleteDir()
	def gradle = tool 'gradle'

	checkout scm

	stage "Compile"
	sh "${gradle}/bin/gradle assemble"
	sh "${gradle}/bin/gradle distZip"
	sh "find"

	archive "build/outputs/apk/*.apk"
}

