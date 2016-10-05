#!groovy                                                                           
                                                                                   
properties(                                                                        
    [                                                                              
        [                                                                          
            $class: 'jenkins.model.BuildDiscarderProperty', strategy: [$class: 'LogRotator', numToKeepStr: '10', artifactNumToKeepStr: '10'],
            $class: 'CopyArtifactPermissionProperty', projectNames: '*'            
        ]                                                                          
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

	archive "build/outputs/apk/*-release.apk"
}

