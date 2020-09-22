#!/usr/bin/env groovy
g
def call(String namePrefix, String networkName, String provider) {
  println("Start getting Openstack Network informations...")
  fetchCloudsConf()
  ipaddr = sh(returnStdout: true,
        script: "openstack server show ${namePrefix} --os-cloud ${provider} -f json | jq '.addresses' | awk -F '[;=]' 'for (i=1; i<=NF; i++) print \$i' | grep -A1 $networkName").trim()
  println("Getting Openstack VM's IP Addresses has been finished!")
  return ipaddr
}
