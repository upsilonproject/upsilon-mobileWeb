#!groovy                                                                           
                                                                                   
properties(                                                                        
    [                                                                              
        [                                                                          
            $class: 'jenkins.model.BuildDiscarderProperty', strategy: [$class: 'LogRotator', numToKeepStr: '10', artifactNumToKeepStr: '10'],
            $class: 'CopyArtifactPermissionProperty', projectNames: '*'            
        ]                                                                          
    ]                                                                              
)                                                                                  

def prepareEnv() {
    unstash 'binaries'                                                             
                                                                                   
    env.WORKSPACE = pwd()                                                          
                                                                                   
    sh "find ${env.WORKSPACE}"                                                     
                                                                                   
    sh 'mkdir -p SPECS SOURCES'                                                    
    sh "cp build/distributions/*.zip SOURCES/upsilon-mobileWeb.zip"                                  
}
                                                                                   
def buildRpm(dist) {                                                               
    deleteDir()                                                                    

	prepareEnv()
                                                                                                                                                                      
    sh 'unzip -jo SOURCES/upsilon-mobileWeb.zip "upsilon-mobileWeb-*/var/pkg/upsilon-mobileWeb.spec" "upsilon-mobileWeb-*/.upsilon-mobileWeb.rpmmacro" -d SPECS/'
    sh "find ${env.WORKSPACE}"                                                     
                                                                                   
    sh "rpmbuild -ba SPECS/upsilon-mobileWeb.spec --define '_topdir ${env.WORKSPACE}' --define 'dist ${dist}'"
                                                                                   
    archive 'RPMS/noarch/*.rpm'                                                    
}                    

def buildDeb(dist) {
	deleteDir()
	
	prepareEnv()
	
	sh 'unzip -jo SOURCES/upsilon-mobileWeb.zip "upsilon-mobileWeb-*/var/pkg/deb/" -d . '
    sh "find ${env.WORKSPACE}"                                                     

	sh "cd /var/pkg/deb/; dpkg-buildpackage"


                                                                                   
}
                                                                                   
node {
	stage "Prep"

	deleteDir()
	def gradle = tool 'gradle'

	checkout scm

	stage "Compile"
	sh "${gradle}/bin/gradle assemble"
	sh "${gradle}/bin/gradle distZip"
	sh "find"

	stash includes:"build/distributions/*.zip", name: "binaries"
}

node {
	stage "Smoke"
	echo "Smokin' :)"
}

stage "Package"

node {                                                                             
    buildRpm("el7")                                                                
}                                                                                  
                                                                                   
node {                                                                             
    buildRpm("el6")                                                                
}                                                                                  
                                                                                   
node {                                                                             
    buildRpm("fc24")                                                               
}

node {
//	buildDeb("ubuntu-16.4")
}


