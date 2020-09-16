#!/usr/bin/env groovy
 
def generateRandom() {
    return org.apache.commons.lang.RandomStringUtils.randomAlphabetic(6).toLowerCase()
}
 
def getOSParam() {
  osAuthUrl = sh(returnStdout: true, script: "cat clouds.yaml | grep auth_url | cut -d':' -f2-").trim()
    osProjectName = sh(returnStdout: true, script: "cat clouds.yaml | grep project_name | cut -d':' -f2-").trim()
    osUsername = sh(returnStdout: true, script: "cat clouds.yaml | grep username | cut -d':' -f2-").trim()
    osPassword = sh(returnStdout: true, script: "cat clouds.yaml | grep password | cut -d':' -f2-").trim()
    osUserDomainName = sh(returnStdout: true, script: "cat clouds.yaml | grep user_domain_name | cut -d':' -f2-").trim()
    osProjectDomainName = sh(returnStdout: true, script: "cat clouds.yaml | grep project_domain_name | cut -d':' -f2-").trim()
    osRegionName = sh(returnStdout: true, script: "cat clouds.yaml | grep region_name | cut -d':' -f2-").trim()
  return "--os-auth-url ${osAuthUrl} --os-project-name ${osProjectName} --os-username ${osUsername} --os-password ${osPassword} --os-user-domain-name ${osUserDomainName} --os-project-domain-name ${osProjectDomainName} --os-region-name ${osRegionName}"
}
 
def waitVolumeAvailable(String volName, String provider="taco-env") {
  for (i=1; i<20; i++) {
    t = 0
    volStatus = sh(returnStdout: true, script: "openstack volume list --os-cloud ${provider} | grep ${volName}").trim()
    println("${volStatus}")
    if (volStatus.contains("error")) {
      error("Volume is an error status. Job is cancelled.")
    }
    else if (volStatus.contains("ing")) {
      t = i * 5
      println("Volume is not ready yet. Waiting ${t} seconds.")
      sleep 5
    }
    else {
      return
    }
    if (i==19) {
      error("Making volumes isn't completed in ${t} seconds.")
    }
  }
}
 
def waitVMActive(String vmName, String provider="taco-env") {
  for (i=1; i<20; i++) {
    t = 0
    vmStatus = sh(returnStdout: true, script: "openstack server list --os-cloud ${provider} |grep ${vmName}").trim()
    println("${vmStatus}")
    if (vmStatus.contains("ERROR")) {
      error("VM is an error status. Job is cancelled.")
    }
    else if (vmStatus.contains("BUILD")) {
      t = i * 5
      println("VM is not ready yet. Waiting ${t} seconds.")
      sleep 5
    }
    else {
      return
    }
    if (i==19) {
      error("Making vms isn't completed in ${t} seconds.")
    }
  }
}
 
 
def call(String namePrefix, String image="centos7", String flavor="m1.xlarge", Integer cnt=1, List volSize = [], String userData = "", Map<String,String> configDriveFiles=null, String securityGroup = "default", String availabilityZone = "nova", boolean online=false, boolean deleteBdm=true, Map<String,String> networks, String provider="taco-env") {
  // fetchCloudsConf()
  /* clouds.yaml 파일을 미리 준비 *
  *                           *
  ****************************/
 
  boolean bySnapshot = false
    if(image=="centos7") {
      // imageName = "CentOS-7-x86_64-2003.raw"
      imageName = "centos7"
    } else if (image=="centos8") {
      // imageName = "Centos8.0.1905-dev"
      imageName = "centos8"
    } else if (image=="ubuntu") {
      // imageName = "Ubuntu-18.04-Bionic.raw"
      imageName = "ubuntu"
    } else {
      imageName = image
    bySnapshot = true
    }
    name = namePrefix + "-" + generateRandom()
 
 
  if (userData != "")
      userData = "--user-data ${userData}"
 
  confDriveParam = ''
  if (configDriveFiles) {
    confDriveParam = "--config-drive true"
    configDriveFiles.each { path, localFile ->
      confDriveParam = confDriveParam + " --file ${path}=${localFile}"
    }
  }
 
  if (deleteBdm==true) {
    bdmShutdown = "remove"
  } else {
    bdmShutdown = "preserve"
  }
 
  flavorUuid = sh(returnStdout: true,
        script: "openstack flavor list --os-cloud ${provider} | grep ${flavor} | awk '{print \$2}'").trim()
 
  firstNetUuid = sh(returnStdout: true,
            script: "openstack network list --os-cloud ${provider} | grep ${networks.mgmt} | awk '{print \$2}'").trim()
  println("firstNetUuid: ${firstNetUuid}.")
 
  secondNetUuid = sh(returnStdout: true,
            script: "openstack network list --os-cloud ${provider} | grep ${networks.flat} | awk '{print \$2}'").trim()
  println("secondNetUuid: ${secondNetUuid}.")
 
  thirdNetUuid = sh(returnStdout: true,
            script: "openstack network list --os-cloud ${provider} | grep ${networks.vxlan} | awk '{print \$2}'").trim()
  println("thirdNetUuid: ${thirdNetUuid}.")
 
  println("Creating ${cnt} VMs on OpenStack cluster...")
 
  vmName = name
 
  imageUUid = imageName
 
  if (bySnapshot == false ) {
    imageUuid = sh(returnStdout: true,
        script: "openstack image list --os-cloud ${provider} | grep ${imageName} | awk '{print \$2}'").trim()
    println("imageUuid: ${imageUuid}")
  }
 
  for (num=1; num <= cnt; num++) {
    if (cnt != 1)
      vmName = name + "-" + num.toString()
 
    if (bySnapshot == false ) {
 
 
      rootVolumeUuid = sh(returnStdout: true,
      script: "openstack volume create --os-cloud ${provider} --image ${imageUuid} --bootable --type rbd2 --size 160 -f value -c id ${vmName}").trim()
      println("rootVolumeUuid: ${rootVolumeUuid}")
 
      bdm = "--block-device source=volume,id=${rootVolumeUuid},dest=volume,size=160,shutdown=${bdmShutdown},bootindex=0"
      volSize.eachWithIndex { it, index ->
        volName = vmName + "-vol-" + (index+1).toString()
 
        println("Creating volume ${volName}...")
        volUuid = sh(returnStdout: true,script: "openstack volume create --os-cloud ${provider} --type rbd2 --size ${it} -f value -c id ${volName}").trim()
 
        bootindex = index+1
        bdm += " --block-device source=volume,id=${volUuid},dest=volume,size=${it},shutdown=${bdmShutdown},bootindex=${bootindex}"
      }
 
      waitVolumeAvailable(vmName, ${provider})
    } else {
      snapshotUuid = sh(returnStdout: true,
        script: "openstack volume snapshot list --os-cloud ${provider} | grep ${imageName} | awk '{print \$2}' | head -1").trim()
      println("snapshotUuid: ${snapshotUuid}")
      bdm = "--block-device source=snapshot,id=${snapshotUuid},dest=volume,size=160,shutdown=${bdmShutdown},bootindex=0"
    }
 
 
    osParam = getOSParam()
    sh "nova ${osParam} boot ${bdm} --flavor ${flavorUuid} --nic net-id=${firstNetUuid} --nic net-id=${secondNetUuid} --nic net-id=${thirdNetUuid} --key-name jenkins ${vmName} --security-group ${securityGroup} --availability-zone ${availabilityZone} ${userData} ${confDriveParam}"
  }
  waitVMActive(name)
  return name
}
