#!/usr/bin/env groovy
g
def call(String namePrefix, String, networkname, String provider) {
  println("Start getting Openstack Network informations...")
  fetchCloudsConf()
  ipaddr = sh(returnStdout: true,
        script: "openstack server show ${namePrefix} --os-cloud ${provider} -f json | jq '.addresses'").trim()
  println("Getting Openstack VM's IP Addresses has been finished!")
  return ipaddr
}